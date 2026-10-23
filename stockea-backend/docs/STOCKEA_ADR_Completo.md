
## ADR-001: Estrategia de operación offline

| Campo                   | Valor                                                |
| ----------------------- | ---------------------------------------------------- |
| Estado                  | **Aprobado**                                         |
| Escenarios relacionados | ESC-CAL-DIS-0001, ESC-CAL-DIS-0005, ESC-CAL-CON-0001 |
| Atributos de calidad    | Disponibilidad, Confiabilidad                        |

### Contexto

STOCKEA debe operar sin internet en tiendas de barrio con conectividad intermitente. El cajero no puede dejar de vender porque se cayó el WiFi. Necesitamos definir cómo el frontend persiste datos localmente y cómo se comporta sin conexión al servidor.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**Offline-first con proceso en segundo plano del navegador + base de datos local embebida + librería de gestión de caché**|El frontend siempre opera contra datos locales, la experiencia es idéntica online y offline. La librería de caché gestiona el almacenamiento y la sincronización diferida. La capa de acceso a la base de datos local simplifica su manejo.|Complejidad alta en la primera implementación. El equipo no tiene experiencia previa. Requiere gestionar conflictos de sincronización.|
|B|Online-first con caché de respaldo (almacenamiento simple del navegador)|Implementación simple. El sistema intenta conectar al servidor y solo usa caché si falla.|El almacenamiento simple del navegador tiene límite de 5MB (insuficiente para catálogo completo). Es síncrono y bloquea el hilo principal. No soporta consultas indexadas. Experiencia degradada offline.|
|C|Aplicación de escritorio con motor de base de datos embebido|Base de datos local robusta con SQL completo. No depende del navegador.|Requiere instalador, distribución manual, actualizaciones complejas. Contradice la decisión de aplicación web instalable. Duplica el esfuerzo de desarrollo (web + escritorio).|

### Decisión

**Alternativa A: Offline-first con proceso en segundo plano del navegador + base de datos local embebida + librería de gestión de caché.**

### Justificación

La alternativa A es la única que cumple simultáneamente con la promesa de operación offline transparente (ESC-CAL-DIS-0001) y con la restricción de ser una aplicación web instalable sin tienda de apps. La alternativa B falla por las limitaciones técnicas del almacenamiento simple del navegador. La alternativa C contradice la restricción estratégica de canal web exclusivo y multiplica el esfuerzo de un desarrollador único


### Tácticas arquitectónicas

- **Cache-First**: assets estáticos siempre se sirven desde caché local.
- **StaleWhileRevalidate**: datos maestros (productos, lotes, precios) se sirven desde caché y se actualizan en background.
- **Sincronización diferida en segundo plano**: operaciones de escritura (ventas, ajustes) se encolan y envían automáticamente al recuperar conexión.
- **Esquema local en base de datos embebida del navegador**: tablas que reflejan las entidades del dominio (productos, lotes, ventas pendientes, turnos de caja).

### PoC / Spike requerido

|Spike|Objetivo|Criterio de éxito|Duración estimada|
|---|---|---|---|
|SPK-001|Validar que la base de datos local embebida y la librería de caché permiten registrar una venta completa sin conexión y sincronizarla al reconectar|El spike reproduce el flujo: abrir caja → agregar producto → confirmar venta → cerrar caja, todo offline. Al reconectar, la venta aparece en el servidor.|3 días|

### Consecuencias

- Se acepta la complejidad de gestionar dos fuentes de datos (base de datos local y base de datos remota).
- Se requiere definir una estrategia de resolución de conflictos (ver ADR-002).
- El equipo debe invertir tiempo en aprendizaje de las herramientas de caché y persistencia local antes de implementar funcionalidades de negocio.

---

## ADR-002: Estrategia de resolución de conflictos en sincronización

| Campo                   | Valor                              |
| ----------------------- | ---------------------------------- |
| Estado                  | **Aprobado**                       |
| Escenarios relacionados | ESC-CAL-DIS-0008, ESC-CAL-REN-0011 |
| Atributos de calidad    | Confiabilidad, Disponibilidad      |

### Contexto

Cuando dos dispositivos operan offline y venden del mismo lote, al sincronizar puede haber sobreventa (la suma de ambas ventas excede el stock real). Necesitamos definir quién gana, cómo se notifica al perdedor y qué pasa con la venta rechazada.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**Servidor como árbitro final (first-write-wins)**|Simple de implementar. Regla clara: la primera venta que llega al servidor se acepta, las posteriores conflictuadas se rechazan. El cajero recibe notificación con motivo.|El cajero que sincroniza segundo pierde aunque su venta fue legítima en el momento. Requiere flujo de resolución manual.|
|B|Merge automático inteligente (reasignar a otro lote)|Experiencia transparente para el cajero. El sistema busca stock en otros lotes del mismo producto.|Complejidad altísima: requiere lógica de reasignación que respete FIFO, descuentos por lote y trazabilidad. Riesgo de bugs difíciles de detectar.|
|C|Reserva de stock por dispositivo (stock particionado)|Elimina los conflictos de raíz asignando cuotas por dispositivo.|Subutiliza el inventario (un dispositivo puede tener stock reservado sin vender mientras otro se queda sin). No escala si hay más de 2 dispositivos. Requiere configuración previa del dueño.|

### Decisión

**Alternativa A: Servidor como árbitro final (first-write-wins).**

### Justificación

La alternativa A tiene la complejidad justa para un MVP: regla clara, implementación directa, conflictos poco frecuentes en el contexto real (2 cajeros vendiendo el mismo producto exacto al mismo tiempo sin internet). La alternativa B introduce riesgo técnico desproporcionado. La alternativa C penaliza la operación normal para resolver un caso edge.

