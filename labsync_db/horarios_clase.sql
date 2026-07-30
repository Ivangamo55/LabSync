create table horarios_clase
(
    id_horario      int auto_increment
        primary key,
    id_ciclo        int                                                                  not null,
    id_grupo        int                                                                  not null,
    id_plan_materia int                                                                  not null,
    id_profesor     int                                                                  not null,
    id_laboratorio  int                                                                  not null,
    dia_semana      enum ('Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado') not null,
    hora_inicio     time                                                                 not null,
    hora_fin        time                                                                 not null,
    activo          tinyint(1) default 1                                                 not null,
    constraint uk_horario_clase
        unique (id_ciclo, id_grupo, id_plan_materia, dia_semana, hora_inicio),
    constraint fk_horario_ciclo
        foreign key (id_ciclo) references ciclos_escolares (id_ciclo)
            on update cascade,
    constraint fk_horario_grupo
        foreign key (id_grupo) references grupos (id_grupo)
            on update cascade,
    constraint fk_horario_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on update cascade,
    constraint fk_horario_plan
        foreign key (id_plan_materia) references plan_materias (id_plan_materia)
            on update cascade,
    constraint fk_horario_profesor
        foreign key (id_profesor) references usuario (id)
            on update cascade,
    constraint chk_horario_horas
        check (`hora_fin` > `hora_inicio`)
)
    engine = InnoDB;

create index idx_horario_laboratorio
    on horarios_clase (id_ciclo, id_laboratorio, dia_semana, hora_inicio, hora_fin);

create index idx_horario_profesor
    on horarios_clase (id_ciclo, id_profesor, dia_semana, hora_inicio, hora_fin);

