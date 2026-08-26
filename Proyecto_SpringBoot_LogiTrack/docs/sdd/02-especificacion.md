# SDD - 02. Especificación Técnica de Requisitos: LogiTrack IQ

> **Proyecto:** LogiTrack IQ - Torre de Control e Integración de Agentes IA  
> **Documento:** Especificación Técnica de Requisitos (SDD 02)  
> **Versión:** 1.0  
> **Estado:** Borrador / Propuesto  

---

## 1. Introducción

El presente documento detalla la especificación funcional y técnica para la implementación de **LogiTrack IQ**. Describe las entidades de datos, lógica de negocio, modelo de seguridad, especificación del Servidor MCP (Model Context Protocol), integración con n8n y los requisitos del Dashboard Web Frontend.

---

## 2. Requisitos Funcionales (RF)

### RF-01: Extensión del Modelo de Datos
El sistema debe incorporar tres nuevas entidades principales en PostgreSQL:

1. **`Proveedor` (`suppliers`):**
   - Campos: `id` (PK), `nombre`, `nit_rut`, `contacto_nombre`, `email`, `telefono`, `direccion`, `activo` (Boolean).
2. **`OrdenCompra` (`purchase_orders`) y `OrdenCompraDetalle` (`purchase_order_details`):**
   - Campos `OrdenCompra`: `id` (PK), `numero_orden` (único, ej. `PO-2026-0001`), `proveedor_id` (FK), `bodega_destino_id` (FK), `estado` (Enum: `BORRADOR`, `APROBADA`, `RECIBIDA`, `CANCELADA`), `fecha_creacion`, `fecha_aprobacion`, `fecha_recepcion`, `total_estimado`, `creado_por` (FK Usuario/Agente).
   - Campos `OrdenCompraDetalle`: `id` (PK), `orden_compra_id` (FK), `producto_id` (FK), `cantidad_solicitada`, `precio_unitario`, `subtotal`.
3. **`ResumenPanel` (`dashboard_summaries`):**
   - Campos: `id` (PK), `fecha_generacion` (LocalDateTime), `resumen_ejecutivo` (Text), `alertas_criticas` (Text), `recomendaciones_agente` (Text), `publicado_por` (FK Usuario/Agente).

---

### RF-02: Cálculo de Indicadores Key Performance Indicators (KPIs)
El backend debe proveer endpoints para calcular y servir los siguientes indicadores fijos:
* **Stock Real Consolidado:** Suma del inventario actual por producto a través de todas las bodegas (`InventarioBodega`).
* **Punto de Reorden:** Umbral mínimo configurado por producto.
* **Días de Cobertura:** `(Stock Actual / Consumo Promedio Diario)`.
* **Productos en Riesgo / Quiebre:**
  * *En Riesgo:* `Stock Actual <= Punto de Reorden`.
  * *En Quiebre:* `Stock Actual == 0`.
* **Ocupación de Bodegas:** Porcentaje de capacidad utilizada por cada bodega activa.

---

### RF-03: Ciclo de Vida de las Órdenes de Compra

Las órdenes de compra se gestionarán bajo una máquina de estados estricta:

```
    ┌──────────┐      Aprobar (Admin)      ┌──────────┐      Recibir (Admin)      ┌──────────┐
    │ BORRADOR │ ─────────────────────────►│ APROBADA │ ─────────────────────────►│ RECIBIDA │
    └────┬─────┘                           └────┬─────┘                           └──────────┘
         │                                      │
         │ Cancelar (Admin)                     │ Cancelar (Admin)
         ▼                                      ▼
    ┌─────────────────────────────────────────────────┐
    │                   CANCELADA                     │
    └─────────────────────────────────────────────────┘
```

