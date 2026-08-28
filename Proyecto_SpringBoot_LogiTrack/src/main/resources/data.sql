-- LogiTrack S.A. - Datos Iniciales de Prueba (PostgreSQL)
-- Tablas en schema proyecto, nombres exactos según entidades JPA

SET search_path TO proyecto;

-- Usuarios (Contraseña BCrypt: "admin123" para admin/jdoe/mgarcia/crodriguez/lhernandez, "agente123" para agente_automatizado)
INSERT INTO usuarios (id, username, email, password, rol) VALUES
(1, 'admin', 'admin@logitrac.com', '$2a$10$R5CO2cejdUE9FJOX8r2h9O4xJDtxNGuhNtRHnxac07qStUYvLJ14i', 'ADMIN'),
(2, 'jdoe', 'j.doe@logitrac.com', '$2a$10$R5CO2cejdUE9FJOX8r2h9O4xJDtxNGuhNtRHnxac07qStUYvLJ14i', 'EMPLEADO'),
(3, 'mgarcia', 'm.garcia@logitrac.com', '$2a$10$R5CO2cejdUE9FJOX8r2h9O4xJDtxNGuhNtRHnxac07qStUYvLJ14i', 'EMPLEADO'),
(4, 'agente_automatizado', 'agente@logitrack.com', '$2a$10$Tm5de5aPREvdctvDcsoDueBY9qxQLDRFwn9Y5vptY1Sfkiplj1rY6', 'AGENTE'),
(5, 'crodriguez', 'c.rodriguez@logitrac.com', '$2a$10$R5CO2cejdUE9FJOX8r2h9O4xJDtxNGuhNtRHnxac07qStUYvLJ14i', 'EMPLEADO'),
(6, 'lhernandez', 'l.hernandez@logitrac.com', '$2a$10$R5CO2cejdUE9FJOX8r2h9O4xJDtxNGuhNtRHnxac07qStUYvLJ14i', 'EMPLEADO')
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password, email = EXCLUDED.email, rol = EXCLUDED.rol;

-- Bodegas
INSERT INTO bodegas (id, nombre, ubicacion, capacidad, encargado_id) VALUES
(1, 'Bodega Central Bogota', 'Calle 26 # 68-10, Bogota', 50000, 1),
(2, 'Centro Distribucion Medellin', 'Carrera 48 # 10-45, Medellin', 35000, 2),
(3, 'Bodega Norte Cali', 'Avenida 6N # 22-00, Cali', 20000, 3),
(4, 'Bodega Sur Barranquilla', 'Calle 30 # 8-50, Barranquilla', 15000, 5),
(5, 'Bodega Occidente Pereira', 'Carrera 8 # 15-30, Pereira', 12000, 6)
ON CONFLICT (id) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    ubicacion = EXCLUDED.ubicacion,
    capacidad = EXCLUDED.capacidad,
    encargado_id = EXCLUDED.encargado_id;

