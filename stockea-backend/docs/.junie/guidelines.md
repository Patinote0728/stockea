# Reglas del proyecto STOCKEA
- Todo el código se escribe en español: clases, métodos, variables, paquetes, pruebas,
  comentarios y mensajes. Sin tildes ni eñes en identificadores (contrasena, anio).
- Excepciones: anotaciones y APIs de librerías, nombres de productos (Keycloak, Vault),
  campos que impone un tercero (grant_type, sub) y la propiedad stockea.vault.role-id.
- Las clases de prueba terminan siempre en Test (PoliticaContrasenaTest), para que Maven las ejecute.
- Paquetes: co.stockea.<modulo>.dominio | aplicacion | infraestructura | interfaces
- Arquitectura hexagonal / Clean Architecture; dominio es Java puro: sin Spring, sin JPA, sin Jackson.
- Regla de dependencia hacia adentro, verificada por ArquitecturaTest.
- Integraciones externas solo mediante puertos (interfaces) y adaptadores.
- TDD en dominio y aplicación: pruebas primero.
- Dinero y porcentajes: BigDecimal, nunca double/float.
- Las contraseñas nunca se persisten, loguean ni guardan en almacenamiento local.
- Los secretos se obtienen de Vault en tiempo de ejecución; nunca en el repo ni en application.properties.
- Backend stateless: validar JWT en cada request.
- Stack: Spring Boot 4.x, Jackson 3 (tools.jackson), JUnit 6. No generes código para Spring Boot 3 / Jackson 2.
- Consulta docs/ antes de decidir; si algo contradice un ADR, avísame.