# Plan de estudios y carga académica de FIA

En **Planes académicos → Nuevo/Editar curso**, el horario se elige en una tabla de días y bloques de una hora, también operable con teclado. Se pueden marcar varios bloques, ajustar sus horas exactas o quitarlos; el resumen muestra el formato enviado a la API. El formulario impide bloques superpuestos del mismo curso y muestra junto a la tabla los cruces que detecta el servidor con cursos del mismo plan y ciclo. Al dejarlo vacío, se mantiene la propuesta automática. El campo **Periodo para cupos** sigue disponible. **Planes académicos**, **Organización docente** y **Cuentas de profesores** comparten tarjetas, formularios, estados y navegación adaptable. Las pruebas de navegador están en `frontend/e2e/academic-schedule.spec.ts` y `frontend/e2e/admin-consistency.spec.ts`; las capturas están en `docs/evidencias-ui-20261109/`.

La [evidencia LP2 Unidad 3 del 7 de noviembre de 2026](docs/EVIDENCIA-LP2-U3-20261107.md) documenta la línea base del build, caché HTTP, decisión sobre Redis, paginación y agregados, auditoría, monitoreo, pruebas de navegador y bitácora de estabilización. Distingue comprobaciones H2 y pendientes Oracle.

La [evidencia actualizada del 8 de noviembre](docs/EVIDENCIA-LP2-U3-20261108.md) añade carga paginada comparable en H2, pruebas de varias páginas, auditoría del cambio, E2E y guía de sustentación. La [validación Oracle aislada](docs/VALIDACION-ORACLE-AISLADA.md) está preparada y pendiente de una instancia de pruebas exclusiva. La nueva columna de auditoría se agrega con `database/migracion-v8-resumen-auditoria-oracle.sql` solo después de v7.

## Versión v7: profesores, alumnos y planes

La guía [Profesores, alumnos y planes](docs/PROFESORES-ALUMNOS-PLANES.md) documenta la arquitectura, los roles `ADMIN`, `TEACHER` y `STUDENT`, las pantallas, las validaciones, los endpoints, la auditoría de cargas y la migración Oracle v7. Para una base existente, ejecutar `database/migracion-v7-profesores-auditoria-oracle.sql` **después de v6**. Para un esquema vacío, usar solamente `database/oracle.sql`.

La cuenta de profesor se crea en **Cuentas de profesores** vinculada a un docente ya registrado. El profesor ve únicamente su perfil y sus asignaciones. El alumno puede actualizar correo, teléfono y apoderado, y consultar su historial de matrículas, pagos y saldos. La pantalla de planes muestra un resumen por ciclo antes de guardar y el servidor protege las relaciones con cursos y cargas. Las contraseñas de administrador y Oracle se suministran mediante `ADMIN_PASSWORD` y `ORACLE_PASSWORD`; el perfil `demo` mantiene credenciales explícitamente de demostración.

## Mejoras de arquitectura y operación

La guía [Arquitectura y operación FIA](docs/ARQUITECTURA-Y-OPERACION-FIA.md) explica las ocho mejoras adaptadas a este proyecto: páginas de estudiantes y matrículas, consultas y cupos optimizados, reportes del módulo dueño, pruebas E2E, estados y errores, Request ID y métricas, avisos simulados persistidos, Keycloak y Oracle. El [guion de demo académica](docs/DEMO-ACADEMICA.md) recorre plan → carrera → matrícula → pago → revisión → contrato. La migración Oracle nueva es [v6](database/migracion-v6-rendimiento-notificaciones-oracle.sql).

El JAR actualizado `target/fia-academico-1.0.0.jar` incluye el portal Angular compilado. Para la demostración local:

```powershell
java -jar target/fia-academico-1.0.0.jar --spring.profiles.active=demo --server.port=18083
```

Abrir `http://localhost:18083/portal/`. Los endpoints nuevos `/api/v1/estudiantes/pagina` y `/api/v1/matriculas/pagina` aceptan `pagina` (desde 0) y `tamano` (1–100). El modo Keycloak protege la API Bearer; el portal Angular todavía requiere integrar Authorization Code + PKCE para SSO.

El portal local usa rutas Angular con guarda administrativa para **Planes académicos** (`/portal/#/planes-academicos`) y **Organización docente** (`/portal/#/organizacion-docente`). La sesión de aplicación expira tras 30 minutos de inactividad y su cookie es HttpOnly y SameSite=Lax. Las escrituras locales llevan token CSRF; Angular maneja 401 y 403 de forma centralizada. En HTTPS, establece `SESSION_COOKIE_SECURE=true`. La integración del portal con JWT/Keycloak mediante Authorization Code + PKCE sigue pendiente. Consulta [Pantallas administrativas y seguridad](docs/PANTALLAS-ADMIN-Y-SEGURIDAD.md).

