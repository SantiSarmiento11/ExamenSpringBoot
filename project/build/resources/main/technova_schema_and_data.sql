-- ============================================================================
-- TechNova IT Helpdesk - Script DDL y DML para PostgreSQL 15+
-- ============================================================================
-- Este archivo contiene la definición estructurada del esquema de base de datos
-- con restricciones de integridad referencial, índices de optimización y datos
-- iniciales semilla requeridos para la lógica del sistema.
-- 
-- NOTA: Este archivo es para ejecución manual (vía psql, pgAdmin, DBeaver, etc.)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. CREACIÓN DE TABLAS (DDL)
-- ----------------------------------------------------------------------------

-- Tabla: permission
CREATE TABLE IF NOT EXISTS permission (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Tabla: role
CREATE TABLE IF NOT EXISTS role (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Tabla intermedia: role_permission (Relación N:M con atributos)
CREATE TABLE IF NOT EXISTS role_permission (
    role_id INTEGER NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id INTEGER NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

-- Tabla: user_account
CREATE TABLE IF NOT EXISTS user_account (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla intermedia: user_role (Relación N:M con atributos)
CREATE TABLE IF NOT EXISTS user_role (
    user_id INTEGER NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- Tabla: ticket_status
CREATE TABLE IF NOT EXISTS ticket_status (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_final BOOLEAN NOT NULL DEFAULT FALSE
);

-- Tabla: status_transition (Transiciones permitidas entre estados)
CREATE TABLE IF NOT EXISTS status_transition (
    id SERIAL PRIMARY KEY,
    from_status_id INTEGER NOT NULL REFERENCES ticket_status(id) ON DELETE RESTRICT,
    to_status_id INTEGER NOT NULL REFERENCES ticket_status(id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_status_transition UNIQUE (from_status_id, to_status_id),
    CONSTRAINT chk_status_transition_different CHECK (from_status_id <> to_status_id)
);

-- Tabla: category
CREATE TABLE IF NOT EXISTS category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabla: priority
CREATE TABLE IF NOT EXISTS priority (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    level INTEGER NOT NULL CHECK (level >= 1)
);

-- Tabla: ticket
CREATE TABLE IF NOT EXISTS ticket (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id INTEGER NOT NULL REFERENCES category(id) ON DELETE RESTRICT,
    priority_id INTEGER NOT NULL REFERENCES priority(id) ON DELETE RESTRICT,
    status_id INTEGER NOT NULL REFERENCES ticket_status(id) ON DELETE RESTRICT,
    requester_id INTEGER NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
    due_date TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Garantizar existencia de due_date en tablas previamente creadas
ALTER TABLE ticket ADD COLUMN IF NOT EXISTS due_date TIMESTAMP WITHOUT TIME ZONE;

-- Tabla: ticket_history (Auditoría de cambios de estado)
CREATE TABLE IF NOT EXISTS ticket_history (
    id SERIAL PRIMARY KEY,
    ticket_id INTEGER NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    previous_status_id INTEGER REFERENCES ticket_status(id) ON DELETE RESTRICT,
    new_status_id INTEGER NOT NULL REFERENCES ticket_status(id) ON DELETE RESTRICT,
    changed_by INTEGER NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
    changed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: comment
CREATE TABLE IF NOT EXISTS comment (
    id SERIAL PRIMARY KEY,
    ticket_id INTEGER NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    author_id INTEGER NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: ticket_assignment (Historial y asignación técnica)
CREATE TABLE IF NOT EXISTS ticket_assignment (
    id SERIAL PRIMARY KEY,
    ticket_id INTEGER NOT NULL REFERENCES ticket(id) ON DELETE CASCADE,
    technician_id INTEGER NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
    assigned_by INTEGER NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT chk_assignment_dates CHECK (unassigned_at IS NULL OR unassigned_at >= assigned_at)
);

-- Tabla: ticket_rating (Calificación de satisfacción CSAT)
CREATE TABLE IF NOT EXISTS ticket_rating (
    id SERIAL PRIMARY KEY,
    ticket_id INTEGER NOT NULL UNIQUE REFERENCES ticket(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES user_account(id) ON DELETE RESTRICT,
    technician_id INTEGER REFERENCES user_account(id) ON DELETE SET NULL,
    puntuacion INTEGER NOT NULL CHECK (puntuacion >= 1 AND puntuacion <= 5),
    comentario VARCHAR(500),
    fecha_calificacion TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 2. ÍNDICES DE RENDIMIENTO
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_ticket_status ON ticket(status_id);
CREATE INDEX IF NOT EXISTS idx_ticket_category ON ticket(category_id);
CREATE INDEX IF NOT EXISTS idx_ticket_priority ON ticket(priority_id);
CREATE INDEX IF NOT EXISTS idx_ticket_requester ON ticket(requester_id);
CREATE INDEX IF NOT EXISTS idx_ticket_created_at ON ticket(created_at);

CREATE INDEX IF NOT EXISTS idx_ticket_history_ticket ON ticket_history(ticket_id);
CREATE INDEX IF NOT EXISTS idx_comment_ticket ON comment(ticket_id);
CREATE INDEX IF NOT EXISTS idx_assignment_ticket ON ticket_assignment(ticket_id);
CREATE INDEX IF NOT EXISTS idx_assignment_technician ON ticket_assignment(technician_id);
CREATE INDEX IF NOT EXISTS idx_ticket_rating_ticket ON ticket_rating(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_rating_technician ON ticket_rating(technician_id);

-- ----------------------------------------------------------------------------
-- 3. DATOS SEMILLA INICIALES (DML)
-- ----------------------------------------------------------------------------

-- Categorías permitidas
INSERT INTO category (name, description, is_active) VALUES
    ('HARDWARE', 'Incidentes relacionados con equipos físicos, periféricos y componentes de hardware.', TRUE),
    ('SOFTWARE', 'Problemas con aplicaciones, sistemas operativos o licencias de software.', TRUE),
    ('REDES', 'Problemas de conectividad, Wi-Fi, VPN, switches o acceso a red local/internet.', TRUE),
    ('ACCESOS', 'Gestión de credenciales, permisos, desbloqueo de cuentas y autorizaciones.', TRUE),
    ('OTROS', 'Otras solicitudes o incidencias de soporte no categorizadas.', TRUE)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active;

-- Prioridades permitidas (Nivel y SLAs)
INSERT INTO priority (name, description, level) VALUES
    ('BAJA', 'Incidencias menores con impacto mínimo en operaciones (SLA: 24 horas)', 1),
    ('MEDIA', 'Incidencias estándar que afectan parcialmente operaciones (SLA: 8 horas)', 2),
    ('ALTA', 'Incidencias graves que detienen labores críticas (SLA: 2 horas)', 3),
    ('CRITICA', 'Fallas catastróficas que impactan a toda la organización o servicios core (SLA: 1 hora)', 4)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    level = EXCLUDED.level;

-- Estados de Ticket (Ciclo de Vida)
INSERT INTO ticket_status (name, description, is_final) VALUES
    ('ABIERTA', 'Ticket registrado y pendiente de asignación', FALSE),
    ('ASIGNADA', 'Ticket asignado formalmente a un técnico de soporte', FALSE),
    ('EN_PROCESO', 'Técnico trabajando activamente en la resolución del ticket', FALSE),
    ('RESUELTA', 'Solución implementada por el técnico, pendiente de confirmación', FALSE),
    ('CERRADA', 'Ticket formalmente cerrado tras conformidad o inactividad', TRUE)
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    is_final = EXCLUDED.is_final;

-- Transiciones permitidas del flujo estricto:
-- ABIERTA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA
INSERT INTO status_transition (from_status_id, to_status_id, is_active)
SELECT f.id, t.id, TRUE
FROM ticket_status f, ticket_status t
WHERE f.name = 'ABIERTA' AND t.name = 'ASIGNADA'
ON CONFLICT (from_status_id, to_status_id) DO NOTHING;

INSERT INTO status_transition (from_status_id, to_status_id, is_active)
SELECT f.id, t.id, TRUE
FROM ticket_status f, ticket_status t
WHERE f.name = 'ASIGNADA' AND t.name = 'EN_PROCESO'
ON CONFLICT (from_status_id, to_status_id) DO NOTHING;

INSERT INTO status_transition (from_status_id, to_status_id, is_active)
SELECT f.id, t.id, TRUE
FROM ticket_status f, ticket_status t
WHERE f.name = 'EN_PROCESO' AND t.name = 'RESUELTA'
ON CONFLICT (from_status_id, to_status_id) DO NOTHING;

INSERT INTO status_transition (from_status_id, to_status_id, is_active)
SELECT f.id, t.id, TRUE
FROM ticket_status f, ticket_status t
WHERE f.name = 'RESUELTA' AND t.name = 'CERRADA'
ON CONFLICT (from_status_id, to_status_id) DO NOTHING;

-- Roles del Sistema
INSERT INTO role (name, description) VALUES
    ('ADMIN', 'Administrador del sistema con privilegios completos'),
    ('TECHNICIAN', 'Especialista de soporte técnico para atención de tickets'),
    ('USER', 'Empleado solicitante de tickets de soporte')
ON CONFLICT (name) DO NOTHING;

-- Permisos del Sistema
INSERT INTO permission (name, description) VALUES
    -- Permisos Administrador (ADMIN)
    ('TICKET_VIEW_ALL', 'Consultar todos los tickets registrados.'),
    ('TECHNICIAN_VIEW_AVAILABLE', 'Visualizar los técnicos disponibles.'),
    ('TICKET_ASSIGN', 'Asignar y reasignar técnicos a los tickets.'),
    ('USER_VIEW_ALL', 'Consultar los usuarios registrados.'),
    ('STATISTICS_VIEW', 'Consultar estadísticas generales de las incidencias.'),
    ('TICKET_HISTORY_VIEW_ALL', 'Visualizar el historial de cualquier ticket.'),
    -- Permisos Técnico (TECHNICIAN)
    ('TICKET_VIEW_ASSIGNED', 'Consultar los tickets asignados a su cuenta.'),
    ('TICKET_DETAILS_VIEW_ASSIGNED', 'Visualizar los detalles de las incidencias asignadas.'),
    ('TICKET_STATUS_UPDATE', 'Cambiar el estado de los tickets según las reglas establecidas.'),
    ('TICKET_COMMENT_TECHNICIAN', 'Agregar comentarios sobre el proceso de atención.'),
    ('TICKET_HISTORY_VIEW_ASSIGNED', 'Consultar el historial de las solicitudes asignadas.'),
    -- Permisos Usuario (USER)
    ('AUTH_REGISTER_LOGIN', 'Registrarse e iniciar sesión.'),
    ('TICKET_CREATE', 'Crear solicitudes de soporte tecnológico.'),
    ('TICKET_VIEW_OWN', 'Consultar únicamente sus propias solicitudes.'),
    ('TICKET_EDIT_OPEN_UNASSIGNED', 'Modificar una solicitud mientras permanezca abierta y sin asignar.'),
    ('TICKET_COMMENT_OWN', 'Agregar comentarios a sus solicitudes activas.'),
    ('TICKET_HISTORY_VIEW_OWN', 'Consultar el estado y el historial de sus tickets.'),
    ('TICKET_CLOSE_RESOLVED', 'Cerrar un ticket cuando haya sido resuelto.')
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description;

-- Asignación de Permisos a Roles
-- ADMIN:
INSERT INTO role_permission (role_id, permission_id, assigned_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM role r, permission p
WHERE r.name = 'ADMIN' AND p.name IN (
    'TICKET_VIEW_ALL',
    'TECHNICIAN_VIEW_AVAILABLE',
    'TICKET_ASSIGN',
    'USER_VIEW_ALL',
    'STATISTICS_VIEW',
    'TICKET_HISTORY_VIEW_ALL'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TECHNICIAN:
INSERT INTO role_permission (role_id, permission_id, assigned_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM role r, permission p
WHERE r.name = 'TECHNICIAN' AND p.name IN (
    'TICKET_VIEW_ASSIGNED',
    'TICKET_DETAILS_VIEW_ASSIGNED',
    'TICKET_STATUS_UPDATE',
    'TICKET_COMMENT_TECHNICIAN',
    'TICKET_HISTORY_VIEW_ASSIGNED'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- USER:
INSERT INTO role_permission (role_id, permission_id, assigned_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM role r, permission p
WHERE r.name = 'USER' AND p.name IN (
    'AUTH_REGISTER_LOGIN',
    'TICKET_CREATE',
    'TICKET_VIEW_OWN',
    'TICKET_EDIT_OPEN_UNASSIGNED',
    'TICKET_COMMENT_OWN',
    'TICKET_HISTORY_VIEW_OWN',
    'TICKET_CLOSE_RESOLVED'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Usuarios Iniciales de Prueba (Contraseña default: admin123 con BCrypt)
-- Hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
INSERT INTO user_account (first_name, last_name, email, password_hash, is_active, created_at, updated_at) VALUES
    ('Admin', 'TechNova', 'admin@technova.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Carlos', 'Técnico', 'tecnico@technova.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Santiago', 'Usuario', 'usuario@technova.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Asignación de Roles a Usuarios de Prueba
INSERT INTO user_role (user_id, role_id, assigned_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM user_account u, role r
WHERE u.email = 'admin@technova.com' AND r.name = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_role (user_id, role_id, assigned_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM user_account u, role r
WHERE u.email = 'tecnico@technova.com' AND r.name = 'TECHNICIAN'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_role (user_id, role_id, assigned_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM user_account u, role r
WHERE u.email = 'usuario@technova.com' AND r.name = 'USER'
ON CONFLICT (user_id, role_id) DO NOTHING;
