# Estado de implementación (actualizado: 29/10/2026)

## Stack real
- Java 26 (JDK local y `java.version` del pom.xml), Maven, Spring Boot 4.1.2 (Jackson 3, JUnit 6)
- Spring Vault 4.1.1 (`spring-vault-core`, scope test por ahora; Boot 4.1.2 no gestiona su versión, se fija con la propiedad `spring-vault.version`)
- Paquete raíz: co.stockea (módulo: co.stockea.cuenta → dominio/aplicacion/infraestructura/interfaces)
- Backend en puerto 8081; Postgres en contenedor, puerto configurable con POSTGRES_PORT (5433 en el equipo de desarrollo)
- Entorno local: Docker Compose con Postgres, Keycloak 26, Vault (modo dev), Mailpit
- ArchitectureTest (ArchUnit) protege la regla de dependencia

## Avance
- [x] Fase 0: cimientos
- [x] Fase 1 / SPK-006: Keycloak validado (alternativa A)
- [x] Fase 1 / SPK-007: Key Vault validado (AppRole + lectura de `secret/stockea/idp`; ver `Spk007VaultAppRoleSpikeTest`)
- [x] Fase 2: dominio (política de contraseña, excepciones de dominio, pruebas y ArquitecturaTest; `mvn clean test` en verde y cobertura del 100 % en `PoliticaContrasena` y `ContrasenaPlana`)
- [x] Fase 3: aplicación (caso de uso para cambiar contraseña y puertos de IDP, secretos, notificaciones y auditoría)
- [ ] Fases 4 a 8 (infraestructura → interfaces → proxy/WAF → frontend → aceptación)

## Decisiones tomadas en la implementación
- Realm `stockea`; clientes `stockea-pwd-verifier` y `stockea-admin-api` (service account con manage-users)
- Access token de 5 min; política: 10+ caracteres, no número de documento ni correo, no reutilizar las últimas 4 credenciales (historial aplicado por el IDP)
- Reset de contraseña NO cierra sesiones: se llama aparte a /users/{id}/logout
- Backend se autentica ante Vault con AppRole (rol `stockea-backend`, política de solo lectura `stockea-idp-read` sobre `secret/data/stockea/idp`, KV v2)
- RoleId por la propiedad `stockea.vault.role-id` (variable de entorno `STOCKEA_VAULT_ROLE_ID`); SecretId desde el archivo `.vault/secret-id` (ignorado por git); nunca se imprimen en consola ni logs
- Spring Vault 4.x: `AppRoleAuthentication` acepta `VaultClient`; lectura con `opsForVersionedKeyValue("secret").get("stockea/idp")`
- Setup del Vault local: `infra/vault/setup-vault.ps1` (idempotente; habilita approle, escribe política, crea rol, siembra el secreto y regenera `.vault/role-id` y `.vault/secret-id`). Hay que ejecutarlo cada vez que se reinicie el contenedor, porque el modo dev guarda todo en memoria
- Dominio en español sin tildes ni eñes en identificadores; pruebas terminan en `Test`
- No se valida contra una lista de contraseñas comunes: riesgo aceptado en el MVP, mitigado por el límite de intentos y la detección de fuerza bruta del IDP

## Pendientes / riesgos abiertos
- Alternativa B (API de cuenta de Keycloak) sin probar
- Secretos de `secret/stockea/idp` con valores de relleno (`CAMBIAR-POR-EL-SECRETO-REAL`): cargar los reales de Keycloak con `STOCKEA_IDP_VERIFIER_CLIENT_SECRET` y `STOCKEA_IDP_ADMIN_CLIENT_SECRET`
- Autenticación del Backend ante Vault en producción: el rol actual usa `secret_id_ttl=0` (solo desarrollo); definir TTL, entrega y rotación del SecretId, y un Vault no-dev con almacenamiento persistente
- Al integrar Vault en la aplicación: mover `spring-vault-core` a scope compile y valorar `LifecycleAwareSessionManager` (renovación de token) en lugar de `SimpleSessionManager`
