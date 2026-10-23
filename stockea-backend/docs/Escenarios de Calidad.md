28 escenarios seleccionados como los más críticos para el producto, cada uno documentado en formato completo con notas de arq

Criterios de selección: valor directo para el negocio, reto técnico significativo, alineación con la visión y los diferenciadores del producto, y cobertura balanceada de los 11 atributos de calidad.

---

## 1. ESC-CAL-CON-0001

### Metadatos

| Campo               | Valor                                                    |
| ------------------- | -------------------------------------------------------- |
| Atributo de Calidad | Confiabilidad                                            |
| Prioridad           | Crítica                                                  |
| Dificultad / Riesgo | Alta                                                     |
| Estado              | Borrador                                                 |
| RF relacionado      | RF-004.1 (Registrar venta), RF-011.1 (Operación offline) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Evento externo: corte de energía eléctrica en el local del negocio durante el horario operativo.|
|2|Estímulo|Se interrumpe la alimentación eléctrica del equipo mientras el cajero está registrando una venta con 5 productos, 3 de los cuales ya habían sido agregados al carrito.|
|3|Artefacto|El módulo de ventas del frontend (almacenamiento local del dispositivo) y la capa de persistencia del backend (base de datos) que gestionan la transacción de venta y la actualización del inventario.|
|4|Entorno|Operación normal del punto de venta durante horario comercial. La venta puede estar en modo online u offline al momento del corte.|
|5|Respuesta|Al reiniciar el equipo, la venta inconclusa no queda registrada y el inventario vuelve exactamente al estado previo al intento: sin descuentos parciales ni ventas a medias.|
|6|Medida de Respuesta|0% de impacto en inventario, 0 registros de venta corruptos, sistema operativo en menos de 30 segundos tras el reinicio.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Una venta a medias descuadra el inventario digital contra el físico sin forma de corregirlo, lo que destruye la confianza del dueño en el sistema.|
|Tácticas Arquitectónicas|La venta y el ajuste de stock ocurren en una sola transacción atómica (todo o nada). Localmente, la venta solo se guarda al confirmar el pago, no al armar el carrito. Al iniciar, el sistema descarta cualquier operación local que haya quedado incompleta.|
|Supuestos y Riesgos|Se asume que el almacenamiento local escribe de forma durable antes de confirmar. Riesgo de registro corrupto si el corte ocurre justo en el instante de escritura; se mitiga verificando la integridad al reiniciar.|
|Prueba de Aceptación|Dado un cajero vendiendo offline y un corte de energía después de agregar 3 de 5 productos pero antes de pagar, al reiniciar el inventario debe quedar igual al previo y no debe existir esa transacción.|

---

## 2. ESC-CAL-CON-0003

### Metadatos

| Campo               | Valor                                     |
| ------------------- | ----------------------------------------- |
| Atributo de Calidad | Confiabilidad                             |
| Prioridad           | Crítica                                   |
| Dificultad / Riesgo | Media                                     |
| Estado              | Borrador                                  |
| RF relacionado      | RF-002.1 (Registrar entrada de mercancía) |

### Definición del Escenario

| Parte | Componente          | Descripción                                                                                                                                                                                                                                          |
| ----- | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1     | Origen              | Error interno del sistema durante el procesamiento de una operación de entrada de mercancía.                                                                                                                                                         |
| 2     | Estímulo            | El supervisor confirma una entrada de 50 unidades de leche del proveedor Lácteos del Valle con fecha de vencimiento 2026-08-15 y el sistema falla a mitad del procesamiento después de crear el lote pero antes de actualizar el stock del producto. |
| 3     | Artefacto           | El servicio de dominio de entradas de mercancía y la base de datos que gestionan la creación del lote y la actualización del stock del producto en una misma transacción.                                                                            |
| 4     | Entorno             | Operación normal del sistema en modo online.                                                                                                                                                                                                         |
| 5     | Respuesta           | La operación se revierte por completo: no queda lote ni entrada registrados, el stock no cambia, y el sistema informa que debe reintentarse.                                                                                                         |
| 6     | Medida de Respuesta | 0 registros huérfanos en la base de datos, stock idéntico al previo, mensaje de error en menos de 2 segundos.                                                                                                                                        |

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Un lote sin stock actualizado dispara alertas y recomendaciones erróneas; un stock sin lote impide rastrear vencimientos, anulando el diferenciador del producto.|
|Tácticas Arquitectónicas|Transacción única que envuelve la creación de la entrada, del lote y la actualización del stock, con reversión automática ante cualquier error. Las restricciones de la base de datos actúan como última línea de defensa.|
|Supuestos y Riesgos|Se asume reversión correcta ante fallos. Riesgo: fallo de conexión que deje la transacción en estado indeterminado, requiriendo verificación al reconectar.|
|Prueba de Aceptación|Si el sistema falla tras crear el lote pero antes de actualizar el stock, no debe existir ni lote ni entrada, y el stock debe permanecer igual al anterior.|

---

## 3. ESC-CAL-CON-0010

### Metadatos

| Campo               | Valor                                                                           |
| ------------------- | ------------------------------------------------------------------------------- |
| Atributo de Calidad | Confiabilidad                                                                   |
| Prioridad           | Crítica                                                                         |
| Dificultad / Riesgo | Media                                                                           |
| Estado              | Borrador                                                                        |
| RF relacionado      | RF-008.4 (Alerta vencimiento próximo), RF-009.3 (Riesgo vencimiento anticipado) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|El reloj interno del sistema al ejecutar el cálculo diario de días restantes para todos los lotes activos del negocio.|
|2|Estímulo|El sistema calcula los días restantes de vencimiento para múltiples lotes con fechas distintas en diferentes momentos del día, incluyendo el caso borde del día exacto de vencimiento.|
|3|Artefacto|El servicio de dominio que calcula los días restantes y determina el estado automático del lote (disponible, próximo a vencer, vencido, agotado).|
|4|Entorno|Operación normal del sistema. El cálculo puede ejecutarse a cualquier hora y debe ser consistente sin importar el momento.|
|5|Respuesta|Los días restantes reflejan siempre la diferencia real entre la fecha de vencimiento y la fecha actual, y el estado del lote se actualiza automáticamente según los umbrales configurados.|
|6|Medida de Respuesta|100% de coincidencia entre el cálculo y la diferencia real de fechas, 0 lotes con valores o estados inconsistentes.|

### Notas de Arquitectura

| Aspecto                  | Descripción                                                                                                                                                                                                              |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Justificación de Negocio | Este cálculo es la base de todo el motor de alertas y recomendaciones; un error de un solo día puede generar alertas falsas o dejar pasar un vencimiento real.                                                           |
| Tácticas Arquitectónicas | Pruebas exhaustivas de casos borde (día exacto de vencimiento, año bisiesto, cambio de hora). Las fechas se manejan en un formato estándar y se comparan solo por día calendario, sin hora, para evitar inconsistencias. |
| Supuestos y Riesgos      | Se asume el reloj del servidor sincronizado. Riesgo: equipo offline con fecha mal configurada; se corrige al sincronizar recalculando con la fecha real del servidor.                                                    |
| Prueba de Aceptación     | Un lote que vence el 2026-08-15 consultado el 2026-08-13 a distintas horas del día debe mostrar siempre 2 días restantes y el estado correspondiente, y aparecer como vencido el día 2026-08-16.                         |

---

## 4. ESC-CAL-CON-0014

### Metadatos

| Campo               | Valor                               |
| ------------------- | ----------------------------------- |
| Atributo de Calidad | Confiabilidad                       |
| Prioridad           | Crítica                             |
| Dificultad / Riesgo | Media                               |
| Estado              | Borrador                            |
| RF relacionado      | RF-004.1 (Registrar venta)          |


### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Cajero que confirma el pago de una venta desde el punto de venta.|
|2|Estímulo|Se confirma una venta de 3 unidades de un producto que tiene 10 unidades de stock disponible distribuidas en un lote con vencimiento próximo (7 unidades) y otro lote más lejano (3 unidades).|
|3|Artefacto|El servicio de dominio de ventas que gestiona el descuento de stock por lote siguiendo la regla de vencimiento más próximo primero y la actualización del stock calculado del producto.|
|4|Entorno|Operación normal del punto de venta en modo online.|
|5|Respuesta|El sistema descuenta del lote con vencimiento más próximo, deja intacto el otro lote, y actualiza el stock total del producto de forma inmediata en la interfaz.|
|6|Medida de Respuesta|Actualización de stock en menos de 2 segundos, el lote correcto es el descontado, el stock total siempre coincide con la suma de los lotes activos.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Vender primero lo más próximo a vencer reduce la merma; descontar del lote equivocado dejaría el producto en riesgo hasta expirar, generando pérdida directa.|
|Tácticas Arquitectónicas|Los lotes se consultan ordenados por fecha de vencimiento más próxima; si la cantidad excede un lote, se fracciona automáticamente entre varios. Toda la operación (venta, ajuste de lotes y recálculo de stock) ocurre en una sola transacción atómica.|
|Supuestos y Riesgos|Se asume que no hay lotes con la misma fecha de vencimiento (se desempata por antigüedad). Riesgo de sobreventa si dos cajeros venden del mismo lote a la vez sin control de concurrencia.|
|Prueba de Aceptación|Con un lote A de 7 unidades (vence antes) y un lote B de 3, al vender 3 unidades el lote A debe quedar en 4, el B intacto, y el stock total en 7, reflejado en menos de 2 segundos.|

---

## 5. ESC-CAL-CON-0025

### Metadatos

| Campo               | Valor                                                    |
| ------------------- | -------------------------------------------------------- |
| Atributo de Calidad | Confiabilidad                                            |
| Prioridad           | Alta                                                     |
| Dificultad / Riesgo | Media                                                    |
| Estado              | Borrador                                                 |
| RF relacionado      | RF-006.2 (Aprobar descuento), RF-004.1 (Registrar venta) |


### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Administrador que aprueba un descuento del 20% sobre un lote de yogurt próximo a vencer.|
|2|Estímulo|El cajero registra una venta del producto que tiene un descuento activo vigente.|
|3|Artefacto|El módulo de ventas que consulta descuentos activos por lote y aplica automáticamente el porcentaje aprobado al precio unitario al agregar el producto al carrito.|
|4|Entorno|Operación normal del punto de venta. El descuento fue aprobado previamente por el administrador y está vigente.|
|5|Respuesta|El precio con descuento se aplica automáticamente sin que el cajero pueda ignorarlo, y el comprobante refleja el precio original, el porcentaje y el precio final.|
|6|Medida de Respuesta|Precio con descuento visible en menos de 1 segundo, diferencia máxima de 1 peso por redondeo, 100% de las ventas con descuento activo reflejadas en el comprobante.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Si el descuento no se aplica solo, el producto expira sin venderse; si se aplica mal, el negocio pierde más de lo previsto. Ambos casos destruyen la confianza en el motor de descuentos.|
|Tácticas Arquitectónicas|Al agregar el producto se verifica si el lote tiene un descuento vigente y se aplica automáticamente, sin intervención del cajero, usando un cálculo decimal preciso. El descuento queda congelado en el detalle de la venta.|
|Supuestos y Riesgos|Se asume un único descuento activo por lote a la vez. Riesgo: si el descuento se aprueba con el producto ya en el carrito, se decidió aplicar el precio vigente solo al confirmar la venta, no al agregarlo.|
|Prueba de Aceptación|Con un producto de $5.000 y descuento activo del 20%, al agregarlo el precio mostrado debe ser $4.000, el comprobante debe indicarlo, y el inventario debe descontar del lote correspondiente.|

---

## 6. ESC-CAL-CON-0029

### Metadatos

| Campo               | Valor                                                         |
| ------------------- | ------------------------------------------------------------- |
| Atributo de Calidad | Confiabilidad                                                 |
| Prioridad           | Crítica                                                       |
| Dificultad / Riesgo | Alta                                                          |
| Estado              | Borrador                                                      |
| RF relacionado      | RF-009.1 (Velocidad de venta), RF-009.4 (Lista de reposición) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Proceso automático del sistema que genera recomendaciones de compra periódicamente.|
|2|Estímulo|El sistema ejecuta el cálculo de recomendaciones de compra para todos los productos activos del negocio basándose en ventas, stock actual y vencimientos.|
|3|Artefacto|El motor de recomendaciones que cruza historial de ventas, stock actual y fecha de vencimiento de los lotes para generar la lista de reposición priorizada.|
|4|Entorno|Operación normal del sistema en modo online con datos sincronizados.|
|5|Respuesta|La velocidad de venta se calcula solo con datos reales recientes; los productos en riesgo de vencer antes de venderse no generan recomendación de compra, sino que se envían al motor de descuentos; los productos sin historial muestran "sin datos suficientes" en vez de un número.|
|6|Medida de Respuesta|100% de recomendaciones basadas en datos recientes, 0 recomendaciones contradictorias con productos por vencer, 0 errores por falta de datos.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Las recomendaciones son el diferenciador principal del producto; datos desactualizados llevan a sobre-stock o desabastecimiento, y recomendar comprar algo que ya se está venciendo destruiría la credibilidad del sistema.|
|Tácticas Arquitectónicas|El cálculo usa solo ventas del período configurado y devuelve un estado descriptivo cuando no hay datos suficientes. Antes de recomendar, se valida si el stock alcanza a venderse antes del vencimiento del lote más próximo; si no alcanza, se redirige al motor de descuentos. El stock usado se congela al generar la recomendación para poder reproducirla después.|
|Supuestos y Riesgos|Se asume un período de cálculo configurable (7 días por defecto), lo que puede subestimar productos de venta estacional. Riesgo adicional con datos offline no sincronizados que generen recomendaciones incompletas.|
|Prueba de Aceptación|Un producto con stock que cubre pocos días de venta y un lote por vencer en más días no debe generar recomendación de compra; uno con stock que no alcanza a venderse antes del vencimiento debe redirigirse al motor de descuentos.|

---

## 7. ESC-CAL-DIS-0001

### Metadatos

| Campo               | Valor                        |
| ------------------- | ---------------------------- |
| Atributo de Calidad | Disponibilidad               |
| Prioridad           | Crítica                      |
| Dificultad / Riesgo | Muy Alta                     |
| Estado              | Borrador                     |
| RF relacionado      | RF-011.1 (Operación offline) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Fallo de conectividad: el proveedor de internet del negocio sufre una interrupción prolongada durante el horario comercial.|
|2|Estímulo|El sistema pierde la conexión a internet mientras hay un turno de caja abierto con clientes esperando para pagar.|
|3|Artefacto|La aplicación completa: mecanismo de sincronización en segundo plano, almacenamiento local con catálogo de productos, lotes activos, precios vigentes y descuentos activos cacheados.|
|4|Entorno|El sistema venía operando en modo online y pierde la conectividad de forma inesperada.|
|5|Respuesta|El sistema sigue funcionando sin interrupción visible: las ventas se registran localmente, el inventario local se actualiza en tiempo real, y un indicador discreto muestra el modo offline.|
|6|Medida de Respuesta|0 segundos de interrupción al perder la conexión, 100% de las funciones esenciales operativas offline, ninguna venta offline se pierde.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|La operación offline es la promesa principal del producto frente a la competencia; en una tienda con conectividad intermitente, exigir internet para vender significa perder ventas reales.|
|Tácticas Arquitectónicas|Arquitectura offline-first: el frontend opera siempre contra el almacenamiento local como fuente de verdad inmediata. Los datos maestros se precargan al iniciar sesión. Las operaciones de escritura se encolan localmente con fecha y hora y se sincronizan automáticamente al recuperar conexión.|
|Supuestos y Riesgos|Se asume catálogo local actualizado al momento de perder conexión. Riesgo principal: dos dispositivos vendiendo offline del mismo lote generan sobreventa al sincronizar (ver ESC-CAL-DIS-0008). Riesgo secundario de datos obsoletos tras varios días sin sincronizar.|
|Prueba de Aceptación|Al desconectar la red con un turno abierto, el cajero debe poder vender, consultar inventario y cerrar caja sin interrupción visible, con un indicador discreto de modo offline.|

