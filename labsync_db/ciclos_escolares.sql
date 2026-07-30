create table ciclos_escolares
(
    id_ciclo     int auto_increment
        primary key,
    nombre       varchar(50)          not null,
    fecha_inicio date                 not null,
    fecha_fin    date                 not null,
    activo       tinyint(1) default 1 not null,
    constraint uk_ciclo_nombre
        unique (nombre),
    constraint chk_ciclo_fechas
        check (`fecha_fin` >= `fecha_inicio`)
)
    engine = InnoDB;