### Tácticas arquitectónicas

- **Bloqueo pesimista**: bloqueo exclusivo sobre el registro del lote al procesar la sincronización para serializar las escrituras concurrentes.
- **Estado CONFLICTO**: nuevo valor en el enum de estado de Venta para marcar operaciones rechazadas sin eliminarlas.
- **Notificación al cajero**: al sincronizar, si una operación se rechaza, el sistema la marca como CONFLICTO y la muestra en la pantalla de operaciones pendientes/conflictivas del dispositivo la próxima vez que esté en línea, junto con el motivo y las opciones de resolución (cambiar de lote, cancelar, ajustar cantidad). No se depende de notificaciones push ni de ningún canal adicional al ya definido (correo, para comprobantes y alertas de inventario).
- **Log de conflictos**: cada conflicto queda registrado con ambas operaciones, el lote afectado y el resultado para auditoría.

### PoC / Spike requerido

|Spike|Objetivo|Criterio de éxito|Duración estimada|
|---|---|---|---|
|SPK-002|Simular dos dispositivos vendiendo del mismo lote offline y validar la resolución de conflictos en el servidor|Dispositivo A sincroniza 8 unidades (de 10 disponibles) y se acepta. Dispositivo B sincroniza 5 unidades del mismo lote y se rechaza con estado CONFLICTO y notificación al cajero. Stock final: 2 unidades (no negativo).|2 días|

### Consecuencias

- Se acepta que el segundo cajero deberá resolver manualmente las ventas conflictuadas.
- Se requiere agregar CONFLICTO como estado al modelo de Venta.
- Se requiere diseñar la pantalla de resolución de conflictos en el frontend.
- En iteración 2 se podría implementar la alternativa B como mejora si los conflictos son frecuentes.

---

## ADR-003: Estrategia de descuento de stock por lote (FIFO por vencimiento)

| Campo                   | Valor                              |
| ----------------------- | ---------------------------------- |
| Estado                  | **Aprobado**                       |
| Escenarios relacionados | ESC-CAL-CON-0014, ESC-CAL-CON-0025 |
| Atributos de calidad    | Confiabilidad                      |

### Contexto

Un mismo producto puede tener múltiples lotes activos con fechas de vencimiento distintas. Al vender, el sistema debe decidir de cuál lote descontar. La decisión impacta directamente las alertas de vencimiento, los descuentos y la trazabilidad.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**FIFO por fecha de vencimiento (primero el que vence antes)**|Reduce la merma vendiendo primero lo que está por vencer. Coincide con la práctica real de los negocios de perecederos. Maximiza la efectividad de los descuentos por vencimiento.|Si la venta excede el stock del primer lote, requiere fraccionamiento entre lotes (mayor complejidad).|
|B|FIFO por fecha de ingreso (primero lo que entró antes)|Implementación simple: ordenar por fecha de registro de la entrada.|No optimiza para perecederos: un lote con vencimiento lejano que entró primero se vendería antes que uno con vencimiento próximo que entró después. Contradice la práctica real del negocio.|
|C|Selección manual por el cajero|Máxima flexibilidad: el cajero decide de qué lote vender.|Agrega fricción a cada venta. El cajero en hora pico no tiene tiempo de evaluar lotes. Decisión propensa a error humano. Contradice el principio de simplicidad operativa.|

### Decisión

**Alternativa A: FIFO por fecha de vencimiento.**

### Justificación

STOCKEA es un sistema diseñado específicamente para perecederos. Vender primero lo que vence antes es la práctica estándar del sector y el comportamiento esperado por el dueño. La complejidad adicional del fraccionamiento entre lotes es manejable y se justifica por la reducción de merma que genera.

### Tácticas arquitectónicas

- **Consulta ordenada**: al vender, el sistema consulta los lotes del producto con estado DISPONIBLE o PROXIMO_A_VENCER ordenados por fecha de vencimiento ascendente.
- **Fraccionamiento automático**: si la cantidad vendida excede el primer lote, se generan múltiples ItemVenta, cada uno referenciando al lote del que se descontó y la cantidad exacta.
- **Validación de stock total**: antes de confirmar, se verifica que la suma de cantidadDisponible de todos los lotes vigentes sea suficiente para la venta.
- **Descuento por lote**: el descuento activo se aplica solo a las unidades vendidas del lote que originó el descuento, no a las unidades de otros lotes del mismo producto.

### PoC / Spike requerido

|Spike|Objetivo|Criterio de éxito|Duración estimada|
|---|---|---|---|
|SPK-003|Validar que el fraccionamiento de venta entre lotes funciona correctamente con descuentos activos|Producto con lote A (7 unidades, vence 10/06, descuento 20%) y lote B (5 unidades, vence 15/07, sin descuento). Venta de 10 unidades: 7 del lote A a precio con descuento + 3 del lote B a precio completo. Total correcto. Stock correcto. Comprobante correcto.|2 días|

### Consecuencias

- ItemVenta siempre referencia exactamente un lote (no un producto genérico).
- Una venta de un producto puede generar múltiples ItemVenta si requiere fraccionamiento.
- El comprobante debe mostrar un solo producto al cliente aunque internamente se descontaron de varios lotes (el detalle de lote es para auditoría, no para el cliente).

---

## ADR-004: Inmutabilidad de registros de auditoría

| Campo                   | Valor                                  |
| ----------------------- | -------------------------------------- |
| Estado                  | **Aprobado**                           |
| Escenarios relacionados | ESC-CAL-AUD-0016, ESC-CAL-AUD-0021     |
| Atributos de calidad    | Capacidad para ser auditado, Seguridad |

