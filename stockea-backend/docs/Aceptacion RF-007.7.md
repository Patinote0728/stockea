# Aceptación local de RF-007.7

## Alcance cubierto automáticamente

`CambioContrasenaAceptacionTest` atraviesa el contrato HTTP, la cadena de seguridad, la extracción de identidad desde el JWT, las respuestas y el limitador. Mantiene dobles para el caso de uso y la auditoría; por ello no reemplaza la prueba integral contra Keycloak y Vault.

Los escenarios automatizados incluyen:

- Rechazar una solicitud sin JWT antes de invocar el caso de uso.
- Aceptar una solicitud autenticada y devolver `204` con `Cache-Control: no-store` y `Pragma: no-cache`.
- Responder a cinco contraseñas actuales inválidas con el error genérico `CREDENCIALES_INVALIDAS`; bloquear la sexta con `429`, `Retry-After` y auditoría sin invocar de nuevo el caso de uso.
- Conservar la cobertura de línea del 100 % en `PoliticaContrasena` y `ContrasenaPlana`, verificada por JaCoCo en `mvn verify`.

## Interfaz del navegador

La pantalla usa el adaptador JavaScript oficial de Keycloak con Authorization Code y PKCE S256. El adaptador mantiene los tokens solo en memoria. La llamada al cambio de contraseña usa `POST /api/v1/cuenta/contrasena`, `Cache-Control: no-store`, no persiste el cuerpo ni lo agrega a una cola offline y limpia los campos después de una respuesta del servidor.

Las pruebas del frontend verifican la política de confirmación, longitud mínima, diferencia frente a la clave actual y que los puertos externos no se consultan cuando la validación local falla.

## Dependencias locales reales

Docker Compose puede levantar PostgreSQL, Keycloak, Vault, Mailpit, el backend y Caddy. Keycloak y Vault siguen siendo necesarios para probar la operación completa de extremo a extremo. El Vault de desarrollo conserva estado solo mientras el contenedor permanezca activo; ejecutar `infra/vault/setup-vault.ps1` vuelve a sembrar el AppRole y los secretos locales.

La consola de Mailpit se publica en `http://localhost:8025`; el puerto SMTP del host usa `1026` por defecto para evitar conflictos con servicios SMTP locales. El backend continúa conectándose a `mailpit:1025` dentro de Compose. Si necesitas cambiar el puerto del host, define `MAILPIT_SMTP_HOST_PORT` en el `.env` local.

Antes de iniciar el conjunto, genera el certificado de desarrollo del backend:

```powershell
cd stockea-backend
.\infra\proxy\generar-certificado-local.ps1
docker compose up -d --build
```

Abre `https://localhost`. La interfaz accede a Keycloak mediante `https://keycloak.localhost`, que Caddy reenvía al servicio interno HTTP; ambas URLs usan certificados de la CA local de Caddy. Un navegador que no confíe en ella mostrará una advertencia hasta que se confíe en la raíz de Caddy. El certificado y la clave de prueba del backend están ignorados por Git.

El tráfico del navegador hacia Caddy y de Caddy al backend viaja cifrado en el entorno local. El navegador usa HTTPS también para Keycloak, aunque Caddy termina TLS y reenvía por HTTP a Keycloak dentro de la red aislada de Compose. El backend llega al JWKS interno por HTTP y comprueba el emisor HTTPS publicado por Keycloak. Vault y Mailpit siguen usando la red interna HTTP local. Esta configuración de desarrollo no es un despliegue de producción ni implementa Cloudflare/WAF.

## Configurar el cliente web en Keycloak

El realm exportado incluye el cliente público `stockea-web` para una instalación nueva. Si el volumen `kcdata` ya tenía creado el realm antes de agregar el cliente, el importador no modifica el realm existente. En ese caso, crea el cliente desde la consola de administración:

- Client ID: `stockea-web`.
- Tipo: OpenID Connect, cliente público (Client authentication apagado).
- Flujo estándar habilitado; flujo implícito y direct access grants apagados.
- PKCE: método `S256`.
- Redirect URIs: `http://localhost:5173/*` y `https://localhost/*`.
- Web Origins: `http://localhost:5173` y `https://localhost`.

## Pendiente para cerrar aceptación integral

Después de provisionar el realm, los clientes de backend, el AppRole y sus secretos, repetir el cambio de contraseña usando una cuenta de prueba real; comprobar en Mailpit la notificación y confirmar en el registro de auditoría que no aparecen contraseñas ni tokens. Esa prueba todavía no está automatizada y depende de configurar Keycloak y Vault.
