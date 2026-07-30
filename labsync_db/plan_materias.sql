create table plan_materias
(
    id_plan_materia int auto_increment
        primary key,
    id_trayectoria  int              not null,
    cuatrimestre    tinyint unsigned not null,
    id_materia      int              not null,
    orden           tinyint unsigned not null,
    constraint uk_plan_materia
        unique (id_trayectoria, cuatrimestre, id_materia),
    constraint uk_plan_orden
        unique (id_trayectoria, cuatrimestre, orden),
    constraint fk_plan_materia
        foreign key (id_materia) references materias (id_materia)
            on update cascade,
    constraint fk_plan_trayectoria
        foreign key (id_trayectoria) references trayectorias (id_trayectoria)
            on update cascade,
    constraint chk_plan_cuatrimestre
        check (`cuatrimestre` between 1 and 11)
)
    engine = InnoDB;