### Contexto

Los registros de ventas, ajustes de inventario, aperturas de caja y cambios de precio deben ser inalterables para cumplir con el Código de Comercio de Colombia y para que el dueño confíe en el sistema como herramienta de control. Necesitamos definir cómo se implementa esta inmutabilidad.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**Inmutabilidad a nivel de aplicación + base de datos**|Doble protección: la API no expone endpoints de modificación y la base de datos no tiene permisos para actualizar/eliminar en tablas de auditoría. Correcciones mediante registros nuevos con motivo CORRECCION.|Requiere diseñar flujos de corrección que generen nuevos registros en vez de editar. Mayor número de registros en la base de datos a largo plazo.|
|B|Inmutabilidad solo a nivel de aplicación|Más simple: solo se controla en el código que no haya endpoints de modificación.|Un administrador de base de datos o un bug podrían modificar registros directamente en la base de datos. No hay protección de última línea.|
|C|Estructura de encadenamiento criptográfico (hash chain) para registros|Máxima garantía criptográfica de inmutabilidad. Permite auditoría externa verificable.|Complejidad desproporcionada para el contexto. Rendimiento degradado en escrituras. El equipo no tiene experiencia. Sobre-ingeniería para un MVP.|

### Decisión

**Alternativa A: Inmutabilidad a nivel de aplicación + base de datos.**

### Justificación

La doble protección (API + BD) es la implementación pragmática que cubre el requisito legal y de confianza sin la complejidad de un esquema criptográfico. La alternativa B deja un hueco de seguridad real. La alternativa C es sobre-ingeniería.

### Tácticas arquitectónicas

- **API sin endpoints destructivos**: no existen rutas de actualización/eliminación para las entidades de auditoría (Venta confirmada, AjusteInventario, TurnoCaja cerrado, HistorialPrecio).
- **Permisos de BD restrictivos**: el usuario de base de datos que usa la aplicación tiene revocados los permisos de actualización y eliminación sobre las tablas de auditoría. Solo inserción y lectura.
- **Patrón de corrección**: toda corrección es un nuevo registro de tipo AjusteInventario con motivo CORRECCION que referencia al registro original, creando una cadena auditable.
- **Validación en dominio**: las entidades de dominio no exponen métodos de modificación para campos inmutables después de la creación.

### PoC / Spike requerido

No se requiere spike técnico. La implementación es directa con herramientas conocidas (framework de seguridad del backend + permisos de base de datos). Se valida con pruebas de integración.

### Consecuencias

- Los errores de usuario no se "borran", se corrigen con registros adicionales.
- El volumen de datos crece más rápido que con registros editables.
- Se debe explicar al usuario final que "corregir" significa "crear un ajuste nuevo" y no "editar el registro".

---

## ADR-005: Control de acceso basado en roles acumulativos (RBAC)

|Campo|Valor|
|---|---|
|Estado|**Aprobado**|
|Fecha|2025-06-01|
|Escenarios relacionados|ESC-CAL-SEG-0006, ESC-CAL-SEG-0004|
|Atributos de calidad|Seguridad|

### Contexto

STOCKEA tiene 3 roles (Cajero, Supervisor, Administrador) con permisos diferentes. Necesitamos definir si los roles son independientes (cada uno con su set de permisos) o acumulativos (cada rol hereda los permisos del inferior).

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**Roles acumulativos (Cajero ⊂ Supervisor ⊂ Administrador)**|Simple: el Administrador puede hacer todo lo del Supervisor y todo lo del Cajero. El dueño de una tienda pequeña con un solo empleado puede operar con un solo usuario Administrador sin perder funcionalidad. Coincide con la realidad del negocio.|Menos granular: no se puede crear un Supervisor que NO pueda vender, por ejemplo.|
|B|Roles independientes con permisos explícitos por operación|Máxima granularidad: cada rol tiene exactamente los permisos que necesita sin heredar.|Complejidad de configuración: el dueño tendría que entender y gestionar permisos individuales. Contradice el principio de simplicidad para usuarios sin experiencia técnica.|
|C|Un solo rol con permisos configurables por usuario|Máxima flexibilidad por usuario individual.|Cada usuario requiere configuración manual. En un negocio con 3-5 empleados es innecesario y confuso.|

### Decisión

**Alternativa A: Roles acumulativos.**

### Justificación

En una tienda de barrio con 1-5 empleados, la simplicidad del modelo acumulativo supera a la granularidad de los otros modelos. El dueño entiende intuitivamente que "el administrador puede hacer todo" sin necesidad de configurar permisos. La alternativa B agrega complejidad sin valor para el mercado objetivo.

### Tácticas arquitectónicas

- **Enum de roles con orden jerárquico**: CAJERO(1), SUPERVISOR(2), ADMINISTRADOR(3). La verificación de permisos compara el nivel numérico del rol del usuario con el nivel mínimo requerido por la operación.
- **Anotaciones de autorización en servicios de dominio**: cada método de servicio declara el rol mínimo requerido. La validación ocurre en el backend, no solo en el frontend.
- **Validación offline**: el token de sesión firmado contiene el rol del usuario. En modo offline, la validación se ejecuta contra el token cacheado localmente.
- **Filtrado de UI por rol**: el frontend oculta las opciones no permitidas según el rol, pero la protección real está en el backend.

### PoC / Spike requerido

No se requiere spike. RBAC es un patrón bien documentado con soporte nativo en frameworks de seguridad estándar del backend.

### Consecuencias

- Un usuario solo tiene un rol a la vez.
- Si en el futuro se necesita un rol que rompa la jerarquía (ej. un "Auditor" que puede ver todo pero no modificar nada), se deberá extender el modelo.
- El frontend debe ocultar opciones no disponibles pero nunca confiar en eso como única protección.

