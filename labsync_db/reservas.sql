create table reservas
(
    id_reserva       int auto_increment
        primary key,
    id_usuario       int                                                                                              not null,
    id_laboratorio   int                                                                                              not null,
    actividad        varchar(150)                                                                                     not null,
    carrera          varchar(100)                                                                                     null,
    grado            varchar(20)                                                                                      null,
    grupo            varchar(20)                                                                                      null,
    turno            varchar(50)                                                                                      null,
    fecha            date                                                                                             not null,
    hora_inicio      time                                                                                             not null,
    hora_fin         time                                                                                             not null,
    cantidad_alumnos int unsigned                                                                                     not null,
    estado           enum ('Pendiente', 'Aprobada', 'Rechazada', 'Cancelada', 'Finalizada') default 'Pendiente'       not null,
    observaciones    text                                                                                             null,
    fecha_registro   timestamp                                                              default CURRENT_TIMESTAMP not null,
    constraint fk_reservas_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on update cascade,
    constraint fk_reservas_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade,
    constraint chk_reservas_horas
        check (`hora_fin` > `hora_inicio`),
    constraint chk_reservas_personas_positivas
        check (`cantidad_alumnos` > 0)
)
    engine = InnoDB;

create index idx_reservas_fecha_estado
    on reservas (fecha, estado);

create index idx_reservas_laboratorio_fecha_estado
    on reservas (id_laboratorio, fecha, estado);

create index idx_reservas_usuario_fecha
    on reservas (id_usuario, fecha);

