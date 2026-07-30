create table estudiante
(
    id_usuario     int                                      not null
        primary key,
    matricula      varchar(50)                              not null,
    id_trayectoria int                                      not null,
    turno          enum ('Matutino', 'Vespertino', 'Mixto') null,
    constraint uk_estudiante_matricula
        unique (matricula),
    constraint fk_estudiante_trayectoria
        foreign key (id_trayectoria) references trayectorias (id_trayectoria)
            on update cascade,
    constraint fk_estudiante_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete cascade
)
    engine = InnoDB;