---

## ADR-006: Motor de recomendaciones de compra basado en reglas

| Campo                   | Valor            |
| ----------------------- | ---------------- |
| Estado                  | **Propuesto**    |
| Escenarios relacionados | ESC-CAL-CON-0029 |
| Atributos de calidad    | Confiabilidad    |

### Contexto

STOCKEA debe generar recomendaciones de compra cruzando velocidad de venta, stock actual y vencimientos. Necesitamos definir si usamos un motor basado en reglas simples o algoritmos de aprendizaje automático.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**Motor basado en reglas deterministas (velocidad promedio × días objetivo − stock actual)**|Predecible, testeable con TDD, explicable al dueño ("te recomiendo 50 unidades porque vendes 10 por día y quieres 7 días de stock"). No requiere datos históricos masivos para funcionar.|No detecta patrones estacionales ni tendencias complejas. Asume demanda lineal.|
|B|Aprendizaje automático (regresión o series temporales)|Puede detectar estacionalidad y tendencias no lineales. Mayor precisión con datos suficientes.|Requiere meses de datos históricos para entrenar. Caja negra: el dueño no entiende por qué se le recomienda X cantidad. Complejidad desproporcionada para MVP. El equipo no tiene experiencia en este tipo de modelos.|

### Decisión

**Alternativa A: Motor basado en reglas deterministas.**

### Justificación

Para un MVP, la transparencia y la testeabilidad del motor de reglas superan la precisión teórica de un modelo de aprendizaje automático. El dueño de una tienda de barrio necesita entender POR QUÉ el sistema le dice que compre 50 unidades. "Porque vendes 10 por día y quieres 7 días de stock, y hoy tienes 20" es una explicación que genera confianza. "Porque el modelo lo predice" no.

### Tácticas arquitectónicas

- **Fórmula central**: cantidadSugerida = (velocidadPromedio × díasObjetivo) − stockActual. Si el resultado es ≤ 0, no se genera recomendación.
- **Protección contra división por cero**: si velocidadPromedio = 0 (producto sin ventas), se muestra "sin datos suficientes" en vez de un número.
- **Regla anti-contradicción (RN-REC-008)**: si diasCobertura > diasRestantes del lote más próximo, el producto va al motor de descuentos, no al de recomendaciones.
- **Niveles de urgencia**: CRITICO (< 3 días cobertura), ALTO (< 7 días), MEDIO (resto).
- **Snapshot inmutable**: stockActualMomento se congela al generar la recomendación para reproducibilidad.

### PoC / Spike requerido

|Spike|Objetivo|Criterio de éxito|Duración estimada|
|---|---|---|---|
|SPK-004|Validar que la fórmula genera recomendaciones coherentes con datos reales de un negocio piloto|Ejecutar el motor con datos simulados de 30 días de ventas de 50 productos. Las recomendaciones deben ser coherentes: no recomendar comprar lo que se está venciendo, no recomendar cantidad negativa, priorizar correctamente por urgencia.|2 días|

### Consecuencias

- Las recomendaciones son explicables y auditables.
- No se detectarán patrones estacionales complejos en el MVP.
- En iteración 2, se podría incorporar un modelo estadístico simple (promedio ponderado con mayor peso en semanas recientes) sin reemplazar la fórmula base.
- El período de cálculo (por defecto 7 días) es configurable por el administrador para ajustar a la realidad de su negocio.

---

## ADR-007: No incluir un API Gateway dedicado en el MVP

| Campo                   | Valor                                                |
| ----------------------- | ---------------------------------------------------- |
| Estado                  | **Aprobado**                                         |
| Escenarios relacionados | ESC-CAL-ESC-0008, ESC-CAL-SEG-0006, ESC-CAL-DIS-0005 |
| Atributos de calidad    | Rendimiento, Seguridad, Escalabilidad                |

### Contexto

STOCKEA es multi-tenant y necesita limitar el consumo por negocio según su plan de suscripción (Básico/Pro/Enterprise), además de validar el token de sesión y el rol en cada request. El patrón habitual para centralizar estas responsabilidades es un API Gateway dedicado.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|API Gateway dedicado|Rate limiting nativo y declarativo. Punto único para políticas de seguridad.|Costo y complejidad operativa adicional. Punto único de fallo nuevo. Para validar el plan del negocio necesita consultar la base de datos del backend igual, duplicando lógica. Un rate limit genérico puede bloquear ráfagas legítimas de sincronización offline (ESC-CAL-DIS-0005).|
|B|**Reverse Proxy simple + middleware de autenticación y rate limiting en el Backend**|El backend ya conoce el plan del negocio (vive en su propia base de datos) y el rol del usuario. Puede aplicar políticas distintas por endpoint (más permisiva para sincronización, más restrictiva para consultas). No agrega infraestructura ni costo adicional.|El rate limiting queda acoplado al código del backend en vez de configuración declarativa externa.|
|C|Sin ningún control de límite de consumo|Ninguna ventaja relevante.|Viola directamente el riesgo documentado en ESC-CAL-ESC-0008: una carga anormal de un negocio podría afectar a otros en la misma infraestructura.|

### Decisión

**Alternativa B: Reverse Proxy + middleware de autenticación y rate limiting dentro del Backend.**

### Justificación

Un Gateway que solo reenvía el token sin validarlo no aporta valor de seguridad real, porque la validación efectiva ocurre igual en el backend (ESC-CAL-SEG-0006 exige además validación local offline-aware, que un Gateway no puede resolver). Si el Gateway validara el token y aplicara el rate limiting él mismo, necesitaría conocer el plan de suscripción del negocio, dato que vive en la base de datos del backend — esto duplica lógica o agrega una llamada adicional. Dado el patrón de tráfico en ráfagas por sincronización offline, es más seguro diferenciar políticas por endpoint dentro del backend que aplicar un límite genérico externo.

