# Producto de Unidad I de LP2

## Arquitectura y alcance

Un ejecutable Spring Boot y un proyecto Maven. Los módulos son `planes`, `carga` e infraestructura transversal `infra`. `planes` expone `PlanesApi` y DTO; sus entidades y repositorios están en `internal`. `carga` depende de esa API pública y administra sus propias entidades. Spring Modulith verifica los límites automáticamente.

La interfaz tiene Plan de estudios, Carga académica y Matrícula y pagos. Se sirve desde `src/main/resources/static` por el mismo backend, sin comunicación HTTP entre módulos internos ni Feign.

El perfil normal es `oracle`. El perfil `demo` y las pruebas usan H2, claramente identificados. La ejecución en H2 no acredita la integración Oracle de BD2.

## Contrato REST

| Método | Recurso | Comportamiento |
|---|---|---|
| GET | `/api/v1/estado` | Comprueba conexión mediante `SELECT 1 FROM DUAL` e informa el perfil. |
| GET | `/api/v1/planes` | Escuelas y versiones presentes en los PDF. |
| GET | `/api/v1/cursos?planId=37&ciclo=4&buscar=Lenguaje&orden=name` | Filtros opcionales combinados; orden permitido por `name`, `cycle` o `credits`. |
| GET | `/api/v1/cursos/{id}` | DTO del curso con el plan y sus relaciones documentadas de docentes. |
| POST | `/api/v1/cursos` | Crear curso; devuelve 201 y Location. |
| PUT | `/api/v1/cursos/{id}` | Actualizar curso y recalcular horas. |
| DELETE | `/api/v1/cursos/{id}` | Eliminar curso; devuelve 204; 409 si está referenciado. |
| GET | `/api/v1/cursos/resumen?planId=37` | Cantidad de cursos, créditos y horas. |
| POST | `/api/v1/cargas` | Registrar cabecera y detalles en una transacción; devuelve 201. |
| GET | `/api/v1/cargas?planId=37&semestre=Regular%202026-2&desde=2026-09-01&hasta=2026-09-30` | Filtrar por plan, semestre y fecha de registro; ordenar por fecha descendente. |
| GET | `/api/v1/cargas/{id}` | Cabecera y detalle completo, con bloques horarios. |
| GET | `/api/v1/cargas/resumen?planId=37` | Cantidad de cargas, cantidad de detalles y suma de horas. Admite los filtros de la lista. |
| POST | `/api/v1/estudiantes` | Registrar un estudiante con código único. |
| POST | `/api/v1/elecciones-carrera` | Guardar la carrera (plan) elegida por estudiante y periodo. |
| PUT | `/api/v1/tarifas-matricula` | Configurar tarifa base, monto por crédito y moneda para un periodo. |
| POST | `/api/v1/matriculas` | Generar matrícula y detalles; valida elección, tarifa, plan de cursos y duplicados. Calcula base + precio por crédito. |
| GET | `/api/v1/matriculas` | Consultar matrículas con monto, estado y saldo. |
| POST | `/api/v1/matriculas/{id}/pagos` | Registrar un pago total o parcial y actualizar el estado atómicamente. |
| GET | `/api/v1/matriculas/{id}/pagos` | Consultar las constancias de pago de una matrícula. |

La UI guía siete operaciones: registro de estudiante, elección de carrera, configuración de tarifa, selección de cursos, generación transaccional de matrícula, registro de pagos y consulta de saldo/historial. El tarifario debe ser ingresado por el equipo según los valores oficiales; no hay montos por defecto. La fórmula es `tarifa base + precio por crédito × créditos seleccionados`. La constancia es manual y no integra una pasarela bancaria. Los prerrequisitos se muestran como texto curricular, no se validan como reglas automáticas.

Las fechas de filtro corresponden al registro de la carga, no a la vigencia del calendario. No se inventa una fecha de inicio o término del semestre.

## DTO de curso

Ejemplo de estructura para una **prueba manual**, no información curricular oficial:

```json
{
  "planId": 37,
  "cycle": 1,
  "name": "FIXTURE CRUD",
  "credits": 2,
  "ht": 1,
  "hp": 2,
  "hnp": 0,
  "prerequisites": ""
}
```

La respuesta añade `id`, `school`, `total`, `source`, `page`, `notes` y `teachers`. Los DTO de entrada y salida son distintos. Las horas totales son calculadas por el servidor; el cliente no las fija. El mapeo a entidades es explícito.

