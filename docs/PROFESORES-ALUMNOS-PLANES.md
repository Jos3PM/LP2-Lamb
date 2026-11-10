# Profesores, alumnos y planes académicos (v7)

## Arquitectura y decisiones

El backend sigue organizado en `planes` (planes, cursos y directorio docente), `carga` (cabecera, grupos, horarios y cupos), `matricula` (alumno, matrícula y pagos) y `security` (cuentas y permisos). Angular consume la API del mismo servidor Spring Boot. Oracle es la base de uso institucional; H2 es una demostración local.

Una cuenta de profesor **se vincula con un docente ya registrado**. El administrador crea esa cuenta y mantiene las asignaciones desde Carga académica; el profesor consulta únicamente las suyas. No se incorporan calificaciones, asistencia ni una regla institucional para que el profesor modifique horarios, porque el proyecto no las define. El alumno solo edita correo, teléfono y apoderado; la carrera, el plan, las matrículas y los pagos confirmados conservan sus flujos y permisos existentes.

## Pantallas y roles

| Rol | Pantalla | Alcance |
| --- | --- | --- |
| ADMIN | `/portal/#/cuentas-profesores` | Crear, editar, activar, desactivar y consultar cuentas vinculadas al directorio docente. |
| ADMIN | `/portal/#/organizacion-docente` y Carga académica | Mantener docentes, grupos y asignaciones; las escrituras de carga registran actor y fecha. |
| ADMIN | `/portal/#/planes-academicos` | Crear y editar planes y cursos, organizar por ciclo y previsualizar créditos y horas. |
| TEACHER | `/portal/` tras iniciar sesión | Ver su perfil y sus cargas, cursos, grupos y horarios; no edita otros datos. |
| STUDENT | `/portal/` tras iniciar sesión | Actualizar datos de contacto; consultar carrera, plan, cursos, horario, créditos, matrículas, pagos y saldos. |

Las cuentas de profesores tienen usuario de 4–80 caracteres (`A-Z`, números, punto, guion y guion bajo), código de 2–30 caracteres, correo válido y contraseña de 12–72 caracteres. El servidor impide duplicar usuario, código, correo y vínculo con docente. La contraseña se almacena con BCrypt; las respuestas nunca la incluyen. Una cuenta desactivada deja de poder iniciar sesión y tampoco puede consultar su perfil con una sesión previa. El administrador debe entregar las credenciales por un canal privado.

## API principal

| Método y ruta | Rol | Acción |
| --- | --- | --- |
| `GET /api/v1/profesores/cuentas` | ADMIN | Listar cuentas. |
| `GET /api/v1/profesores/cuentas/{username}` | ADMIN | Ver una cuenta. |
| `POST /api/v1/profesores/cuentas` | ADMIN | Crear cuenta con `username`, `password`, `docenteId`, `codigo`, `correo`. |
| `PUT /api/v1/profesores/cuentas/{username}` | ADMIN | Editar `codigo`, `correo`, `activo` y contraseña opcional. |
| `GET /api/v1/profesor/perfil` | TEACHER | Perfil del usuario autenticado. |
| `GET /api/v1/profesor/asignaciones` | TEACHER | Solo sus cargas y horarios; no acepta un ID de otro profesor. |
| `GET /api/v1/cargas/{id}/auditoria` | ADMIN | Actor, acción y fecha de cambios en carga. |
| `GET /api/v1/alumno/perfil` y `PUT /api/v1/alumno/perfil` | STUDENT | Datos propios; correo, teléfono y apoderado editables. |
| `GET /api/v1/alumno/matriculas` | STUDENT | Historial propio de matrículas y pagos. |

Las demás rutas de planes, cursos, docentes y cargas conservan sus contratos. Spring Security exige `ADMIN` para escritura administrativa, `TEACHER` para `/api/v1/profesor/**` y `STUDENT` para `/api/v1/alumno/**`. El catálogo público de planes y cursos mantiene la visibilidad previa; ninguna ruta pública revela datos personales del profesor. Las validaciones de DTO devuelven errores HTTP 400 y Angular muestra el mensaje en la pantalla.