---

## 8. ESC-CAL-DIS-0005

### Metadatos

| Campo               | Valor                              |
| ------------------- | ---------------------------------- |
| Atributo de Calidad | Disponibilidad                     |
| Prioridad           | Crítica                            |
| Dificultad / Riesgo | Alta                               |
| Estado              | Borrador                           |
| RF relacionado      | RF-011.2 (Sincronizar operaciones) |


### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Evento de red: el sistema detecta que la conexión a internet se ha restablecido después de un período de operación offline.|
|2|Estímulo|El equipo recupera la conectividad después de 2 horas de operación offline durante las cuales se registraron 15 ventas, 2 ajustes de inventario y 1 cierre de caja.|
|3|Artefacto|El módulo de sincronización del frontend y la API de sincronización del backend que reciben, validan y persisten las operaciones encoladas.|
|4|Entorno|El sistema transiciona de modo offline a modo online. El cajero puede estar atendiendo clientes al momento de la reconexión.|
|5|Respuesta|La sincronización inicia sola en segundo plano, envía las operaciones en orden cronológico sin interrumpir al cajero, y avisa al finalizar que los datos quedaron respaldados.|
|6|Medida de Respuesta|Inicio en menos de 5 segundos, 0% de interrupción al cajero, 100% de operaciones sincronizadas correctamente, aviso de éxito en menos de 2 segundos.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Las ventas offline son ingresos reales; si no se sincronizan bien, el dueño pierde visibilidad y los reportes de caja no cuadran con la realidad.|
|Tácticas Arquitectónicas|Las operaciones pendientes se envían automáticamente al detectar conexión, en orden cronológico, marcándose como completadas para evitar duplicados. Si una operación falla, las demás siguen procesándose y esa se reintenta. El proceso es idempotente: reenviar la misma operación no la duplica.|
|Supuestos y Riesgos|Se asume que el servidor procesa en el orden recibido. Riesgo: si la conexión se corta a mitad de la sincronización, lo pendiente permanece en cola sin pérdida; los conflictos se manejan sin bloquear el resto.|
|Prueba de Aceptación|Tras 2 horas offline con 15 ventas, al reconectar la sincronización debe iniciar sola, completarse en menos de 30 segundos, y el cajero debe poder seguir vendiendo durante todo el proceso.|

---

## 9. ESC-CAL-DIS-0008

### Metadatos

| Campo               | Valor                                             |
| ------------------- | ------------------------------------------------- |
| Atributo de Calidad | Disponibilidad                                    |
| Prioridad           | Alta                                              |
| Dificultad / Riesgo | Muy Alta                                          |
| Estado              | Borrador                                          |
| RF relacionado      | RF-011.3 (Gestionar conflictos de sincronización) |


### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Dos dispositivos del mismo negocio que operaron offline simultáneamente y vendieron del mismo lote.|
|2|Estímulo|El dispositivo A sincroniza una venta de 8 unidades del lote X (que tenía 10 disponibles). Minutos después, el dispositivo B intenta sincronizar una venta de 5 unidades del mismo lote X, pero el lote ya solo tiene 2 unidades disponibles en el servidor.|
|3|Artefacto|El servicio de sincronización del backend que valida cada operación contra el estado actual del inventario en el servidor.|
|4|Entorno|Fase de sincronización posterior a un período de operación offline en dos dispositivos del mismo negocio.|
|5|Respuesta|La venta de A se acepta por haber llegado primero con stock suficiente; la de B se marca como conflicto y se notifica al cajero con el motivo exacto, ofreciendo resolución manual.|
|6|Medida de Respuesta|Primera operación válida procesada en menos de 3 segundos, 100% de conflictos detectados y notificados, 0 stock negativo, aviso al afectado en menos de 5 segundos.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Aceptar una sobreventa en silencio generaría stock negativo sin sentido físico y destruiría la confianza en el inventario digital; marcar el conflicto permite resolverlo sin comprometer la integridad del sistema.|
|Tácticas Arquitectónicas|El servidor bloquea el registro del lote al verificar su stock real durante la sincronización, aplicando la regla de que la primera operación válida gana. Las operaciones en conflicto quedan visibles para el cajero y el administrador.|
|Supuestos y Riesgos|Se asume que los conflictos son poco frecuentes. Riesgo: con muchos dispositivos operando offline a la vez, los conflictos podrían volverse frecuentes; se podría mitigar reservando stock por dispositivo a futuro.|
|Prueba de Aceptación|Con un lote de 10 unidades y dos dispositivos offline, si A sincroniza 8 y luego B sincroniza 5 del mismo lote, la venta de A queda aceptada (lote en 2), la de B queda en conflicto y B recibe la notificación correspondiente.|

---

## 10. ESC-CAL-DIS-0029

### Metadatos

| Campo               | Valor                      |
| ------------------- | -------------------------- |
| Atributo de Calidad | Disponibilidad             |
| Prioridad           | Alta                       |
| Dificultad / Riesgo | Media                      |
| Estado              | Borrador                   |
| RF relacionado      | RF-004.1 (Registrar venta) |


### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Fallo interno de un módulo secundario del sistema durante la operación del negocio.|
|2|Estímulo|El motor de recomendaciones de compra presenta un error no recuperable que impide generar la lista de reposición priorizada.|
|3|Artefacto|El módulo de ventas y el módulo de inventario como funcionalidades prioritarias que deben permanecer operativas ante fallos de otros módulos.|
|4|Entorno|Operación normal del punto de venta con clientes esperando para pagar.|
|5|Respuesta|El cajero sigue vendiendo y consultando inventario sin interrupción; solo la sección de recomendaciones del panel muestra un aviso de módulo no disponible.|
|6|Medida de Respuesta|0 segundos de interrupción en ventas, 100% de funciones de venta e inventario operativas, aviso visible en menos de 1 segundo.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|La capacidad de vender es lo más crítico del negocio; un fallo en recomendaciones nunca puede impedir cobrar.|
|Tácticas Arquitectónicas|Arquitectura modular con aislamiento de fallos: cada módulo opera de forma independiente y se degrada sin propagar el error. Cada sección del panel se renderiza por separado con su propio manejo de errores.|
|Supuestos y Riesgos|Se asume que ventas no depende directamente de recomendaciones. Riesgo: un fallo en la base de datos compartida afectaría a todos los módulos simultáneamente; el modo offline entra como mitigación.|
|Prueba de Aceptación|Ante una excepción no recuperable del motor de recomendaciones, el cajero debe seguir vendiendo, el supervisor consultando inventario, y el panel mostrar el aviso solo en esa sección.|

---

## 11. ESC-CAL-DIS-0033

### Metadatos

| Campo               | Valor                                                     |
| ------------------- | --------------------------------------------------------- |
| Atributo de Calidad | Disponibilidad                                            |
| Prioridad           | Alta                                                      |
| Dificultad / Riesgo | Media                                                     |
| Estado              | Borrador                                                  |
| RF relacionado      | RF-011.1 (Operación offline)                              |


### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Un equipo de gama baja típico del mercado objetivo (recursos de memoria y procesamiento modestos).|
|2|Estímulo|El dueño opera el sistema en un equipo con estas especificaciones mínimas durante una jornada completa de 8+ horas.|
|3|Artefacto|La aplicación completa, incluyendo el mecanismo de sincronización, el almacenamiento local y todas las operaciones de renderizado y cálculo del cliente.|
|4|Entorno|El equipo ejecuta simultáneamente el sistema, el navegador con otras pestañas y una hoja de cálculo.|
|5|Respuesta|Todos los módulos cargan rápido, las operaciones de venta responden con tiempos similares a un equipo más potente, y no hay bloqueos ni errores de memoria durante la jornada.|
|6|Medida de Respuesta|Carga inicial menor a 5 segundos, consumo de memoria controlado tras 8 horas, 0 errores de memoria, degradación no mayor al 20% frente a un equipo con más recursos.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|El mercado objetivo usa equipos de gama baja adquiridos hace años; exigir hardware moderno excluiría a la mayoría del mercado potencial.|
|Tácticas Arquitectónicas|Carga inicial liviana con lo esencial primero y el resto bajo demanda. Limpieza periódica de datos locales antiguos, listas largas renderizadas solo en lo visible, y manejo cuidadoso de recursos para evitar fugas de memoria.|
|Supuestos y Riesgos|Se asume un mínimo de recursos y un disco funcional. Riesgo: discos más lentos podrían ralentizar el almacenamiento local; se mitiga probando en equipos representativos del mercado.|
|Prueba de Aceptación|En un equipo modesto con varias pestañas abiertas, tras 8 horas continuas de uso no debe haber bloqueos, errores de memoria ni degradación progresiva.|

---

## 12. ESC-CAL-REN-0021

### Metadatos

| Campo               | Valor                      |
| ------------------- | -------------------------- |
| Atributo de Calidad | Rendimiento                |
| Prioridad           | Crítica                    |
| Dificultad / Riesgo | Media                      |
| Estado              | Borrador                   |
| RF relacionado      | RF-004.1 (Registrar venta) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Cajero que confirma el pago de una venta con múltiples productos durante el horario comercial.|
|2|Estímulo|El cajero presiona el botón de confirmar pago en una venta que contiene 10 productos distintos con cantidades variadas, algunos con descuento activo.|
|3|Artefacto|El flujo completo de procesamiento de venta: validación de stock por lote, cálculo de descuentos, actualización de lotes y stock, y generación de comprobante.|
|4|Entorno|Operación normal del punto de venta con cliente esperando frente al cajero. El sistema puede estar en modo online u offline.|
|5|Respuesta|La venta se registra completamente, el inventario se actualiza, el comprobante se genera y la interfaz confirma el éxito al cajero.|
|6|Medida de Respuesta|Tiempo total desde confirmar hasta ver la confirmación no mayor a 3 segundos.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|El cliente espera de pie frente al cajero; cualquier demora perceptible deteriora la experiencia y se compara negativamente con el método manual anterior.|
|Tácticas Arquitectónicas|En línea, se agrupan las actualizaciones de inventario en pocas consultas. Sin conexión, todo se procesa contra el almacenamiento local, que responde en milisegundos. Totales y descuentos se calculan en el dispositivo sin esperar al servidor.|
|Supuestos y Riesgos|Se asume que 10 productos es un máximo realista. Riesgo: productos que requieren fraccionarse entre varios lotes agregan pasos adicionales; se mitiga precargando los lotes activos por producto.|
|Prueba de Aceptación|Con 10 productos en el carrito, 3 con descuento y 2 que requieren fraccionamiento entre lotes, la venta debe completarse en no más de 3 segundos, online u offline.|

---

## 13. ESC-CAL-REN-0011

### Metadatos

| Campo               | Valor                                                               |
| ------------------- | ------------------------------------------------------------------- |
| Atributo de Calidad | Rendimiento                                                         |
| Prioridad           | Alta                                                                |
| Dificultad / Riesgo | Alta                                                                |
| Estado              | Borrador                                                            |
| RF relacionado      | RF-004.1 (Registrar venta), RF-011.3 (Conflictos de sincronización) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Dos cajeros operando simultáneamente en dos dispositivos distintos del mismo negocio.|
|2|Estímulo|Ambos cajeros registran ventas del mismo producto al mismo tiempo, descontando unidades del mismo lote.|
|3|Artefacto|La capa de persistencia y el servicio de dominio de ventas que gestionan la concurrencia sobre el inventario compartido.|
|4|Entorno|Operación en modo online con ambos dispositivos conectados al servidor.|
|5|Respuesta|Ambas ventas se procesan sin sobreventa, descontando el lote en orden de llegada, con un stock final consistente y sin errores visibles para ninguno de los cajeros.|
|6|Medida de Respuesta|Cada venta procesada en menos de 3 segundos, 0 sobreventas, 0 errores de concurrencia visibles, stock final exacto según ambas ventas.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Con dos cajeros simultáneos es inevitable vender del mismo producto a la vez; sin buen manejo de concurrencia, el inventario digital se descuadra del físico.|
|Tácticas Arquitectónicas|Control de versión sobre el lote: si dos operaciones lo modifican a la vez, la segunda detecta el conflicto y reintenta automáticamente con el dato actualizado. Sin conexión, el conflicto se resuelve al sincronizar. Las transacciones son breves para minimizar el bloqueo.|
|Supuestos y Riesgos|Se asume que estas ventas concurrentes exactas son poco frecuentes. Riesgo: si el reintento falla repetidamente, la operación debe fallar con un mensaje claro en vez de quedar esperando indefinidamente.|
|Prueba de Aceptación|Con un lote de 10 unidades, si el cajero A vende 3 y el B vende 4 al mismo tiempo del mismo lote, ambas deben completarse, el lote quedar en 3, y el stock reflejar la resta total de 7.|

---

## 14. ESC-CAL-REN-0029

### Metadatos

| Campo               | Valor                                                       |
| ------------------- | ----------------------------------------------------------- |
| Atributo de Calidad | Rendimiento                                                 |
| Prioridad           | Alta                                                        |
| Dificultad / Riesgo | Media                                                       |
| Estado              | Borrador                                                    |
| RF relacionado      | RF-010.1 (Dashboard inventario), RF-004.2 (Consultar venta) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Administrador que consulta el inventario o genera reportes en un negocio con varios años de operación.|
|2|Estímulo|El administrador consulta el inventario completo o genera un reporte de ventas del mes en un sistema con 3 años de datos históricos acumulados.|
|3|Artefacto|La capa de consulta del backend y la base de datos con tablas de productos, lotes, ventas e historial con alto volumen de registros.|
|4|Entorno|Operación normal en modo online con 10.000 productos registrados, más de 1.000 lotes activos y más de 50.000 registros de ventas acumulados.|
|5|Respuesta|Las consultas de inventario y los reportes se generan dentro de los tiempos establecidos sin degradación perceptible respecto a una base recién instalada.|
|6|Medida de Respuesta|Inventario en menos de 3 segundos, reportes mensuales en menos de 5 segundos, búsquedas en menos de 2 segundos, degradación máxima del 30%.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Un sistema que se vuelve lento con el tiempo pierde adopción y el dueño vuelve a métodos manuales.|
|Tácticas Arquitectónicas|Índices en las columnas de mayor consulta, paginación obligatoria en listados largos, precálculo de agregados para evitar sumas en tiempo real, y posible particionamiento futuro de tablas grandes.|
|Supuestos y Riesgos|Se asume que la base de datos con índices adecuados soporta este volumen. Riesgo: reportes con consultas complejas podrían exceder el umbral; se mitiga con vistas precalculadas y cálculos pesados en segundo plano.|
|Prueba de Aceptación|Con 3 años de datos, 10.000 productos y 50.000 ventas, el inventario debe responder en menos de 3 segundos y el reporte mensual en menos de 5 segundos.|

