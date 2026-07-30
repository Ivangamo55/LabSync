create table inventario
(
    id_inventario        int auto_increment
        primary key,
    id_laboratorio       int                                                                                    not null,
    codigo               varchar(50)                                                                            not null,
    nombre_equipo        varchar(100)                                                                           not null,
    tipo_dispositivo     varchar(50)                                                                            not null,
    marca                varchar(80)                                                                            null,
    modelo               varchar(80)                                                                            null,
    no_serie             varchar(100)                                                                           null,
    estado               enum ('Disponible', 'En mantenimiento', 'Con falla', 'Baja') default 'Disponible'      not null,
    ultimo_mantenimiento date                                                                                   null,
    observaciones        text                                                                                   null,
    fecha_registro       timestamp                                                    default CURRENT_TIMESTAMP not null,
    constraint uk_inventario_codigo
        unique (codigo),
    constraint uk_inventario_serie
        unique (no_serie),
    constraint fk_inventario_laboratorio
        foreign key (id_laboratorio) references laboratorios (id_laboratorio)
            on update cascade
)
    engine = InnoDB;

create index idx_inventario_laboratorio
    on inventario (id_laboratorio);

