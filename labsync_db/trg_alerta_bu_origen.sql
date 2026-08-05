create definer = root@localhost trigger trg_alerta_bu_origen
    before update
    on alertas
    for each row
BEGIN
  SET NEW.id_reserva=NULL;
  SET NEW.id_falla=NULL;
  SET NEW.id_mantenimiento=NULL;
  SET NEW.id_inventario=NULL;
  IF NEW.tipo IN ('RESERVA_PENDIENTE','RESERVA_APROBADA','RESERVA_RECHAZADA') THEN
    SET NEW.id_reserva=CAST(NEW.referencia AS UNSIGNED);
  ELSEIF NEW.tipo='FALLA_PENDIENTE' THEN
    SET NEW.id_falla=CAST(NEW.referencia AS UNSIGNED);
  ELSEIF NEW.tipo IN ('MANTENIMIENTO_PROXIMO','MANTENIMIENTO_VENCIDO','SOFTWARE_ACTUALIZACION') THEN
    SET NEW.id_mantenimiento=CAST(NEW.referencia AS UNSIGNED);
  ELSEIF NEW.tipo IN ('MANTENIMIENTO_REQUERIDO','EQUIPO_BAJA','EQUIPO_REVISION') THEN
    SET NEW.id_inventario=(SELECT id_inventario FROM inventario
      WHERE codigo=NEW.referencia LIMIT 1);
  ELSE
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tipo de alerta desconocido';
  END IF;
  IF (NEW.id_reserva IS NOT NULL)+(NEW.id_falla IS NOT NULL)
     +(NEW.id_mantenimiento IS NOT NULL)+(NEW.id_inventario IS NOT NULL)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La alerta debe tener exactamente un origen válido';
  END IF;
  IF (NEW.id_reserva IS NOT NULL AND CAST(NEW.referencia AS UNSIGNED)<>NEW.id_reserva)
     OR (NEW.id_falla IS NOT NULL AND CAST(NEW.referencia AS UNSIGNED)<>NEW.id_falla)
     OR (NEW.id_mantenimiento IS NOT NULL AND CAST(NEW.referencia AS UNSIGNED)<>NEW.id_mantenimiento)
     OR (NEW.id_inventario IS NOT NULL AND NEW.referencia<>(SELECT codigo FROM inventario WHERE id_inventario=NEW.id_inventario)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La referencia no corresponde con el origen de la alerta';
  END IF;
END;
