# Pantallas administrativas y sesión local

## Rutas del portal

- `/portal/#/planes-academicos`: crear, editar y eliminar planes; seleccionar un plan y administrar sus cursos por ciclo. Cada curso tiene créditos, horas HT/HP/HNP, prerrequisito, horario y cupos. El botón **Proponer horarios faltantes** completa solo los cursos sin programación. También se pueden aumentar cupos y consultar los disponibles para un periodo.
- `/portal/#/organizacion-docente`: asignar, buscar, filtrar, editar y eliminar docentes por escuela y curso. El servidor comprueba que el curso exista en la escuela; el directorio conserva las asignaciones de docentes con el mismo nombre.

Las dos rutas usan `adminGuard`. La guarda oculta la navegación a quien no sea `ADMIN`; las escrituras están protegidas además por Spring Security en `/api/v1/**`. Angular muestra de forma centralizada las respuestas 401 y 403.

## Sesión y CSRF

El modo local usa `JSESSIONID` con `HttpOnly`, `SameSite=Lax` y expiración de sesión tras 30 minutos de inactividad. En HTTPS debe establecerse `SESSION_COOKIE_SECURE=true`. La SPA obtiene una cookie separada `XSRF-TOKEN` al consultar `/api/v1/auth/status` y Angular la envía como `X-XSRF-TOKEN` en las peticiones de escritura. Esta cookie debe ser legible por JavaScript; **la cookie de sesión sigue siendo HttpOnly**. Spring rechaza escrituras sin token CSRF con HTTP 403. El modo Keycloak usa JWT Bearer sin sesión de navegador y mantiene CSRF deshabilitado; la integración SSO de la SPA sigue pendiente.

## Comprobaciones de esta revisión

- Compilación Angular de producción y empaquetado Maven: correctos.
- HTTP 200 para el portal; 401 en `/api/v1/docentes` sin sesión; 403 en `POST /api/v1/auth/login` sin CSRF; 200 con token.
- CRUD temporal de docente: alta 201, filtro exacto y baja 204; escuela/curso inexistentes: 400.
- CRUD temporal de plan y curso: alta 201; horario propuesto; segundo curso con horario superpuesto: 400; aumento de cupos y eliminación: correctos.
- La prueba E2E de navegación está en `frontend/e2e/admin-modules.spec.ts`. Para ejecutarla: `npm ci`, instalar Chromium con `npx playwright install chromium`, iniciar el backend y ejecutar `npm run test:e2e -- e2e/admin-modules.spec.ts`.

En esta sesión Playwright no pudo abrir Chromium ni el Edge instalado dentro del entorno de ejecución; la comprobación en navegador queda pendiente en una máquina con navegador Playwright disponible. Las peticiones HTTP y los flujos CRUD anteriores sí se ejecutaron contra el JAR actualizado.

Los horarios generados son referenciales y deben ser confirmados por administración. La migración Oracle y el inicio de sesión de Keycloak requieren validación en sus entornos reales antes de producción.
