-- database/01_schema.sql
-- Esquema canónico para el Programa Municipal de Bienestar Animal "Mascotas Córdoba"
-- Basado en: docs/03-Modelo-logico-mascotas-cba.pdf
-- Alcance: Perfil Refugio (RF13 y RF15)

USE mascotas_cordoba;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- 1. Eliminar restricciones de clave foránea existentes para permitir recreación limpia
IF OBJECT_ID('dbo.FK_publicaciones_mascotas', 'F') IS NOT NULL
    ALTER TABLE dbo.publicaciones_adopcion DROP CONSTRAINT FK_publicaciones_mascotas;
IF OBJECT_ID('dbo.FK_publicaciones_refugios', 'F') IS NOT NULL
    ALTER TABLE dbo.publicaciones_adopcion DROP CONSTRAINT FK_publicaciones_refugios;
IF OBJECT_ID('dbo.FK_mascotas_refugios', 'F') IS NOT NULL
    ALTER TABLE dbo.mascotas DROP CONSTRAINT FK_mascotas_refugios;
IF OBJECT_ID('dbo.FK_mascotas_responsable', 'F') IS NOT NULL
    ALTER TABLE dbo.mascotas DROP CONSTRAINT FK_mascotas_responsable;
IF OBJECT_ID('dbo.FK_refugios_responsable', 'F') IS NOT NULL
    ALTER TABLE dbo.refugios DROP CONSTRAINT FK_refugios_responsable;
IF OBJECT_ID('dbo.FK_ciudadanos_refugios', 'F') IS NOT NULL
    ALTER TABLE dbo.ciudadanos DROP CONSTRAINT FK_ciudadanos_refugios;
GO

-- 2. Eliminar tablas previas si existen
IF OBJECT_ID('dbo.publicaciones_adopcion', 'U') IS NOT NULL DROP TABLE dbo.publicaciones_adopcion;
IF OBJECT_ID('dbo.mascotas', 'U') IS NOT NULL DROP TABLE dbo.mascotas;
IF OBJECT_ID('dbo.usuarios', 'U') IS NOT NULL DROP TABLE dbo.usuarios;
IF OBJECT_ID('dbo.ciudadanos', 'U') IS NOT NULL DROP TABLE dbo.ciudadanos;
IF OBJECT_ID('dbo.refugios', 'U') IS NOT NULL DROP TABLE dbo.refugios;
GO

-- 3. Tabla: refugios
CREATE TABLE dbo.refugios (
    id_refugio INT IDENTITY(100, 1) NOT NULL,
    razon_social NVARCHAR(150) NOT NULL,
    correo NVARCHAR(150) NOT NULL,
    telefono NVARCHAR(50) NOT NULL,
    domicilio NVARCHAR(255) NOT NULL,
    habilitacion_municipal NVARCHAR(100) NOT NULL,
    id_responsable INT NULL,
    CONSTRAINT PK_refugios PRIMARY KEY CLUSTERED (id_refugio)
);
GO

-- 4. Tabla: ciudadanos (Usuarios del sistema municipal)
CREATE TABLE dbo.ciudadanos (
    id_ciudadano INT IDENTITY(1, 1) NOT NULL,
    cuil NVARCHAR(20) NOT NULL,
    clave NVARCHAR(255) NOT NULL,
    nombre NVARCHAR(100) NOT NULL,
    apellido NVARCHAR(100) NOT NULL,
    correo NVARCHAR(150) NOT NULL,
    telefono NVARCHAR(50) NULL,
    domicilio NVARCHAR(255) NULL,
    habilitado BIT NOT NULL DEFAULT 1,
    id_refugio INT NULL,
    CONSTRAINT PK_ciudadanos PRIMARY KEY CLUSTERED (id_ciudadano),
    CONSTRAINT UQ_ciudadanos_cuil UNIQUE (cuil),
    CONSTRAINT FK_ciudadanos_refugios FOREIGN KEY (id_refugio) REFERENCES dbo.refugios (id_refugio)
);
GO

-- 5. Vincular id_responsable de refugios a ciudadanos
ALTER TABLE dbo.refugios
ADD CONSTRAINT FK_refugios_responsable FOREIGN KEY (id_responsable) REFERENCES dbo.ciudadanos (id_ciudadano);
GO

-- 6. Tabla: mascotas (Registro Único Municipal)
CREATE TABLE dbo.mascotas (
    nro_reg_municipal INT IDENTITY(50000, 1) NOT NULL,
    nombre NVARCHAR(100) NOT NULL,
    especie NVARCHAR(50) NOT NULL,
    sexo NVARCHAR(20) NOT NULL,
    [año_nacimiento] INT NOT NULL,
    raza NVARCHAR(100) NULL,
    microchip NVARCHAR(50) NULL,
    vive BIT NOT NULL DEFAULT 1,
    id_refugio INT NOT NULL,
    id_responsable INT NULL,
    CONSTRAINT PK_mascotas PRIMARY KEY CLUSTERED (nro_reg_municipal),
    CONSTRAINT FK_mascotas_refugios FOREIGN KEY (id_refugio) REFERENCES dbo.refugios (id_refugio),
    CONSTRAINT FK_mascotas_responsable FOREIGN KEY (id_responsable) REFERENCES dbo.ciudadanos (id_ciudadano)
);
GO

-- 7. Tabla: publicaciones_adopcion
CREATE TABLE dbo.publicaciones_adopcion (
    nro_publicacion INT IDENTITY(1000, 1) NOT NULL,
    nro_reg_municipal INT NOT NULL,
    id_refugio INT NOT NULL,
    fecha_publicacion DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    caracteristicas_mascota NVARCHAR(1000) NOT NULL,
    condicion_adopcion NVARCHAR(1000) NOT NULL,
    foto NVARCHAR(MAX) NOT NULL,
    estado_publicacion NVARCHAR(50) NOT NULL,
    CONSTRAINT PK_publicaciones_adopcion PRIMARY KEY CLUSTERED (nro_publicacion),
    CONSTRAINT FK_publicaciones_mascotas FOREIGN KEY (nro_reg_municipal) REFERENCES dbo.mascotas (nro_reg_municipal),
    CONSTRAINT FK_publicaciones_refugios FOREIGN KEY (id_refugio) REFERENCES dbo.refugios (id_refugio),
    CONSTRAINT CHK_publicaciones_estado CHECK (estado_publicacion IN ('Activa', 'Pausada', 'Finalizada'))
);
GO

-- 8. Índices de unicidad y reglas de negocio
-- RF13: Una mascota sólo podrá tener una publicación de adopción activa
CREATE UNIQUE NONCLUSTERED INDEX UQ_mascota_publicacion_activa 
ON dbo.publicaciones_adopcion (nro_reg_municipal) 
WHERE estado_publicacion = 'Activa';
GO

-- Regla de registro: Evitar duplicidades de microchip en animales registrados
CREATE UNIQUE NONCLUSTERED INDEX UQ_mascotas_microchip 
ON dbo.mascotas (microchip) 
WHERE microchip IS NOT NULL;
GO