### Tácticas arquitectónicas

- El Reverse Proxy únicamente termina TLS y reenvía tráfico, sin inspeccionar el token.
- El componente Auth & Access Control del Backend valida el token contra el proveedor de identidad, aplica autorización por rol y aplica rate limiting (librería en memoria) con políticas diferenciadas: más permisiva para el endpoint de sincronización, más restrictiva para dashboards y reportes.

### PoC / Spike requerido

|Spike|Objetivo|Criterio de éxito|Duración estimada|
|---|---|---|---|
|SPK-005 (opcional, baja prioridad)|Validar que una librería de rate limiting en memoria soporta el rate limiting esperado sin degradar el rendimiento del backend en una sola instancia|Prueba de carga simple simulando el tráfico de 100 negocios concurrentes; latencia del backend no se degrada más del 20% con el middleware activo.|1 día|

### Consecuencias

- Se reevaluará esta decisión si se agregan más clientes además de la aplicación web (app móvil nativa, integraciones de terceros con socios), si el backend se descompone en microservicios, o si el rate limiting en código se vuelve difícil de mantener.
- La librería de rate limiting en memoria funciona bien con una sola instancia de backend. Si en el futuro el backend se escala horizontalmente a varias instancias, el contador de rate limiting deja de ser compartido entre ellas y se debe migrar a un almacén compartido. Se documenta como deuda técnica aceptada conscientemente para el MVP.

---

## ADR-008: Sincronización offline vía REST + outbox pattern (sin Message Broker dedicado)

| Campo                   | Valor                                                |
| ----------------------- | ---------------------------------------------------- |
| Estado                  | **Aprobado**                                         |
| Escenarios relacionados | ESC-CAL-DIS-0001, ESC-CAL-DIS-0005, ESC-CAL-DIS-0008 |
| Atributos de calidad    | Disponibilidad, Rendimiento, Confiabilidad           |

### Contexto

El frontend debe encolar operaciones localmente sin conexión y sincronizarlas automáticamente al reconectar (RF-011.1, RF-011.2). Existen dos patrones típicos de transporte hacia el backend: comunicación síncrona REST con cola local en el cliente (outbox pattern), o un Message Broker que desacopla productor y consumidor de forma asíncrona.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**REST + outbox local en el cliente (base de datos local del navegador)**|Arquitectura simple, sin infraestructura adicional que mantener ni pagar. Suficiente para el volumen esperado (pocos dispositivos por negocio).|Si el backend está caído (no solo el cliente sin internet), no hay amortiguación de la carga; el cliente debe reintentar por su cuenta.|
|B|Message Broker entre frontend y backend/worker|Desacopla productor y consumidor. Durabilidad garantizada por el broker. Resiste caídas del consumidor sin perder mensajes.|Nuevo contenedor de infraestructura que mantener, monitorear y potencialmente pagar en la nube. Choca con la restricción de presupuesto (USD 50/mes por 100 negocios). Sobre-ingeniería para el volumen real (un dispositivo por negocio en la mayoría de los casos, no miles de eventos entre microservicios).|

### Decisión

**Alternativa A: REST + outbox pattern, sin Message Broker dedicado.**

### Justificación

El volumen esperado (decenas de operaciones por negocio por sincronización) no justifica la infraestructura y complejidad operativa de un broker. La resolución de conflictos (ADR-002) se resuelve con validación optimista en el backend al recibir la sincronización vía REST, sin necesidad de un intermediario asíncrono.

### Tácticas arquitectónicas

- El Sync Controller recibe lotes de operaciones con identificador único y marca de tiempo.
- El procesamiento es idempotente: reenviar la misma operación no la duplica.
- Sync Conflict Resolution valida cada operación contra el stock real; la primera operación válida gana (ver ADR-002).

### PoC / Spike requerido

Cubierto por SPK-001 y SPK-002, ya definidos en ADR-001 y ADR-002. No se requiere spike adicional.

### Consecuencias

- Si el backend está temporalmente caído durante una reconexión, el cliente debe reintentar con backoff exponencial (a implementar en el frontend); no existe una cola intermedia que amortigüe esa caída.
- Se reevaluará esta decisión si el número de dispositivos concurrentes por negocio crece significativamente, o si el backend necesita desacoplarse en varios servicios que consuman los mismos eventos de sincronización.

---

## ADR-009: Acceso a datos centralizado (Unit of Work) en vez de repositorios por dominio

| Campo                   | Valor                                                                  |
| ----------------------- | ---------------------------------------------------------------------- |
| Estado                  | **Aprobado**                                                           |
| Escenarios relacionados | ESC-CAL-CON-0001, ESC-CAL-CON-0003, ESC-CAL-CON-0014, ESC-CAL-ESC-0008 |
| Atributos de calidad    | Confiabilidad, Escalabilidad                                           |

### Contexto

