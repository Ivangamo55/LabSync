# Credenciales ficticias de demostración

La instalación completa requiere únicamente importar:

```text
labsync_db/labsync_db.sql
```

El archivo incluye datos mínimos ficticios y una cuenta de laboratorista que
permite recorrer los módulos administrativos:

```text
Correo: laboratorista.demo@labsync.example
Contraseña: LabSyncDemo2026!
Rol: Laboratorista
```

También se incluyen un profesor y un estudiante ficticios, laboratorios, ciclo,
horario, inventario, software, reserva, falla y mantenimiento. La contraseña se
guarda como hash SHA-256 hexadecimal mediante el mecanismo que utiliza LabSync;
no se almacena en texto plano.

Estas identidades y datos son únicamente para demostraciones locales. No deben
utilizarse en producción.
