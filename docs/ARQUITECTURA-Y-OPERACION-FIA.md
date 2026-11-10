# FIA Académico: arquitectura, operación y ocho mejoras

Esta guía adapta la propuesta comercial a los procesos reales del repositorio. **No existen ventas, compras, OSE ni stock comercial** en FIA Académico. Los equivalentes son plan y curso, asignación de carrera, matrícula cabecera/detalle, cupos, pago y comprobante de demostración.

## 1. Rendimiento y paginación

- `GET /api/v1/estudiantes/pagina?buscar=ana&pagina=0&tamano=25` y `GET /api/v1/matriculas/pagina?periodo=2026-1&estado=PENDIENTE&planId=1&buscar=ana&pagina=0&tamano=25` devuelven `items`, `total`, `pagina`, `tamano`, `paginas`. El servidor rechaza tamaños fuera de 1–100.
- La SPA pagina las listas principales de estudiantes y matrículas. Al seleccionar una matrícula consulta `/api/v1/matriculas/{id}` para cargar los detalles, en lugar de incluirlos en cada fila.
- El reporte de matrícula agrega montos en SQL; el conteo de cupos usa una consulta SQL sobre `FIA_MATRICULA_DET` y `FIA_MATRICULA`, evitando cargar todas las cabeceras y detalles en memoria.
- Ejecutar `database/migracion-v6-rendimiento-notificaciones-oracle.sql` después de medir con un volumen representativo. Los índices propuestos cubren orden de matrícula, detalle por curso, solicitudes por periodo, pagos por estado y búsquedas de estudiantes. Los patrones `%texto%` no aprovechan plenamente índices B-tree: para búsqueda amplia de nombres evaluar Oracle Text cuando el volumen lo justifique.
- Medir p50/p95 de `/matriculas/pagina`, `/estudiantes/pagina` y `/alumno/cupos` antes y después de la migración; revisar planes con `DBMS_XPLAN.DISPLAY_CURSOR` y estadísticas de tabla actualizadas. Evitar añadir índices no usados porque ralentizan escrituras.

## 2. Dueño de reportes

La ruta compatible `/api/v1/reportes/matriculas` se conserva, pero su consulta y agregación viven en `matricula/internal/MatriculaService`. Construye la consulta JPA con parámetros solo para los filtros presentes; no carga toda la lista de matrícula. El resultado mantiene `total`, `monto`, `pagado`, `saldo`, `matriculas` y añade metadatos de página. Reportes futuros de planes deben consultar el módulo Planes, y los de carga académica el módulo Carga.

```java
Query page = consultaMatriculas("select m, s.nombre", periodo, estado, planId, buscar);
page.setFirstResult(pagina * tamano).setMaxResults(tamano);
// Cabecera y nombre se recuperan juntos. Los detalles se consultan por ID.
```

## 3. Pruebas E2E y transacciones

Flujo principal: administrador crea alumno y asigna plan/periodo; registra tarifa; alumno completa teléfono, correo y apoderado; selecciona cursos con horario, cupos y máximo 25 créditos; el sistema genera matrícula cabecera/detalle y reserva cupos; alumno declara pago de contado o cuotas con voucher; administrador confirma; alumno ve comprobante de demostración y contrato. El pago rechazado conserva saldo y permite nuevo intento.

Casos negativos: carrera no asignada, prerrequisito incumplido, curso sin horario, cruce de horario, cupos agotados, 26 créditos, pago mayor al saldo, voucher inválido, confirmación repetida. En cada rechazo comprobar que no cambian `FIA_MATRICULA`, `FIA_MATRICULA_DET`, `FIA_PAGO` ni los cupos. La prueba de integración `MatriculaIntegrationTests` verifica que un pago superior al saldo no persiste y que la consulta paginada devuelve la cabecera esperada. `frontend/e2e/matricula.spec.ts` prepara datos aislados por ejecución y prueba en navegador la matrícula, el pago Yape, la revisión y el contrato; incluye un caso de 26 créditos. Ejecutar con el backend de demostración levantado y el portal compilado:

```powershell
cd frontend
npm ci
npx playwright install chromium
$env:FIA_BASE_URL="http://localhost:8080"
$env:FIA_ADMIN_PASSWORD="<contraseña de la demo>"
npm run test:e2e
```

La prueba deja registros de demostración; usar una base H2 descartable o restaurar el snapshot de prueba. Ampliar la suite con una carrera no asignada, cupos agotados, choque de horario, voucher rechazado y confirmación concurrente.

## 4. Estados y errores en la SPA

Mostrar en cada matrícula `PENDIENTE`, `PARCIAL` o `PAGADA`, junto a monto, pagado y saldo. Para pagos, mostrar `PENDIENTE_VERIFICACION`, `CONFIRMADO` o `RECHAZADO` y la observación del administrador. En la selección de cursos, mostrar horario y cupos disponibles; explicar al alumno qué prerrequisito o cruce impide continuar. El backend devuelve `ProblemDetail` con `detail`, `requestId` y `accionSugerida`. Soporte debe pedir el Request ID, nunca la contraseña ni la imagen del voucher.

## 5. Observabilidad

`RequestLog` acepta un `X-Request-ID` válido o genera UUID, lo coloca en MDC, lo devuelve en la respuesta y registra método, ruta, estado y duración. Expone `fia.request.duration` y `fia.request.errors` por operación, método y estado, evitando etiquetas de alta cardinalidad como estudiante o URL completa. Spring Actuator ya expone `health`, `info` y `metrics`; para producción integrar un recolector de métricas y centralizar logs JSON con retención y control de acceso.