-- Proveedores (tabla suppliers)
INSERT INTO suppliers (id, nombre, nit_rut, contacto_nombre, email, telefono, direccion, activo, dias_entrega) VALUES
(1, 'Proveedor General LogiTrack', '900123456-1', 'Carlos Mendez', 'c.mendez@proveedor.com', '3001234567', 'Bogota, Colombia', true, 5),
(2, 'TechSupply Colombia', '900234567-2', 'Ana Torres', 'a.torres@techsupply.co', '3102345678', 'Medellin, Colombia', true, 7),
(3, 'Global Hardware SAS', '900345678-3', 'Luis Gomez', 'l.gomez@globalhw.com', '3203456789', 'Cali, Colombia', true, 10),
(4, 'Office Solutions Ltda', '900456789-4', 'Maria Lopez', 'm.lopez@officesol.co', '3154567890', 'Barranquilla, Colombia', true, 3),
(5, 'Industrial Parts Inc', '900567890-5', 'Jorge Ruiz', 'j.ruiz@indparts.com', '3125678901', 'Pereira, Colombia', false, 15),
(6, 'Distribuidora Andina SA', '900678901-6', 'Patricia Vargas', 'p.vargas@distrandina.com', '3186789012', 'Bucaramanga, Colombia', true, 8),
(7, 'ElectroComponentes del Pacifico', '900789012-7', 'Roberto Silva', 'r.silva@electropacifico.com', '3147890123', 'Cartagena, Colombia', true, 12),
(8, 'Mundo Oficina Express', '900890123-8', 'Diana Herrera', 'd.herrera@mundooficina.co', '3118901234', 'Cucuta, Colombia', true, 4),
(9, 'Redes y Conectividad Total', '900901234-9', 'Fernando Castro', 'f.castro@redesytotal.com', '3199012345', 'Santa Marta, Colombia', true, 6),
(10, 'Suministros Industriales Unidos', '901012345-0', 'Gabriela Moreno', 'g.moreno@suminunidos.com', '3130123456', 'Ibague, Colombia', true, 9),
(11, 'TechZone Distribution', '901123456-1', 'Andres Felipe', 'a.felipe@techzone.co', '3161234567', 'Manizales, Colombia', true, 5),
(12, 'Hardware Solutions Premium', '901234567-2', 'Claudia Jimenez', 'c.jimenez@hardwaresol.com', '3172345678', 'Pasto, Colombia', false, 14),
(13, 'Ofitec Global SAS', '901345678-3', 'Ricardo Pardo', 'r.pardo@ofitecglobal.com', '3103456789', 'Neiva, Colombia', true, 7),
(14, 'Componentes Electronicos SA', '901456789-4', 'Sandra Milena', 's.milena@compelectronicos.co', '3124567890', 'Armenia, Colombia', true, 11),
(15, 'Logistica y Suministros 360', '901567890-5', 'Oscar Gutierrez', 'o.gutierrez@logisumin360.com', '3185678901', 'Villavicencio, Colombia', true, 8)
ON CONFLICT (id) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    nit_rut = EXCLUDED.nit_rut,
    contacto_nombre = EXCLUDED.contacto_nombre,
    email = EXCLUDED.email,
    telefono = EXCLUDED.telefono,
    direccion = EXCLUDED.direccion,
    activo = EXCLUDED.activo,
    dias_entrega = EXCLUDED.dias_entrega;

-- Productos (con proveedor principal)
INSERT INTO productos (id, nombre, categoria, stock, precio, proveedor_id) VALUES
(1, 'Laptop Lenovo ThinkPad T14', 'Electronica', 145, 1200.00, 1),
(2, 'Monitor Dell UltraSharp 27"', 'Perifericos', 8, 450.50, 2),
(3, 'Teclado Mecanico Logitech MX', 'Perifericos', 65, 120.00, 2),
(4, 'Silla Ergonomica Herman Miller', 'Mobiliario', 4, 950.00, 4),
(5, 'Disco Duro Externo SSD 2TB', 'Almacenamiento', 200, 180.00, 3),
(6, 'Mouse Inalambrico Logitech MX Master 3', 'Perifericos', 5, 130.00, 2),
(7, 'Impresora HP LaserJet Pro', 'Electronica', 12, 580.00, 1),
(8, 'Escritorio Ejecutivo Madera', 'Mobiliario', 6, 750.00, 4),
(9, 'Router Cisco Meraki MX64', 'Redes', 3, 1200.00, 3),
(10, 'Switch Cisco Catalyst 2960', 'Redes', 8, 890.00, 3),
(11, 'UPS APC Smart-UPS 1500VA', 'Energia', 15, 650.00, 1),
(12, 'Proyector Epson PowerLite', 'Electronica', 4, 980.00, 5),
(13, 'Silla Operativa Mesh', 'Mobiliario', 25, 280.00, 4),
(14, 'Monitor Curvo Samsung 32"', 'Perifericos', 7, 520.00, 2),
(15, 'Laptop Dell Latitude 5430', 'Electronica', 10, 1100.00, 1),
(16, 'Tablet iPad Pro 12.9"', 'Electronica', 2, 1400.00, 5),
(17, 'Disco Duro HDD 4TB', 'Almacenamiento', 30, 110.00, 3),
(18, 'Memoria RAM DDR5 32GB', 'Componentes', 18, 150.00, 3),
(19, 'Tarjeta Grafica RTX 4070', 'Componentes', 1, 800.00, 5),
(20, 'Fuente Poder 850W 80+ Gold', 'Componentes', 9, 140.00, 3)
ON CONFLICT (id) DO NOTHING;