Varias operaciones de negocio (registrar venta, ajustar inventario) requieren modificar más de una entidad relacionada (venta + ajuste de lote + stock) dentro de una sola transacción atómica. Además, toda consulta debe filtrar automáticamente por business_id (multi-tenant). Se debe decidir si el acceso a datos se organiza como un repositorio compartido (Unit of Work) o como repositorios independientes por dominio (SalesRepository, InventoryRepository, etc.).

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**Repositorio compartido (Data Repository / Unit of Work)**|Garantiza fácilmente transacciones atómicas cross-entidad (venta+lote+stock en una sola unidad de trabajo). Un solo punto donde se aplica el filtro multi-tenant de forma consistente.|Concentra responsabilidad en un solo componente; puede leerse como un "God Object" si no se documenta el límite del patrón.|
|B|Repositorios independientes por dominio|Más alineado con Domain-Driven Design estricto. Cada controller/servicio habla solo con su propio repositorio.|Coordinar una transacción atómica entre varios repositorios requiere un mecanismo adicional explícito (Unit of Work manual o Saga), duplicando la complejidad que se busca evitar. Mayor riesgo de que dos repositorios olviden aplicar el filtro multi-tenant de forma consistente.|

### Decisión

**Alternativa A: Data Repository como Unit of Work único, documentado explícitamente como patrón consciente.**

### Justificación

Los frameworks de persistencia estándar del backend ya proveen manejo transaccional nativo sobre un contexto de persistencia compartido. Envolver múltiples entidades relacionadas en una transacción es más simple y menos propenso a errores con un Unit of Work único que coordinando varios repositorios independientes. El riesgo de "God Object" se mitiga documentando el límite de responsabilidad: el Data Repository solo hace persistencia y filtrado multi-tenant, nunca lógica de negocio (que vive en los domain services y controllers).

### Tácticas arquitectónicas

- Filtro automático por business_id inyectado en cada consulta.
- Método transaccional explícito para operaciones cross-entidad (ej. registrarVentaConAjusteDeLote).
- Separación estricta de responsabilidades: el repositorio no contiene reglas de negocio, solo persistencia.

### PoC / Spike requerido

No se requiere spike técnico (patrón estándar de los frameworks de persistencia del backend).

### Consecuencias

- Si el dominio crece significativamente y el Data Repository se vuelve inmanejable en tamaño, se reevaluará dividirlo en repositorios por agregado (Sales Aggregate, Inventory Aggregate), manteniendo el Unit of Work como fachada que los coordina sin perder la garantía transaccional.

---

## ADR-010: Infraestructura de soporte self-hosted y open source

| Campo                   | Valor                                                                                       |
| ----------------------- | ------------------------------------------------------------------------------------------- |
| Estado                  | **Aprobado**                                                                                |
| Escenarios relacionados | Restricciones de presupuesto y de stack tecnológico (documento de restricciones de negocio) |
| Atributos de calidad    | Viabilidad económica del producto (transversal)                                             |

### Contexto

STOCKEA necesita componentes de infraestructura de soporte: proveedor de identidad (IDP), almacenamiento de objetos (comprobantes/backups), gestor de secretos y observabilidad (APM). Existen alternativas SaaS gestionadas y alternativas open source autohospedadas equivalentes para cada categoría.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**Self-hosted open source (proveedor de identidad, almacenamiento de objetos, gestor de secretos y stack de observabilidad, todos open source) sobre el mismo servidor**|Sin costo de licencia. Cumple la restricción de presupuesto (USD 50/mes por 100 negocios) y la restricción de construir el MVP con herramientas open source y gratuitas.|Responsabilidad operativa propia (mantenimiento, actualizaciones, parches de seguridad). Sin soporte comercial.|
|B|Componentes equivalentes SaaS gestionados (identidad, almacenamiento de objetos, gestión de secretos, observabilidad)|Cero mantenimiento operativo. Alta disponibilidad garantizada por el proveedor. Soporte comercial.|Costo mensual acumulado que supera con facilidad el techo de presupuesto definido como restricción de negocio. Varios tienen tiers gratuitos muy limitados que no escalan con el crecimiento esperado.|

### Decisión

**Alternativa A: Self-hosted open source sobre el mismo servidor de aplicación.**

### Justificación

La restricción de presupuesto es explícita y crítica para la viabilidad del modelo de negocio (planes de suscripción entre COP 30.000 y 80.000 mensuales por negocio). Cada componente gestionado adicional introduce un costo recurrente que compromete ese margen a escala. La complejidad operativa de autohospedar estos componentes es asumible: son herramientas maduras, con documentación extensa, que solo requieren configuración y despliegue, no desarrollo propio.

### Tácticas arquitectónicas

- Todos los componentes de soporte corren como contenedores dentro del mismo servidor de aplicación (ver Diagrama de Despliegue).
- Se documenta el plan de migración a servicios gestionados como camino de escalamiento futuro si el volumen de negocios lo justifica.

### PoC / Spike requerido

No se requiere spike de riesgo técnico (herramientas estándar de la industria). Se recomienda una validación de configuración inicial con una herramienta de orquestación de contenedores local antes de comprometer tiempos de desarrollo.

### Consecuencias

- El equipo asume la responsabilidad de mantener actualizados estos componentes (parches de seguridad, respaldo de su propia configuración).
- Si el negocio escala significativamente, se evaluará migrar selectivamente componentes críticos (por ejemplo, la base de datos o el proveedor de identidad) a servicios gestionados, priorizando por impacto en disponibilidad.

---

## ADR-011: Verificación de la contraseña actual y actualización de la credencial en el IDP

| Campo                   | Valor                                  |
| ----------------------- | -------------------------------------- |
| Estado                  | **Propuesto** (pendiente de SPK-006)   |
| Escenarios relacionados | ESC-CAL-SEG-0012, ESC-CAL-SEG-0014     |
| Atributos de calidad    | Seguridad                              |

### Contexto

