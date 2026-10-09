-- database/02_seed_data.sql
-- Datos iniciales de prueba para el Perfil Refugio (RF13 y RF15)
-- Refugio: Asociación Protectora Huellitas Cordobesas (Martín Gómez, CUIL 20-12345678-9)
-- Refugio: Refugio Patitas del Sur (Camila Fernández, CUIL 27-12345678-4)

USE mascotas_cordoba;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- =============================================================================
-- 1. Insertar Refugios Habilitados
-- =============================================================================
SET IDENTITY_INSERT dbo.refugios ON;
INSERT INTO dbo.refugios (id_refugio, razon_social, correo, telefono, domicilio, habilitacion_municipal, id_responsable)
VALUES 
(101, N'Asociación Protectora Huellitas Cordobesas', N'contacto@huellitascba.org.ar', N'0351-4890123', N'Av. Colón 4500, Córdoba Capital', N'HAB-MUNIC-CBA-2024-0412', NULL),
(102, N'Refugio Patitas del Sur', N'info@patitasdelsur.org.ar', N'0351-4998877', N'Av. Valparaíso 3200, Córdoba', N'HAB-MUNIC-CBA-2023-0199', NULL);
SET IDENTITY_INSERT dbo.refugios OFF;
GO

-- =============================================================================
-- 2. Insertar Ciudadanos (Usuarios de Refugio Habilitados)
-- Clave por defecto de prueba: "1234"
-- =============================================================================
SET IDENTITY_INSERT dbo.ciudadanos ON;
INSERT INTO dbo.ciudadanos (id_ciudadano, cuil, clave, nombre, apellido, correo, telefono, domicilio, habilitado, id_refugio)
VALUES
(1, N'20-12345678-9', N'1234', N'Martín', N'Gómez', N'martin.gomez@huellitascba.org.ar', N'351-4567890', N'Av. Colón 4500, Córdoba Capital', 1, 101),
(2, N'27-12345678-4', N'1234', N'Camila', N'Fernández', N'camila@patitasdelsur.org.ar', N'351-7891234', N'Av. Valparaíso 3200, Córdoba', 1, 102);
SET IDENTITY_INSERT dbo.ciudadanos OFF;
GO

-- =============================================================================
-- 3. Actualizar Responsable en Refugios
-- =============================================================================
UPDATE dbo.refugios SET id_responsable = 1 WHERE id_refugio = 101;
UPDATE dbo.refugios SET id_responsable = 2 WHERE id_refugio = 102;
GO

-- =============================================================================
-- 4. Insertar Mascotas Registradas
-- =============================================================================
SET IDENTITY_INSERT dbo.mascotas ON;
INSERT INTO dbo.mascotas (nro_reg_municipal, nombre, especie, sexo, [año_nacimiento], raza, microchip, vive, id_refugio, id_responsable)
VALUES
(50123, N'Milo', N'Perro', N'Macho', 2023, N'Mestizo de Labrador', N'MC-CBA-99124', 1, 101, 1),
(50124, N'Luna', N'Gato', N'Hembra', 2024, N'Europeo Común', N'MC-CBA-99125', 1, 101, 1),
(50125, N'Thor', N'Perro', N'Macho', 2021, N'Mestizo Ovejero', NULL, 1, 101, 1),
(50126, N'Pelusa', N'Perro', N'Hembra', 2022, N'Mestiza Caniche', N'MC-CBA-88712', 1, 101, 1),
(50127, N'Simba', N'Gato', N'Macho', 2025, N'Siamés mestizo', NULL, 1, 102, 2);
SET IDENTITY_INSERT dbo.mascotas OFF;
GO

-- =============================================================================
-- 5. Insertar Publicaciones de Adopción Iniciales
-- =============================================================================
SET IDENTITY_INSERT dbo.publicaciones_adopcion ON;
INSERT INTO dbo.publicaciones_adopcion (nro_publicacion, nro_reg_municipal, id_refugio, fecha_publicacion, caracteristicas_mascota, condicion_adopcion, foto, estado_publicacion)
VALUES
(1001, 50123, 101, '2026-09-15', 
 N'Muy cariñoso, sociable con niños y otros perros. Tiene energía media-alta y sabe pasear con correa.', 
 N'Hogar con patio cerrado o compromiso de paseos diarios. Esquema de vacunación al día y seguimiento.', 
 N'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=600&q=80', 
 N'Activa'),
(1002, 50124, 101, '2026-09-20', 
 N'Tranquila, ronronea mucho. Ideal para departamento. Acostumbrada a usar rascador y piedritas.', 
 N'Depto con protección en balcones/ventanas. Compromiso de mantener al día desparasitación.', 
 N'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=600&q=80', 
 N'Activa'),
(1003, 50125, 101, '2026-08-10', 
 N'Excelente guardián y compañero de caminatas. En tratamiento por leve dermatitis ya controlada.', 
 N'Disponibilidad pausada momentáneamente por control veterinario programado.', 
 N'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&w=600&q=80', 
 N'Pausada'),
(1004, 50126, 101, '2026-07-01', 
 N'Dócil y juguetona. ¡Ya encontró su familia definitiva en barrio General Paz!', 
 N'Proceso de adopción finalizado exitosamente.', 
 N'https://images.unsplash.com/photo-1537151625747-768eb6cf92b2?auto=format&fit=crop&w=600&q=80', 
 N'Finalizada'),
(1005, 50127, 102, '2026-09-28', 
 N'Joven y muy curioso. Se lleva bien con otros felinos.', 
 N'Ambiente seguro sin acceso libre a la calle.', 
 N'https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=600&q=80', 
 N'Activa');
SET IDENTITY_INSERT dbo.publicaciones_adopcion OFF;
GO
