# SDD - 03. Diseño de Arquitectura y Automatización: LogiTrack IQ

> **Proyecto:** LogiTrack IQ - Torre de Control e Integración MCP/n8n  
> **Documento:** Diseño de Arquitectura y Componentes (SDD 03)  
> **Versión:** 1.0  
> **Estado:** Borrador / Propuesto  

---

## 1. Arquitectura General del Sistema

La solución **LogiTrack IQ** expande el ecosistema Spring Boot existente integrando un **Servidor MCP (Model Context Protocol)** y un orquestador de flujos automatizados con **n8n** y modelos LLM.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          ORQUESTADOR Y AGENTE IA                                │
│                                                                                 │
│   ┌─────────────────────┐      Cron 6:00 AM     ┌──────────────────────────┐   │
│   │   n8n Workflow      │ ─────────────────────►│  Agente IA (LLM Agent)   │   │
│   │  (Trigger Diario)   │                       └────────────┬─────────────┘   │
│   └─────────────────────┘                                    │                 │
└──────────────────────────────────────────────────────────────┼──────────────────┘
                                                               │ Protocolo MCP
                                                               │ (stdio / SSE)
                                                               ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            SERVIDOR MCP LOGITRACK                               │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │  MCP Server Tools:                                                        │  │
│  │  1. get_inventory_status          4. create_draft_purchase_order          │  │
│  │  2. get_low_stock_products        5. get_dashboard_kpis                  │  │
│  │  3. get_suppliers_by_product      6. publish_daily_summary                │  │
│  └─────────────────────────────────────┬─────────────────────────────────────┘  │
└────────────────────────────────────────┼────────────────────────────────────────┘
                                         │ REST API (Bearer JWT - ROL AGENTE)
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        BACKEND SPRING BOOT (LOGITRACK)                          │
│                                                                                 │
│  ┌───────────────────┐    ┌────────────────────┐    ┌────────────────────────┐  │
│  │ Security (JWT)    │───►│ Controller REST    │───►│ Services & Business    │  │
│  │ (Rol AGENTE/ADMIN)│    │ /api/ordenes-...   │    │ (@Transactional)       │  │
│  └───────────────────┘    └────────────────────┘    └────────────┬───────────┘  │
│                                                                  │              │
│                                                                  ▼              │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ Base de Datos PostgreSQL (Tablas: proveedores, purchase_orders, etc.)     │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                         ▲
                                         │ REST API (Bearer JWT - ROL ADMIN)
┌────────────────────────────────────────┴────────────────────────────────────────┐
│                         FRONTEND DASHBOARD (HTML/JS)                             │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │ - Panel de Torre de Control (KPIs + Resumen Diario Publicado)             │  │
│  │ - Aprobación / Recepción de Órdenes de Compra                             │  │
│  │ - Generación de PDFs con marca de agua "BORRADOR - NO OFICIAL"            │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Especificación del Servidor MCP (Model Context Protocol)

El Servidor MCP actúa como puente entre el flujo automatizado en n8n y la API REST del backend de LogiTrack. Cada llamada de herramienta autentica la solicitud con las credenciales del rol `AGENTE`.

### 2.1 Herramientas Disponibles (Tools)

#### 1. `get_inventory_status`
* **Descripción:** Retorna el inventario actual consolida por producto y bodega.
* **Parámetros:**
  * `bodegaId` (Long, Opcional): ID de bodega para filtrar.
* **Respuesta:** JSON con lista de productos, stock disponible, capacidad ocupada y estado.

#### 2. `get_low_stock_products`
* **Descripción:** Retorna todos los productos cuyo stock actual sea igual o menor al punto de reorden.
* **Parámetros:**
  * `soloQuiebre` (Boolean, Opcional): Si es `true`, filtra solo stock == 0.
* **Respuesta:** Arreglo de productos con `id`, `nombre`, `stockActual`, `puntoReorden`, `diasCobertura`.

#### 3. `get_suppliers_by_product`
* **Descripción:** Retorna la lista de proveedores activos capacitados para suministrar un producto determinado.
* **Parámetros:**
  * `productoId` (Long, Requerido): Identificador único del producto.
* **Respuesta:** Arreglo de proveedores con `id`, `nombre`, `nit`, `email`, `tiempoEntregaDias`.

#### 4. `create_draft_purchase_order`
* **Descripción:** Crea una nueva orden de compra en estado `BORRADOR` con sus líneas de detalle.
* **Parámetros:**
  * `proveedorId` (Long, Requerido): ID del proveedor.
  * `bodegaDestinoId` (Long, Requerido): ID de la bodega donde se recibirá el stock.
  * `detalles` (Array, Requerido): Lista de objetos `{ productoId, cantidad, precioUnitario }`.
* **Respuesta:** Objeto de la orden creada con `id`, `numeroOrden`, `estado` (`BORRADOR`) y `totalEstimado`.

#### 5. `get_dashboard_kpis`
* **Descripción:** Obtiene los indicadores consolidados para alimentar el panel de control.
* **Parámetros:** Ninguno.
* **Respuesta:** Objeto con `totalProductos`, `productosEnRiesgo`, `productosEnQuiebre`, `valorTotalInventario`, `ocupacionPromedioBodegas`.

#### 6. `publish_daily_summary`
* **Descripción:** Registra el informe diario elaborado por el Agente de IA para ser visualizado en la interfaz.
* **Parámetros:**
  * `resumenEjecutivo` (String, Requerido): Texto sintético del estado operativo.
  * `alertasCriticas` (String, Requerido): Detalle de quiebres e imprevistos.
  * `recomendacionesAgente` (String, Requerido): Acciones sugeridas para el administrador.
