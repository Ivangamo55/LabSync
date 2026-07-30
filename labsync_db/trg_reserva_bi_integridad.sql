create definer = root@localhost trigger trg_reserva_bi_integridad
    before insert
    on reservas
    for each row
BEGIN
  DECLARE v_capacidad INT;
  DECLARE v_ocupacion INT DEFAULT 0;
  DECLARE v_rol VARCHAR(50);
  SELECT u.rol,
         CASE WHEN u.rol='Profesor' THEN l.capacidad_personas ELSE l.total_equipos END
    INTO v_rol,v_capacidad FROM laboratorios l JOIN usuario u ON u.id=NEW.id_usuario
   WHERE l.id_laboratorio=NEW.id_laboratorio FOR UPDATE;
  IF NEW.cantidad_alumnos>v_capacidad THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='La cantidad de usuarios excede la capacidad del laboratorio';
  END IF;
  IF NEW.estado IN ('Pendiente','Aprobada') THEN
    IF v_rol='Profesor' AND EXISTS (
      SELECT 1 FROM reservas r WHERE r.id_laboratorio=NEW.id_laboratorio
        AND r.fecha=NEW.fecha AND r.estado IN ('Pendiente','Aprobada')
        AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='El laboratorio ya tiene ocupación activa en ese horario';
    ELSEIF v_rol<>'Profesor' AND EXISTS (
      SELECT 1 FROM reservas r JOIN usuario u ON u.id=r.id_usuario
       WHERE r.id_laboratorio=NEW.id_laboratorio AND r.fecha=NEW.fecha
         AND r.estado IN ('Pendiente','Aprobada') AND u.rol='Profesor'
         AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='El laboratorio está reservado por un profesor en ese horario';
    ELSEIF v_rol<>'Profesor' THEN
      SELECT COALESCE(SUM(r.cantidad_alumnos),0) INTO v_ocupacion
        FROM reservas r JOIN usuario u ON u.id=r.id_usuario
       WHERE r.id_laboratorio=NEW.id_laboratorio AND r.fecha=NEW.fecha
         AND r.estado IN ('Pendiente','Aprobada') AND u.rol<>'Profesor'
         AND NEW.hora_inicio<r.hora_fin AND NEW.hora_fin>r.hora_inicio;
      IF v_ocupacion+NEW.cantidad_alumnos>v_capacidad THEN
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT='No hay suficientes equipos disponibles en ese horario';
      END IF;
    END IF;
  END IF;
END;

