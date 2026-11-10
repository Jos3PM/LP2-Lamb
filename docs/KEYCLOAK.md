# Seguridad administrativa con Keycloak

La aplicación funciona por defecto con un usuario administrador local para la demostración. El acceso está restringido a `ADMIN`; las rutas `/api/v1/**` no se ejecutan sin autenticación.

## Opción local (demo)

Inicia el JAR con el perfil normal y abre `http://localhost:8080/portal/`:

```powershell
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="<contraseña_local_segura>"
java -jar target\fia-academico-1.0.0.jar --spring.profiles.active=dev
```

El usuario inicial es `admin`; establece su contraseña mediante `ADMIN_PASSWORD` antes de iniciar fuera del perfil de demostración.

## Opción Keycloak

1. Establece `KC_BOOTSTRAP_ADMIN_PASSWORD` en tu entorno local e inicia Keycloak en Docker:

```powershell
docker run -d --name fia-keycloak -p 8180:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin `
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=$env:KC_BOOTSTRAP_ADMIN_PASSWORD `
  quay.io/keycloak/keycloak:26.2 start-dev
```

2. Entra a `http://localhost:8180`, inicia sesión con el usuario bootstrap y crea el realm `fia`.
3. En el realm crea el cliente `fia-api` (OIDC), los roles `ADMIN`, `TEACHER` y `STUDENT`, y usuarios para cada rol. Asigna cada rol al usuario correspondiente. Configura un audience mapper para que el access token destinado a la API incluya `fia-api` en `aud`.
4. Obtén un access token para ese usuario y llama al backend con `Authorization: Bearer <token>`.
5. Inicia FIA en modo Keycloak:

```powershell
$env:KEYCLOAK_ISSUER_URI="http://localhost:8180/realms/fia"
java -jar target\fia-academico-1.0.0.jar --spring.profiles.active=dev,keycloak
```

El backend valida emisor y audiencia `fia-api`, extrae roles de `realm_access.roles` y del cliente `fia-api`; roles de otros clientes no conceden acceso. Las operaciones administrativas requieren `ADMIN`, `/api/v1/alumno/**` requiere `STUDENT` y `/api/v1/profesor/**` requiere `TEACHER`. Para una instalación distribuida, usa HTTPS y configura el issuer con el dominio real de Keycloak.

**Estado actual de la SPA:** el formulario de acceso del portal usa sesiones locales. El modo Keycloak protege la API para clientes Bearer, pero antes de usarlo desde el navegador hay que integrar Authorization Code + PKCE y adaptar el vínculo entre identidad Keycloak y los registros `FIA_ESTUDIANTE` y `FIA_PROFESOR_CUENTA`. No anunciar SSO del portal hasta completar y probar ese flujo.

## Verificación

```powershell
curl http://localhost:8080/api/v1/auth/status
curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"<contraseña_local_segura>"}'
```

Swagger está disponible en `http://localhost:8080/swagger-ui.html`; las operaciones protegidas deben ejecutarse con sesión local o token Bearer según el modo activo.