## Planes y relaciones

El formulario de planes muestra el resumen por ciclo antes de guardar. El backend rechaza una combinación duplicada de escuela, versión y variante, cursos duplicados en el mismo plan y ciclo, créditos/horas/cupos fuera de rango, cambios de escuela de un plan que ya tiene cursos y el traslado implícito de un curso a otro plan. Los prerrequisitos y los cruces de horario siguen verificándose en el servidor. Un plan con cursos no se elimina. Las claves foráneas impiden retirar cursos referenciados por carga o matrícula; en ese caso se debe conservar el historial y administrar una nueva versión del plan.

## Migración y configuración

En un esquema Oracle existente, hacer respaldo y ejecutar **una vez, en orden**, las migraciones previas hasta v6 y luego `database/migracion-v7-profesores-auditoria-oracle.sql` como `FIA_APP`. V7 crea `FIA_PROFESOR_CUENTA`, `FIA_CARGA_AUDIT`, su secuencia e índice, y las columnas de actor en `FIA_CARGA`. No crea cuentas ni contraseñas. El script es reejecutable mediante comprobaciones del diccionario Oracle. Una instalación vacía ejecuta `database/oracle.sql`; no mezcla ambas rutas. Verificar tablas y columnas antes de iniciar con `ddl-auto=validate`.

Variables: `ADMIN_USERNAME` y `ADMIN_PASSWORD` para el administrador local; `FIA_ORACLE_PASSWORD` y las variables de conexión Oracle documentadas en README; `SESSION_COOKIE_SECURE=true` cuando se usa HTTPS. El perfil `demo` usa H2 y una contraseña de **demostración** configurable por `ADMIN_PASSWORD`; sustituirla fuera de la demo. H2 crea el esquema de demostración automáticamente, sin ejecutar SQL Oracle. No hay contraseñas institucionales en los archivos versionados.

## Comprobación por rol

1. Iniciar la aplicación con `--spring.profiles.active=demo` y abrir `/portal/`.
2. Como administrador, crear un docente y vincularle una cuenta en **Cuentas de profesores**. Asignarle una carga en **Carga académica**. Verificar el historial de auditoría de la carga.
3. Iniciar sesión con la cuenta de profesor: debe ver perfil y asignaciones propias. Confirmar HTTP 403 al consultar `/api/v1/profesores/cuentas` u otra ruta administrativa. Desactivar la cuenta y comprobar que pierde acceso.
4. Como alumno, comprobar perfil, carrera asignada, horarios, créditos, historial de matrículas, pagos y saldos. Intentar consultar rutas de otro rol: HTTP 403. Editar solo datos de contacto.
5. Como administrador, crear un plan futuro y cursos por ciclo; probar duplicados y eliminación de un plan con cursos. Verificar que se conserva el plan existente.

## Resultado de verificación de esta entrega

El 5 de noviembre de 2026 se ejecutaron las 26 pruebas Maven en H2: **26 correctas, sin fallos ni errores**. Incluyen permisos de profesor, validaciones de cuentas, CRUD de planes, auditoría y rollback de carga, matrícula, HTTP y límites modulares. El build Angular de producción y el empaquetado Spring Boot también finalizaron correctamente. Para repetir los tests en este entorno Windows con Java 25 se pasó `mockito-core` como `-javaagent` a Surefire; sin ese agente, el autoacoplamiento de Mockito se bloqueó en la JVM local.

Oracle y Keycloak deben probarse en sus instalaciones reales; no se incluyen accesos a esas instancias en este repositorio. El login Keycloak de la SPA aún requiere Authorization Code + PKCE; el modo Keycloak actual valida tokens Bearer en la API.
