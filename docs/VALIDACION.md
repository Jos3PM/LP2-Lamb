# Validación de la entrega

## Versión Angular y matrícula — 18 de octubre de 2026

- La aplicación Angular 22 compiló con `npm run build:portal` y el resultado está publicado en `src/main/resources/static/portal/`.
- Maven compiló el backend y creó `target/fia-academico-1.0.0.jar` con `BUILD SUCCESS`.
- Se ejecutaron 21 pruebas con Maven: 21 correctas, 0 fallos, 0 errores. Incluyen el flujo transaccional de estudiante, elección de carrera, tarifa, matrícula, pago parcial y pago total; rechazo de sobrepago y de cambio de carrera tras matricularse. Las pruebas HTTP usan MockMvc para validar respuestas, CORS, filtros e interfaces sin requerir abrir un puerto de red.
- La base de pruebas fue H2 en memoria. El DDL y la migración Oracle están preparados, pero no se verificaron contra una instancia Oracle en esta ejecución porque Docker Desktop no tenía el motor disponible.
- No se hicieron pruebas visuales automatizadas del portal Angular. La interfaz compilada está incluida para abrirla desde Spring Boot en `/portal/`.
- El ZIP de código fuente excluye el JAR antiguo y los directorios generados. Para producir el ejecutable localmente hay que ejecutar `mvn package` con JDK 21 o posterior.

## Evidencia histórica de la versión anterior

> Esta evidencia corresponde a la versión del 7 de septiembre de 2026 y no cubre el módulo Matrícula y pagos añadido posteriormente. La sintaxis JavaScript de la versión actual pasó `node --check`. La compilación Java actual queda pendiente: Maven no está disponible en el entorno de edición.

Fecha: 7 de septiembre de 2026.

## Datos

- 37 archivos PDF originales de cinco escuelas de FIA incluidos.
- 2638 registros de cursos de los distintos planes; identificadores únicos.
- 370 totales de ciclo comparados con los PDF: todos coinciden en créditos, HT, HP, HNP y horas totales.
- 392 relaciones de docentes y cursos extraídas de los bloques inequívocos del Word. Las coincidencias con los planes no se presentan como asignaciones vigentes.
- Verificada la existencia de cada PDF referenciado y la capacidad de los campos para conservar los textos extraídos completos.

## Backend

**20 pruebas ejecutadas; 0 fallos, 0 errores y 0 omitidas.**

| Grupo | Pruebas | Resultado |
|---|---:|---|
| AgendaIntegrationTests | 6 | Correcto |
| CargaIntegrationTests | 6 | Correcto |
| HttpIntegrationTests | 4 | Correcto |
| ModularityTests | 1 | Correcto |
| PlanesIntegrationTests | 3 | Correcto |

Se comprobaron registro de cabecera y detalles, cálculo del total, rollback tras persistir el primer detalle, rechazo de cursos de otro plan, rechazo de grupos repetidos y bloques superpuestos, filtros de cargas, CRUD de cursos, filtros combinados, validación HTTP 400, trazabilidad, CORS permitido y rechazado, entrega de la interfaz y estado de conexión. Spring Modulith verificó los límites de acceso entre módulos.

Las pruebas se ejecutaron con H2 en memoria, en modo Oracle y con una base independiente por contexto. Los reportes originales de Maven Surefire están en `pruebas/`.

La compilación estándar de Maven encontró una restricción del entorno Windows al resolver las rutas de los archivos JAR (`AccessDeniedException` en `toRealPath`). Para verificar el código se utilizó el compilador JDK 17 mediante la API JavaCompiler con las mismas dependencias resueltas por Maven, un administrador de archivos que lee las clases permitidas y sin procesadores de anotaciones. Compilaron las 30 unidades de código principal y las cinco de pruebas. Luego Maven Surefire ejecutó las 20 pruebas normalmente. Este ajuste se limitó al entorno de verificación; el proyecto conserva un POM Maven convencional.

Maven Jar Plugin y Spring Boot Repackage finalizaron con **BUILD SUCCESS**. Se incluye el ejecutable `fia-academico-1.0.0.jar`, con las dependencias necesarias para iniciarlo con Java 17 o posterior.

## Interfaz

La sintaxis de JavaScript de aquella versión pasó `node --check`. La prueba HTTP comprobó la respuesta 200 de la página servida por Spring Boot. La interfaz usa datos originales y estilos basados en las capturas. No se afirma una prueba automatizada de clics ni una comparación visual píxel por píxel.

## Límite de la evidencia

No se recibió una conexión ni un esquema del equipo de BD2. El DDL y el perfil Oracle están preparados, pero su ejecución contra Oracle real **está pendiente**. H2 no sustituye esa parte de la sustentación. Tampoco se acredita aprobación docente, integración con un modelo ADS no proporcionado ni autenticación institucional.

La actualización verifica generación idempotente de cinco carreras, ausencia de cruces por ciclo, conflictos de docentes y ambientes entre cargas, edición de cupos, rollback de una actualización conflictiva y búsqueda/asignación global de docentes. Se añadieron 23 registros de nombres del bloque ambiguo al catálogo global sin inferir carrera.
