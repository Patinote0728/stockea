# Estado de implementación (actualizado: 26/10/2026)

## Stack real
- Java 26, Maven, Spring Boot 4.1.2 (Jackson 3, JUnit 6)
- Paquete raíz: co.stockea (módulo: co.stockea.account → domain/application/infrastructure/interfaces)
- Backend en puerto 8081; Postgres en contenedor, puerto configurable con POSTGRES_PORT (5433 en el equipo de desarrollo)
- Entorno local: Docker Compose con Postgres, Keycloak 26, Vault (modo dev), Mailpit
- ArchitectureTest (ArchUnit) protege la regla de dependencia

## Avance
- [x] Fase 0: cimientos
- [x] Fase 1 / SPK-006: Keycloak validado (alternativa A)
- [ ] Fase 1 / SPK-007: Key Vault
- [ ] Fases 2 a 8 (dominio → aplicación → infraestructura → interfaces → proxy/WAF → frontend → aceptación)

## Decisiones tomadas en la implementación
- Realm `stockea`; clientes `stockea-pwd-verifier` y `stockea-admin-api` (service account con manage-users)
- Access token de 5 min; política: 10+ caracteres, no usuario, no correo, no reciente (3)
- Reset de contraseña NO cierra sesiones: se llama aparte a /users/{id}/logout

## Pendientes / riesgos abiertos
- Alternativa B (API de cuenta de Keycloak) sin probar
- Lista de contraseñas comunes no configurada
- Cómo se autentica el Backend ante Vault (SPK-007)