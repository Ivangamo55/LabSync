create table alertas
(
    id_alerta        int auto_increment
        primary key,
    id_reserva       int                                                                 null,
    id_falla         int                                                                 null,
    id_mantenimiento int                                                                 null,
    id_inventario    int                                                                 null,
    tipo             varchar(40)                                                         not null,
    referencia       varchar(50)                                                         not null,
    titulo           varchar(120)                                                        not null,
    detalle          varchar(500)                                                        not null,
    prioridad        enum ('Baja', 'Media', 'Alta', 'Crítica') default 'Media'           not null,
    estado           enum ('Nueva', 'Leída', 'Atendida')       default 'Nueva'           not null,
    fecha_creacion   timestamp                                 default CURRENT_TIMESTAMP not null,
    fecha_lectura    timestamp                                                           null,
    fecha_atencion   timestamp                                                           null,
    constraint uk_alerta_origen
        unique (tipo, referencia),
    constraint fk_alerta_falla
        foreign key (id_falla) references reporte_fallas (id_falla)
            on delete cascade,
    constraint fk_alerta_inventario
        foreign key (id_inventario) references inventario (id_inventario)
            on delete cascade,
    constraint fk_alerta_mantenimiento
        foreign key (id_mantenimiento) references mantenimiento (id_mantenimiento)
            on delete cascade,
    constraint fk_alerta_reserva
        foreign key (id_reserva) references reservas (id_reserva)
            on delete cascade,
    constraint chk_alerta_origen_unico
        check (((((`id_reserva` is not null) + (`id_falla` is not null)) + (`id_mantenimiento` is not null)) +
                (`id_inventario` is not null)) = 1)
)
    engine = InnoDB
    default character set = utf8mb4
    collate = utf8mb4_unicode_ci;

create index idx_alerta_estado_prioridad
    on alertas (estado, prioridad);
