create table laboratorista
(
    id_usuario     int                                      not null
        primary key,
    turno          enum ('Matutino', 'Vespertino', 'Mixto') not null,
    piso_encargado varchar(50)                              not null,
    constraint fk_laboratorista_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete cascade
)
    engine = InnoDB;

