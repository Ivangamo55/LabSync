create table materias
(
    id_materia int auto_increment
        primary key,
    nombre     varchar(150) not null,
    constraint uk_materia_nombre
        unique (nombre)
)
    engine = InnoDB;

