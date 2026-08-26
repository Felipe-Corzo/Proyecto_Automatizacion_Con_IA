# SDD - 04. Plan de Micro-Tareas e Implementación: LogiTrack IQ

> **Proyecto:** LogiTrack IQ - Torre de Control e Integración MCP/n8n  
> **Documento:** Plan de Micro-Tareas para Desarrollo (SDD 04)  
> **Versión:** 1.0  
> **Estado:** Aprobado  

---

## 1. Resumen de la Estrategia de Implementación

El desarrollo se ejecutará de forma secuencial en **5 Fases**, garantizando que las dependencias base (modelo de datos y seguridad) estén totalmente operativas antes de integrar el Servidor MCP, la automatización en n8n y las actualizaciones en el Frontend.

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     FASE 1      │ ──► │     FASE 2      │ ──► │     FASE 3      │ ──► │     FASE 4      │ ──► │     FASE 5      │
│ Base de Datos & │     │ Lógica Negocio  │     │  Servidor MCP   │     │  Flujo n8n &    │     │  Frontend SPA & │
│ Entidades JPA   │     │ & Endpoints REST│     │    (6 Tools)    │     │  Orquestación   │     │   Reportes PDF  │
└─────────────────┘     └─────────────────┘     └─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## 2. Desglose de Micro-Tareas por Fase

### Fase 1: Modelo de Datos, Persistencia y Seguridad
**Objetivo:** Crear las nuevas tablas, entidades JPA, repositorios y configurar el rol `AGENTE` en Spring Security.

* [ ] **T-1.1:** Actualizar `schema.sql` y `data.sql` con la definición DDL para las tablas `suppliers`, `purchase_orders`, `purchase_order_details` y `dashboard_summaries`.
* [ ] **T-1.2:** Crear enums Java: `EstadoOrden` (`BORRADOR`, `APROBADA`, `RECIBIDA`, `CANCELADA`) y actualizar enum `Rol` agregando `AGENTE`.
* [ ] **T-1.3:** Crear entidades JPA `@Entity`: `Proveedor`, `OrdenCompra`, `OrdenCompraDetalle` y `ResumenPanel`.
* [ ] **T-1.4:** Crear repositorios de Spring Data JPA: `ProveedorRepository`, `OrdenCompraRepository`, `OrdenCompraDetalleRepository` y `ResumenPanelRepository`.
* [ ] **T-1.5:** Actualizar `SecurityConfig` para soportar permisos del rol `AGENTE`:
  * Permitir `GET` en inventarios, productos, bodegas y proveedores.
  * Permitir `POST /api/ordenes-compra` (solo en estado `BORRADOR`).
  * Permitir `POST /api/resumenes-panel`.
  * Restringir aprobación/recepción/cancelación exclusivamente a `ADMIN`.
* [ ] **T-1.6:** Registrar un usuario inicial con rol `AGENTE` en `data.sql` para consumo del servidor MCP.

---

### Fase 2: Servicios de Negocio, Cálculo de KPIs y Endpoints REST
**Objetivo:** Implementar las reglas de negocio, cálculo de indicadores y la transacción de recepción de órdenes.

* [ ] **T-2.1:** Crear DTOs: `ProveedorDTO`, `OrdenCompraRequestDTO`, `OrdenCompraResponseDTO`, `KpiDashboardDTO` y `ResumenPanelDTO`.
* [ ] **T-2.2:** Implementar `ProveedorService` y `ProveedorController` para la gestión CRUD de proveedores.
* [ ] **T-2.3:** Implementar servicio de cálculo de KPIs `KpiService`:
  * Cálculo de stock consolidado, punto de reorden, días de cobertura, productos en riesgo y porcentaje de ocupación de bodegas.
* [ ] **T-2.4:** Implementar `OrdenCompraService` con gestión del ciclo de vida de órdenes:
  * Método `crearBorrador(...)`.
  * Método `aprobarOrden(id)`.
  * Método `cancelarOrden(id)`.
* [ ] **T-2.5:** Implementar método transaccional `@Transactional` `recibirOrden(id)` en `OrdenCompraService`:
  * Cambio de estado a `RECIBIDA`.
  * Generación de `MovimientoInventario` de tipo `ENTRADA`.
  * Actualización de stock en `InventarioBodega` y en la entidad `Producto`.
