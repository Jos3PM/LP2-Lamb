# Evidencia de optimización y estabilización LP2 Unidad 3

Fecha local: 7 de noviembre de 2026 (America/Lima). Proyecto: FIA Académico, dominio de planes, cargas y matrícula. El documento ADS adjunto por el equipo es una plantilla de diseño; la [rúbrica LP2 U3](https://262ciclo4.github.io/bomerp/proyecto-integrador/u3/lp2-producto/) indica que BomERP es un ejemplo y que cada grupo demuestra su propio dominio.

## Entorno y comandos reproducibles

- Windows, Temurin Java 25.0.3, Maven 3.9.16, Node 24.19.0 y npm 11.17.0.
- Frontend: `cd frontend; npm run build:portal`.
- Backend: `mvn test` y `mvn -DskipTests package`. En este Windows con Java 25, Surefire necesitó `-DargLine=-javaagent:C:\Users\Usuario\.m2\repository\org\mockito\mockito-core\5.20.0\mockito-core-5.20.0.jar` para evitar el bloqueo del autoacoplamiento de Mockito.
- Instancia de prueba: `java -jar target/fia-academico-1.0.0.jar --spring.profiles.active=demo --server.port=18086 --app.demo.sql-persistence=false`. Usa H2; se deshabilitó la persistencia del respaldo de demostración.
- Navegador: Chrome instalado, `FIA_BASE_URL=http://localhost:18086`, `FIA_BROWSER_PATH` al ejecutable de Chrome y `FIA_ADMIN_PASSWORD` desde la configuración local de demo; `cd frontend; npm run test:e2e -- --reporter=line`.
- Maven terminó con **30 pruebas, 0 fallos, 0 errores, 0 omitidas** (Agenda 6, Carga 8, HTTP 5, Matrícula 4, Modularidad 1, Planes 4 y Profesor 2). El empaquetado Spring Boot terminó correctamente antes de la prueba HTTP y de Playwright.
- Tras ajustar una etiqueta de operación en `RequestLog`, se repitieron **5/5 pruebas HTTP** y se volvió a empaquetar el JAR. El resto de la suite completa y Playwright se ejecutaron antes de ese ajuste de una línea.

## Implementado y comprobado

| Criterio | Implementación | Verificación ejecutada |
| --- | --- | --- |
| Build y carga diferida | Angular conserva rutas diferidas de planes, organización y cuentas docentes. | Dos builds de producción: inicial **461,21 kB** (transferencia estimada 107,31 kB), final **461,21 kB** (107,31 kB). Chunks diferidos: 19,50; 10,01; 9,88 kB. No cambió el código Angular de producción. |
| Caché | `CachePolicy` usa `public, max-age=31536000, immutable` únicamente para bundles Angular con hash en el nombre; HTML, API, Actuator y demás recursos usan `no-store, max-age=0`. El nuevo hash de cada build invalida el recurso anterior. | HTTP real: `/portal/` y `/api/v1/estado` devolvieron `no-store`; `/portal/main-YXCVOBPA.js` devolvió `immutable`. Prueba MockMvc añadida. |
| Observabilidad | `RequestLog` conserva `X-Request-ID`, tiempo y estado; Micrometer registra duración y errores por operación. `/actuator/metrics` exige ADMIN; salud sigue pública. | HTTP real: salud `UP`; `/actuator/metrics` sin sesión `401`; como ADMIN, `fia.request.duration` tenía 194 solicitudes y `fia.request.errors` 2 al consultar. Los valores son de esa sesión, no un benchmark. Log correlacionado observado, por ejemplo `request=d6b03be3-8536-42ad-a319-99826f21c981 ... path=/actuator/health status=200`. |
| Paginación | Estudiantes y matrículas se paginan en el servidor; Angular muestra página y total. El reporte calcula agregados con los mismos filtros sobre **todas** las coincidencias, separados de `setFirstResult` y `setMaxResults`. | Prueba H2 con dos matrículas y tamaño de página 1: `total=2`, una fila visible y agregado de ambas matrículas. |
| Auditoría | `FIA_CARGA_AUDIT` guarda CREAR y EDITAR con actor y fecha. Consulta `/api/v1/cargas/{id}/auditoria` restringida a ADMIN. | Prueba de integración crea y edita una carga, y verifica ambas acciones, actor no vacío y fecha. |
| Flujo integrado | Plan/carrera/periodo → matrícula/cupo → pago declarado → revisión → contrato y constancia. | Prueba de integración H2 y Playwright en Chrome: **3/3** E2E en verde. El caso de 26 créditos obtuvo HTTP 400 y ninguna matrícula. |

## Decisión sobre Redis

No se incorporó Redis. En la demostración H2 se observaron 194 peticiones acumuladas y los recursos de navegador tienen una política de caché definida; eso no demuestra carga sostenida ni saturación de base. Añadir una dependencia externa ahora elevaría operación y riesgo sin una mejora medida. Antes de reconsiderarlo se realizará una prueba de carga con datos representativos en Oracle. Umbrales propuestos para investigarlo, no reglas institucionales: p95 de lecturas repetidas >300 ms durante 15 minutos, más de 70 % de tiempo de base ocupado, y una tasa de repetición de las mismas consultas >80 %. Se revisarán primero índices, consultas y paginación. No se cachearán datos personales ni saldos sin un diseño de invalidación y permisos.

## Checklist E2E reproducible

1. Administrador inicia sesión y abre Planes académicos y Organización docente: **pasó en Chrome**.
2. Administrador registra alumno, asigna carrera y periodo, y configura tarifa: **pasó mediante API dentro de Playwright**.
3. Alumno inicia sesión, confirma contacto/apoderado, elige un curso programado y ve horario: **pasó en Chrome**.
4. Alumno confirma cursos, ocupa cupo, declara Yape con voucher y ve estado pendiente: **pasó en Chrome**.
5. Administrador confirma el pago; alumno actualiza estado, ve boleta de demostración y contrato: **pasó en Chrome**.
6. Selección de 26 créditos devuelve 400 y el listado filtrado conserva total cero: **pasó mediante API dentro de Playwright**.
7. Recuperación/restauración y transacción contra Oracle real: **pendiente**, no hubo instancia Oracle disponible para esta ejecución.

## Bitácora de estabilización

| Hallazgo real | Reproducción y causa | Corrección | Nueva prueba |
| --- | --- | --- | --- |
| `/actuator/metrics` era público. | GET sin sesión: la regla final `anyRequest().permitAll()` dejaba pasar Actuator salvo salud. | `/actuator/**` ahora requiere ADMIN, conservando salud pública. | HTTP real: metrics 401 sin sesión; ADMIN consultó las métricas. |
| Falta de política de caché explícita. | GET de HTML, API y bundles sin política propia verificable. | Filtro `CachePolicy` con `immutable` solo para nombres versionados y `no-store` para el resto. | Prueba MockMvc y respuestas HTTP reales. |
| Selector E2E ambiguo para la boleta. | Primer Playwright: `getByText('COMPROBANTE DE DEMOSTRACIÓN')` coincidió con título y nota; 1/3 falló después de confirmar el pago. | Selector exacto del título. | Segunda ejecución Playwright: **3/3** en verde. |
| JAR bloqueado al volver a empaquetar en Windows. | Spring Boot Maven `repackage` no pudo renombrar el JAR mientras la instancia de comprobación lo estaba ejecutando. | Se cerró la instancia temporal y se repitió `package`. | JAR final de 89 224 719 bytes generado correctamente. |

## Limitaciones y validaciones pendientes

- Oracle real: no se ejecutó la migración ni el flujo contra una instancia Oracle; H2 no prueba planes de ejecución ni privilegios Oracle. No se cambió ninguna base compartida.
- No se ejecutó carga concurrente representativa; los contadores de Actuator son observabilidad funcional, no una medición de capacidad ni una justificación cuantitativa definitiva de Redis.
- Keycloak en navegador aún requiere Authorization Code + PKCE; el modo local y sus roles se probaron, no SSO.
- Los pagos y comprobantes son simulados; no se procesó dinero real.