Alertas iniciales: error HTTP 5xx sostenido, p95 de matrícula o pago fuera del objetivo acordado, aumento de pagos pendientes y fallos de notificación. Nunca escribir contraseñas, tokens ni bytes del voucher en logs.

## 6. Integraciones aisladas

`CanalNotificacion` es el puerto para aviso de resultado de pago. `CanalNotificacionDemo` no envía mensajes externos. Tras confirmar o rechazar un pago, el evento `PagoRevisado` se procesa **después del commit**, en una transacción nueva, y registra el intento en `FIA_NOTIFICACION_INTENTO` con destino, canal, estado, resultado y fecha. Un adaptador futuro de correo o WhatsApp puede implementar el mismo puerto, con idempotencia por pago/estado, reintentos limitados y credenciales fuera del repositorio. **OSE no aplica** a este proyecto académico; el comprobante actual es de demostración, sin validez tributaria. Para cobro real se requeriría un proveedor de pagos y conciliación bancaria.

## 7. Operación, Keycloak, Oracle y migraciones

### Keycloak

Modo local: sesiones de administrador y alumno manejadas por la aplicación. Modo `keycloak`: definir `KEYCLOAK_ISSUER_URI`, crear realm `fia`, cliente OIDC `fia-api` y roles `ADMIN` y `STUDENT`. El conversor solo toma roles de realm y del cliente `fia-api`; roles de otros clientes no elevan permisos. Verificar que `/api/v1/alumno/**` exija `STUDENT` y las operaciones administrativas `ADMIN`. El portal actual usa el inicio de sesión local, por lo que **antes de usar Keycloak en la SPA** hay que integrar Authorization Code + PKCE y sustituir la sesión local por tokens Bearer; el modo Keycloak ya protege la API, no completa esa integración de navegador. Configurar HTTPS, issuer estable, cliente público para SPA y cliente separado para tareas servidor. No guardar secretos en código.

### Oracle y recuperación

Aplicar `oracle.sql` solo en instalación nueva y después las migraciones que falten según el esquema real. En una instalación anterior, aplicar `migracion-v2` y `v3` si corresponde; `v4` solo si `FIA_ALUMNO_CUENTA.PUEDE_EDITAR_CARRERA` aún falta, porque algunas versiones de `v3` ya la incluyen. Aplicar `v5` si no existe `FIA_SOLICITUD_MATRICULA` y luego `migracion-v6-rendimiento-notificaciones-oracle.sql`, que completa columnas de revisión de pago si faltan. Registrar en una tabla de control o herramienta de migración el número, checksum, fecha y operador de cada versión antes de automatizar despliegues. No ejecutar `oracle.sql` encima de un esquema existente. Probar cada migración en copia restaurada y verificar `spring.jpa.hibernate.ddl-auto=validate` en producción.

Respaldo físico de ejemplo por un DBA, con destino y política de retención definidos por la institución:

```sql
RMAN> BACKUP DATABASE PLUS ARCHIVELOG;
RMAN> RESTORE DATABASE VALIDATE;
```

Restauración ante desastre: detener escritura de la aplicación; verificar el respaldo y las credenciales de cifrado; restaurar control file si corresponde; ejecutar `RESTORE DATABASE` y `RECOVER DATABASE` bajo procedimiento DBA; abrir base según el escenario de recuperación; validar conteos y claves de `FIA_ESTUDIANTE`, `FIA_MATRICULA`, `FIA_MATRICULA_DET`, `FIA_PAGO` y `FIA_NOTIFICACION_INTENTO`; iniciar la aplicación; revisar `/actuator/health` y un flujo de consulta. Ensayar periódicamente con una instancia aislada. El respaldo de Oracle no sustituye respaldo de archivos externos si los vouchers se migran fuera de la base de datos.

### Incidente de aplicación

1. Registrar hora, versión, Request ID y operación fallida.
2. Consultar health, tasa de errores y logs por Request ID. Para falla de DB, verificar conectividad y espacio antes de reintentar operaciones.
3. Si falló una migración, detener despliegue y restaurar el respaldo o aplicar una migración correctiva aprobada; no editar tablas manualmente en producción.
4. Si falló una integración de aviso, la matrícula y el pago permanecen confirmados; consultar `FIA_NOTIFICACION_INTENTO` y reintentar solo el aviso.
5. Tras recuperación, probar consulta de matrícula, cupos, pago y comprobante con datos de prueba.

## 8. Demo basada en el negocio académico

Usar `docs/DEMO-ACADEMICA.md`. Presentar primero el problema de cupos, cruces y pagos; luego recorrer plan → alumno → matrícula → pago → revisión → comprobante. Cerrar con un intento de 26 créditos o cruce de horario y mostrar que no se generan detalle ni cobro. Así se explica el valor transaccional del sistema y la separación entre roles, no solo pantallas aisladas.

## Referencias oficiales

- [Spring Data JPA: páginas y slices](https://docs.spring.io/spring-data/jpa/reference/repositories/query-return-types-reference.html)
- [Keycloak: administración de realms, clientes y roles](https://www.keycloak.org/docs/26.5.7/server_admin/index.html)
- [Oracle: respaldo y recuperación con RMAN](https://docs.oracle.com/en/database/oracle/oracle-database/19/bradv/getting-started-rman.html)
- [Playwright: aserciones de navegador](https://playwright.dev/docs/test-assertions)