-- Órdenes de Compra (varios estados)
INSERT INTO ordenes_compra (id, producto_id, proveedor_id, bodega_destino_id, cantidad,
                            precio_unitario, total, fecha_creacion, estado, creado_por) VALUES
(1, 1, 1, 1, 10, 1200.00, 12000.00, CURRENT_TIMESTAMP - INTERVAL '10 days', 'BORRADOR', 'admin'),
(2, 2, 2, 2, 5, 450.50, 2252.50, CURRENT_TIMESTAMP - INTERVAL '9 days', 'APROBADA', 'jdoe'),
(3, 3, 2, 1, 20, 120.00, 2400.00, CURRENT_TIMESTAMP - INTERVAL '8 days', 'RECIBIDA', 'mgarcia'),
(4, 4, 4, 3, 3, 950.00, 2850.00, CURRENT_TIMESTAMP - INTERVAL '7 days', 'BORRADOR', 'admin'),
(5, 5, 3, 2, 15, 180.00, 2700.00, CURRENT_TIMESTAMP - INTERVAL '6 days', 'APROBADA', 'crodriguez'),
(6, 6, 2, 1, 10, 130.00, 1300.00, CURRENT_TIMESTAMP - INTERVAL '5 days', 'RECIBIDA', 'lhernandez'),
(7, 7, 1, 4, 2, 580.00, 1160.00, CURRENT_TIMESTAMP - INTERVAL '4 days', 'BORRADOR', 'jdoe'),
(8, 8, 4, 5, 4, 750.00, 3000.00, CURRENT_TIMESTAMP - INTERVAL '3 days', 'CANCELADA', 'admin'),
(9, 9, 3, 1, 5, 1200.00, 6000.00, CURRENT_TIMESTAMP - INTERVAL '2 days', 'APROBADA', 'mgarcia'),
(10, 10, 3, 2, 3, 890.00, 2670.00, CURRENT_TIMESTAMP - INTERVAL '1 day', 'BORRADOR', 'crodriguez'),
(11, 11, 1, 3, 8, 650.00, 5200.00, CURRENT_TIMESTAMP - INTERVAL '12 hours', 'APROBADA', 'lhernandez'),
(12, 12, 5, 4, 2, 980.00, 1960.00, CURRENT_TIMESTAMP - INTERVAL '6 hours', 'BORRADOR', 'admin'),
(13, 13, 4, 5, 10, 280.00, 2800.00, CURRENT_TIMESTAMP - INTERVAL '3 hours', 'APROBADA', 'jdoe'),
(14, 14, 2, 1, 5, 520.00, 2600.00, CURRENT_TIMESTAMP - INTERVAL '1 hour', 'RECIBIDA', 'mgarcia'),
(15, 15, 1, 2, 3, 1100.00, 3300.00, CURRENT_TIMESTAMP, 'BORRADOR', 'crodriguez')
ON CONFLICT (id) DO UPDATE SET
    producto_id = EXCLUDED.producto_id,
    proveedor_id = EXCLUDED.proveedor_id,
    bodega_destino_id = EXCLUDED.bodega_destino_id,
    cantidad = EXCLUDED.cantidad,
    precio_unitario = EXCLUDED.precio_unitario,
    total = EXCLUDED.total,
    estado = EXCLUDED.estado,
    creado_por = EXCLUDED.creado_por;

