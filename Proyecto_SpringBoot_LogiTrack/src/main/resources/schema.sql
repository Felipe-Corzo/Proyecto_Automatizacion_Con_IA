-- LogiTrack S.A. - Schema de Base de Datos PostgreSQL
-- Tablas mapeadas exactamente a las entidades JPA

CREATE SCHEMA IF NOT EXISTS proyecto;
SET search_path TO proyecto;

-- Usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL
);

-- Bodegas
CREATE TABLE IF NOT EXISTS bodegas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150) NOT NULL,
    capacidad INT NOT NULL,
    encargado_id BIGINT,
    FOREIGN KEY (encargado_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- Proveedores (tabla suppliers)
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    nit_rut VARCHAR(50),
    contacto_nombre VARCHAR(100),
    email VARCHAR(100),
    telefono VARCHAR(50),
    direccion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT true,
    dias_entrega INT
);

-- Productos
CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    categoria VARCHAR(50),
    stock INT NOT NULL DEFAULT 0,
    precio DECIMAL(10, 2) NOT NULL,
    proveedor_id BIGINT,
    FOREIGN KEY (proveedor_id) REFERENCES suppliers(id) ON DELETE SET NULL
);

-- Movimientos de Inventario
CREATE TABLE IF NOT EXISTS movimientos (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_movimiento VARCHAR(20) NOT NULL,
    usuario_id BIGINT NOT NULL,
    bodega_origen_id BIGINT,
    bodega_destino_id BIGINT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (bodega_origen_id) REFERENCES bodegas(id),
    FOREIGN KEY (bodega_destino_id) REFERENCES bodegas(id)
);

-- Detalle de Movimientos
CREATE TABLE IF NOT EXISTS movimiento_detalles (
    id BIGSERIAL PRIMARY KEY,
    movimiento_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    FOREIGN KEY (movimiento_id) REFERENCES movimientos(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Inventario por Bodega (stock de cada producto por bodega)
CREATE TABLE IF NOT EXISTS inventario_bodega (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    bodega_id BIGINT NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE,
    FOREIGN KEY (bodega_id) REFERENCES bodegas(id) ON DELETE CASCADE,
    UNIQUE (producto_id, bodega_id)
);

-- Órdenes de Compra
CREATE TABLE IF NOT EXISTS ordenes_compra (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    bodega_destino_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10, 2),
    total DECIMAL(10, 2),
    fecha_creacion TIMESTAMP,
    estado VARCHAR(20),
    creado_por VARCHAR(100),
    pdf_data BYTEA,
    pdf_fecha_generacion TIMESTAMP,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (proveedor_id) REFERENCES suppliers(id),
    FOREIGN KEY (bodega_destino_id) REFERENCES bodegas(id)
);

-- Auditoría
CREATE TABLE IF NOT EXISTS auditorias (
    id BIGSERIAL PRIMARY KEY,
    tipo_operacion VARCHAR(20) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id BIGINT,
    entidad_afectada VARCHAR(50) NOT NULL,
    entidad_id BIGINT,
    valores_anteriores TEXT,
    valores_nuevos TEXT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

-- Resumen Diario del Panel (Torre de Control)
CREATE TABLE IF NOT EXISTS resumenes_panel (
    id BIGSERIAL PRIMARY KEY,
    fecha DATE NOT NULL UNIQUE,
    contenido_json TEXT NOT NULL,
    autor VARCHAR(100)
);

-- Índices útiles para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos(fecha);
CREATE INDEX IF NOT EXISTS idx_movimientos_tipo ON movimientos(tipo_movimiento);
CREATE INDEX IF NOT EXISTS idx_movimientos_usuario ON movimientos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_bodega_origen ON movimientos(bodega_origen_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_bodega_destino ON movimientos(bodega_destino_id);
CREATE INDEX IF NOT EXISTS idx_movimiento_detalles_movimiento ON movimiento_detalles(movimiento_id);
CREATE INDEX IF NOT EXISTS idx_movimiento_detalles_producto ON movimiento_detalles(producto_id);
CREATE INDEX IF NOT EXISTS idx_inventario_bodega_producto ON inventario_bodega(producto_id);
CREATE INDEX IF NOT EXISTS idx_inventario_bodega_bodega ON inventario_bodega(bodega_id);
CREATE INDEX IF NOT EXISTS idx_ordenes_compra_estado ON ordenes_compra(estado);
CREATE INDEX IF NOT EXISTS idx_ordenes_compra_fecha ON ordenes_compra(fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_auditorias_fecha ON auditorias(fecha_hora);
CREATE INDEX IF NOT EXISTS idx_auditorias_entidad ON auditorias(entidad_afectada, entidad_id);
CREATE INDEX IF NOT EXISTS idx_auditorias_usuario ON auditorias(usuario_id);