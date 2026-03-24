-- ==============================================================================
-- 1. BASE DE DATOS: USUARIOS
-- ==============================================================================
USE usuarios;

-- Tabla de Roles
CREATE TABLE IF NOT EXISTS rol (
                                   id_rol BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   name VARCHAR(50) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
    );

-- Tabla de Usuarios (Con campos para el juego FastFingers)
CREATE TABLE IF NOT EXISTS usuarios (
                                        id_user BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        nombre_usuario VARCHAR(100) NOT NULL,
    apellido_usuario VARCHAR(150) NOT NULL,
    correo_usuario VARCHAR(254) NOT NULL UNIQUE,
    contrasenha_usuario VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creditos INT NOT NULL DEFAULT 0,
    color_tema VARCHAR(50) DEFAULT 'default'
    );

-- Tabla intermedia: Relación Usuarios - Roles (N:M)
CREATE TABLE IF NOT EXISTS roles_usuario (
                                             id_user BIGINT NOT NULL,
                                             id_rol BIGINT NOT NULL,
                                             PRIMARY KEY (id_user, id_rol),
    FOREIGN KEY (id_user) REFERENCES usuarios(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol) ON DELETE CASCADE
    );

-- Tabla del Catálogo de la Tienda (Cosméticos)
CREATE TABLE IF NOT EXISTS cosmeticos (
                                          id_cosmetico VARCHAR(50) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    precio INT NOT NULL,
    css_class VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
    );

-- Tabla de Inventario: Relación Usuarios - Cosméticos comprados (N:M)
CREATE TABLE IF NOT EXISTS inventario (
                                          id_user BIGINT NOT NULL,
                                          id_cosmetico VARCHAR(50) NOT NULL,
    fecha_compra TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_user, id_cosmetico),
    FOREIGN KEY (id_user) REFERENCES usuarios(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_cosmetico) REFERENCES cosmeticos(id_cosmetico) ON DELETE CASCADE
    );

-- Tabla de Partidas (Historial del juego FastFingers)
CREATE TABLE IF NOT EXISTS partidas (
                                        id_partida BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        id_user BIGINT NOT NULL,
                                        puntuacion INT NOT NULL,
                                        creditos_ganados INT NOT NULL,
                                        palabras_correctas INT NOT NULL DEFAULT 0,
                                        fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (id_user) REFERENCES usuarios(id_user) ON DELETE CASCADE
    );

-- Inserción de Datos Base (Usuarios y Roles)
INSERT IGNORE INTO rol (id_rol, activo, name) VALUES (1, 1, 'ADMIN');
INSERT IGNORE INTO rol (id_rol, activo, name) VALUES (2, 1, 'USUARIO');

-- Insertar Usuario Administrador (Jorge) - Password: 'admin'
INSERT IGNORE INTO usuarios (id_user, activo, nombre_usuario, apellido_usuario, correo_usuario, contrasenha_usuario, creditos, color_tema)
VALUES (1, 1, 'Jorge', 'Barriga', 'admin@gmail.com', '$2a$10$fKi12tHGj5lTBfgZ38.arelgjrIOseK241JA4FPCm6K7D4qLe.BCq', 9999, 'default');

-- Vincular Jorge con el Rol ADMIN
INSERT IGNORE INTO roles_usuario (id_user, id_rol) VALUES (1, 1);

-- Insertar Usuario de Pruebas (Alumno) - Password: 'agora'
INSERT IGNORE INTO usuarios (id_user, activo, nombre_usuario, apellido_usuario, correo_usuario, contrasenha_usuario, creditos, color_tema)
VALUES (2, 1, 'Alumno', 'Pruebas', 'alumno@gmail.com', '$2a$10$XURPShQNCsLjp1ESc2laoObo9QZDhxz73hJPaEv7/cBha4pk0AgP.', 100, 'default');

-- Vincular Alumno con el Rol USUARIO
INSERT IGNORE INTO roles_usuario (id_user, id_rol) VALUES (2, 2);

-- Insertar el Catálogo de Cosméticos de la Tienda
INSERT IGNORE INTO cosmeticos (id_cosmetico, nombre, tipo, precio, css_class, descripcion) VALUES
('c1', 'Tema Fuego', 'tema', 500, 'theme-fire', 'Fondo animado de fuego'),
('c2', 'Tema Matrix', 'tema', 800, 'theme-matrix', 'Lluvia de código verde'),
('c3', 'Borde Dorado', 'borde', 200, 'border-gold', 'Marco brillante para el juego'),
('c4', 'Modo Hacker', 'tema', 1000, 'theme-hacker', 'Estilo terminal retro');

-- ==============================================================================
-- 2. BASE DE DATOS: INCIDENCIAS
-- ==============================================================================
USE incidencias;

-- Tabla de Incidencias (Registro de errores del sistema)
CREATE TABLE IF NOT EXISTS incidencias (
                                           id_incidencia BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           endpoint VARCHAR(255),
    tipo VARCHAR(255),
    clase VARCHAR(255),
    metodo VARCHAR(255),
    traza TEXT,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_usuario BIGINT,
    -- IMPORTANTE: Fíjate en el "usuarios.usuarios" de la línea siguiente para referenciar la otra BBDD
    FOREIGN KEY (id_usuario) REFERENCES usuarios.usuarios(id_user) ON DELETE SET NULL
    );