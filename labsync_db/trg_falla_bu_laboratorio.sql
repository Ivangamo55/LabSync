create definer = root@localhost trigger trg_falla_bu_laboratorio
    before update
    on reporte_fallas
    for each row
BEGIN
  IF NEW.id_inventario IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM inventario WHERE id_inventario=NEW.id_inventario
      AND id_laboratorio=NEW.id_laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El equipo reportado no pertenece al laboratorio indicado';
  END IF;
  IF NEW.id_reserva IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM reservas WHERE id_reserva=NEW.id_reserva
      AND id_laboratorio=NEW.id_laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La reserva reportada no pertenece al laboratorio indicado';
  END IF;
END;