**Reglas de Transición y Negocio:**
* **`BORRADOR`:** Creada de forma automatizada por el `AGENTE` (vía MCP) o manualmente por un `ADMIN`. No afecta stock.
* **`APROBADA`:** Transición ejecutada únicamente por un `ADMIN`. Confirma la orden ante el proveedor.
* **`RECIBIDA`:** Ejecutada por un `ADMIN` al llegar la mercancía. **Genera automáticamente un Movimiento de Inventario de tipo `ENTRADA`** en la bodega de destino seleccionada y actualiza el stock correspondiente.
* **`CANCELADA`:** Anula la orden desde los estados `BORRADOR` o `APROBADA`.

---

### RF-04: Modelo de Seguridad y Rol `AGENTE`

Se crea el nuevo rol de usuario **`AGENTE`**:
* **Autenticación:** Vía token JWT generado en `/api/auth/login`.
* **Permisos Restringidos (`AGENTE`):**
  * `GET` en endpoints de consulta de inventarios, productos, bodegas y proveedores.
  * `POST /api/ordenes-compra` únicamente con estado inicial `BORRADOR`.
  * `POST /api/resumenes-panel` para publicar el informe diario.
  * **Prohibido:** No puede aprobar (`APROBADA`), recibir (`RECIBIDA`) ni cancelar (`CANCELADA`) órdenes. Tampoco puede modificar usuarios, ni crear o eliminar bodegas/productos.

---

### RF-05: Servidor MCP y Automatización n8n

1. **Servidor MCP Local (Model Context Protocol):**
   Desarrollo de un servidor MCP (vía stdio o HTTP/SSE) expuesto para consumo de Agentes de IA con **6 Herramientas (Tools)**:
   1. `get_inventory_status`: Consulta el nivel de stock global y filtrado por bodega.
   2. `get_low_stock_products`: Obtiene los productos en riesgo o quiebre de stock.
   3. `get_suppliers_by_product`: Consulta los proveedores disponibles para un producto específico.
   4. `create_draft_purchase_order`: Crea una nueva orden de compra en estado `BORRADOR`.
   5. `get_dashboard_kpis`: Obtiene las métricas clave consolidadas del panel.
   6. `publish_daily_summary`: Registra el resumen ejecutivo diario generado por el agente.

2. **Flujo de Automatización n8n (6:00 AM):**
   * Programado diariamente a las 6:00 AM.
   * Ejecuta el agente IA que invoca las herramientas MCP para analizar riesgos de stock.
   * Genera órdenes de compra preventivas en estado `BORRADOR`.
   * Publica el resumen consolidado del día en el sistema.

---

### RF-06: Dashboard Web Frontend y Generación de Reportes PDF

1. **Dashboard Interactivo:**
   * Mapeo y visualización del resumen diario, alertas de quiebre y KPIs clave.
   * Módulo de gestión de Órdenes de Compra con acciones directas para aprobar y recibir mercancía.
2. **Generación de PDFs:**
   * Exportación de la Orden de Compra en formato PDF.
   * **Marca de agua diagonal:** Si la orden se encuentra en estado `BORRADOR`, el PDF debe incluir de manera prominente la leyenda visual en diagonal: **"BORRADOR - NO OFICIAL"**.

---

## 3. Requisitos No Funcionales (RNF)

* **RNF-01 (Seguridad y Aislamiento):** Las credenciales del rol `AGENTE` deben ser almacenadas en variables de entorno seguras. Las peticiones MCP deberán autenticarse contra el backend vía JWT.
* **RNF-02 (Integridad Transaccional):** La recepción de una orden (`RECIBIDA`) y el incremento de stock correspondiente mediante un movimiento de `ENTRADA` deben ejecutarse dentro de una misma transacción atómica (`@Transactional`).
* **RNF-03 (Desempeño):** El cálculo de los KPIs y la consulta de herramientas MCP deben ejecutarse con un tiempo de respuesta inferior a 500 ms.
* **RNF-04 (Compatibilidad):** Mantenimiento de compatibilidad con PostgreSQL local y la estructura Maven/Spring Boot existente.