-- Movimientos Inventario
INSERT INTO movimientos (id, fecha, tipo_movimiento, usuario_id, bodega_origen_id, bodega_destino_id) VALUES
(1, CURRENT_TIMESTAMP - INTERVAL '15 days', 'ENTRADA', 1, NULL, 1),
(2, CURRENT_TIMESTAMP - INTERVAL '14 days', 'TRANSFERENCIA', 2, 1, 2),
(3, CURRENT_TIMESTAMP - INTERVAL '13 days', 'SALIDA', 3, 2, NULL),
(4, CURRENT_TIMESTAMP - INTERVAL '12 days', 'ENTRADA', 5, NULL, 3),
(5, CURRENT_TIMESTAMP - INTERVAL '11 days', 'TRANSFERENCIA', 6, 3, 4),
(6, CURRENT_TIMESTAMP - INTERVAL '10 days', 'ENTRADA', 1, NULL, 2),
(7, CURRENT_TIMESTAMP - INTERVAL '9 days', 'SALIDA', 2, 1, NULL),
(8, CURRENT_TIMESTAMP - INTERVAL '8 days', 'TRANSFERENCIA', 3, 2, 5),
(9, CURRENT_TIMESTAMP - INTERVAL '7 days', 'ENTRADA', 5, NULL, 1),
(10, CURRENT_TIMESTAMP - INTERVAL '6 days', 'SALIDA', 6, 3, NULL),
(11, CURRENT_TIMESTAMP - INTERVAL '5 days', 'ENTRADA', 1, NULL, 4),
(12, CURRENT_TIMESTAMP - INTERVAL '4 days', 'TRANSFERENCIA', 2, 4, 5),
(13, CURRENT_TIMESTAMP - INTERVAL '3 days', 'ENTRADA', 3, NULL, 5),
(14, CURRENT_TIMESTAMP - INTERVAL '2 days', 'SALIDA', 5, 1, NULL),
(15, CURRENT_TIMESTAMP - INTERVAL '1 day', 'TRANSFERENCIA', 6, 5, 1)
ON CONFLICT (id) DO NOTHING;

-- Detalle de Movimientos (tabla movimiento_detalles)
INSERT INTO movimiento_detalles (id, movimiento_id, producto_id, cantidad) VALUES
(1, 1, 1, 50),
(2, 1, 3, 20),
(3, 2, 2, 5),
(4, 3, 1, 10),
(5, 4, 5, 30),
(6, 5, 4, 2),
(7, 6, 6, 15),
(8, 7, 3, 8),
(9, 8, 7, 3),
(10, 9, 8, 4),
(11, 10, 9, 2),
(12, 11, 10, 6),
(13, 12, 11, 5),
(14, 13, 12, 2),
(15, 14, 13, 10),
(16, 15, 14, 3)
ON CONFLICT (id) DO NOTHING;

-- Auditoría (tabla auditorias)
INSERT INTO auditorias (id, tipo_operacion, fecha_hora, usuario_id, entidad_afectada, entidad_id, valores_anteriores, valores_nuevos) VALUES
(1, 'INSERT', CURRENT_TIMESTAMP - INTERVAL '15 days', 1, 'Bodega', 1, NULL, '{"id": 1, "nombre": "Bodega Central Bogota", "capacidad": 50000}'),
(2, 'INSERT', CURRENT_TIMESTAMP - INTERVAL '15 days', 1, 'Producto', 1, NULL, '{"id": 1, "nombre": "Laptop Lenovo ThinkPad T14", "stock": 145}'),
(3, 'UPDATE', CURRENT_TIMESTAMP - INTERVAL '10 days', 2, 'Producto', 2, '{"stock": 15}', '{"stock": 8}'),
(4, 'INSERT', CURRENT_TIMESTAMP - INTERVAL '9 days', 3, 'OrdenCompra', 2, NULL, '{"id": 2, "estado": "APROBADA"}'),
(5, 'UPDATE', CURRENT_TIMESTAMP - INTERVAL '8 days', 1, 'OrdenCompra', 3, '{"estado": "APROBADA"}', '{"estado": "RECIBIDA"}'),
(6, 'DELETE', CURRENT_TIMESTAMP - INTERVAL '7 days', 5, 'OrdenCompra', 8, '{"id": 8, "estado": "BORRADOR"}', NULL),
(7, 'INSERT', CURRENT_TIMESTAMP - INTERVAL '6 days', 2, 'Movimiento', 6, NULL, '{"id": 6, "tipo": "ENTRADA"}'),
(8, 'UPDATE', CURRENT_TIMESTAMP - INTERVAL '5 days', 3, 'Bodega', 3, '{"capacidad": 20000}', '{"capacidad": 22000}'),
(9, 'INSERT', CURRENT_TIMESTAMP - INTERVAL '4 days', 1, 'Producto', 15, NULL, '{"id": 15, "nombre": "Laptop Dell Latitude 5430", "stock": 10}'),
(10, 'UPDATE', CURRENT_TIMESTAMP - INTERVAL '3 days', 2, 'Producto', 4, '{"stock": 4}', '{"stock": 2}'),
(11, 'INSERT', CURRENT_TIMESTAMP - INTERVAL '2 days', 3, 'OrdenCompra', 14, NULL, '{"id": 14, "estado": "RECIBIDA"}'),
(12, 'UPDATE', CURRENT_TIMESTAMP - INTERVAL '1 day', 1, 'OrdenCompra', 11, '{"estado": "APROBADA"}', '{"estado": "RECIBIDA"}')
ON CONFLICT (id) DO NOTHING;