* [ ] **T-2.6:** Crear `OrdenCompraController`, `KpiController` y `ResumenPanelController` exponiendo los endpoints REST correspondientes.

---

### Fase 3: Servidor MCP (Model Context Protocol)
**Objetivo:** Desarrollar el servidor MCP con las 6 herramientas para interacción con Agentes de IA.

* [ ] **T-3.1:** Crear módulo/proyecto del Servidor MCP (Spring Boot AI / Node.js MCP SDK / Python FastMCP o Java MCP).
* [ ] **T-3.2:** Configurar cliente HTTP para autenticarse automáticamente contra el backend mediante JWT con credenciales del usuario `AGENTE`.
* [ ] **T-3.3:** Implementar Tool 1: `get_inventory_status`.
* [ ] **T-3.4:** Implementar Tool 2: `get_low_stock_products`.
* [ ] **T-3.5:** Implementar Tool 3: `get_suppliers_by_product`.
* [ ] **T-3.6:** Implementar Tool 4: `create_draft_purchase_order`.
* [ ] **T-3.7:** Implementar Tool 5: `get_dashboard_kpis`.
* [ ] **T-3.8:** Implementar Tool 6: `publish_daily_summary`.
* [ ] **T-3.9:** Probar las herramientas MCP mediante cliente MCP (Claude Desktop, Inspector MCP o Script de prueba).

---

### Fase 4: Flujo de Automatización en n8n
**Objetivo:** Orquestar el flujo diario de análisis y generación de órdenes preventivas a las 6:00 AM.

* [ ] **T-4.1:** Configurar nodo `Cron Trigger` en n8n para ejecutarse diariamente a las 6:00 AM.
* [ ] **T-4.2:** Integrar conector MCP en n8n para comunicarse con el Servidor MCP local.
* [ ] **T-4.3:** Diseñar prompt y nodo de Agente IA (OpenAI/Anthropic/Local LLM) en n8n para evaluar quiebres de stock y seleccionar proveedores.
* [ ] **T-4.4:** Configurar llamadas a la herramienta `create_draft_purchase_order` cuando detecte productos con `stock <= punto_reorden`.
* [ ] **T-4.5:** Configurar generación del informe ejecutivo y llamada a `publish_daily_summary`.
* [ ] **T-4.6:** Validar la ejecución completa del flujo automatizado simulando el horario del cron.

---

### Fase 5: Frontend Dashboard, Visualización y Reportes PDF
**Objetivo:** Desarrollar las vistas para la Torre de Control y la generación de PDFs con marca de agua.

* [ ] **T-5.1:** Crear vista HTML `dashboard-torre-control.html` con las tarjetas de KPIs y el bloque de Resumen Inteligente del día.
* [ ] **T-5.2:** Crear módulo de gestión de Órdenes de Compra en el frontend con listado y filtros por estado.
* [ ] **T-5.3:** Implementar botones de acción en la interfaz (`Aprobar Orden` y `Recibir Orden`) visibles únicamente para el rol `ADMIN`.
* [ ] **T-5.4:** Integrar librería JS de PDF (`pdfmake` o `jspdf`) para descargar la Orden de Compra.
* [ ] **T-5.5:** Implementar la marca de agua diagonal **"BORRADOR - NO OFICIAL"** en rojo con opacidad al generar el PDF de órdenes en estado `BORRADOR`.
* [ ] **T-5.6:** Realizar pruebas de integración de extremo a extremo (E2E) comprobando todo el flujo desde n8n hasta la recepción física del stock en la web.

---

## 3. Matriz de Entregables e Hitos

| Hito | Descripción | Criterio de Aceptación |
|---|---|---|
| **Hito 1** | Base de Datos & Backend Base | Tablas creadas, rol `AGENTE` operativo y endpoints REST probados con Postman. |
| **Hito 2** | Servidor MCP Operativo | Las 6 herramientas responden correctamente en formato JSON-Schema. |
| **Hito 3** | Automatización n8n Lista | n8n se ejecuta automáticamente y genera órdenes en `BORRADOR` y resumen diario. |
| **Hito 4** | Dashboard & PDF Final | Administrador puede visualizar KPIs, aprobar/recibir órdenes y exportar PDFs con marca de agua. |
