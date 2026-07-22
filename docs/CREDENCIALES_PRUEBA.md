# Credenciales de prueba de horarios UTJ-CCD

La migración `20260722_agregar_horarios_escolares_ccd.sql` crea catorce cuentas
ficticias con rol `Profesor`. Todas usan, exclusivamente para pruebas locales:

```text
Contraseña: LabSync-CCD-2026!
```

| Grupo asignado | Correo |
|---|---|
| DSM 1.º A matutino | `prueba.dsm1@utj.edu.mx` |
| DSM 2.º A matutino | `prueba.dsm2@utj.edu.mx` |
| DSM 3.º A matutino | `prueba.dsm3@utj.edu.mx` |
| DSM 4.º A matutino | `prueba.dsm4@utj.edu.mx` |
| DSM 5.º A matutino | `prueba.dsm5@utj.edu.mx` |
| EVND 1.º A matutino | `prueba.evnd1@utj.edu.mx` |
| EVND 2.º A matutino | `prueba.evnd2@utj.edu.mx` |
| EVND 3.º A matutino | `prueba.evnd3@utj.edu.mx` |
| EVND 4.º A matutino | `prueba.evnd4@utj.edu.mx` |
| EVND 5.º B vespertino | `prueba.evnd5@utj.edu.mx` |
| Ingeniería TI 7.º B vespertino | `prueba.iti7@utj.edu.mx` |
| Ingeniería TI 8.º B vespertino | `prueba.iti8@utj.edu.mx` |
| Ingeniería TI 9.º B vespertino | `prueba.iti9@utj.edu.mx` |
| Ingeniería TI 10.º B vespertino | `prueba.iti10@utj.edu.mx` |

Las contraseñas se almacenan con SHA-256, utilizando exactamente el mismo
mecanismo de autenticación que las cuentas existentes de LabSync.

Estas cuentas no deben utilizarse en producción. Cambia o elimina sus
credenciales después de terminar las demostraciones.