-- Inventario por Bodega (tabla inventario_bodega)
INSERT INTO inventario_bodega (producto_id, bodega_id, stock) VALUES
-- Bodega Central Bogota (1)
(1, 1, 145),   -- Laptop
(3, 1, 65),    -- Teclado
(5, 1, 200),   -- Disco Duro
(6, 1, 5),     -- Mouse
(7, 1, 12),    -- Impresora
(11, 1, 15),   -- UPS
(15, 1, 10),   -- Laptop Dell
(17, 1, 30),   -- HDD 4TB
(18, 1, 18),   -- RAM DDR5
-- Centro Distribucion Medellin (2)
(2, 2, 8),     -- Monitor
(5, 2, 0),     -- Disco Duro
(9, 2, 3),     -- Router
(10, 2, 8),    -- Switch
(14, 2, 7),    -- Monitor Curvo
(16, 2, 2),    -- iPad Pro
(19, 2, 1),    -- RTX 4070
-- Bodega Norte Cali (3)
(4, 3, 4),     -- Silla
(13, 3, 25),   -- Silla Operativa
(18, 3, 0),    -- RAM
(20, 3, 9),    -- Fuente Poder
-- Bodega Sur Barranquilla (4)
(8, 4, 6),     -- Escritorio
(12, 4, 4),    -- Proyector
-- Bodega Occidente Pereira (5)
(1, 5, 0),     -- Laptop
(2, 5, 0),     -- Monitor
(3, 5, 0),     -- Teclado
(15, 5, 0)     -- Laptop Dell
ON CONFLICT (producto_id, bodega_id) DO UPDATE SET stock = EXCLUDED.stock;

-- Resumen diario del panel (Torre de Control)
INSERT INTO resumenes_panel (id, fecha, contenido_json, autor) VALUES
(1, CURRENT_DATE, '{"narrativa": "Dia operativamente estable. Se recibieron 3 ordenes de compra (ORD-3, ORD-6, ORD-14) totalizando 6,400 USD en inventario nuevo. Las transferencias entre bodegas Bogota-Medellin y Cali-Barranquilla se completaron sin incidencias.", "alertas": ["Stock critico: Monitor Dell UltraSharp (8 und en Medellin, 0 en Bogota)", "Quiebre inminente: Silla Ergonomica Herman Miller (4 und, consumo 2/dia)", "Router Cisco Meraki solo 3 unidades, proveedor con 10 dias entrega"], "accionesSugeridas": ["Generar orden de reposicion para monitores (sugerido: 20 und a TechSupply)", "Crear orden urgente para sillas ergonomicas (proveedor Office Solutions, 3 dias)", "Evaluar proveedor alternativo para equipos de red (dias entrega > 7)"], "kpis": {"ordenesPendientes": 7, "valorPendiente": 28420, "productosRiesgo": 5, "quiebres": 2}}', 'agente_automatizado')
ON CONFLICT (fecha) DO UPDATE SET contenido_json = EXCLUDED.contenido_json, autor = EXCLUDED.autor;

-- Ajustar secuencias para evitar conflictos de ID
SELECT setval('proyecto.usuarios_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.usuarios));
SELECT setval('proyecto.bodegas_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.bodegas));
SELECT setval('proyecto.productos_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.productos));
SELECT setval('proyecto.suppliers_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.suppliers));
SELECT setval('proyecto.ordenes_compra_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.ordenes_compra));
SELECT setval('proyecto.movimientos_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.movimientos));
SELECT setval('proyecto.movimiento_detalles_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.movimiento_detalles));
SELECT setval('proyecto.auditorias_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.auditorias));
SELECT setval('proyecto.inventario_bodega_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.inventario_bodega));
SELECT setval('proyecto.resumenes_panel_id_seq', (SELECT COALESCE(MAX(id), 0) FROM proyecto.resumenes_panel));