---

## 15. ESC-CAL-REN-0037

### Metadatos

| Campo               | Valor                              |
| ------------------- | ---------------------------------- |
| Atributo de Calidad | Rendimiento                        |
| Prioridad           | Alta                               |
| Dificultad / Riesgo | Baja                               |
| Estado              | Borrador                           |
| RF relacionado      | RF-004.1 (Registrar venta)         |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Cajero que modifica la cantidad de un producto dentro del carrito de venta.|
|2|Estímulo|El cajero cambia la cantidad de un producto de 2 a 5 unidades mientras el carrito ya contiene otros 4 productos con sus subtotales calculados.|
|3|Artefacto|El componente de frontend que gestiona el carrito de venta y los cálculos de subtotales, descuentos y total en tiempo real.|
|4|Entorno|Operación normal del punto de venta con cliente presente.|
|5|Respuesta|El subtotal y el total se recalculan al instante, manteniendo correctamente aplicado cualquier descuento activo.|
|6|Medida de Respuesta|Actualización visible en menos de 1 segundo, sin errores de redondeo mayores a 1 peso.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|El recálculo inmediato evita errores de cobro y descuadres de caja; una demora perceptible se siente "trabada" y frustra al cajero en horas pico.|
|Tácticas Arquitectónicas|Todos los cálculos del carrito ocurren en el dispositivo del cajero, sin consultar al servidor, usando un manejo de decimales preciso. El carrito se guarda de forma permanente solo al confirmar el pago.|
|Supuestos y Riesgos|Se asume que precios y descuentos vigentes están cacheados localmente durante la venta. Riesgo mínimo: un cambio de precio aprobado a mitad de venta no se reflejaría hasta la siguiente.|
|Prueba de Aceptación|Con un carrito de $25.000, al cambiar un producto de $5.000 de 2 a 5 unidades, el total debe pasar a $40.000 en menos de 1 segundo sin recargar la pantalla.|

---

## 16. ESC-CAL-SEG-0004

### Metadatos

| Campo               | Valor                    |
| ------------------- | ------------------------ |
| Atributo de Calidad | Seguridad                |
| Prioridad           | Alta                     |
| Dificultad / Riesgo | Baja                     |
| Estado              | Borrador                 |
| RF relacionado      | RF-007.6 (Cerrar sesión) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|El reloj interno del sistema que monitorea la actividad de la sesión del usuario.|
|2|Estímulo|Han transcurrido 15 minutos sin que el usuario realice ninguna interacción con el sistema.|
|3|Artefacto|El módulo de gestión de sesiones del frontend que controla el tiempo de inactividad y la invalidación de la sesión.|
|4|Entorno|El sistema está activo con una sesión abierta y el usuario se ausentó del equipo sin cerrar sesión.|
|5|Respuesta|La sesión se cierra automáticamente y redirige al login, el turno de caja abierto permanece intacto para que el cajero lo cierre al volver, y el carrito sin confirmar se descarta.|
|6|Medida de Respuesta|Cierre a los 15 minutos (±10 segundos), redirección en menos de 1 segundo, el turno de caja no se cierra automáticamente.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|En una tienda el equipo puede quedar accesible para otras personas; el cierre automático protege los datos sin depender de que el usuario recuerde cerrar sesión.|
|Tácticas Arquitectónicas|Un contador de inactividad se reinicia con cualquier interacción; al expirar, la sesión se invalida localmente y se redirige al login. El turno de caja nunca se cierra solo porque el monto final debe declararlo la persona real. Sin conexión, el cierre de sesión ocurre localmente sin depender del servidor.|
|Supuestos y Riesgos|Se asume 15 minutos como umbral adecuado. Riesgo: perder un carrito con productos ya agregados; se puede mitigar con un aviso previo de 60 segundos antes del cierre.|
|Prueba de Aceptación|Con sesión y turno abiertos, tras 15 minutos sin actividad el sistema debe cerrar sesión, redirigir al login, mantener el turno abierto y descartar la venta sin confirmar.|

---

## 17. ESC-CAL-SEG-0006

### Metadatos

| Campo               | Valor                         |
| ------------------- | ----------------------------- |
| Atributo de Calidad | Seguridad                     |
| Prioridad           | Alta                          |
| Dificultad / Riesgo | Baja                          |
| Estado              | Borrador                      |
| RF relacionado      | RF-002.3 (Ajustar inventario) |
### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Usuario con sesión activa y rol de Cajero, autenticado en el sistema durante su turno de caja.|
|2|Estímulo|El cajero intenta eliminar un producto o modificar manualmente la cantidad disponible de un lote desde la interfaz de inventario, sin contar con el permiso correspondiente a su rol.|
|3|Artefacto|El módulo de control de acceso y autorización del backend, que valida el rol del usuario antes de ejecutar cualquier operación sobre el inventario.|
|4|Entorno|El sistema opera normalmente, en línea o sin conexión; el bloqueo debe comportarse igual en ambos casos.|
|5|Respuesta|El sistema rechaza la operación antes de afectar el inventario, avisa al cajero que no tiene permisos, y registra el intento con usuario, fecha, hora y acción intentada.|
|6|Medida de Respuesta|0% de impacto en el inventario, aviso en menos de 1 segundo, 100% de intentos registrados en el log de auditoría.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Solo supervisores y administradores deben poder modificar el inventario manualmente, ya que afecta stock, rotación y recomendaciones; permitirlo a cualquiera expondría al negocio a pérdidas no auditables.|
|Tácticas Arquitectónicas|Control de acceso por roles acumulativos, validado siempre en el backend y no solo en la interfaz, para que no pueda evadirse manipulando la aplicación. Cada intento no autorizado queda registrado de forma permanente.|
|Supuestos y Riesgos|Se asume que el cajero tiene solo acceso de lectura al inventario. Riesgo: si la validación solo estuviera en el frontend, podría evadirse; también debe validarse localmente en modo offline sin depender del servidor.|
|Prueba de Aceptación|Un cajero que intente eliminar un producto o ajustar un lote debe recibir el rechazo, el aviso en menos de 1 segundo y quedar registrado, tanto online como offline.|

---

## 18. ESC-CAL-SEG-0010

### Metadatos

| Campo               | Valor                                        |
| ------------------- | -------------------------------------------- |
| Atributo de Calidad | Seguridad                                    |
| Prioridad           | Crítica                                      |
| Dificultad / Riesgo | Baja                                         |
| Estado              | Borrador                                     |
| RF relacionado      | RF-011.2 (Sincronizar operaciones)           |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|El módulo de sincronización del sistema que envía datos desde el equipo local hacia el servidor en la nube.|
|2|Estímulo|El sistema inicia la sincronización de ventas, ajustes de inventario y datos del negocio con el servidor al recuperar la conexión a internet.|
|3|Artefacto|El canal de comunicación entre la aplicación y el servidor por donde viajan los datos del negocio, incluyendo ventas, precios, inventario y datos de usuarios.|
|4|Entorno|Transición de modo offline a modo online en una red potencialmente no confiable (wifi público, red compartida del local).|
|5|Respuesta|Toda la comunicación se realiza cifrada de extremo a extremo, y el sistema rechaza cualquier intento de conexión sin cifrar.|
|6|Medida de Respuesta|100% de comunicaciones cifradas, 0 transmisiones en texto plano, certificados de seguridad válidos y no autofirmados en producción.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|La sincronización transporta información financiera y datos personales; interceptarla en una red insegura violaría la protección de datos personales y destruiría la confianza en el sistema.|
|Tácticas Arquitectónicas|Cifrado obligatorio en toda la aplicación mediante un proxy de red con certificados gestionados automáticamente, y mecanismos que evitan que la conexión se "degrade" a un canal sin cifrar. En desarrollo se usan certificados de prueba equivalentes.|
|Supuestos y Riesgos|Se asume gestión correcta del cifrado por el proxy de red. Riesgo: si un certificado expira sin renovarse, el sistema deja de funcionar; se mitiga con renovación automática y monitoreo.|
|Prueba de Aceptación|Al sincronizar sobre una red wifi compartida, el 100% de los datos deben viajar cifrados y cualquier intento sin cifrar debe redirigirse automáticamente al canal seguro.|

