create table mantenimiento
(
    id_mantenimiento   int auto_increment
        primary key,
    id_inventario      int                                                                                  not null,
    id_falla           int                                                                                  null,
    tipo_mantenimiento varchar(100)                                                                         not null,
    fecha_programada   date                                                                                 not null,
    estado             enum ('Pendiente', 'En proceso', 'Realizado', 'Cancelado') default 'Pendiente'       not null,
    responsable        varchar(150)                                                                         not null,
    observaciones      text                                                                                 null,
    fecha_registro     timestamp                                                  default CURRENT_TIMESTAMP not null,
    constraint fk_mantenimiento_falla
        foreign key (id_falla) references reporte_fallas (id_falla)
            on update cascade on delete set null,
    constraint fk_mantenimiento_inventario
        foreign key (id_inventario) references inventario (id_inventario)
            on update cascade
)
    engine = InnoDB;

create index idx_mantenimiento_estado_fecha
    on mantenimiento (estado, fecha_programada);

create index idx_mantenimiento_inventario
    on mantenimiento (id_inventario);

