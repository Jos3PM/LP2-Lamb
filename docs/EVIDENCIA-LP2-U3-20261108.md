# Evidencia LP2 U3: criterios 1, 2 y 3

Fecha: 8 de noviembre de 2026, America/Lima. Entorno: Windows, Java 25.0.3, Maven 3.9.16, Node 24.19.0, npm 11.17.0, Chrome y H2 2.4.240. Dominio: planes, cargas, estudiantes, matrícula y pagos simulados. Esta ejecución no usó Oracle ni modificó una base compartida.

La [rúbrica LP2 U3](https://262ciclo4.github.io/bomerp/proyecto-integrador/u3/lp2-producto/) permite adaptar el ejemplo BomERP al dominio académico. Su nivel A del criterio 3 pide E2E contra la base real; por eso los resultados H2 de este documento **no acreditan por sí solos** ese punto.

## Reproducción y alcance

- `cd frontend; npm run build:portal`: completado antes y después de los cambios de backend. En ambas corridas el bundle inicial fue **461.18 kB** en bruto y **107.45 kB** de transferencia estimada; `main` 460.75 kB. Rutas diferidas: planes 27.18 kB, cuentas docentes 10.01 kB, organización docente 9.87 kB. La segunda salida está en [angular-build-final-20261108.log](pruebas/angular-build-final-20261108.log). No hubo optimización Angular nueva en esta iteración; la medición anterior del 7 de noviembre (461.21 kB) corresponde a otra revisión de interfaz y no es una comparación causal.
- `mvn -Dtest=PagedEndpointLoadScenario test`: escenario H2 aislado, optativo, con **800 estudiantes y 800 matrículas**. De ellas, 400 cumplen el filtro de estado. Inserta filas temporales, ejecuta 20 peticiones de calentamiento y 100 medidas **secuenciales** por endpoint, y borra sus filas. La salida `BENCH` queda en [bench-baseline-run.log](pruebas/bench-baseline-run.log) y [bench-after-run.log](pruebas/bench-after-run.log). En este equipo el launcher de Maven usó `-DargLine=-javaagent:<mockito-core-local>` por Java 25; no se usó una contraseña para el benchmark.
- `mvn test`: pasada final con **33 pruebas**, 0 fallos, 0 errores, 0 omitidas. Incluye filtros y metadatos entre páginas, agregados, auditoría y permisos. La clase optativa de carga no se incluye en ese conteo. También se ejecutó `mvn -Dtest=HttpIntegrationTests test`: 7/7 (resultado en `pruebas/http-after-log-20261108.log`).
- `mvn -DskipTests package`: JAR construido. `java -jar target/fia-academico-1.0.0.jar --spring.profiles.active=demo --server.port=18088 --app.demo.sql-persistence=false`: instancia H2 temporal. La contraseña ADMIN se suministró con `ADMIN_PASSWORD` desde un valor temporal, sin registrarla; se eliminó el archivo temporal tras la ejecución. `cd frontend; FIA_BASE_URL=http://localhost:18088; FIA_BROWSER_PATH=<Chrome>; FIA_ADMIN_PASSWORD=<variable del entorno>; npm run test:e2e -- --reporter=line`: **4/4 en Chrome**, repetidos sobre el JAR final. Salidas en [e2e-20261108.log](pruebas/e2e-20261108.log) y [e2e-final-20261108.log](pruebas/e2e-final-20261108.log).

### Medición comparable de endpoints paginados

| Endpoint y filtro | Antes p50/p95 ms | Después p50/p95 ms | Errores antes/después |
| --- | ---: | ---: | ---: |
| Estudiantes, página 3 de 25, nombre | 8.951 / 11.406 | 9.235 / 11.091 | 0/100 y 0/100 |
| Matrículas, página 3 de 25, periodo/estado/plan | 4.620 / 7.223 | 4.767 / 6.428 | 0/100 y 0/100 |
| Reporte de matrículas con los mismos filtros | 4.360 / 5.884 | 4.081 / 5.639 | 0/100 y 0/100 |

Se midieron **300 solicitudes por corrida**; las dos corridas usaron el mismo código de fixture y tamaño de datos, en JVM separadas. El cambio del reporte agrupa conteo, monto y pagado en una agregación y reduce su número de consultas SQL de tres a dos por solicitud. Las variaciones de latencia son pequeñas y pueden ser ruido de H2/JVM: no son una prueba de mejora en Oracle ni de capacidad concurrente. `MockMvc` ejercita controladores, filtros y H2 dentro del proceso; **no mide red ni usuarios simultáneos**. Para capacidad final hace falta una prueba con concurrencia y Oracle aislado.

### Redis y caché

No se agregó Redis. Con 800 filas, p95 de 5.639–11.091 ms y 0 errores en las tres lecturas, no apareció un cuello que justifique un servicio externo; la muestra secuencial tampoco permite declarar el límite del sistema. Reconsiderar tras medir carga concurrente representativa en Oracle si el p95 de consultas repetidas supera 300 ms durante 15 minutos, la base supera 70 % de ocupación y más de 80 % de lecturas son idénticas. Esos valores son disparadores de investigación, no reglas de la institución. Datos personales, saldos y respuestas autenticadas siguen `no-store`; solo bundles Angular con hash usan `public, max-age=31536000, immutable`.

### Monitoreo y confidencialidad

En la instancia H2: `/actuator/health` devolvió HTTP 200 y `UP`; `/actuator/metrics` anónimo devolvió 401 y como ADMIN devolvió 200. `fia.request.duration` expuso etiquetas `method,operation,status`; `fia.request.errors` expuso `operation,status`. No hay nombres, correos, IDs de alumno, cookies, vouchers ni contraseñas en esas etiquetas. `/portal/` devolvió `Cache-Control: no-store, max-age=0` y `X-Request-ID`; un `main-<hash>.js` devolvió `public, max-age=31536000, immutable`. `RequestLog` registra la plantilla de ruta, método, estado, duración e ID de petición, sin cuerpo, query string ni cabeceras; una prueba HTTP comprueba que un filtro que contiene un correo no aparece en el log. El log observado tras el ajuste usó `route=/api/v1/estado` y no la URL completa.

## Paginación, integración y auditoría

- Prueba H2 con **9 estudiantes** filtrados y páginas de 4: tamaños 4/4/1, `total=9`, `paginas=3` y nombres ordenados. Con **7 matrículas** del mismo periodo/plan y páginas de 3: tamaños 3/3/1, `total=7` en cada página, sin duplicados y orden descendente estable por fecha/ID. Estado `PAGADA` bajo ese filtro devolvió cero. El reporte de la página 3 sumó las siete matrículas, no solo la fila visible.
- El controlador rechaza desplazamientos de página que desbordan `int`; prueba HTTP exige 400. El tamaño sigue limitado a 1–100.
- E2E 4/4: administración y planes con horario visual; alumno registrado por ADMIN, carrera y periodo asignados, datos de contacto, selección de curso y horario, cupo, pago Yape con voucher, revisión ADMIN, boleta simulada y contrato; rechazo de 26 créditos sin crear cabecera ni detalle.
- La auditoría de carga registra `CREAR` y `EDITAR`, actor, fecha y resumen de **detalles, horas y cupos antes→después**; prueba H2 comprueba ambas acciones con actor de prueba identificado y el cambio `detalles 1→2`. Endpoint `/api/v1/cargas/{id}/auditoria` es ADMIN; prueba de permisos confirma 403 para profesor. La columna nueva es nullable para eventos históricos.

## Bitácora real de esta iteración

| Síntoma y reproducción | Causa | Corrección | Nueva comprobación |
| --- | --- | --- | --- |
| Al inspeccionar `RequestLog`, `path` usaba la ruta concreta; una URL con nombre de cuenta podía quedar en el log. | Registro directo de `getRequestURI()`. | Se registra la plantilla de Spring MVC; sin coincidencia, `/unmatched`. | Prueba HTTP de filtro con correo y log sin ese dato; revisión de salida de ejecución. |
| Una página extrema (`pagina=2147483647&tamano=100`) podía desbordar el desplazamiento entero. Detectado por inspección, no por fallo productivo observado. | Multiplicación `pagina*tamano` sin límite del producto. | Validación previa del desplazamiento y `Math.multiplyExact`. | Prueba HTTP: 400. |
| Primer intento de levantar Tomcat local falló con `Permission denied: connect`. | Restricción de socket del sandbox, fuera del código de la aplicación. | Se habilitó red local para la ejecución y se reinició la instancia H2. | Salud HTTP 200 y Playwright 4/4. |

## Demostración en vivo

1. Iniciar una instancia **H2 de demostración vacía/aislada** con `ADMIN_PASSWORD` desde entorno y `--app.demo.sql-persistence=false`. Preparar un ADMIN temporal y un alumno nuevo; no mostrar contraseñas en pantalla ni logs.
2. Como ADMIN, abrir planes y carga; mostrar cursos, créditos, horario, cupos y auditoría de CREAR/EDITAR con actor, fecha y resumen. Mostrar la restricción 403 con rol profesor si hay una cuenta de prueba.
3. Asignar al alumno carrera y periodo y tarifa de demostración. Como alumno, completar datos, seleccionar curso programado, ver horario/cupos y confirmar hasta 25 créditos. Declarar pago simulado; como ADMIN, revisar y confirmar; como alumno, mostrar estado, saldo, boleta simulada y contrato.
4. Mostrar `health`, métricas con ADMIN y un log por Request ID; explicar que `MockMvc` mide H2 secuencial, que el reporte pasó de tres a dos consultas y que Redis se pospuso por falta de beneficio demostrado. Ejecutar el rechazo de 26 créditos.

**Automatizado:** las 33 pruebas Maven, las dos corridas de carga H2 y las 4 pruebas Playwright indicadas arriba. **Defensa en vivo:** operación del sistema, lectura de métricas, interpretación de decisiones técnicas y comprobación visual con datos de demostración. No se acreditó Oracle ni concurrencia real.

## Pendientes externos

- No había variables ni instancia Oracle de pruebas aislada disponible. `database/migracion-v8-resumen-auditoria-oracle.sql` está preparada, pero no ejecutada. Seguir [VALIDACION-ORACLE-AISLADA.md](VALIDACION-ORACLE-AISLADA.md) con un esquema exclusivo de prueba; H2 no sustituye esta evidencia.
- La capacidad bajo usuarios simultáneos y la optimización de planes SQL de Oracle requieren esa instancia y datos representativos. No se afirma nivel A definitivo para esos puntos sin dicha comprobación.