---

## 19. ESC-CAL-AUD-0005

### Metadatos

| Campo               | Valor                       |
| ------------------- | --------------------------- |
| Atributo de Calidad | Capacidad para ser auditado |
| Prioridad           | Crítica                     |
| Dificultad / Riesgo | Baja                        |
| Estado              | Borrador                    |
| RF relacionado      | RF-004.1 (Registrar venta)  |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Cajero que confirma el pago de una venta en el punto de venta.|
|2|Estímulo|El cajero registra una venta de 3 productos con método de pago efectivo y confirma la transacción.|
|3|Artefacto|El servicio de dominio de ventas que persiste la transacción con todos los metadatos de auditoría (quién, cuándo, qué, cómo).|
|4|Entorno|Operación normal del punto de venta en modo online u offline.|
|5|Respuesta|El sistema guarda automáticamente el cajero responsable, la fecha y hora exacta, el turno asociado, el detalle de cada producto (con lote, precio, descuento y subtotal), método de pago, total y cambio, sin que el cajero deba ingresar nada adicional.|
|6|Medida de Respuesta|100% de ventas con auditoría completa, 0 ventas sin cajero o sin fecha/hora, sin agregar más de 100 milisegundos al proceso.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|El dueño necesita saber quién vendió qué, cuándo y a qué precio para controlar el manejo del dinero; sin trazabilidad automática, el sistema no sirve como herramienta de control.|
|Tácticas Arquitectónicas|Los campos de auditoría se asignan automáticamente a partir de la sesión activa y del reloj del sistema, nunca de datos enviados por el dispositivo del usuario. Sin conexión se usa la fecha local, validada por el servidor al sincronizar.|
|Supuestos y Riesgos|Se asume que la sesión identifica correctamente al usuario y al negocio. Riesgo: un reloj local desconfigurado en modo offline podría registrar una hora incorrecta; se mitiga validando la coherencia temporal al sincronizar.|
|Prueba de Aceptación|Tras confirmar una venta de 3 productos, el historial del día debe mostrar cajero, hora exacta, productos con cantidades, precios y descuentos, método de pago y total, sin que el cajero haya digitado nada de eso.|

---

## 20. ESC-CAL-AUD-0016

### Metadatos

| Campo               | Valor                       |
| ------------------- | --------------------------- |
| Atributo de Calidad | Capacidad para ser auditado |
| Prioridad           | Crítica                     |
| Dificultad / Riesgo | Baja                        |
| Estado              | Borrador                    |
| RF relacionado      | RF-005.1 (Apertura de caja) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Cajero que inicia su turno de trabajo y registra la apertura de caja.|
|2|Estímulo|El cajero abre la caja declarando un monto inicial de $200.000 en efectivo.|
|3|Artefacto|El servicio de dominio de turnos de caja que persiste el registro de apertura con todos los campos de auditoría marcados como inmodificables.|
|4|Entorno|Operación normal del punto de venta al inicio del turno.|
|5|Respuesta|El sistema registra la apertura con usuario, fecha, hora y monto inicial, y ese registro queda inmediatamente imposible de modificar, incluso para el administrador.|
|6|Medida de Respuesta|100% de registros de apertura completos, 0 registros modificados después de creados, cualquier intento de cambio es rechazado.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|El monto inicial es la referencia para calcular la diferencia al cierre; si pudiera modificarse después, un cajero con faltante podría ocultarlo, y un cajero honesto quedaría desprotegido ante acusaciones falsas.|
|Tácticas Arquitectónicas|No existe forma de editar ni eliminar este registro una vez creado, ni desde la interfaz ni directamente sobre la base de datos: los permisos de modificación están deshabilitados a ese nivel. Cualquier corrección exige cerrar el turno y abrir uno nuevo.|
|Supuestos y Riesgos|Se asume que el cajero declara el monto real. Riesgo: el sistema no puede verificar si el monto declarado es falso; esto es una limitación del modelo de negocio, mitigable con verificaciones físicas periódicas del dueño.|
|Prueba de Aceptación|Tras abrir la caja con $200.000, cualquier intento de modificar el monto (por interfaz, API o base de datos) debe rechazarse y el registro debe conservar sus valores originales.|

---

## 21. ESC-CAL-AUD-0021

### Metadatos

| Campo               | Valor                           |
| ------------------- | ------------------------------- |
| Atributo de Calidad | Capacidad para ser auditado     |
| Prioridad           | Crítica                         |
| Dificultad / Riesgo | Baja                            |
| Estado              | Borrador                        |
| RF relacionado      | RNF-017 (Registro de auditoría) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Cualquier usuario del sistema, incluido el administrador con el rol de mayor privilegio.|
|2|Estímulo|El administrador intenta modificar o eliminar una venta ya confirmada, un ajuste de inventario ya registrado o un registro del historial de precios.|
|3|Artefacto|La capa de persistencia que protege la inmutabilidad de los registros de auditoría a nivel de aplicación y de base de datos.|
|4|Entorno|Operación normal del sistema en modo online.|
|5|Respuesta|El sistema rechaza la operación, informa que los registros históricos son inmodificables por política del sistema, y registra el intento con usuario, fecha y hora. Cualquier corrección debe hacerse mediante un nuevo registro que referencie al original.|
|6|Medida de Respuesta|0 registros de auditoría modificados o eliminados, 100% de intentos rechazados y registrados, mensaje de rechazo en menos de 1 segundo.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|La trazabilidad legal exige conservar registros mercantiles inalterados por años; permitir borrarlos destruiría la credibilidad del sistema como herramienta de auditoría.|
|Tácticas Arquitectónicas|No existen funciones de edición ni eliminación para estas entidades en ningún nivel del sistema, incluyendo los permisos de base de datos del usuario de la aplicación. Las correcciones se modelan como nuevos registros que referencian al original con un motivo explícito.|
|Supuestos y Riesgos|Se asume que no hay acceso directo a producción por fuera de la aplicación. Riesgo: un acceso directo a la base de datos podría modificar registros; se mitiga restringiendo y auditando ese acceso.|
|Prueba de Aceptación|Un administrador que intente eliminar una venta antigua o modificar un ajuste pasado debe recibir el rechazo en ambos casos, con el mensaje de inmutabilidad y el intento registrado.|

---

## 22. ESC-CAL-AUD-0027

### Metadatos

