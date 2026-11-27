CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    contrasena TEXT NOT NULL,
    email TEXT,
    rol TEXT DEFAULT 'operador',
    activo BOOLEAN DEFAULT 1,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL CHECK (length(trim(nombre)) > 0 AND length(nombre) <= 100),
    descripcion TEXT,
    fecha TEXT NOT NULL,
    estado TEXT NOT NULL DEFAULT 'pendiente'
        CHECK (estado IN ('pendiente', 'activo', 'cerrado', 'cancelado')),
    usuario_id INTEGER,
    fecha_creacion TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL CHECK (length(trim(nombre)) > 0 AND length(nombre) <= 80),
    descripcion TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_nombre_nocase
    ON categories (nombre COLLATE NOCASE);

CREATE TABLE IF NOT EXISTS event_categories (
    evento_id INTEGER NOT NULL,
    categoria_id INTEGER NOT NULL,
    PRIMARY KEY (evento_id, categoria_id),
    FOREIGN KEY (evento_id) REFERENCES events (id),
    FOREIGN KEY (categoria_id) REFERENCES categories (id)
);

CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL CHECK (length(trim(nombre)) > 0 AND length(nombre) <= 100),
    descripcion TEXT,
    precio INTEGER NOT NULL CHECK (typeof(precio) = 'integer' AND precio BETWEEN 0 AND 9007199254740991),
    stock INTEGER CHECK (stock IS NULL OR (typeof(stock) = 'integer' AND stock BETWEEN 0 AND 9007199254740991)),
    categoria_id INTEGER NOT NULL,
    evento_id INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT 1 CHECK (activo IN (0, 1)),
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (evento_id, categoria_id) REFERENCES event_categories (evento_id, categoria_id)
);

CREATE INDEX IF NOT EXISTS idx_products_evento_categoria ON products (evento_id, categoria_id);

