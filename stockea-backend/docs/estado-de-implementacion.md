# Estado de implementación (actualizado: 04/11/2026)

## Stack real
- Java 26 (JDK local y `java.version` del pom.xml), Maven, Spring Boot 4.1.2 (Jackson 3, JUnit 6)
- Spring Vault 4.1.1 (`spring-vault-core` en scope compile; Boot 4.1.2 no gestiona su versión, se fija con la propiedad `spring-vault.version`)
- Paquete raíz: co.stockea (módulo: co.stockea.cuenta → dominio/aplicacion/infraestructura/interfaces)
- Backend en puerto 8081; Postgres en contenedor, puerto configurable con POSTGRES_PORT (5433 en el equipo de desarrollo)
- Entorno local: Docker Compose con Postgres, Keycloak 26, Vault (modo dev), Mailpit
- ArchitectureTest (ArchUnit) protege la regla de dependencia

## Avance
- Alcance actual: implementación del flujo RF-007.7 (cambio de contraseña) y los componentes requeridos por ese flujo; las demás funcionalidades del producto quedan fuera de este trabajo
- [x] Fase 0: cimientos
- [x] Fase 1 / SPK-006: Keycloak validado (alternativa A)
- [x] Fase 1 / SPK-007: Key Vault validado (AppRole + lectura de `secret/stockea/idp`; ver `Spk007VaultAppRoleSpikeTest`)
- [x] Fase 2: dominio (política de contraseña, excepciones de dominio, pruebas y ArquitecturaTest; `mvn clean test` en verde y cobertura del 100 % en `PoliticaContrasena` y `ContrasenaPlana`)
- [x] Fase 3: aplicación (caso de uso para cambiar contraseña y puertos de IDP, secretos, notificaciones y auditoría)
- [x] Fase 4: adaptadores de Vault, Keycloak, SMTP y auditoría
- [x] Fase 5: interfaces para RF-007.7 (endpoint REST, validación JWT, respuestas HTTP y límite de intentos)
- [ ] Fases 6 a 8 (proxy/WAF → frontend → aceptación)
- Preparación de despliegue temporal: el backend ya tiene una imagen Docker multi-etapa y acepta el puerto `PORT`; todavía no está publicado ni cuenta con WAF delante

## HTTPS
- Requisito decidido en `docs/Restricciones Técnicas.md` y ADR-007: tráfico con HTTPS/TLS 1.2 o superior; el Reverse Proxy termina TLS. No está implementado todavía: corresponde a la fase 6, y no hay frontend ni Reverse Proxy/WAF configurados
- El entorno local actual expone el Backend en HTTP `:8081`, Keycloak en HTTP `:8080` y Vault en HTTP `:8200`; estos son endpoints locales de desarrollo, no prueban cumplimiento del requisito HTTPS para el tráfico web

## Decisiones tomadas en la implementación
- Realm `stockea`; clientes `stockea-pwd-verifier` y `stockea-admin-api` (service account con manage-users)
- Access token de 5 min; política: 10+ caracteres, no número de documento ni correo, no reutilizar las últimas 4 credenciales (historial aplicado por el IDP)
- Reset de contraseña NO cierra sesiones: se llama aparte a /users/{id}/logout
- Backend se autentica ante Vault con AppRole (rol `stockea-backend`, política de solo lectura `stockea-idp-read` sobre `secret/data/stockea/idp` y `secret/data/stockea/mail`, KV v2)
- RoleId por la propiedad `stockea.vault.role-id` (variable de entorno `STOCKEA_VAULT_ROLE_ID`); SecretId desde el archivo `.vault/secret-id` (ignorado por git); nunca se imprimen en consola ni logs
- Spring Vault 4.x: `AppRoleAuthentication` acepta `VaultClient`; lectura con `opsForVersionedKeyValue("secret").get("stockea/idp")`
- Setup del Vault local: `infra/vault/setup-vault.ps1` (idempotente; habilita approle, escribe política, crea rol, siembra el secreto y regenera `.vault/role-id` y `.vault/secret-id`). Hay que ejecutarlo cada vez que se reinicie el contenedor, porque el modo dev guarda todo en memoria
- Infraestructura: `AdaptadorKeycloakProveedorIdentidad` valida la contraseña actual con el flujo password y usa client credentials para actualizarla y cerrar sesiones; `AdaptadorVaultSecretos` entrega credenciales de IDP y SMTP en tiempo de ejecución; `AdaptadorNotificacionesSmtp` usa Mailpit local y reintenta de forma asíncrona hasta 5 veces; `AdaptadorAuditoriaLog` registra intentos sin credenciales
- Interfaces de RF-007.7: `POST /api/v1/cuenta/contrasena` obtiene `sub`, `preferred_username` y `email` del JWT; no acepta identidad desde el cuerpo. JWT validado por emisor y JWKS; endpoint sin estado, respuestas no almacenables y máximo de 5 fallos por usuario en 15 minutos; el intento bloqueado se audita. El contador en memoria es para una sola instancia, según la deuda aceptada del ADR-007
- El envío SMTP autenticado toma usuario y contraseña de `secret/stockea/mail`; para sembrarlos, definir `STOCKEA_MAIL_SMTP_USERNAME` y `STOCKEA_MAIL_SMTP_PASSWORD` al ejecutar el setup. Mailpit local no requiere autenticación
- Verificación local en IntelliJ: adaptadores de fase 4, 9/9 pruebas; caso de uso de fase 3, 5/5 pruebas; ArchUnit, 2/2 reglas. La ejecución completa más reciente anterior a estas pruebas reportó 29 pruebas, 0 fallos, 0 errores y 1 omitida; falta repetir `clean test` integral con los adaptadores incluidos
- Dominio en español sin tildes ni eñes en identificadores; pruebas terminan en `Test`
- No se valida contra una lista de contraseñas comunes: riesgo aceptado en el MVP, mitigado por el límite de intentos y la detección de fuerza bruta del IDP

## Pendientes / riesgos abiertos
- Alternativa B (API de cuenta de Keycloak) sin probar
- Habilitar y verificar el recorrido web HTTPS (TLS 1.2+, HSTS y origen accesible solo desde WAF/CDN) durante la fase Reverse Proxy/WAF; la API local sigue expuesta por HTTP
- Los secretos locales pueden contener valores de relleno (`CAMBIAR-POR-EL-SECRETO-REAL`): cargar los reales de Keycloak con `STOCKEA_IDP_VERIFIER_CLIENT_SECRET` y `STOCKEA_IDP_ADMIN_CLIENT_SECRET`; para correo autenticado, cargar las credenciales SMTP con las variables correspondientes
- Autenticación del Backend ante Vault en producción: el rol actual usa `secret_id_ttl=0` (solo desarrollo); definir TTL, entrega y rotación del SecretId, y un Vault no-dev con almacenamiento persistente. También valorar `LifecycleAwareSessionManager` (renovación de token) en lugar de `SimpleSessionManager`
- SPK-008 pendiente: validar el envío por el proveedor SMTP elegido y su entrega en una bandeja real; la prueba local de Mailpit no cubre la entrega externa