| Campo               | Valor                                       |
| ------------------- | ------------------------------------------- |
| Atributo de Calidad | Capacidad para ser auditado                 |
| Prioridad           | Alta                                        |
| Dificultad / Riesgo | Media                                       |
| Estado              | Borrador                                    |
| RF relacionado      | RF-002.4 (Consultar movimientos inventario) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Administrador que necesita investigar por qué un lote de yogurt llegó a vencimiento con unidades sin vender.|
|2|Estímulo|El administrador consulta la trazabilidad completa de un lote específico desde su ingreso hasta su estado actual.|
|3|Artefacto|El módulo de consulta de historial que cruza entradas de mercancía, lotes, ventas, ajustes y descuentos para reconstruir el ciclo de vida completo de un lote.|
|4|Entorno|Operación normal del sistema en modo online.|
|5|Respuesta|El sistema muestra el ciclo completo del lote: entrada (proveedor, cantidad, costo, vencimiento, responsable), ventas que descontaron de él, ajustes realizados, descuentos aplicados y estado actual.|
|6|Medida de Respuesta|100% de operaciones visibles sin faltantes, respuesta en menos de 3 segundos, la suma de salidas más el saldo actual coincide con la cantidad inicial del lote.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Entender por qué un lote se venció con unidades restantes permite mejorar futuras decisiones de compra; sin esta trazabilidad, las pérdidas por vencimiento quedan sin explicación.|
|Tácticas Arquitectónicas|Cada venta y ajuste queda vinculado al lote específico, permitiendo reconstruir el historial con una sola consulta. La cantidad inicial del lote nunca cambia, sirviendo de referencia fija para validar consistencia.|
|Supuestos y Riesgos|Se asume que todas las operaciones referencian correctamente su lote. Riesgo: un error en la lógica de descuento por vencimiento mostraría datos inconsistentes; se mitiga validando que la cantidad inicial siempre cuadre con lo disponible más lo salido.|
|Prueba de Aceptación|Para un lote con ventas, un ajuste por daño y unidades restantes, la consulta debe mostrar todas las ventas con cajero y fecha, el ajuste con su motivo y responsable, y el estado actual con la fecha de vencimiento.|

---

## 23. ESC-CAL-USA-0001

### Metadatos

| Campo               | Valor                      |
| ------------------- | -------------------------- |
| Atributo de Calidad | Usabilidad                 |
| Prioridad           | Crítica                    |
| Dificultad / Riesgo | Media                      |
| Estado              | Borrador                   |
| RF relacionado      | RF-004.1 (Registrar venta) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Cajero nuevo sin experiencia previa con el sistema ni con software de gestión en general.|
|2|Estímulo|El dueño le pide al cajero nuevo que registre una venta de un solo producto el primer día de trabajo.|
|3|Artefacto|La interfaz completa del módulo de ventas: login, selección de turno, búsqueda de producto, carrito, confirmación de pago y comprobante.|
|4|Entorno|Primer uso del sistema por parte del cajero. No ha recibido capacitación previa ni ha leído un manual.|
|5|Respuesta|El cajero logra iniciar sesión, abrir el turno, buscar el producto, agregarlo al carrito, confirmar el pago y generar el comprobante sin ayuda externa ni errores críticos.|
|6|Medida de Respuesta|Primera venta completada en menos de 30 minutos, no más de 5 clics una vez abierto el turno, ninguna pantalla requiere conocimientos técnicos.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|La alta rotación de personal en tiendas de barrio hace que una curva de aprendizaje larga sea un bloqueador real de adopción del sistema.|
|Tácticas Arquitectónicas|Flujo de venta lineal sin bifurcaciones, con lenguaje cotidiano en vez de términos técnicos, mensajes de error claros con la acción correctiva, y botones principales bien diferenciados por color y tamaño.|
|Supuestos y Riesgos|Se asume que el cajero sabe leer y usar un teclado y un mouse. Riesgo: dificultades de lectoescritura podrían extender el tiempo; se mitiga acompañando todo texto clave con iconos.|
|Prueba de Aceptación|Un cajero sin capacitación previa debe lograr registrar una venta completa de un producto en menos de 30 minutos, sin ayuda ni bloqueos por falta de comprensión.|

---

## 24. ESC-CAL-USA-0017

### Metadatos

| Campo               | Valor                                                          |
| ------------------- | -------------------------------------------------------------- |
| Atributo de Calidad | Usabilidad                                                     |
| Prioridad           | Alta                                                           |
| Dificultad / Riesgo | Media                                                          |
| Estado              | Borrador                                                       |
| RF relacionado      | RF-008.4 (Alerta vencimiento), RF-010.1 (Dashboard inventario) |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|El sistema detecta que un lote de queso tiene 3 días para vencer con 15 unidades sin vender.|
|2|Estímulo|El administrador abre el dashboard del sistema al inicio del día.|
|3|Artefacto|El componente de alertas del dashboard que muestra notificaciones priorizadas con información accionable.|
|4|Entorno|Operación normal al inicio de la jornada comercial.|
|5|Respuesta|El dashboard muestra la alerta con producto, días restantes, unidades y valor en riesgo, y una acción sugerida visible, priorizada por color según urgencia frente a otras alertas.|
|6|Medida de Respuesta|El administrador identifica el producto y la acción sugerida en menos de 10 segundos, sin navegar a otra pantalla, con alertas ordenadas de mayor a menor urgencia.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Las alertas convierten datos operativos en decisiones; si no son claras y accionables de un vistazo, el dueño las ignora y el producto se vence.|
|Tácticas Arquitectónicas|Cada alerta muestra en un solo componente el producto, días restantes, cantidad y valor en riesgo, con un botón de acción directa. Se usan colores tipo semáforo según urgencia, acompañados de iconos para accesibilidad, y el botón lleva directo a la acción sin pasos intermedios.|
|Supuestos y Riesgos|Se asume que el administrador revisa el dashboard al menos una vez al día. Riesgo: si no lo revisa, la alerta pasa desapercibida; se contempla a futuro un aviso por otro canal.|
|Prueba de Aceptación|Con un lote de queso a 3 días de vencer y 15 unidades disponibles, el dashboard debe mostrar la alerta con su nivel de urgencia, producto, días restantes, unidades, valor y un botón de acción visible sin necesidad de scroll ni navegación.|

---

## 25. ESC-CAL-ESC-0008

### Metadatos

| Campo               | Valor                                             |
| ------------------- | ------------------------------------------------- |
| Atributo de Calidad | Escalabilidad                                     |
| Prioridad           | Media                                             |
| Dificultad / Riesgo | Baja                                              |
| Estado              | Borrador                                          |
| RF relacionado      | RF-013.1 (Registrar negocio)                      |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Un dueño que opera un minimercado exitoso y decide abrir un segundo punto de venta usando el sistema.|
|2|Estímulo|El administrador registra un segundo negocio en la plataforma y comienza a operar con catálogo y empleados independientes del primer negocio.|
|3|Artefacto|La arquitectura que gestiona múltiples negocios en la misma infraestructura con aislamiento estricto de datos.|
|4|Entorno|El primer negocio tiene 6 meses de operación con 500 productos, 100 lotes activos y 10.000 ventas históricas.|
|5|Respuesta|El segundo negocio opera de forma completamente independiente: sin datos visibles cruzados entre negocios y sin afectar el rendimiento del primero.|
|6|Medida de Respuesta|0 datos cruzados entre negocios, degradación menor al 10% en el primer negocio, segundo negocio operativo en menos de 10 minutos.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Soportar múltiples negocios desde el inicio es la base del modelo comercial; si agregar uno afecta al otro, el producto no es escalable comercialmente.|
|Tácticas Arquitectónicas|Todas las entidades del dominio incluyen un identificador de negocio obligatorio, y todas las consultas filtran automáticamente por el negocio de la sesión activa, con índices optimizados para ese filtro. El backend puede escalar horizontalmente sin dependencia entre negocios.|
|Supuestos y Riesgos|Se asume independencia total entre negocios en esta etapa. Riesgo: una carga anormalmente alta de un negocio podría afectar a otros en la misma infraestructura; se mitiga con límites de consumo por negocio.|
|Prueba de Aceptación|Al registrar un negocio B junto a un negocio A con 6 meses de historia, B debe operar con su propio catálogo, empleados y ventas sin ver datos de A, y los tiempos de respuesta de A deben mantenerse dentro de los umbrales definidos.|

---

## 26. ESC-CAL-SEG-0012

### Metadatos

