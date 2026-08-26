# SDD - 01. Propuesta de Solución: LogiTrack IQ

Este documento define la justificación, objetivos y alcance de la torre de control de inventarios automatizada **LogiTrack IQ**.

## 1. Declaración del Problema
Actualmente, **LogiTrack S.A.** opera un backend en Spring Boot que registra movimientos de inventario de manera manual. Sin embargo, carece de una herramienta centralizada que permita:
* Monitorear el inventario en tiempo real.
* Identificar automáticamente productos en riesgo de desabastecimiento.
* Preparar órdenes de compra de forma proactiva.
* Visualizar el estado diario en un panel unificado para toma de decisiones.

## 2. Objetivo del Proyecto
Construir una extensión del backend existente de LogiTrack que complete el flujo automatizado: detectar productos con bajo stock, proponer compras mediante órdenes automáticas (en borrador) usando un agente inteligente de IA a través del protocolo **MCP** en un flujo de **n8n**, y permitir su aprobación y recepción transaccional de forma segura por parte de un administrador en un dashboard web frontend.

## 3. Alcance del Proyecto (En Alcance)
* **Modelo de Datos:** Implementación de entidades de base de datos para `Proveedor`, `OrdenCompra`, y `ResumenPanel`.
* **Cálculo de Indicadores fijos:** Stock real consolidado a partir de movimientos, punto de reorden, días de cobertura, productos en riesgo, productos en quiebre, y ocupación de bodegas.
* **Ciclo de Vida de las Órdenes:** Estados `BORRADOR`, `APROBADA`, `RECIBIDA` y `CANCELADA` con control estricto de transiciones.
* **Seguridad:** Inclusión del rol `AGENTE` con acceso limitado (solo consulta de inventarios, creación de órdenes en `BORRADOR` y publicación de resúmenes).
* **Integración MCP/n8n:** Desarrollo de un servidor MCP local con 6 herramientas específicas para el agente y automatización diaria mediante n8n a las 6:00 AM.
* **Visualización y Reportes:** Creación de un Dashboard HTML/CSS/JS que muestre resúmenes de datos y genere PDFs interactivos de órdenes con marca de agua diagonal de `BORRADOR` cuando aplique.

## 4. Fuera de Alcance (Out of Scope)
* Reemplazar o reescribir funcionalidades de auditoría o autenticación existentes del backend anterior.
* Validación semántica o análisis por lenguaje natural de los textos del resumen diario publicado por n8n.
* Uso de bases de datos externas en la nube (se usará PostgreSQL en entorno local).
* Adaptación del frontend para dispositivos móviles o inclusión de animaciones avanzadas.
