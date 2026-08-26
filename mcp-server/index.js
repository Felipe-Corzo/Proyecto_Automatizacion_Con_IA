require('dotenv').config();
const { Server } = require('@modelcontextprotocol/sdk/server/index.js');
const { StdioServerTransport } = require('@modelcontextprotocol/sdk/server/stdio.js');
const {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} = require('@modelcontextprotocol/sdk/types.js');
const axios = require('axios');

const SPRING_BOOT_BASE_URL = process.env.SPRING_BOOT_BASE_URL || 'http://localhost:8080';
const AGENT_USERNAME = process.env.AGENT_USERNAME || 'agente_automatizado';
const AGENT_PASSWORD = process.env.AGENT_PASSWORD || 'agente123';
const AUTH_LOGIN_ENDPOINT = process.env.AUTH_LOGIN_ENDPOINT || '/api/auth/login';

const apiClient = axios.create({
  baseURL: SPRING_BOOT_BASE_URL,
  timeout: parseInt(process.env.HTTP_TIMEOUT_MS) || 10000,
});

let authToken = null;

async function authenticate() {
  try {
    const response = await apiClient.post(AUTH_LOGIN_ENDPOINT, {
      username: AGENT_USERNAME,
      password: AGENT_PASSWORD,
    });
    authToken = response.data.token;
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${authToken}`;
    console.error(`[MCP] Autenticación exitosa para usuario: ${AGENT_USERNAME}`);
    return true;
  } catch (error) {
    console.error(`[MCP] Error de autenticación: ${error.message}`);
    if (error.response) {
      console.error(`[MCP] Status: ${error.response.status}, Data: ${JSON.stringify(error.response.data)}`);
    }
    return false;
  }
}

async function ensureAuthenticated() {
  if (!authToken) {
    const success = await authenticate();
    if (!success) throw new Error('No se pudo autenticar con el backend Spring Boot');
  }
  return true;
}

async function apiGet(endpoint, params = {}) {
  await ensureAuthenticated();
  try {
    const response = await apiClient.get(endpoint, { params });
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      authToken = null;
      delete apiClient.defaults.headers.common['Authorization'];
      await ensureAuthenticated();
      const response = await apiClient.get(endpoint, { params });
      return response.data;
    }
    throw error;
  }
}

async function apiPost(endpoint, data) {
  await ensureAuthenticated();
  try {
    const response = await apiClient.post(endpoint, data);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      authToken = null;
      delete apiClient.defaults.headers.common['Authorization'];
      await ensureAuthenticated();
      const response = await apiClient.post(endpoint, data);
      return response.data;
    }
    throw error;
  }
}

const server = new Server(
  {
    name: process.env.MCP_SERVER_NAME || 'logitrack-iq-mcp',
    version: process.env.MCP_SERVER_VERSION || '1.0.0',
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: 'get_inventory_status',
        description: 'Consulta el nivel de stock global consolidado y filtrado por bodega. Retorna lista de productos con stock actual, capacidad ocupada y estado.',
        inputSchema: {
          type: 'object',
          properties: {
            bodegaId: {
              type: 'number',
              description: 'ID de la bodega para filtrar (opcional). Si no se proporciona, retorna stock global consolidado.',
            },
          },
          required: [],
        },
      },
      {
        name: 'get_low_stock_products',
        description: 'Obtiene los productos en riesgo de desabastecimiento (stock <= punto de reorden) o en quiebre (stock == 0).',
        inputSchema: {
          type: 'object',
          properties: {
            soloQuiebre: {
              type: 'boolean',
              description: 'Si es true, filtra solo productos con stock igual a 0 (quiebre total). Por defecto false (incluye riesgo).',
            },
          },
          required: [],
        },
      },
      {
        name: 'get_suppliers_by_product',
        description: 'Consulta los proveedores activos capacitados para suministrar un producto específico.',
        inputSchema: {
          type: 'object',
          properties: {
            productoId: {
              type: 'number',
              description: 'Identificador único del producto (requerido).',
            },
          },
          required: ['productoId'],
        },
      },
      {
        name: 'create_draft_purchase_order',
        description: 'Crea una nueva orden de compra en estado BORRADOR con sus líneas de detalle. Solo rol AGENTE o ADMIN.',
        inputSchema: {
          type: 'object',
          properties: {
            proveedorId: {
              type: 'number',
              description: 'ID del proveedor (requerido).',
            },
            bodegaDestinoId: {
              type: 'number',
              description: 'ID de la bodega donde se recibirá el stock (requerido).',
            },
            detalles: {
              type: 'array',
              description: 'Lista de líneas de la orden: cada una con productoId, cantidad y precioUnitario.',
              items: {
                type: 'object',
                properties: {
                  productoId: { type: 'number' },
                  cantidad: { type: 'number' },
                  precioUnitario: { type: 'number' },
                },
                required: ['productoId', 'cantidad', 'precioUnitario'],
              },
              minItems: 1,
            },
          },
          required: ['proveedorId', 'bodegaDestinoId', 'detalles'],
        },
      },
      {
        name: 'get_dashboard_kpis',
        description: 'Obtiene los indicadores clave consolidados para el panel de control (KPIs): total productos, productos en riesgo, productos en quiebre, valor total inventario, ocupación promedio bodegas.',
        inputSchema: {
          type: 'object',
          properties: {},
          required: [],
        },
      },
      {
        name: 'publish_daily_summary',
        description: 'Registra el informe diario elaborado por el Agente de IA para ser visualizado en la interfaz (Torre de Control).',
        inputSchema: {
          type: 'object',
          properties: {
            resumenEjecutivo: {
              type: 'string',
              description: 'Texto sintético del estado operativo del día.',
            },
            alertasCriticas: {
              type: 'string',
              description: 'Detalle de quiebres, riesgos críticos e imprevistos detectados.',
            },
            recomendacionesAgente: {
              type: 'string',
              description: 'Acciones sugeridas para el administrador (aprobar órdenes, contactar proveedores, etc.).',
            },
          },
          required: ['resumenEjecutivo', 'alertasCriticas', 'recomendacionesAgente'],
        },
      },
    ],
  };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case 'get_inventory_status': {
        const endpoint = `${process.env.API_INVENTORY_ENDPOINT || '/api/inventarios'}/status`;
        const data = await apiGet(endpoint, { bodegaId: args.bodegaId });
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      case 'get_low_stock_products': {
        const endpoint = `${process.env.API_PRODUCTS_ENDPOINT || '/api/productos'}/low-stock`;
        const data = await apiGet(endpoint, { soloQuiebre: args.soloQuiebre });
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      case 'get_suppliers_by_product': {
        const endpoint = `${process.env.API_SUPPLIERS_ENDPOINT || '/api/proveedores'}/by-product/${args.productoId}`;
        const data = await apiGet(endpoint);
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      case 'create_draft_purchase_order': {
        const endpoint = process.env.API_ORDERS_ENDPOINT || '/api/ordenes';
        const payload = {
          proveedorId: args.proveedorId,
          bodegaDestinoId: args.bodegaDestinoId,
          detalles: args.detalles,
        };
        const data = await apiPost(endpoint, payload);
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      case 'get_dashboard_kpis': {
        const endpoint = process.env.API_KPIS_ENDPOINT || '/api/kpis';
        const data = await apiGet(endpoint);
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      case 'publish_daily_summary': {
        const endpoint = process.env.API_SUMMARIES_ENDPOINT || '/api/resumenes-panel';
        const payload = {
          resumenEjecutivo: args.resumenEjecutivo,
          alertasCriticas: args.alertasCriticas,
          recomendacionesAgente: args.recomendacionesAgente,
        };
        const data = await apiPost(endpoint, payload);
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      default:
        throw new Error(`Herramienta desconocida: ${name}`);
    }
  } catch (error) {
    const errorMessage = error.response
      ? `Error HTTP ${error.response.status}: ${JSON.stringify(error.response.data)}`
      : error.message;
    console.error(`[MCP] Error en herramienta ${name}: ${errorMessage}`);
    return {
      content: [{ type: 'text', text: `Error: ${errorMessage}` }],
      isError: true,
    };
  }
});

async function main() {
  console.error('[MCP] Iniciando servidor LogiTrack IQ MCP...');
  console.error(`[MCP] Backend objetivo: ${SPRING_BOOT_BASE_URL}`);

  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('[MCP] Servidor MCP conectado y escuchando en stdio');
}

main().catch((error) => {
  console.error('[MCP] Error fatal:', error);
  process.exit(1);
});