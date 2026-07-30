create table trayectorias
(
    id_trayectoria int auto_increment
        primary key,
    codigo         varchar(20)                not null,
    nombre         varchar(150)               not null,
    nivel          enum ('TSU', 'Ingeniería') not null,
    activa         tinyint(1) default 1       not null,
    constraint uk_trayectoria_codigo
        unique (codigo),
    constraint uk_trayectoria_nombre
        unique (nombre)
)
    engine = InnoDB;

