create table usuario
(
    id         int auto_increment
        primary key,
    nombre     varchar(80)                                                 not null,
    apellido_p varchar(80)                                                 not null,
    apellido_m varchar(80)                                                 null,
    rol        enum ('Estudiante', 'Profesor', 'Laboratorista', 'Externo') not null,
    correo     varchar(150)                                                not null,
    password   char(64)                                                    not null comment 'Hash SHA-256 hexadecimal heredado; migrar a Argon2id/bcrypt con sal',
    constraint uk_usuario_correo
        unique (correo)
)
    engine = InnoDB;

