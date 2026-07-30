create table externo
(
    id_usuario         int          not null
        primary key,
    institucion_origen varchar(150) not null,
    motivo_visita      text         null,
    constraint fk_externo_usuario
        foreign key (id_usuario) references usuario (id)
            on update cascade on delete cascade
)
    engine = InnoDB;