* **Respuesta:** Objeto `ResumenPanel` almacenado con ID y fecha de publicación.

---

## 3. Diseño del Flujo de Automatización en n8n

El proceso de automatización diario se ejecuta mediante el siguiente workflow en n8n:

```
[Cron Trigger (6:00 AM)]
        │
        ▼
[Nodo HTTP: Autenticación JWT - Rol AGENTE]
        │ (Obtiene Bearer Token)
        ▼
[Nodo MCP Tool: get_low_stock_products]
        │
        ▼
[Nodo IF: ¿Existen productos en riesgo?]
       ├── SÍ ──► [Nodo MCP Tool: get_suppliers_by_product]
       │                 │
       │                 ▼
       │          [Nodo Agent IA (LLM)]: Calcula cantidades a pedir y selecciona proveedor
       │                 │
       │                 ▼
       │          [Nodo MCP Tool: create_draft_purchase_order] (Estado BORRADOR)
       │                 │
       └─────────────────┴──► [Nodo Agent IA (LLM)]: Redacta Resumen Ejecutivo y Alertas
                                 │
                                 ▼
                          [Nodo MCP Tool: publish_daily_summary]
                                 │
                                 ▼
                          [Notificación opcional (Email/Slack/Webhook)]
```

---

## 4. Diseño del Modelo de Datos (Extensión ER)

```
┌─────────────────────────────────┐           ┌─────────────────────────────────┐
│           Proveedor             │           │           OrdenCompra           │
├─────────────────────────────────┤           ├─────────────────────────────────┤
│ PK  id: Long                    │ 1       * │ PK  id: Long                    │
│     nombre: String              │◄──────────┤ FK  proveedor_id: Long        │
│     nit_rut: String             │           │ FK  bodega_destino_id: Long     │
│     contacto_nombre: String     │           │     numero_orden: String (UQ)   │
│     email: String               │           │     estado: EstadoOrden (Enum)  │
│     telefono: String            │           │     fecha_creacion: DateTime    │
│     activo: Boolean             │           │     fecha_aprobacion: DateTime  │
└─────────────────────────────────┘           │     fecha_recepcion: DateTime   │
                                              │     total_estimado: BigDecimal  │
                                              │ FK  creado_por: Long (Usuario)  │
                                              └────────────────┬────────────────┘
                                                               │ 1
                                                               │
                                                               │ *
                                              ┌────────────────▼────────────────┐
                                              │       OrdenCompraDetalle        │
                                              ├─────────────────────────────────┤
                                              │ PK  id: Long                    │
                                              │ FK  orden_compra_id: Long       │
                                              │ FK  producto_id: Long           │
                                              │     cantidad_solicitada: Integer│
                                              │     precio_unitario: BigDecimal │
                                              │     subtotal: BigDecimal        │
                                              └─────────────────────────────────┘

┌─────────────────────────────────┐
│          ResumenPanel           │
├─────────────────────────────────┤
│ PK  id: Long                    │
│     fecha_generacion: DateTime  │
│     resumen_ejecutivo: Text     │
│     alertas_criticas: Text      │
│     recomendaciones_agente: Text│
│ FK  publicado_por: Long         │
└─────────────────────────────────┘
```

---

## 5. Transición Transaccional de Recepción de Órdenes

Cuando el Administrador hace clic en **"Recibir Orden"** desde el Dashboard, el Backend Spring Boot ejecuta la siguiente lógica atómica dentro de `@Transactional`:

```
1. Validar que la OrdenCompra exista y su estado actual sea 'APROBADA'.
2. Cambiar estado de OrdenCompra a 'RECIBIDA' y setear fecha_recepcion = LocalDateTime.now().
3. Crear registro MovimientoInventario:
   - tipoMovimiento = ENTRADA
   - bodegaDestino = orden.getBodegaDestino()
   - usuario = usuarioAutenticado (Admin)
   - observaciones = "Entrada automática por recepción de Orden " + numeroOrden
4. Por cada detalle en OrdenCompraDetalle:
   - Crear MovimientoDetalle(movimiento, detalle.getProducto(), detalle.getCantidadSolicitada()).
   - Incrementar stock en InventarioBodega(bodegaDestino, producto) por la cantidad recibida.
   - Actualizar stock global denormalizado en entidad Producto.
5. Guardar entidades y hacer Commit de la Transacción.
```

---

## 6. Diseño Frontend y Generación de PDF

### 6.1 Panel de Torre de Control
* **Header / Secciones:**
  1. **Tarjetas KPI:** Stock global, productos en riesgo, productos en quiebre, órdenes en borrador.
  2. **Bloque Resumen Inteligente:** Muestra el último `ResumenPanel` publicado por el Agente n8n.
  3. **Tabla de Órdenes de Compra:** Listado con filtros por estado (`BORRADOR`, `APROBADA`, `RECIBIDA`, `CANCELADA`) y botones de acción (`Aprobar`, `Recibir`, `Descargar PDF`).

### 6.2 Generación de PDF con Marca de Agua
Se integrará una librería JS como `pdfmake` o `html2pdf.js` en el cliente web:
* Si `orden.estado == 'BORRADOR'`:
  * Renderizar texto en diagonal rotado -45° centrado en la página: **"BORRADOR - NO OFICIAL"** con opacidad del 20% y color rojo.
* Si `orden.estado == 'APROBADA'` o `'RECIBIDA'`:
  * Renderizar documento oficial sin la marca de agua.
