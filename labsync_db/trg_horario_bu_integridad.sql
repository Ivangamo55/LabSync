create definer = root@localhost trigger trg_horario_bu_integridad
    before update
    on horarios_clase
    for each row
BEGIN
  IF (SELECT rol FROM usuario WHERE id=NEW.id_profesor) <> 'Profesor' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El usuario asignado al horario no es profesor';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM grupos g JOIN plan_materias p
      ON p.id_trayectoria=g.id_trayectoria AND p.cuatrimestre=g.cuatrimestre
     WHERE g.id_grupo=NEW.id_grupo AND p.id_plan_materia=NEW.id_plan_materia
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='La materia no pertenece al plan y cuatrimestre del grupo';
  END IF;
  IF NEW.activo=1 AND EXISTS (
    SELECT 1 FROM horarios_clase h WHERE h.id_horario<>OLD.id_horario
      AND h.activo=1 AND h.id_ciclo=NEW.id_ciclo AND h.dia_semana=NEW.dia_semana
      AND (h.id_laboratorio=NEW.id_laboratorio OR h.id_profesor=NEW.id_profesor
           OR h.id_grupo=NEW.id_grupo)
      AND NEW.hora_inicio < h.hora_fin AND NEW.hora_fin > h.hora_inicio
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existe un horario activo traslapado';
  END IF;
END;

