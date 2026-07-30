create table grupos
(
    id_grupo       int auto_increment
        primary key,
    id_trayectoria int                             not null,
    cuatrimestre   tinyint unsigned                not null,
    letra          varchar(10)                     not null,
    turno          enum ('Matutino', 'Vespertino') not null,
    activo         tinyint(1) default 1            not null,
    constraint uk_grupo
        unique (id_trayectoria, cuatrimestre, letra, turno),
    constraint fk_grupo_trayectoria
        foreign key (id_trayectoria) references trayectorias (id_trayectoria)
            on update cascade,
    constraint chk_grupo_cuatrimestre
        check (`cuatrimestre` between 1 and 11)
)
    engine = InnoDB;

