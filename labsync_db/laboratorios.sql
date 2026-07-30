create table laboratorios
(
    id_laboratorio     int auto_increment
        primary key,
    nombre             varchar(20)                                                                   not null,
    total_equipos      int unsigned                                                                  not null comment 'Estaciones de cómputo disponibles para reservas individuales',
    capacidad_personas int unsigned                                                                  not null comment 'Aforo físico máximo para reservas grupales',
    estado             enum ('Disponible', 'No disponible', 'En mantenimiento') default 'Disponible' not null,
    constraint uk_laboratorio_nombre
        unique (nombre),
    constraint chk_laboratorio_capacidad
        check (`capacidad_personas` > 0),
    constraint chk_laboratorio_equipos
        check (`total_equipos` > 0)
)
    engine = InnoDB;

