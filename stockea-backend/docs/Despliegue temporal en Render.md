# Despliegue temporal en Render

## Alcance de esta configuración

El archivo `render.yaml` de la raíz del repositorio define un servicio Docker para `stockea-backend` y una base PostgreSQL gratuita para una demostración temporal. El servicio recibe el puerto `PORT` que Render proporciona; Spring Boot ya lo usa. La base de datos se conecta por la red interna de Render.

Esto prepara la infraestructura como código, pero no despliega nada por sí solo. Al crear un Blueprint desde el repositorio, Render construirá la imagen y podrá desplegarla automáticamente con los cambios de la rama configurada.

## Dependencias para que funcione RF-007.7

El backend requiere servicios externos reales para completar el cambio de contraseña:

- **Keycloak** accesible por HTTPS, con el realm `stockea`, los clientes `stockea-pwd-verifier` y `stockea-admin-api`, y sus secretos configurados en Vault.
- **HashiCorp Vault** accesible por HTTPS, con KV v2, las rutas `secret/stockea/idp` y `secret/stockea/mail`, y un AppRole de mínimo privilegio. Hay que generar el SecretId en este Vault y cargarlo como archivo secreto de Render llamado `vault-secret-id`.
- **SMTP** con autenticación y TLS. El Blueprint deja el puerto 2525 como valor inicial porque el nivel gratuito de Render bloquea salida SMTP por los puertos 25, 465 y 587. El proveedor elegido debe admitir el puerto configurado.
- **Variables de entorno**: Render solicitará `VAULT_ADDR`, `STOCKEA_VAULT_ROLE_ID`, `STOCKEA_IDP_URL`, `STOCKEA_IDP_ISSUER_URI`, `STOCKEA_IDP_JWK_SET_URI`, `MAIL_HOST` y `MAIL_FROM`. Los secretos SMTP y de clientes de Keycloak permanecen en Vault.

El Vault local de Docker Compose no se puede reutilizar desde Render: `localhost` dentro del servicio apunta al propio contenedor, y el Vault de desarrollo guarda sus datos en memoria. Tampoco se deben copiar los valores de `.env` al repositorio ni a `render.yaml`.

## Crear el Blueprint

1. Asegúrate de que los cambios estén en GitHub en la rama que usarás para la revisión.
2. En Render, crea un **Blueprint** y conecta el repositorio `Patinote0728/stockea`. El archivo está en la raíz; el `rootDir` del servicio apunta a `stockea-backend`.
3. Revisa que el plan de ambos recursos sea `Free` y confirma la creación. El Blueprint solicitará los valores que aparecen con `sync: false`.
4. En los ajustes del servicio, carga el SecretId de AppRole en **Secret Files**, con nombre `vault-secret-id`. El archivo debe contener solo el SecretId. La ruta configurada es `/etc/secrets/vault-secret-id`.
5. Antes de probar el endpoint, confirma en los logs que el servicio inicia y en Render que PostgreSQL está conectado. Usa un access token emitido por el realm configurado para llamar a `POST /api/v1/cuenta/contrasena`.
6. Al terminar la evaluación, exporta cualquier evidencia necesaria y elimina los recursos temporales. No confíes en la persistencia de la base gratuita.

## Límites y cumplimiento

- El servicio web gratuito se suspende después de 15 minutos sin tráfico y puede tardar alrededor de un minuto en reactivarse. No es adecuado para una demostración que requiera disponibilidad inmediata.
- PostgreSQL gratuito ofrece 1 GB y expira 30 días después de su creación; después de un periodo de gracia, Render elimina la base y sus datos. No se ofrecen copias de seguridad en este plan.
- Render entrega HTTPS al dominio `onrender.com`, pero termina TLS en su balanceador y reenvía HTTP al contenedor. Por tanto, este despliegue no satisface por sí solo el requisito de cifrado TLS de extremo a extremo hasta el backend ni el de aceptar tráfico de origen únicamente desde un WAF/CDN. La solución documentada requiere completar la fase 6 con proxy/WAF; Cloudflare delante de un dominio propio también requiere registrar y configurar ese dominio.
- El rate limiting actual reside en memoria y el plan gratuito ejecuta una sola instancia. Esto mantiene la limitación conocida en ADR-007; no debe habilitarse escalado horizontal sin mover ese contador a un almacén compartido.
- Esta configuración solo despliega el backend y PostgreSQL. No incluye frontend, Cloudflare, Reverse Proxy, Keycloak ni Vault.

## Referencias

- [Documentación de servicios web de Render](https://render.com/docs/web-services)
- [Limitaciones del plan gratuito de Render](https://render.com/docs/free)
- [Health checks de Render](https://render.com/docs/health-checks)
- [Blueprints de Render](https://render.com/docs/blueprint-spec)