## Portal Angular: administración y matrícula del alumno

El portal nuevo se abre en `http://localhost:8080/portal/`. La pantalla anterior de planes y carga sigue en `http://localhost:8080/` y tiene un enlace al portal. El código Angular y los modelos TypeScript están en `frontend/src/app/`; el cliente HTTP de Angular consume la API REST del mismo Spring Boot. Los archivos compilados ya están incluidos en `src/main/resources/static/portal/`, así que **no hace falta Node para usar el portal compilado**.

1. **Administración:** los módulos de planes académicos y organización docente permiten estructurar cursos por ciclo, créditos, prerrequisitos, horarios y cupos, y asignar docentes por escuela y curso. También se administran horarios, tarifas, cuentas de alumnos y asignaciones de carrera/periodo.
2. **Alumno, datos personales:** completa correo, teléfono y apoderado en su cuenta.
3. **Alumno, cursos y horario:** elige cursos de su plan asignado, ve cupos y horario; se validan prerrequisitos, cruces y el máximo de 25 créditos. La selección genera matrícula y reserva cupos sin aprobación adicional del administrador.
4. **Alumno, pago y documentos:** elige contado o cuotas y declara efectivo, Yape o transferencia con su comprobante. El administrador revisa el pago; tras confirmarlo, el alumno ve la boleta de demostración y el contrato de matrícula.
5. **Consultas administrativas:** estudiantes y matrículas se presentan en páginas; el detalle de cada matrícula se consulta por separado. El reporte resume montos, pagos y saldos.

Para modificar Angular en VS Code, instala Node compatible con Angular 22 y ejecuta:

```powershell
cd frontend
npm ci
npm start
```

El servidor de desarrollo abre `http://localhost:4200/` y redirige `/api` a Spring Boot en el puerto 8080. Antes de empaquetar el backend después de un cambio de interfaz, vuelve a la carpeta raíz y ejecuta `./publicar-angular.ps1`; después, `mvn package`. La versión empaquetada se abre en `/portal/`.

El esquema Oracle ya incluye las tablas `FIA_ESTUDIANTE`, `FIA_ELECCION_CARRERA`, `FIA_TARIFA`, `FIA_MATRICULA`, `FIA_MATRICULA_DET` y `FIA_PAGO`. Para una base nueva ejecuta `database/oracle.sql`; si el esquema FIA ya tiene planes y cargas, ejecuta una sola vez `database/migracion-matricula-oracle.sql`. Consulta `database/README-MATRICULA.md` antes de ejecutar scripts sobre una base existente.

## Oracle compartido con BomERP Local

FIA Académico se conecta al mismo Oracle de BomERP Local (`127.0.0.1:1521/FREEPDB1`), pero no usa las tablas de BomERP. Oracle separa los datos mediante esquemas: `BOMERP_APP` continúa siendo el esquema de BomERP y `FIA_APP` es el esquema nuevo y exclusivo de FIA.

En tu cliente Oracle, conectado como `SYSTEM` a `FREEPDB1` tal como muestran tus capturas, ejecuta en este orden:

1. `database/01-crear-usuario-fia.sql` para crear `FIA_APP` y concederle los permisos mínimos.
2. Crea una conexión nueva con usuario `FIA_APP`, la contraseña que estableciste al ejecutar el script, host `127.0.0.1`, puerto `1521` y base `FREEPDB1`. Configura `FIA_ORACLE_PASSWORD` en el entorno local.
3. En esa nueva conexión ejecuta `database/oracle.sql`; allí se crean solamente las tablas, índices y secuencias `FIA_*`.
4. Ejecuta `database/03-verificar-fia.sql` y confirma que aparecen las tablas FIA.

Inicia el proyecto con `mvn spring-boot:run`. Por defecto ya emplea esas credenciales. Puedes sustituirlas sin editar código con `BOMERP_ORACLE_URL`, `FIA_ORACLE_USER` y `FIA_ORACLE_PASSWORD`.

Swagger queda disponible en `http://localhost:8080/swagger-ui.html` y salud en `http://localhost:8080/actuator/health`. El botón **Authorize** de Swagger acepta un Bearer JWT cuando se usa Keycloak.