| Campo               | Valor                                                              |
| ------------------- | ------------------------------------------------------------------ |
| Atributo de Calidad | Seguridad                                                          |
| Prioridad           | Alta                                                               |
| Dificultad / Riesgo | Media                                                              |
| Estado              | Borrador                                                           |
| RF relacionado      | RF-007.7 (Cambiar contraseña), RF-007.5 (Iniciar sesión)           |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Usuario autenticado de cualquier rol (Cajero, Supervisor o Administrador) con sesión vigente.|
|2|Estímulo|El usuario ingresa su contraseña actual y una nueva contraseña que cumple la política, y confirma el cambio desde su perfil.|
|3|Artefacto|El caso de uso de cambio de contraseña del Backend, el proveedor de identidad (Keycloak) que almacena la credencial, el Key Vault que entrega al Backend las credenciales de acceso al IDP y la pasarela de notificaciones.|
|4|Entorno|Operación en línea. El tráfico viaja por HTTPS a través de WAF + CDN y Reverse Proxy, con un token de acceso vigente. El cambio de contraseña no está disponible sin conexión.|
|5|Respuesta|El Backend valida el token, verifica la contraseña actual, valida la política de contraseñas, actualiza la credencial en el IDP, cierra las demás sesiones del usuario, envía un correo de aviso de seguridad y registra el evento de auditoría sin datos secretos. El usuario recibe la confirmación del cambio.|
|6|Medida de Respuesta|Confirmación en menos de 3 segundos (p95), correo de aviso enviado en menos de 60 segundos, 0 apariciones de contraseñas en logs, trazas, respuestas o almacenamiento local, 100% de los cambios con registro de auditoría (usuario, fecha, hora y resultado), 100% de las comunicaciones cifradas.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|La credencial es la puerta de entrada a ventas, inventario y caja. Permitir su rotación con verificación de la contraseña actual protege ante una sesión abierta desatendida, y el correo de aviso permite al dueño detectar un cambio no autorizado.|
|Tácticas Arquitectónicas|Seguridad en profundidad con validación en cliente, backend e IDP. Backend sin estado: el JWT se valida en cada request. Puertos y adaptadores para IDP, secretos y notificaciones. Credenciales hacia el IDP obtenidas del Key Vault en tiempo de ejecución. La ruta no se cachea (NetworkOnly en el Service Worker y Cache-Control: no-store). Enmascaramiento de campos sensibles en logs. Revocación de las demás sesiones del usuario en el IDP (ADR-012). Correo sin contraseñas ni datos sensibles.|
|Supuestos y Riesgos|Se asume que el correo registrado del usuario es válido. Riesgo: al ser stateless, los tokens de acceso ya emitidos siguen siendo válidos hasta expirar; se mitiga con tokens de vida corta. Riesgo: el mecanismo de verificación de la contraseña actual depende del IDP (SPK-006).|
|Prueba de Aceptación|Dado un cajero con sesión vigente, contraseña actual correcta y nueva contraseña válida, el sistema debe confirmar el cambio, el siguiente inicio de sesión solo debe funcionar con la nueva contraseña, debe llegar el correo de aviso y ningún log debe contener la contraseña.|

---

## 27. ESC-CAL-SEG-0014

### Metadatos

| Campo               | Valor                                  |
| ------------------- | -------------------------------------- |
| Atributo de Calidad | Seguridad                              |
| Prioridad           | Alta                                   |
| Dificultad / Riesgo | Media                                  |
| Estado              | Borrador                               |
| RF relacionado      | RF-007.7 (Cambiar contraseña)          |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Persona no autorizada que accede a una sesión abierta y desatendida de un usuario del negocio.|
|2|Estímulo|La persona intenta cambiar la contraseña adivinando la contraseña actual: realiza 6 intentos fallidos en 15 minutos.|
|3|Artefacto|El middleware de rate limiting y el caso de uso de cambio de contraseña del Backend, con el IDP como validador de la contraseña actual.|
|4|Entorno|Operación en línea, con la sesión aún vigente (antes del cierre por inactividad de 15 minutos).|
|5|Respuesta|Cada intento fallido se rechaza con un mensaje genérico. Al superar el límite de 5 intentos en 15 minutos, el sistema responde que debe esperar, no cambia la credencial y registra todos los intentos.|
|6|Medida de Respuesta|0 cambios de credencial, 100% de los intentos registrados con usuario, fecha, hora y origen, respuesta de bloqueo en menos de 1 segundo tras superar el límite, límite restablecido al terminar la ventana.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Una sesión abierta en un equipo compartido es un riesgo real en tiendas de barrio; sin límite de intentos, un tercero podría tomar control de la cuenta adivinando la contraseña actual.|
|Tácticas Arquitectónicas|Rate limiting diferenciado por endpoint dentro del Backend (ADR-007), más estricto para esta ruta. Reglas de rate limit adicionales en el WAF. Detección de fuerza bruta del IDP como segunda capa. Registro de cada intento en el log de auditoría.|
|Supuestos y Riesgos|Se asume un límite inicial de 5 intentos por usuario cada 15 minutos. Riesgo: la detección de fuerza bruta del IDP podría bloquear temporalmente la cuenta del usuario legítimo; se documenta el procedimiento de desbloqueo por el Administrador. Riesgo: el contador en memoria no se comparte entre instancias (deuda aceptada en ADR-007).|
|Prueba de Aceptación|Tras 5 intentos fallidos, el sexto debe ser rechazado por límite de intentos sin consultar al IDP, la contraseña debe permanecer sin cambios y los 6 intentos deben quedar registrados.|

---

## 28. ESC-CAL-DIS-0036

### Metadatos

| Campo               | Valor                                  |
| ------------------- | -------------------------------------- |
| Atributo de Calidad | Disponibilidad                         |
| Prioridad           | Media                                  |
| Dificultad / Riesgo | Media                                  |
| Estado              | Borrador                               |
| RF relacionado      | RF-007.7 (Cambiar contraseña)          |

### Definición del Escenario

|Parte|Componente|Descripción|
|---|---|---|
|1|Origen|Fallo de un componente externo: la pasarela de notificaciones, el Key Vault o el IDP.|
|2|Estímulo|El usuario confirma el cambio de contraseña y ocurre una de dos situaciones: (a) el proveedor de correo no responde después de actualizada la credencial, o (b) el Key Vault o el IDP no están disponibles.|
|3|Artefacto|El caso de uso de cambio de contraseña y sus adaptadores hacia IDP, Key Vault y notificaciones.|
|4|Entorno|Operación en línea, con el resto del sistema (ventas e inventario) funcionando normalmente.|
|5|Respuesta|En (a), el cambio se mantiene, el usuario recibe la confirmación y el correo se reintenta de forma asíncrona. En (b), la operación falla de forma controlada con un mensaje de "intenta más tarde", sin cambios parciales.|
|6|Medida de Respuesta|0 cambios parciales de credencial, respuesta de error en menos de 5 segundos (timeouts), hasta 5 reintentos de correo con backoff exponencial en menos de 1 hora, 0 segundos de interrupción en ventas e inventario.|

### Notas de Arquitectura

|Aspecto|Descripción|
|---|---|
|Justificación de Negocio|Un fallo en un servicio de soporte no puede impedir la operación del negocio ni dejar la credencial en un estado inconsistente; el aviso por correo es una medida de seguridad, no una condición para que el cambio sea válido.|
|Tácticas Arquitectónicas|Timeouts y reintentos acotados en los adaptadores. Envío de correo asíncrono con reintentos. Errores de dominio diferenciados. Aislamiento de fallos entre módulos (ESC-CAL-DIS-0029). Fallos de notificación registrados para seguimiento.|
|Supuestos y Riesgos|Se asume que el cambio en el IDP es atómico (se aplica o no se aplica). Riesgo: si el proveedor de correo está caído por mucho tiempo, el usuario no recibe el aviso; se mitiga con alerta en monitoreo.|
|Prueba de Aceptación|Con el proveedor de correo simulado como caído, el cambio debe confirmarse y el correo reintentarse. Con el Key Vault simulado como caído, el cambio debe fallar de forma controlada y la contraseña original debe seguir funcionando.|