RF-007.7 exige que el usuario ingrese su contraseña actual y la nueva. La API administrativa del IDP permite establecer una contraseña nueva, pero no verifica la contraseña actual. Necesitamos definir cómo se valida la actual y cómo se actualiza la credencial manteniendo el Backend sin estado.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**El Backend verifica la contraseña actual contra el token endpoint del IDP (grant de contraseña con un cliente confidencial dedicado) y actualiza la credencial con la API administrativa usando un service account. Ambos secretos se obtienen del Key Vault.**|Control total del flujo en el Backend. Permite aplicar rate limiting y auditoría propios. Coincide con la arquitectura de componentes ya definida (Backend → Key Vault → IDP).|El grant de contraseña está en desuso en OAuth 2.1, aunque sigue disponible en el IDP. Los intentos fallidos cuentan para la detección de fuerza bruta del IDP. El service account requiere permisos de administración sobre usuarios.|
|B|Invocar la API de cuenta del IDP con el token del propio usuario, enviando contraseña actual y nueva|No requiere service account con privilegios de administración. El IDP valida todo.|Depende de que la versión del IDP soporte ese endpoint; menos control sobre auditoría y rate limiting. A validar en el spike.|
|C|Redirigir al usuario a la pantalla de cuenta alojada por el IDP|Sin código propio de manejo de contraseñas.|Rompe la experiencia de la PWA y no permite el correo de aviso desde el Backend de forma natural.|

### Decisión

**Alternativa A**, sujeta a la validación de SPK-006. Si el spike demuestra que la alternativa B es soportada de forma estable por la versión del IDP, se prefiere B por menor privilegio.

### Justificación

La alternativa A es la que encaja con la arquitectura documentada y mantiene el control del flujo (rate limiting, auditoría y notificación) en el Backend. Se acepta temporalmente el uso del grant de contraseña, restringido a un cliente dedicado y confidencial.

### Tácticas arquitectónicas

- **Cliente dedicado** en el IDP con grant de contraseña habilitado únicamente para este caso de uso.
- **Service account de mínimo privilegio** para actualizar credenciales, con su secreto en el Key Vault.
- **Puerto IdentityProviderPort**: el dominio no conoce Keycloak.
- **Sin persistencia de contraseñas** en el Backend: viajan en memoria solo durante la solicitud y se excluyen de logs y trazas.
- **Timeouts** hacia el IDP.

### PoC / Spike requerido

|Spike|Objetivo|Criterio de éxito|Duración estimada|
|---|---|---|---|
|SPK-006|Validar la verificación de contraseña actual y la actualización de credencial contra el IDP, además del cierre de las demás sesiones|Con contraseña actual correcta se actualiza la credencial; con una incorrecta se rechaza sin cambios; tras el cambio, los refresh tokens anteriores quedan revocados.|2 días|

### Consecuencias

- Se acepta el uso temporal del grant de contraseña en un cliente restringido.
- Los intentos fallidos pueden bloquear temporalmente la cuenta; se documenta el desbloqueo por el Administrador.
- Se reevaluará la decisión al actualizar el IDP o si se prefiere la alternativa B.

---

## ADR-012: Invalidación de sesiones tras el cambio de contraseña en un Backend stateless

| Campo                   | Valor                                                |
| ----------------------- | ---------------------------------------------------- |
| Estado                  | **Propuesto**                                        |
| Escenarios relacionados | ESC-CAL-SEG-0012, ESC-CAL-SEG-0004                   |
| Atributos de calidad    | Seguridad                                            |

### Contexto

El Backend es stateless y valida JWT. Tras cambiar la contraseña, las sesiones y tokens ya emitidos podrían seguir siendo usados por un tercero que haya obtenido acceso antes del cambio. Además, ADR-005 permite validación offline contra el token cacheado.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**Tokens de acceso de vida corta (por defecto 5 minutos) y revocación de las sesiones del usuario en el IDP al cambiar la contraseña**|Mantiene el Backend sin estado. Los refresh tokens se invalidan de inmediato, por lo que nadie puede renovar el acceso.|Un token de acceso ya emitido sigue siendo válido hasta que expire. Los dispositivos offline conservan el token cacheado hasta reconectar.|
|B|Lista de revocación de tokens en el Backend|Invalidación inmediata.|Introduce estado en el Backend, contradice la restricción stateless y complica la escala.|
|C|No invalidar sesiones|Implementación mínima.|Una sesión comprometida sobrevive al cambio de contraseña, anulando su propósito.|

### Decisión

**Alternativa A.**

### Justificación

Es el equilibrio que respeta la restricción stateless: la ventana de exposición queda acotada a la vida del token de acceso, y la renovación queda bloqueada de inmediato.

### Tácticas arquitectónicas

- **Vida corta del access token** (valor inicial: 5 minutos).
- **Revocación de sesiones** del usuario en el IDP como parte del caso de uso.
- **Reautenticación** en el dispositivo actual tras el cambio, o renovación con la nueva credencial.

### PoC / Spike requerido

Cubierto por SPK-006.

### Consecuencias

- Existe una ventana residual de hasta la vida del access token.
- Los dispositivos offline mantienen sesión hasta reconectar; se acepta como riesgo del modo offline.
- Si se exige invalidación inmediata, se reevaluará la alternativa B.

---

## ADR-013: Política de contraseñas con el IDP como fuente de verdad

| Campo                   | Valor                              |
| ----------------------- | ---------------------------------- |
| Estado                  | **Propuesto**                      |
| Escenarios relacionados | ESC-CAL-SEG-0012                   |
| Atributos de calidad    | Seguridad, Usabilidad              |

### Contexto