Proyecto de LP2 con **Plan de estudios**, **Carga académica** y un portal Angular de administración y autoservicio del alumno. Ambas interfaces se sirven desde el mismo ejecutable Spring Boot. Incluye 37 planes PDF de FIA y los datos extraídos del Word suministrado. No requiere instalar Node para ejecutar la interfaz ya compilada.

## Ejecutar con Oracle

Requisitos: JDK 21 o posterior, Maven 3.9 y una instancia Oracle disponible. El proyecto utiliza Spring Boot 4.0.7 y Spring Modulith 2.0.7. Abrir esta carpeta como proyecto Maven en VS Code o IntelliJ IDEA.

1. Ejecutar `database/oracle.sql` con el usuario de aplicación en un esquema vacío. El script no modifica otros esquemas ni crea usuarios.
2. Configurar las variables de entorno con los valores reales de su base de BD2:

```powershell
$env:ORACLE_URL = 'jdbc:oracle:thin:@//SERVIDOR:1521/SERVICIO'
$env:ORACLE_USER = 'USUARIO_DEL_ESQUEMA'
$env:ORACLE_PASSWORD = 'CONTRASENA_LOCAL'
$env:IMPORTAR_FUENTES = 'true'
mvn spring-boot:run
```

3. Abrir [la aplicación local](http://localhost:8080).

Los valores en mayúsculas son marcadores para reemplazar, no credenciales existentes. Las credenciales no se versionan. La importación está deshabilitada por defecto y únicamente se ejecuta cuando se habilita y la tabla de planes está vacía. Toda la importación se confirma en una transacción. Después de la primera importación, desactivar `IMPORTAR_FUENTES`.

El esquema propuesto debe alinearse con el esquema real de BD2. No se recibió la conexión Oracle del equipo ni su DDL; la integración real de ADS y BD2 exige ejecutar y verificar la aplicación contra esa base. No se afirma que esa evidencia ya esté cumplida.

En un esquema FIA existente que ya tiene planes y carga, ejecutar una sola vez `database/migracion-matricula-oracle.sql` con el usuario `FIA_APP` antes de iniciar esta versión. No volver a ejecutar la migración si sus tablas ya existen.

## Demostración local sin Oracle

Después de `mvn package`, en Windows puedes ejecutar `iniciar-demo.cmd` y abrir [la aplicación local](http://localhost:8080). El iniciador utiliza primero `target/fia-academico-1.0.0.jar`. La entrega de código fuente no incluye el JAR anterior para evitar ejecutar una versión sin Angular. Se requiere un JDK compatible con el `pom.xml` (Java 21 o posterior).

Para iniciar desde el código fuente:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Este perfil usa H2 persistente en `data/`, importa los mismos datos y muestra **Modo demostración · H2** en la cabecera. Sirve para revisar la interfaz y el flujo completo; no sustituye la evidencia Oracle exigida por el curso. No hay asignaciones inventadas al inicio.

## Uso

- **Plan de estudios:** elegir escuela, plan y ciclo; buscar cursos; consultar créditos, HT, HP, HNP, total y prerrequisitos; abrir el PDF original. Crear, editar y eliminar cursos y docentes mediante REST; los estudiantes también tienen endpoints de alta, edición y baja protegidos. Los cambios manuales se identifican como tales.
- **Carga académica:** seleccionar los tres filtros mostrados en las capturas, abrir una escuela y elegir el plan. Las cinco carreras tienen cargas generadas y guardadas al iniciar demo. Editar filas, aumentar cupos, añadir grupos y asignar docentes desde el autocompletado global. Guardar valida los cruces de docente, ambiente y ciclo/grupo. Los horarios y ambientes son propuestas editables. Consultar las cargas registradas y su horario semanal. El endpoint `/api/v1/reportes/matriculas` permite filtrar por periodo, estado, plan y texto, y devuelve totales de monto, pago y saldo.
- Los cambios de asignación se conservan primero en el borrador en memoria. **Guardar carga** los envía al backend y confirma la cabecera y los detalles en la base de datos.
- **Matrícula y pagos:** registrar estudiantes, elegir carrera y periodo, configurar tarifa base y precio por crédito, seleccionar cursos del plan, generar matrícula con el total calculado y registrar pagos parciales. Configura el tarifario institucional real antes de usarlo; no se incluyen importes oficiales. La app registra constancias, no procesa pagos bancarios.
- El calendario es semanal, sin fechas de vigencia precargadas: no se entregó una programación completa por fechas.

## Fuentes y alcance

Consultar `docs/fuentes-y-alcance.md`, `docs/brief.md` y `docs/lp2-demo.md`.

La seguridad está activa por defecto: el portal distingue cuentas `ADMIN`, `TEACHER` y `STUDENT`, y el backend restringe cada operación según el rol. La integración opcional con Keycloak valida JWT en la API; para usar SSO en la SPA falta integrar Authorization Code + PKCE. Consulta `docs/KEYCLOAK.md`. Los pagos y comprobantes son simulados: la aplicación registra declaraciones y revisión administrativa, sin procesar pagos bancarios. Los valores de tarifa oficiales deben ingresarse; no se precargan montos supuestos.

## Pruebas y empaquetado

```powershell
mvn test
mvn package
java -jar target/fia-academico-1.0.0.jar --spring.profiles.active=demo
```

Las pruebas automáticas usan H2 en memoria y datos de prueba identificados por `FIXTURE` o `PRUEBA-TRANSACCION`. Comprueban límites modulares, CRUD, filtros combinados, validaciones, CORS y rollback después de insertar la cabecera y un primer detalle. Revisar `docs/VALIDACION.md` para el resultado de ejecución de esta entrega.

## Organización

```text
src/main/java/pe/edu/upeu/fia/
  FiaApplication.java
  planes/                 API pública del catálogo
    internal/             Entidades, repositorios, servicio y controller
  carga/
    internal/             Cabecera, detalles, horario, servicio y controller
  matricula/              Estudiantes, elecciones, tarifas, matrículas y pagos
  infra/                  CORS, errores, logs y estado
src/main/resources/static/ Interfaz HTML, CSS y JavaScript y PDF originales
database/oracle.sql        DDL propuesto para el esquema de aplicación
src/test/                  Pruebas automáticas
```

Un proyecto Maven, una JVM y un datasource. Carga y Matrícula utilizan `PlanesApi`; no acceden a los repositorios del catálogo. Se usa HTML/CSS/JavaScript para la interfaz porque los enlaces no exigen un framework de frontend.
## Perfiles de usuario y planes futuros

La pantalla inicial permite crear una cuenta de alumno. El registro crea el estudiante y asigna el rol `STUDENT`; ese perfil solo puede consultar planes/cursos y usar `/api/v1/alumno/matricular`, `/api/v1/alumno/matriculas` y su contrato. El administrador (`ADMIN`) mantiene la gestión de estudiantes, carreras, cursos, docentes, horarios, pagos y reportes. El administrador puede ver inmediatamente la matrícula creada por el alumno y su estado de pago.

Los planes y cursos no están limitados al año actual: los cursos se pueden crear o editar desde `POST/PUT /api/v1/cursos` indicando el plan, ciclo, créditos, horas y prerrequisitos. Para agregar un plan futuro se usa **Planes académicos** en el portal administrativo o el endpoint de planes; después se asocian los cursos. Los cupos y horarios siguen administrándose desde el módulo de carga académica.

Las cuentas de alumno se guardan en `FIA_ALUMNO_CUENTA`; los estudiantes creados se conservan en la base de datos. Para producción, conviene sustituir el gestor local por Keycloak y persistir las credenciales en el realm, siguiendo `docs/KEYCLOAK.md`.

## Funciones nuevas de administración y alumno

En `/portal/`, el administrador crea y edita planes, cursos, horarios, cupos, docentes y cuentas de alumnos. Los cursos usan horario en formato `Lun 08:00-10:00; Mar 10:00-12:00`. El administrador registra cursos aprobados en **Usuarios alumnos** para habilitar el avance de ciclo. El alumno completa correo, teléfono y apoderado antes de matricularse; el servidor valida plan, prerrequisitos, ciclos anteriores, 25 créditos, cupos y cruces de horario. La matrícula reserva cupo y permite ver e imprimir el contrato de estudios. El pago es un registro manual de abono, sin pasarela bancaria.

Las cuentas de alumnos se guardan en `FIA_ALUMNO_CUENTA` y sobreviven a reinicios. Para una base Oracle existente ejecuta una vez `database/migracion-v3-oracle.sql` antes de arrancar con `ddl-auto: validate`. La demo H2 carga `data/fia-v3-backup.sql` en memoria al iniciar y guarda una nueva copia SQL después de cada cambio. Así conserva los datos entre sesiones, incluso en entornos donde H2 no puede reabrir un archivo `.mv.db` para escritura. El archivo `data/fia-v3.mv.db` queda como respaldo histórico. Cambia la contraseña administrativa por defecto mediante variables de entorno antes de desplegar fuera de una demo local.

La base de demostración incluye un plan de Ingeniería de Sistemas 2027-1 con tres cursos, horario y docente, y una matrícula pagada de ejemplo. Para revisar el portal de alumno usa `alumno-demo` / `AlumnoDemo2027!`; cambia esa contraseña desde Usuarios alumnos antes de usar datos reales.
Los datos de demostración también se entregan en `data/fia-v3-backup.sql` como respaldo exportable de H2.

## Flujo de matrícula y permiso de carrera (actualización 26/10/2026)

El administrador asigna la carrera y el periodo en **Elección de carrera**. El alumno registrado selecciona sus cursos y confirma su matrícula según las reglas vigentes. El límite es **25 créditos**: 24 o menos y exactamente 25 están permitidos, siempre que se cumplan cupos, prerrequisitos y horarios.

En **Usuarios alumnos → Editar cuenta**, el administrador puede activar **Permitir que el alumno cambie la carrera asignada** para una cuenta concreta. El permiso está apagado inicialmente. Aun con permiso, el alumno solo modifica una asignación de periodo existente y no puede cambiar el plan de una matrícula ya generada. En Oracle, si ya aplicaste la migración v3, ejecuta `database/migracion-v4-permisos-oracle.sql` una vez.

## Portal del alumno en tres apartados

1. **Mis datos y apoderado:** el alumno guarda correo, teléfono y padre, madre o apoderado.
2. **Seleccionar cursos y horario:** se muestran solo los cursos del plan y periodo asignados. El importe estimado se calcula con la tarifa vigente. Una solicitud válida de hasta 25 créditos reserva los cupos mientras el administrador la revisa. Cursos sin horario aún no se pueden solicitar. El administrador ve las solicitudes en **Generar matrícula**, revisa sus cursos y emite o rechaza la matrícula.
3. **Matrícula y pago:** la matrícula emitida muestra importe, saldo, contrato y formulario para registrar el pago. El pago es manual; no se conecta a una pasarela bancaria.

El esquema Oracle requiere `database/migracion-v5-solicitudes-oracle.sql` una vez. La demostración H2 crea la tabla automáticamente y guarda los cambios en `data/fia-v3-backup.sql`.

## Horarios y cupos para todas las carreras

En **Planes académicos → Generar horarios faltantes**, el administrador puede asignar horarios referenciales a todos los cursos sin programación, conservando los ya existentes. La generación distribuye los cursos del mismo ciclo sin cruces y usa como orientación las horas HT y HP (máximo seis horas semanales por curso cuando el dato heredado representa horas de todo el semestre). Los horarios se muestran como **referenciales** y pueden editarse antes de su uso oficial.

En la lista de cursos de cada plan, el administrador ve cupos totales, ocupados o reservados y disponibles. El campo **Aumentar cupos por curso** y el botón **+ Cupos** suman la cantidad indicada. El alumno ve los cupos disponibles y la reserva se descuenta al enviar su selección.

### Horarios sin cruces
Los cursos importados tienen horario referencial en la base de demostración. Al crear un curso nuevo en cualquier plan, un horario vacío genera automáticamente bloques disponibles en su ciclo. Un horario ingresado manualmente se valida contra los demás cursos del mismo plan y ciclo; los cruces se rechazan. El alumno tampoco puede solicitar una combinación de cursos con cruces entre ciclos. Administración puede editar el horario y los cupos.

### Matrícula autónoma del alumno (demostración)
Administración crea la cuenta, asigna carrera y periodo, configura tarifas, planes, horarios y cupos. El alumno completa sus datos y los de su responsable, selecciona cursos del plan asignado y confirma la matrícula directamente: se validan prerrequisitos, 25 créditos como máximo, cupos y cruces de horario. El módulo de pago permite contado o de 2 a 12 cuotas. Tras registrar un pago aparecen un comprobante de demostración y el contrato de estudios con cursos y horarios. No se realiza un cobro bancario ni se emite un comprobante tributario. La administración puede buscar alumnos por código o nombre y consultar sus matrículas y pagos. Los planes nuevos tienen un botón propio para completar horarios faltantes.

### Verificación de pagos del alumno
En el portal, el alumno elige contado o cuotas y un medio separado: efectivo, Yape o transferencia. Yape y transferencia exigen referencia y foto JPG, PNG o WebP del voucher (máximo 5 MB); efectivo queda pendiente hasta que se reciba en caja. La declaración no acredita el saldo. Administración ve los pagos pendientes, abre el voucher y confirma o rechaza con un motivo. Solo la confirmación aumenta lo pagado, reduce el saldo y habilita la boleta de demostración y el contrato. Un rechazo permite enviar otra constancia. No se conecta con Yape, bancos ni facturación tributaria.