## Operación cabecera-detalle

Obtener un `cursoId` real mediante GET de cursos antes de enviar una carga. `docenteId` es opcional; cuando se selecciona, debe existir en el catálogo global `/api/v1/docentes`. Un valor nulo significa sin asignar, no un docente ficticio.

Estructura del cuerpo; reemplazar `CURSO_ID_REAL` por el identificador obtenido (este bloque es ilustrativo y no JSON ejecutable):

```text
{
  "planId": 37,
  "sede": "Filial Juliaca",
  "semestre": "Regular 2026-2",
  "contrato": "Escuela Profesional Pregrado",
  "detalles": [{
    "cursoId": CURSO_ID_REAL,
    "docenteId": null,
    "grupo": 1,
    "cupos": null,
    "ambiente": "",
    "condicion": "",
    "modalidad": "",
    "bloques": []
  }]
}
```

Un bloque contiene `dia` (0 domingo a 6 sábado), `inicio` y `fin` en minutos desde medianoche. Los bloques se generan como propuesta editable en demo; el usuario asigna los docentes.

`CargaService.crear()` tiene `@Transactional`. Inserta y fuerza el envío de la cabecera a la base; después valida y persiste cada detalle. Si falla el segundo detalle, la excepción abandona el método y Spring revierte cabecera y primer detalle. La prueba `falloEnSegundoDetalleRevierteCabeceraYPrimerDetalle` compara ambos conteos antes y después.

El total de la cabecera suma HT + HP + HNP de las filas editadas, una vez por detalle/grupo. El servidor valida la pertenencia al plan y la unicidad curso/grupo. No se implementa aprobación académica ni se presume una regla institucional de cupos máximos.

## Errores, trazabilidad y CORS

- 400: DTO inválido, formato incorrecto o inconsistencia del registro.
- 404: identificador inexistente.
- 409: restricción relacional, por ejemplo eliminar un curso utilizado.
- Respuestas de error `ProblemDetail`; no se exponen contraseñas ni SQL interno.
- Cada petición recibe `X-Request-ID`; el log incluye identificador, método, ruta, estado HTTP y duración, sin cuerpos ni credenciales.
- `CORS_ORIGINS` configura los orígenes permitidos. Por defecto: `http://localhost:8080`. Las pruebas verifican un origen aceptado y uno rechazado.

## Sustentación de 18 minutos por integrante

1. **8 minutos:** explicar los módulos, el contrato y el mapeo Plan–Curso y Carga–Detalle. Mostrar la procedencia de los datos y los límites de información del Word.
2. **5 minutos de demo:** consultar un plan; ejecutar CRUD con caso válido e inválido; guardar una carga; provocar el rollback; mostrar filtros y resumen.
3. **5 minutos de preguntas:** explicar DTO, atomicidad, CORS por propiedad, consultas combinadas y verificación de dependencias.

Mostrar `mvn test` y, para la entrega de clase, ejecutar la misma aplicación sobre Oracle real. El documento no reemplaza la sustentación individual ni acredita autoría de un integrante no identificado.

## Integración pendiente con ADS y BD2

El DDL de `database/oracle.sql` es una propuesta ejecutable del modelo de esta entrega. Para un esquema FIA previo con planes y carga, `database/migracion-matricula-oracle.sql` añade una vez los objetos del nuevo módulo. No se recibió el esquema ya construido en BD2, PL/SQL del equipo ni vistas C3 de ADS. Por tanto, están pendientes la adaptación a esos objetos reales, su conexión y la demostración en vivo. No se crearon evidencias ficticias de esa integración.

## API de edición y agenda

- GET `/api/v1/docentes?buscar=apellido`: búsqueda global de docentes.
- PUT `/api/v1/cargas/{id}`: actualización atómica de filas y horarios; valida cruces entre cargas.
- POST `/api/v1/cargas/generar`: genera las cinco cargas faltantes del último plan Regular, sin reemplazar las existentes.
- Los detalles admiten `cursoNombre`, `ciclo`, `creditos`, `ht`, `hp`, `hnp` además de cupos y bloques. Si se omiten, se toman del catálogo.
- Una instalación Oracle anterior necesita `database/migracion-v2-oracle.sql`; una instalación nueva usa solamente `database/oracle.sql`. H2 demo migra automáticamente mediante Hibernate.