La nueva contraseña debe cumplir una política mínima. Si cada capa define su propia política, aparecen reglas contradictorias entre cliente, Backend e IDP.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|**El IDP es la fuente de verdad; el Backend y el cliente replican las reglas para dar retroalimentación inmediata**|Seguridad en profundidad. Buena experiencia de usuario. Una sola política efectiva.|Hay que mantener sincronizadas las reglas replicadas.|
|B|Política solo en el Backend|Control total del código propio.|Duplica capacidades que el IDP ya tiene y puede quedar fuera de sincronía con él.|
|C|Política solo en el IDP|Sin duplicación.|El usuario solo ve el error tras el viaje al servidor; peor usabilidad.|

### Decisión

**Alternativa A.** Valores iniciales propuestos, a confirmar: longitud mínima de 10 caracteres, distinta de la contraseña actual y de las últimas 3, que no contenga el número de documento ni el correo del usuario, y que no esté en una lista de contraseñas comunes.

### Justificación

Prioriza longitud sobre complejidad artificial, adecuado a un público no técnico, y aplica seguridad en profundidad sin duplicar la fuente de verdad.

### Tácticas arquitectónicas

- **Value Object PasswordPolicy** en el dominio con las reglas replicables.
- **Política del IDP** como validación final.
- **Mensajes de error claros** con la regla incumplida, en lenguaje cotidiano (ESC-CAL-USA-0001).

### PoC / Spike requerido

No se requiere spike.

### Consecuencias

- Cambiar la política exige actualizar el IDP, el dominio y el cliente.
- Se documenta la política vigente en un único lugar.

---

## ADR-014: Proveedor de correo de costo cero detrás de un puerto de notificaciones

| Campo                   | Valor                                                            |
| ----------------------- | ---------------------------------------------------------------- |
| Estado                  | **Propuesto**                                                    |
| Escenarios relacionados | ESC-CAL-DIS-0036, restricciones de presupuesto                   |
| Atributos de calidad    | Viabilidad económica, Disponibilidad                             |

### Contexto

El modelo C4 define SendGrid como pasarela de notificaciones. SendGrid retiró su plan gratuito permanente el 27 de mayo de 2025 y hoy ofrece solo una prueba de 60 días. Con un presupuesto cero en la etapa de validación y un solo desarrollador, se necesita un proveedor con plan gratuito permanente.

### Alternativas evaluadas

|#|Alternativa|Ventajas|Desventajas|
|---|---|---|---|
|A|SendGrid|Ya definido en el C4.|Sin plan gratuito permanente; plan de pago desde aproximadamente USD 19,95 al mes.|
|B|**Brevo (SMTP / API)**|Plan gratuito con 300 correos por día, sin tarjeta.|Límite diario compartido entre todos los negocios; el plan gratuito puede incluir marca del proveedor en los correos.|
|C|Mailgun|Plan gratuito permanente de 100 correos por día.|Límite menor y retención de logs de 1 día.|
|D|SMTP2GO|Plan gratuito permanente sin tarjeta.|Límites bajos; menos documentación para el stack.|

### Decisión

**Alternativa B (Brevo) como proveedor inicial, detrás del puerto NotificationPort.** En desarrollo se usa un servidor SMTP local de pruebas (Mailpit). Los límites y condiciones deben verificarse en la página oficial del proveedor antes de adoptarlo.

### Justificación

El patrón puerto-adaptador ya exigido en las restricciones técnicas permite cambiar de proveedor por configuración. Brevo ofrece el mayor cupo diario gratuito entre las opciones y cubre el MVP.

### Tácticas arquitectónicas

- **Puerto NotificationPort** con adaptador SMTP genérico: cambiar de proveedor no toca el dominio.
- **Envío asíncrono con reintentos** y backoff exponencial.
- **Plantilla sin datos sensibles**, con indicación de qué hacer si el cambio no fue autorizado.
- **Autenticación del dominio remitente** (SPF y DKIM) para evitar que los avisos caigan en spam.
- **Monitoreo del consumo diario** del cupo.

### PoC / Spike requerido

|Spike|Objetivo|Criterio de éxito|Duración estimada|
|---|---|---|---|
|SPK-008|Validar el envío por el adaptador SMTP con el proveedor elegido y su entrega en una bandeja real|El correo de aviso llega a la bandeja de entrada (no a spam) en menos de 60 segundos.|1 día|

### Consecuencias

- El límite diario del plan gratuito se comparte con alertas de inventario y comprobantes; si el volumen lo supera, se migra a un plan de pago dentro del techo de USD 50 por cada 100 negocios.
- Se requiere un dominio propio para autenticar el remitente, lo que implica un costo anual pequeño que debe incluirse en el presupuesto.
- Se actualiza la descripción del contenedor de notificaciones en el C4.


---
## Resumen de Spikes requeridos

| Spike | ADR | Descripción | Prioridad |
|---|---|---|---|---|
| SPK-001 | ADR-001 | Venta completa offline con base de datos local embebida y librería de caché, con sincronización al reconectar | Crítica |
| SPK-002 | ADR-002 | Simulación de conflicto de sincronización entre 2 dispositivos vendiendo del mismo lote | Crítica |
| SPK-003 | ADR-003 | Fraccionamiento de venta entre lotes con descuentos activos (FIFO por vencimiento) | Alta |
| SPK-004 | ADR-006 | Motor de recomendaciones con datos simulados de 30 días de un negocio piloto | Alta |
| SPK-005 | ADR-007 | Prueba de carga de rate limiting con librería en memoria sobre una sola instancia de backend | Opcional / Baja |
| SPK-006 | ADR-011, ADR-012 | Verificación de contraseña actual, actualización de credencial y revocación de sesiones contra el IDP | Alta |
| SPK-007 | ADR-011 | Obtención de secretos del Key Vault por el Backend (autenticación del Backend y lectura de secretos) | Alta |
| SPK-008 | ADR-014 | Envío de correo por el adaptador SMTP con el proveedor gratuito elegido | Media | 
