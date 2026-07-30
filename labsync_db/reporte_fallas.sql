create table reporte_fallas
(
    id_falla          int auto_increment
        primary key,
    id_usuario        int                                                                                  null,
    id_inventario     int                                                                                  null,
    id_reserva        int                                                                                  null,
    id_laboratorio    int                                                                                  not null,
    descripcion_falla text                                                                                 not null,
    prioridad         enum ('Baja', 'Media', 'Alta', 'Crítica')                  default 'Media'           not null,
    estado            enum ('Pendiente', 'En revisión', 'Atendida', 'Cancelada') default 'Pendiente'       not null,
    fecha_reporte     timestamp                                                  default CURRENT_TIMESTAMP not null,
    fecha_revision    date                                                                                 null,
    observaciones     text                                                                                 null,
    constraint fk_falla_inventario
        foreign key (id_inventario) references inventario (id_inventario)
            on update cascade on delete set null,
    constraint fk_falla_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on update cascade,
    constraint fk_falla_reserva
        foreign key (id_reserva) references reservas (id_reserva)
            on update cascade on delete set null,
    constraint fk_falla_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete set null
)
    engine = InnoDB;

create index idx_falla_estado_fecha
    on reporte_fallas (estado, fecha_reporte);

create index idx_falla_inventario
    on reporte_fallas (id_inventario);

create index idx_falla_laboratorio_estado
    on reporte_fallas (id_laboratorio, estado);

