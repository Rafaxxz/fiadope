-- =====================================================================
-- FiadoPe - Esquema inicial (PostgreSQL 16+)
-- Basado en el diccionario de datos del Trabajo Parcial (sección 8.2).
-- Montos en NUMERIC (sin redondeos de coma flotante) y tasas con 7+ decimales.
-- =====================================================================

-- Parámetros configurables (topes BCRP que se actualizan cada semestre)
CREATE TABLE parametro (
    clave        VARCHAR(40)   PRIMARY KEY,
    valor        NUMERIC(12,7) NOT NULL,
    descripcion  VARCHAR(200)  NOT NULL,
    actualizado  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- 8.2.1 tienda
CREATE TABLE tienda (
    id_tienda     SERIAL        PRIMARY KEY,
    ruc           CHAR(11)      NOT NULL UNIQUE CHECK (ruc ~ '^(10|20)[0-9]{9}$'),
    razon_social  VARCHAR(120)  NOT NULL,
    propietario   VARCHAR(100)  NOT NULL,
    giro          VARCHAR(30)   NOT NULL,
    ubicacion     VARCHAR(60)   NOT NULL,
    direccion     VARCHAR(150)  NOT NULL,
    telefono      VARCHAR(9)    NOT NULL CHECK (telefono ~ '^9[0-9]{8}$'),
    limite_global NUMERIC(12,2) NOT NULL CHECK (limite_global > 0),
    estado        VARCHAR(10)   NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','BAJA')),
    fecha_alta    TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- 8.2.2 usuario
CREATE TABLE usuario (
    id_usuario        SERIAL       PRIMARY KEY,
    id_tienda         INTEGER      REFERENCES tienda(id_tienda),
    login             VARCHAR(11)  NOT NULL UNIQUE CHECK (login ~ '^([0-9]{8}|[0-9]{11})$'),
    nombre            VARCHAR(100) NOT NULL,
    email             VARCHAR(100) NOT NULL UNIQUE,
    hash_clave        VARCHAR(72)  NOT NULL,
    rol               VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN_PLATAFORMA','ADMIN_NEGOCIO','CLIENTE')),
    mfa_secreto       VARCHAR(72),
    mfa_token         VARCHAR(64)  UNIQUE,
    mfa_expira        TIMESTAMP,
    mfa_intentos      SMALLINT     NOT NULL DEFAULT 0,
    intentos_fallidos SMALLINT     NOT NULL DEFAULT 0,
    bloqueado_hasta   TIMESTAMP,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_usuario_tienda CHECK (
        (rol = 'ADMIN_PLATAFORMA' AND id_tienda IS NULL) OR (rol <> 'ADMIN_PLATAFORMA' AND id_tienda IS NOT NULL))
);

-- 8.2.3 cliente
CREATE TABLE cliente (
    id_cliente          SERIAL        PRIMARY KEY,
    id_tienda           INTEGER       NOT NULL REFERENCES tienda(id_tienda),
    id_usuario          INTEGER       REFERENCES usuario(id_usuario),
    dni                 VARCHAR(12)   NOT NULL CHECK (dni ~ '^[0-9]{8,12}$'),
    nombres             VARCHAR(100)  NOT NULL,
    direccion           VARCHAR(150)  NOT NULL,
    telefono            VARCHAR(9)    NOT NULL CHECK (telefono ~ '^9[0-9]{8}$'),
    tipo_tasa           VARCHAR(10)   NOT NULL CHECK (tipo_tasa IN ('NOMINAL','EFECTIVA')),
    dias_plazo_tasa     SMALLINT      NOT NULL CHECK (dias_plazo_tasa > 0),
    dias_capitalizacion SMALLINT      DEFAULT 1 CHECK (dias_capitalizacion IS NULL OR dias_capitalizacion > 0),
    tasa                NUMERIC(10,7) NOT NULL CHECK (tasa > 0),
    tipo_tasa_mora      VARCHAR(10)   NOT NULL CHECK (tipo_tasa_mora IN ('NOMINAL','EFECTIVA')),
    tasa_mora           NUMERIC(10,7) NOT NULL CHECK (tasa_mora >= 0),
    moneda              CHAR(3)       NOT NULL DEFAULT 'PEN' CHECK (moneda IN ('PEN','USD')),
    limite_credito      NUMERIC(12,2) NOT NULL CHECK (limite_credito > 0),
    deuda_vigente       NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (deuda_vigente >= 0),
    max_meses           SMALLINT      NOT NULL CHECK (max_meses BETWEEN 2 AND 24),
    dia_corte           SMALLINT      NOT NULL CHECK (dia_corte BETWEEN 1 AND 28),
    dia_pago            SMALLINT      NOT NULL CHECK (dia_pago BETWEEN 1 AND 28),
    estado              VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','BAJA')),
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cliente_dni_tienda UNIQUE (id_tienda, dni),
    CONSTRAINT ck_cliente_dias CHECK (dia_corte <> dia_pago),
    CONSTRAINT ck_cliente_limite CHECK (deuda_vigente <= limite_credito)
);

-- 8.2.4 producto
CREATE TABLE producto (
    id_producto     SERIAL        PRIMARY KEY,
    id_tienda       INTEGER       NOT NULL REFERENCES tienda(id_tienda),
    nombre          VARCHAR(100)  NOT NULL,
    marca           VARCHAR(60),
    proveedor       VARCHAR(60),
    unidad          VARCHAR(20)   NOT NULL DEFAULT 'UND',
    categoria       VARCHAR(30)   NOT NULL DEFAULT 'Abarrotes y granel',
    sku             VARCHAR(15)   NOT NULL,
    precio_contado  NUMERIC(12,2) NOT NULL CHECK (precio_contado > 0),
    precio_lista    NUMERIC(12,2) NOT NULL,
    permite_fin_mes BOOLEAN       NOT NULL DEFAULT TRUE,
    permite_cuotas  BOOLEAN       NOT NULL DEFAULT FALSE,
    imagen_url      VARCHAR(255),
    estado          VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','BAJA')),
    CONSTRAINT uq_producto_sku UNIQUE (id_tienda, sku),
    CONSTRAINT ck_producto_precio CHECK (precio_lista >= precio_contado),
    CONSTRAINT ck_producto_modalidad CHECK (permite_fin_mes OR permite_cuotas)
);

-- 8.2.5 compra (las tasas se congelan en la compra: un cambio de tasa solo afecta compras futuras)
CREATE TABLE compra (
    id_compra           SERIAL        PRIMARY KEY,
    id_cliente          INTEGER       NOT NULL REFERENCES cliente(id_cliente),
    id_producto         INTEGER       NOT NULL REFERENCES producto(id_producto),
    fecha_hora          TIMESTAMP     NOT NULL,
    cantidad            NUMERIC(10,3) NOT NULL CHECK (cantidad > 0),
    precio_unitario     NUMERIC(12,2) NOT NULL,
    capital             NUMERIC(14,2) NOT NULL CHECK (capital > 0),
    modalidad           VARCHAR(10)   NOT NULL CHECK (modalidad IN ('FIN_DE_MES','CUOTAS')),
    n_cuotas            SMALLINT,
    fecha_corte         DATE          NOT NULL,
    fecha_pago_asignada DATE          NOT NULL,
    dias                SMALLINT      NOT NULL CHECK (dias >= 0),
    monto_s             NUMERIC(14,2) NOT NULL,
    interes             NUMERIC(14,2) NOT NULL,
    ted                 NUMERIC(14,12) NOT NULL,
    ted_mora            NUMERIC(14,12) NOT NULL,
    tem                 NUMERIC(14,12) NOT NULL,
    tea                 NUMERIC(14,10) NOT NULL,
    cuota_fija          NUMERIC(14,2),
    total_plazo         NUMERIC(14,2),
    pagada              BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_compra_cuotas CHECK (
        (modalidad = 'CUOTAS' AND n_cuotas BETWEEN 2 AND 24) OR (modalidad = 'FIN_DE_MES' AND n_cuotas IS NULL))
);
CREATE INDEX ix_compra_cliente ON compra(id_cliente, fecha_pago_asignada);

-- 8.2.6 cuota (método francés vencido ordinario)
CREATE TABLE cuota (
    id_cuota      SERIAL        PRIMARY KEY,
    id_compra     INTEGER       NOT NULL REFERENCES compra(id_compra),
    numero        SMALLINT      NOT NULL CHECK (numero > 0),
    vencimiento   DATE          NOT NULL,
    saldo_inicial NUMERIC(14,2) NOT NULL,
    interes       NUMERIC(14,2) NOT NULL,
    amortizacion  NUMERIC(14,2) NOT NULL,
    cuota         NUMERIC(14,2) NOT NULL,
    saldo_final   NUMERIC(14,2) NOT NULL,
    pagada        BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_cuota_numero UNIQUE (id_compra, numero)
);
CREATE INDEX ix_cuota_vencimiento ON cuota(vencimiento);

-- 8.2.7 estado_cuenta
CREATE TABLE estado_cuenta (
    id_estado     SERIAL        PRIMARY KEY,
    id_cliente    INTEGER       NOT NULL REFERENCES cliente(id_cliente),
    fecha_corte   DATE          NOT NULL,
    fecha_pago    DATE          NOT NULL,
    total_periodo NUMERIC(14,2) NOT NULL CHECK (total_periodo >= 0),
    estado        VARCHAR(10)   NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','PAGADO')),
    fecha_emision TIMESTAMP     NOT NULL DEFAULT NOW(),
    enviado       BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_estado_corte UNIQUE (id_cliente, fecha_corte)
);

-- 8.2.8 estado_cuenta_item
CREATE TABLE estado_cuenta_item (
    id_item   SERIAL        PRIMARY KEY,
    id_estado INTEGER       NOT NULL REFERENCES estado_cuenta(id_estado),
    tipo      VARCHAR(10)   NOT NULL CHECK (tipo IN ('COMPRA','CUOTA','MORA')),
    id_compra INTEGER       REFERENCES compra(id_compra),
    id_cuota  INTEGER       REFERENCES cuota(id_cuota),
    detalle   VARCHAR(150)  NOT NULL,
    fecha     DATE          NOT NULL,
    dias      SMALLINT      NOT NULL,
    monto     NUMERIC(14,2) NOT NULL,
    interes   NUMERIC(14,2) NOT NULL,
    total     NUMERIC(14,2) NOT NULL
);

-- 8.2.9 pago (solo pago total, relación 1:1 con el estado de cuenta)
CREATE TABLE pago (
    id_pago       SERIAL        PRIMARY KEY,
    id_estado     INTEGER       NOT NULL UNIQUE REFERENCES estado_cuenta(id_estado),
    metodo_pago   VARCHAR(15)   NOT NULL DEFAULT 'Yape/Plin' CHECK (metodo_pago IN ('Yape/Plin','Efectivo','Transferencia')),
    nro_operacion VARCHAR(20),
    fecha_real    DATE          NOT NULL,
    dias_mora     SMALLINT      NOT NULL CHECK (dias_mora >= 0),
    int_comp_mora NUMERIC(14,2) NOT NULL,
    int_mora      NUMERIC(14,2) NOT NULL,
    total_pagado  NUMERIC(14,2) NOT NULL,
    registrado    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_pago_operacion CHECK (metodo_pago = 'Efectivo' OR (nro_operacion IS NOT NULL AND length(nro_operacion) BETWEEN 1 AND 20))
);

-- 8.2.10 consentimiento (Ley N.° 29733)
CREATE TABLE consentimiento (
    id_consentimiento SERIAL      PRIMARY KEY,
    id_cliente        INTEGER     NOT NULL REFERENCES cliente(id_cliente),
    version_politica  VARCHAR(10) NOT NULL,
    acepta            BOOLEAN     NOT NULL CHECK (acepta),
    fecha_hora        TIMESTAMP   NOT NULL DEFAULT NOW(),
    ip                INET        NOT NULL
);

-- 8.2.11 bitacora (inalterable)
CREATE TABLE bitacora (
    id_bitacora   SERIAL      PRIMARY KEY,
    id_usuario    INTEGER     REFERENCES usuario(id_usuario),
    id_tienda     INTEGER     REFERENCES tienda(id_tienda),
    usuario_login VARCHAR(100),
    accion        VARCHAR(30) NOT NULL,
    tabla         VARCHAR(30) NOT NULL,
    id_registro   INTEGER,
    valor_antes   JSONB,
    valor_despues JSONB,
    fecha_hora    TIMESTAMP   NOT NULL DEFAULT NOW(),
    ip            INET        NOT NULL
);
CREATE INDEX ix_bitacora_fecha ON bitacora(fecha_hora DESC);
CREATE INDEX ix_bitacora_tienda ON bitacora(id_tienda, fecha_hora DESC);

-- No repudio: la bitácora no admite UPDATE ni DELETE (Ley N.° 30096)
CREATE OR REPLACE FUNCTION fn_bitacora_inalterable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La bitácora es inalterable: no se permite % sobre sus registros', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_bitacora_inalterable
    BEFORE UPDATE OR DELETE ON bitacora
    FOR EACH ROW EXECUTE FUNCTION fn_bitacora_inalterable();

-- Topes BCRP vigentes (mayo - octubre 2026), en tanto por uno
INSERT INTO parametro (clave, valor, descripcion) VALUES
    ('tope_tea_compensatoria', 1.1413000, 'Tasa máxima compensatoria (TEA) fijada por el BCRP'),
    ('tope_tea_moratoria',     0.1712000, 'Tasa máxima moratoria (TEA): 15 % de la compensatoria');
