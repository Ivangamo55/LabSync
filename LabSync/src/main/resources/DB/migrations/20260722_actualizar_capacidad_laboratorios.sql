-- Ejecutar una sola vez en instalaciones existentes de LabSync.
-- PB-05 es un taller sin equipos y M-19 deja de formar parte del catálogo.
DELETE FROM laboratorios WHERE nombre IN ('PB-05', 'M-19');

INSERT INTO laboratorios (nombre, total_equipos, estado) VALUES
    ('M-11', 19, 'Disponible'),
    ('M-02', 30, 'Disponible'),
    ('5-03', 22, 'Disponible'),
    ('5-06', 30, 'Disponible'),
    ('M-12', 12, 'Disponible'),
    ('M-14', 25, 'Disponible'),
    ('M-13', 20, 'Disponible'),
    ('PB-07', 20, 'Disponible'),
    ('M-05', 27, 'Disponible')
ON DUPLICATE KEY UPDATE
    total_equipos = VALUES(total_equipos),
    estado = VALUES(estado);

-- Protege también las inserciones realizadas fuera de la aplicación.
ALTER TABLE reservas
    ADD CONSTRAINT chk_reservas_maximo_personas
    CHECK (cantidad_alumnos BETWEEN 1 AND 31);

ALTER TABLE bitacora
    ADD CONSTRAINT chk_bitacora_maximo_personas
    CHECK (total_usuarios BETWEEN 1 AND 31);
