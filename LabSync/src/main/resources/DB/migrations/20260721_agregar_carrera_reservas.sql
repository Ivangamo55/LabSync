-- Ejecutar una sola vez en instalaciones existentes de LabSync.
ALTER TABLE reservas
    ADD COLUMN carrera VARCHAR(100) NOT NULL DEFAULT 'N/A' AFTER actividad;

