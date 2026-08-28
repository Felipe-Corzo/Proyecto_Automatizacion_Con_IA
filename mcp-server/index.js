require('dotenv').config();
const { Server } = require('@modelcontextprotocol/sdk/server/index.js');
const { StdioServerTransport } = require('@modelcontextprotocol/sdk/server/stdio.js');
const { SSEServerTransport } = require('@modelcontextprotocol/sdk/server/sse.js');
const {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} = require('@modelcontextprotocol/sdk/types.js');
const axios = require('axios');
const express = require('express');

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

function extractProductId(args) {
  const candidate = args.productoId
    ?? args.productId
    ?? args.producto_id
    ?? args.idProducto
    ?? args.id
    ?? args.producto?.id
    ?? args.product?.id;
  const productId = Number(candidate);
  return Number.isInteger(productId) && productId > 0 ? productId : null;
}

function createServer() {
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
              description: 'Identificador único del producto (requerido). Usa el campo productoId, no el nombre del producto.',
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
  const toolArgs = args || {};

  try {
    switch (name) {
      case 'get_inventory_status': {
        const endpoint = `${process.env.API_INVENTORY_ENDPOINT || '/api/inventarios'}/status`;
        const data = await apiGet(endpoint, { bodegaId: toolArgs.bodegaId });
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      case 'get_low_stock_products': {
        const endpoint = process.env.API_LOW_STOCK_ENDPOINT || '/api/productos/bajo-stock';
        const data = await apiGet(endpoint, { soloQuiebre: toolArgs.soloQuiebre });
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      case 'get_suppliers_by_product': {
        const productoId = extractProductId(toolArgs);
        if (productoId === null) {
          throw new Error('productoId es obligatorio y debe ser un número entero positivo.');
        }
        const endpoint = `${process.env.API_SUPPLIERS_ENDPOINT || '/api/proveedores'}/by-product/${productoId}`;
        const data = await apiGet(endpoint);
        return {
          content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
        };
      }

      case 'create_draft_purchase_order': {
        const endpoint = process.env.API_ORDERS_ENDPOINT || '/api/ordenes';
        if (!Number.isInteger(Number(toolArgs.proveedorId)) || Number(toolArgs.proveedorId) <= 0) {
          throw new Error('proveedorId es obligatorio y debe ser un número entero positivo.');
        }
        if (!Number.isInteger(Number(toolArgs.bodegaDestinoId)) || Number(toolArgs.bodegaDestinoId) <= 0) {
          throw new Error('bodegaDestinoId es obligatorio y debe ser un número entero positivo.');
        }
        if (!Array.isArray(toolArgs.detalles) || toolArgs.detalles.length === 0) {
          throw new Error('detalles es obligatorio y debe contener al menos una línea.');
        }
        const payload = {
          proveedorId: Number(toolArgs.proveedorId),
          bodegaDestinoId: Number(toolArgs.bodegaDestinoId),
          detalles: toolArgs.detalles,
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
          resumenEjecutivo: toolArgs.resumenEjecutivo,
          alertasCriticas: toolArgs.alertasCriticas,
          recomendacionesAgente: toolArgs.recomendacionesAgente,
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

  return server;
}

const server = createServer();

async function main() {
  console.error('[MCP] Iniciando servidor LogiTrack IQ MCP...');
  console.error(`[MCP] Backend objetivo: ${SPRING_BOOT_BASE_URL}`);

  const transportMode = (process.env.MCP_TRANSPORT || 'stdio').toLowerCase();

  if (transportMode === 'stdio') {
    const transport = new StdioServerTransport();
    await server.connect(transport);
    console.error('[MCP] Servidor MCP conectado y escuchando en stdio');
    return;
  }

  if (transportMode !== 'sse') {
    throw new Error(`Transporte MCP no soportado: ${transportMode}. Usa "stdio" o "sse".`);
  }

  const app = express();
  const port = Number(process.env.MCP_PORT || 3001);
  const ssePath = process.env.MCP_SSE_PATH || '/sse';
  const messagesPath = process.env.MCP_MESSAGES_PATH || '/messages';
  const sharedSecret = process.env.MCP_SHARED_SECRET;
  const sessions = new Map();

  const requireMcpAuth = (request, response, next) => {
    if (!sharedSecret) {
      response.status(503).json({ error: 'MCP_SHARED_SECRET no está configurado.' });
      return;
    }

    const authorization = request.get('authorization');
    if (authorization !== `Bearer ${sharedSecret}`) {
      response.status(401).json({ error: 'No autorizado.' });
      return;
    }

    next();
  };

  app.get('/health', (request, response) => {
    response.json({ status: 'ok', transport: 'sse' });
  });

  app.get(ssePath, requireMcpAuth, async (request, response) => {
    const transport = new SSEServerTransport(messagesPath, response);
    const sessionServer = createServer();
    transport.onclose = () => {
      sessions.delete(transport.sessionId);
    };

    try {
      await sessionServer.connect(transport);
      sessions.set(transport.sessionId, { server: sessionServer, transport });
    } catch (error) {
      console.error('[MCP] Error al abrir la conexión SSE:', error);
      if (!response.headersSent) {
        response.status(500).json({ error: 'No se pudo abrir la conexión MCP.' });
      }
    }
  });

  app.post(messagesPath, requireMcpAuth, async (request, response) => {
    const sessionId = request.query.sessionId;
    const session = sessions.get(sessionId);
    if (!session) {
      response.status(400).send('Sesión MCP inválida o expirada.');
      return;
    }

    try {
      await session.transport.handlePostMessage(request, response);
    } catch (error) {
      console.error('[MCP] Error al procesar mensaje SSE:', error);
      if (!response.headersSent) {
        response.status(500).json({ error: 'No se pudo procesar el mensaje MCP.' });
      }
    }
  });

  app.listen(port, '0.0.0.0', () => {
    console.error(`[MCP] Servidor SSE escuchando en http://0.0.0.0:${port}${ssePath}`);
    console.error(`[MCP] Endpoint de mensajes: ${messagesPath}`);
  });
}

main().catch((error) => {
  console.error('[MCP] Error fatal:', error);
  process.exit(1);
});