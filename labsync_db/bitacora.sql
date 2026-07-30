create table bitacora
(
    id_bitacora         int auto_increment
        primary key,
    id_usuario          int                                   null,
    id_reserva          int                                   null,
    id_horario          int                                   null,
    id_laboratorio      int                                   null,
    fecha               date                                  not null,
    nombre_usuario      varchar(240)                          not null comment 'Fotografía histórica',
    rol_usuario         varchar(50)                           not null comment 'Fotografía histórica',
    carrera_dependencia varchar(100)                          not null,
    grado               varchar(20)                           null,
    grupo               varchar(20)                           null,
    laboratorio         varchar(50)                           not null comment 'Fotografía histórica del recurso usado',
    actividad_materia   varchar(150)                          not null,
    turno               varchar(50)                           not null,
    horario             varchar(50)                           not null,
    total_usuarios      int unsigned                          not null,
    observaciones       text                                  null,
    estado              varchar(50) default 'Registrado'      not null,
    fecha_registro      timestamp   default CURRENT_TIMESTAMP not null,
    constraint uk_bitacora_horario_fecha
        unique (id_horario, fecha),
    constraint fk_bitacora_horario
        foreign key (id_horario) references horarios_clase (id_horario)
            on update cascade on delete set null,
    constraint fk_bitacora_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on delete set null,
    constraint fk_bitacora_reserva
        foreign key (id_reserva) references reservas (id_reserva)
            on update cascade on delete set null,
    constraint fk_bitacora_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete set null,
    constraint chk_bitacora_personas_positivas
        check (`total_usuarios` > 0)
)
    engine = InnoDB;

create index idx_bitacora_fecha_registro
    on bitacora (fecha_registro);

create index idx_bitacora_usuario_fecha
    on bitacora (id_usuario, fecha);

