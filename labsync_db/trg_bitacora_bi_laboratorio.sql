create definer = root@localhost trigger trg_bitacora_bi_laboratorio
    before insert
    on bitacora
    for each row
BEGIN
  IF NEW.id_laboratorio IS NULL THEN
    SET NEW.id_laboratorio=(SELECT id_laboratorio FROM laboratorios
      WHERE nombre=NEW.laboratorio LIMIT 1);
  END IF;
  IF NEW.id_laboratorio IS NULL OR NOT EXISTS (
    SELECT 1 FROM laboratorios WHERE id_laboratorio=NEW.id_laboratorio
      AND nombre=NEW.laboratorio
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='El nombre y el ID del laboratorio de bitácora no corresponden';
  END IF;
END;