CREATE TABLE IF NOT EXISTS inventory_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id INTEGER NOT NULL,
    cantidad_anterior INTEGER,
    cantidad_nueva INTEGER,
    tipo_movimiento TEXT NOT NULL CHECK (tipo_movimiento IN ('venta', 'ajuste', 'entrada')),
    ticket_id INTEGER,
    usuario_id INTEGER,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (producto_id) REFERENCES products (id),
    FOREIGN KEY (usuario_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS box (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    evento_id INTEGER NOT NULL UNIQUE,
    estado TEXT NOT NULL DEFAULT 'cerrada'
        CHECK (estado IN ('cerrada', 'abierta')),
    saldo_inicial INTEGER,
    saldo_actual INTEGER,
    fecha_apertura TEXT,
    fecha_cierre TEXT,
    FOREIGN KEY (evento_id) REFERENCES events (id)
);

CREATE TABLE IF NOT EXISTS cash_periods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    caja_id INTEGER NOT NULL,
    saldo_inicial INTEGER NOT NULL CHECK (typeof(saldo_inicial) = 'integer' AND saldo_inicial BETWEEN 0 AND 9007199254740991),
    fecha_apertura DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre DATETIME,
    efectivo_esperado INTEGER,
    efectivo_contado INTEGER,
    diferencia INTEGER,
    usuario_apertura_id INTEGER,
    usuario_cierre_id INTEGER,
    CHECK ((fecha_cierre IS NULL AND efectivo_esperado IS NULL AND efectivo_contado IS NULL AND diferencia IS NULL)
        OR (fecha_cierre IS NOT NULL AND typeof(efectivo_esperado) = 'integer'
            AND efectivo_esperado BETWEEN 0 AND 9007199254740991
            AND typeof(efectivo_contado) = 'integer' AND efectivo_contado BETWEEN 0 AND 9007199254740991
            AND typeof(diferencia) = 'integer' AND diferencia = efectivo_contado - efectivo_esperado)),
    FOREIGN KEY (caja_id) REFERENCES box (id),
    FOREIGN KEY (usuario_apertura_id) REFERENCES users (id),
    FOREIGN KEY (usuario_cierre_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_cash_periods_abierto ON cash_periods (caja_id) WHERE fecha_cierre IS NULL;

CREATE TABLE IF NOT EXISTS cash_contributions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    periodo_id INTEGER NOT NULL,
    responsable TEXT NOT NULL CHECK (length(trim(responsable)) > 0 AND length(responsable) <= 100),
    importe INTEGER NOT NULL CHECK (typeof(importe) = 'integer' AND importe BETWEEN 1 AND 9007199254740991),
    FOREIGN KEY (periodo_id) REFERENCES cash_periods (id)
);

CREATE TABLE IF NOT EXISTS tickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    evento_id INTEGER NOT NULL REFERENCES events (id),
    estado TEXT NOT NULL DEFAULT 'abierto' CHECK (estado IN ('abierto', 'pagado', 'deuda', 'anulado')),
    total INTEGER NOT NULL CHECK (typeof(total) = 'integer' AND total BETWEEN 0 AND 9007199254740991),
    metodo_pago TEXT CHECK (metodo_pago IN ('efectivo', 'transferencia', 'deuda')),
    deudor TEXT,
    detalle TEXT,
    periodo_id INTEGER REFERENCES cash_periods (id),
    fecha_creacion TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_confirmacion TEXT,
    CHECK ((estado = 'abierto' AND metodo_pago IS NULL AND deudor IS NULL AND periodo_id IS NULL AND fecha_confirmacion IS NULL)
        OR estado = 'anulado'
        OR (estado = 'pagado' AND metodo_pago IS NOT NULL AND metodo_pago IN ('efectivo', 'transferencia') AND deudor IS NULL AND periodo_id IS NOT NULL AND fecha_confirmacion IS NOT NULL)
        OR (estado = 'deuda' AND metodo_pago IS NOT NULL AND metodo_pago = 'deuda' AND deudor IS NOT NULL AND length(trim(deudor)) BETWEEN 1 AND 100 AND periodo_id IS NOT NULL AND fecha_confirmacion IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_tickets_evento ON tickets (evento_id, id);

CREATE TABLE IF NOT EXISTS ticket_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_id INTEGER NOT NULL REFERENCES tickets (id),
    producto_id INTEGER NOT NULL REFERENCES products (id),
    cantidad INTEGER NOT NULL CHECK (typeof(cantidad) = 'integer' AND cantidad BETWEEN 1 AND 9007199254740991),
    precio_unitario INTEGER NOT NULL CHECK (typeof(precio_unitario) = 'integer' AND precio_unitario BETWEEN 0 AND 9007199254740991),
    subtotal INTEGER NOT NULL CHECK (typeof(subtotal) = 'integer' AND subtotal BETWEEN 0 AND 9007199254740991),
    UNIQUE (ticket_id, producto_id)
);

CREATE TABLE IF NOT EXISTS box_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    caja_id INTEGER NOT NULL REFERENCES box (id),
    periodo_id INTEGER NOT NULL REFERENCES cash_periods (id),
    ticket_id INTEGER NOT NULL UNIQUE REFERENCES tickets (id),
    tipo TEXT NOT NULL CHECK (tipo = 'ingreso'),
    monto INTEGER NOT NULL CHECK (typeof(monto) = 'integer' AND monto BETWEEN 0 AND 9007199254740991),
    fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ticket_cancellations (
    ticket_id INTEGER PRIMARY KEY REFERENCES tickets (id),
    estado_anterior TEXT NOT NULL CHECK (estado_anterior IN ('abierto', 'pagado', 'deuda')),
    motivo TEXT NOT NULL CHECK (length(trim(motivo)) BETWEEN 1 AND 500),
    devolucion_transferencia INTEGER NOT NULL DEFAULT 0 CHECK (devolucion_transferencia IN (0, 1)),
    fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cash_refunds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    caja_id INTEGER NOT NULL REFERENCES box (id),
    periodo_id INTEGER NOT NULL REFERENCES cash_periods (id),
    ticket_id INTEGER NOT NULL UNIQUE REFERENCES ticket_cancellations (ticket_id),
    monto INTEGER NOT NULL CHECK (typeof(monto) = 'integer' AND monto BETWEEN 0 AND 9007199254740991),
    fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activity_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL REFERENCES users (id),
    accion TEXT NOT NULL,
    entidad TEXT NOT NULL,
    entidad_id INTEGER NOT NULL,
    fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activity_log_entidad ON activity_log (entidad, entidad_id, id);
