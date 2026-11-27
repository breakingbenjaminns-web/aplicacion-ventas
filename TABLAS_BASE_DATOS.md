# Control de tablas de la base de datos

Propuesta inicial de tablas. Marcar cada casilla cuando la tabla esté completada y actualizar sus campos si se agregan o modifican durante el desarrollo.

## Tablas

- [x] **USUARIOS (users)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre | TEXT NOT NULL UNIQUE | Nombre de usuario |
  | contrasena | TEXT NOT NULL | Contraseña almacenada de forma segura |
  | email | TEXT | Correo electrónico |
  | rol | TEXT DEFAULT 'operador' | Por ejemplo, `admin` u `operador` |
  | activo | BOOLEAN DEFAULT 1 | Indica si el usuario está activo |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |

  Se admite una única cuenta administradora inicial con contraseña BCrypt. No hay gestión de cuentas adicionales, cambio o recuperación de contraseña. El usuario temporal generado por Spring ya no sirve para iniciar sesión.

- [x] **EVENTOS (events)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre | TEXT NOT NULL | Nombre del evento |
  | descripcion | TEXT | Descripción |
  | fecha | DATE NOT NULL | Fecha del evento |
  | estado | TEXT DEFAULT 'pendiente' | `pendiente`, `activo`, `cerrado` o `cancelado` |
  | usuario_id | INTEGER FOREIGN KEY → users.id | Usuario asociado |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |

- [x] **CATEGORIAS (categories)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre | TEXT NOT NULL | Nombre de la categoría |
  | descripcion | TEXT | Descripción |

- [x] **EVENTO_CATEGORIA (event_categories)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | evento_id | INTEGER NOT NULL FOREIGN KEY → events.id | Evento asociado |
  | categoria_id | INTEGER NOT NULL FOREIGN KEY → categories.id | Categoría asociada |

  La clave primaria es la combinación de `evento_id` y `categoria_id`, para evitar asociaciones duplicadas.

- [x] **PRODUCTOS (products)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre | TEXT NOT NULL | Nombre del producto, hasta 100 caracteres |
  | descripcion | TEXT | Descripción |
  | precio | INTEGER NOT NULL | Precio en centavos, entero no negativo |
  | stock | INTEGER | `NULL`: sin control; `0`: sin unidades; mayor a `0`: unidades disponibles |
  | categoria_id | INTEGER NOT NULL | Categoría asociada al evento |
  | evento_id | INTEGER NOT NULL | Evento al que pertenece |
  | activo | BOOLEAN NOT NULL DEFAULT 1 | Indica si el producto está activo |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |

  La clave foránea compuesta (`evento_id`, `categoria_id`) referencia `event_categories`. Precio y stock admiten hasta 9007199254740991 para preservar la precisión en el navegador.

- [x] **CAJA (box)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | evento_id | INTEGER NOT NULL UNIQUE FOREIGN KEY → events.id | Evento asociado |
  | estado | TEXT NOT NULL DEFAULT 'cerrada' | `cerrada` o `abierta` |
  | saldo_inicial | INTEGER | Base del período actual, en centavos |
  | saldo_actual | INTEGER | Efectivo esperado mientras está abierta; contado al cerrar |
  | fecha_apertura | DATETIME | Apertura más reciente |
  | fecha_cierre | DATETIME | Cierre más reciente; `NULL` mientras está abierta |

- [x] **PERÍODOS DE CAJA (cash_periods)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | caja_id | INTEGER NOT NULL FOREIGN KEY → box.id | Caja asociada |
  | saldo_inicial | INTEGER NOT NULL | Base del período, en centavos |
  | fecha_apertura | DATETIME NOT NULL | Fecha de apertura |
  | fecha_cierre | DATETIME | Fecha de cierre; `NULL` mientras está abierto |
  | efectivo_esperado | INTEGER | Saldo esperado al cerrar, en centavos |
  | efectivo_contado | INTEGER | Efectivo contado al cerrar, en centavos |
  | diferencia | INTEGER | Contado menos esperado, en centavos |
  | usuario_apertura_id | INTEGER FOREIGN KEY → users.id | Usuario que abrió, pendiente de integrar |
  | usuario_cierre_id | INTEGER FOREIGN KEY → users.id | Usuario que cerró, pendiente de integrar |

  Solo puede haber un período abierto por caja. Los campos de cierre permanecen vacíos hasta cerrar el período.

- [x] **APORTES DE CAJA (cash_contributions)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | periodo_id | INTEGER NOT NULL FOREIGN KEY → cash_periods.id | Período inicial |
  | responsable | TEXT NOT NULL | Nombre de quien aportó |
  | importe | INTEGER NOT NULL | Aporte positivo en centavos |

- [x] **TICKETS (tickets)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | evento_id | INTEGER NOT NULL FOREIGN KEY → events.id | Evento asociado |
  | estado | TEXT NOT NULL DEFAULT 'abierto' | `abierto`, `pagado`, `deuda` o `anulado` |
  | total | INTEGER NOT NULL | Total en centavos |
  | metodo_pago | TEXT | `efectivo`, `transferencia` o `deuda` al confirmar |
  | deudor | TEXT | Nombre obligatorio para una deuda |
  | detalle | TEXT | Observación opcional |
  | periodo_id | INTEGER FOREIGN KEY → cash_periods.id | Período abierto al confirmar |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |
  | fecha_confirmacion | DATETIME | Fecha de confirmación de la venta |

  El identificador `id` se usa como número de ticket. La asociación con usuarios está pendiente.

- [x] **DETALLES_TICKET (ticket_details)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | ticket_id | INTEGER FOREIGN KEY → tickets.id | Ticket asociado |
  | producto_id | INTEGER FOREIGN KEY → products.id | Producto vendido |
  | cantidad | INTEGER NOT NULL | Cantidad vendida |
  | precio_unitario | INTEGER NOT NULL | Precio unitario en centavos |
  | subtotal | INTEGER NOT NULL | Subtotal en centavos |

- [ ] **MOVIMIENTOS_CAJA (box_movements)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | caja_id | INTEGER NOT NULL FOREIGN KEY → box.id | Caja asociada |
  | periodo_id | INTEGER NOT NULL FOREIGN KEY → cash_periods.id | Período de la venta |
  | ticket_id | INTEGER NOT NULL UNIQUE FOREIGN KEY → tickets.id | Ticket asociado |
  | tipo | TEXT NOT NULL | `ingreso` por venta en efectivo |
  | monto | INTEGER NOT NULL | Importe en centavos |
  | fecha | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha del movimiento |

  Registra ingresos de ventas en efectivo. Las devoluciones por anulación se conservan en `cash_refunds`; otros egresos y ajustes siguen pendientes.

- [x] **ANULACIONES (ticket_cancellations)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | ticket_id | INTEGER PRIMARY KEY FOREIGN KEY → tickets.id | Ticket anulado |
  | estado_anterior | TEXT NOT NULL | `abierto`, `pagado` o `deuda` antes de anular |
  | motivo | TEXT NOT NULL | Motivo obligatorio, hasta 500 caracteres |
  | devolucion_transferencia | INTEGER NOT NULL | Confirmación manual de devolución externa |
  | fecha | DATETIME NOT NULL | Fecha de anulación en UTC |

- [x] **DEVOLUCIONES DE EFECTIVO (cash_refunds)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | caja_id | INTEGER NOT NULL FOREIGN KEY → box.id | Caja de la devolución |
  | periodo_id | INTEGER NOT NULL FOREIGN KEY → cash_periods.id | Período abierto al devolver |
  | ticket_id | INTEGER NOT NULL UNIQUE FOREIGN KEY → ticket_cancellations.ticket_id | Anulación asociada |
  | monto | INTEGER NOT NULL | Efectivo devuelto en centavos |
  | fecha | DATETIME NOT NULL | Fecha en UTC |

- [x] **HISTORIAL_INVENTARIO (inventory_history)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | producto_id | INTEGER FOREIGN KEY → products.id | Producto asociado |
  | cantidad_anterior | INTEGER | Cantidad anterior |
  | cantidad_nueva | INTEGER | Cantidad nueva |
  | tipo_movimiento | TEXT | `venta`, `ajuste` o `entrada` |
  | ticket_id | INTEGER | Ticket asociado, opcional; sin clave foránea por ahora |
  | usuario_id | INTEGER FOREIGN KEY → users.id | Usuario asociado |
  | fecha | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha del movimiento |

  Se registran entradas iniciales, ajustes, ventas y devoluciones. Para mantener la compatibilidad con bases existentes, la devolución se guarda como `entrada` vinculada al ticket y se muestra como `devolucion` en el historial. El autor de las operaciones nuevas se registra en `activity_log`. La tabla existente todavía no tiene una clave foránea para `ticket_id`: agregarla requiere una migración que preserve los datos.

- [x] **ACTIVIDAD (activity_log)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | usuario_id | INTEGER NOT NULL FOREIGN KEY → users.id | Autor autenticado |
  | accion | TEXT NOT NULL | Operación realizada |
  | entidad | TEXT NOT NULL | Tipo de registro afectado |
  | entidad_id | INTEGER NOT NULL | Identificador del registro afectado |
  | fecha | DATETIME NOT NULL | Fecha en UTC |

  La actividad se registra en la misma transacción que cada operación. Los registros anteriores se conservan sin atribuirles un usuario.

- [ ] **COPIAS_SEGURIDAD (backups)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre_archivo | TEXT NOT NULL UNIQUE | Nombre del archivo |
  | evento_id | INTEGER FOREIGN KEY → events.id | Evento asociado |
  | tamaño | INTEGER | Tamaño en bytes |
  | hash | TEXT | Valor para verificar integridad |
  | usuario_id | INTEGER FOREIGN KEY → users.id | Usuario asociado |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |
  | fecha_restauracion | DATETIME | Fecha de restauración |

## Notas

- Los importes se expresan en unidades menores enteras, por ejemplo, centavos.
- Las relaciones propuestas utilizan claves foráneas y los estados se expresan como texto.
- El diseño es inicial: los detalles pendientes de las funcionalidades y las restricciones definitivas se decidirán durante el desarrollo.

## Relaciones

`1 → N` indica que un registro puede asociarse con varios registros del otro lado.

| Relaciones | Tipo | Descripción |
| --- | --- | --- |
| Evento ↔ Categoría | Muchos a muchos | Un evento puede tener varias categorías y una categoría puede usarse en varios eventos, mediante `event_categories`. |
| Evento → Evento_categoría | Uno a muchos | Un evento puede tener varias asociaciones con categorías. |
| Categoría → Evento_categoría | Uno a muchos | Una categoría puede tener varias asociaciones con eventos. |
| Evento → Productos | Uno a muchos | Un evento puede tener varios productos. |
| Categoría → Productos | Uno a muchos | Una categoría puede tener varios productos, incluso de distintos eventos. |
| Evento → Caja | Uno a uno | Un evento puede tener como máximo una caja; `box.evento_id` es único. |
| Caja → Períodos de caja | Uno a muchos | Cada apertura o reapertura crea un período y conserva sus datos de cierre. |
| Período de caja → Aportes | Uno a muchos | Los aportes se registran en el período de apertura inicial. |
| Evento → Tickets | Uno a muchos | Un evento puede tener varios tickets. |
| Ticket → Detalles de ticket | Uno a muchos | Un ticket puede tener varias líneas de detalle. |
| Ticket → Anulación | Uno a uno | Una anulación conserva el estado anterior y su motivo. |
| Anulación → Devolución de efectivo | Uno a uno | Una anulación en efectivo registra un egreso en el período abierto. |
| Producto → Detalles de ticket | Uno a muchos | Un producto puede aparecer en varios detalles de ticket. |
| Caja → Movimientos de caja | Uno a muchos | Una caja puede tener varios movimientos. |
| Producto → Historial de inventario | Uno a muchos | Un producto puede tener varios registros de inventario. |
| Ticket → Historial de inventario | Uno a muchos | Un ticket puede asociarse a varios registros de inventario; los ajustes manuales pueden no tener ticket. |
| Evento → Copias de seguridad | Uno a muchos | Relación propuesta, pendiente de revisar: cada copia respalda toda la base de datos. |
| Usuario → Eventos | Uno a muchos | Un usuario puede asociarse a varios eventos. |
| Usuario → Tickets | Uno a muchos | Un usuario puede asociarse a varios tickets. |
| Usuario → Movimientos de caja | Uno a muchos | Un usuario puede asociarse a varios movimientos. |
| Usuario → Historial de inventario | Uno a muchos | Un usuario puede asociarse a varios registros de inventario. |
| Usuario → Copias de seguridad | Uno a muchos | Un usuario puede asociarse a varias copias. |
| Usuario → Cajas abiertas | Uno a muchos | Un usuario puede figurar como responsable de apertura de varias cajas. |
| Usuario → Cajas cerradas | Uno a muchos | Un usuario puede figurar como responsable de cierre de varias cajas. |

La relación entre **tickets y productos** también es muchos a muchos: un ticket puede contener varios productos y un producto puede aparecer en varios tickets. Se representa mediante `ticket_details`, que además guarda la cantidad y el precio de cada línea.

Cada producto debe tener evento y categoría asignados, y la pareja debe existir en `event_categories`; la clave foránea compuesta garantiza esta relación.

Salvo donde se indica `NOT NULL`, las claves foráneas de la propuesta admiten `NULL`; por lo tanto, esas asociaciones son opcionales por ahora. La obligatoriedad de cada relación queda pendiente de definir.

Los movimientos de efectivo se vinculan al ticket y al período de caja. Los períodos conservan aperturas, cierres y reaperturas.

## Orden sugerido de implementación

Implementar cada etapa como una funcionalidad completa, con sus tablas, backend y pruebas; después se puede conectar la pantalla correspondiente.

- [x] **1. Definir reglas y relaciones básicas:** acordar categorías compartidas, estados de tickets y control de stock, y actualizar esta propuesta.
- [x] **2. Eventos:** crear la tabla, el modelo, repositorio, servicio y controller para crear y consultar eventos y cambiar su estado.
- [x] **3. Categorías y productos:** implementar categorías compartidas, su asociación con eventos y los productos de cada evento, con precio en centavos y stock opcional.
- [x] **4. Caja:** implementar apertura, cierre y reapertura con períodos para conservar el historial.
- [x] **5. Tickets y detalles:** crear ventas con productos, cantidades y precios; confirmar el pago y actualizar el stock y la caja en una misma operación.
- [x] **6. Anulaciones e historial:** registrar anulaciones y los movimientos de inventario y caja que correspondan, sin borrar el historial de ventas.
- [ ] **7. Usuarios y permisos:** cuenta administradora local y auditoría implementadas; quedan pendientes la gestión de usuarios adicionales, recuperación de cuenta y la seguridad necesaria para acceso por red.
- [ ] **8. Copias de seguridad e informes:** integrar los respaldos existentes en la interfaz y generar informes con datos reales; decidir si hace falta una tabla de historial de copias.

## Reglas y relaciones básicas (borrador para acordar)

Estas reglas concretan la propuesta inicial. Se consideran criterios de trabajo hasta que se validen; si se acuerdan cambios, actualizar también las tablas y relaciones afectadas.

### Categorías

- Las categorías son compartidas entre eventos: una categoría se crea una sola vez y puede asociarse a varios eventos mediante `event_categories`.
- La pareja (`evento_id`, `categoria_id`) no se puede repetir.
- Un producto de un evento solo puede usar una categoría asociada a ese mismo evento. Si se asignan ambas relaciones, la pareja (`evento_id`, `categoria_id`) del producto debe existir en `event_categories`.
- Quitar una categoría de un evento no debe borrar la categoría ni los productos asociados; antes de eliminarla, se debe comprobar que ningún producto la esté usando.
- Al guardar un evento desde el formulario, sus categorías seleccionadas se crean o reutilizan por nombre y se reemplazan sus asociaciones. El nombre no distingue mayúsculas/minúsculas.

### Eventos en la interfaz

- La opción visible `Abierto` corresponde al estado persistido `activo`; `Pendiente`, `Cerrado` y `Cancelado` se guardan como `pendiente`, `cerrado` y `cancelado`.
- Al crear un evento se guarda el estado actualmente seleccionado en el formulario (por defecto, `activo`).
- La pantalla muestra la caja como `Cerrada` cuando el evento todavía no tiene un registro en `box`; la apertura y el cierre siguen siendo responsabilidad de la funcionalidad de caja.

### Apertura y cierre de caja

- La apertura inicial acepta aportes positivos con responsable; si no hay aportes, la base es cero.
- El cierre guarda el efectivo esperado, el efectivo contado y la diferencia entre ambos.
- La reapertura crea un período nuevo cuya base es el efectivo contado en el cierre anterior. Los períodos anteriores y los aportes iniciales se conservan.
- Los importes son enteros en centavos, con un máximo de 9007199254740991. Las fechas almacenadas por SQLite se expresan en UTC.
- Si existen datos antiguos de caja sin períodos, no se sobrescriben automáticamente. Las nuevas operaciones de caja identifican al usuario en `activity_log`.
- `GET /api/cajas/evento/{eventoId}` consulta la caja y sus períodos; `POST` a la misma ruta con `/abrir`, `/cerrar` o `/reabrir` ejecuta cada operación. La apertura recibe `{"aportes":[{"responsable":"...","importe":10000}]}`, el cierre `{"efectivoContado":10000}` y la reapertura no requiere cuerpo.

### Estados y ciclo de vida de tickets

- `abierto`: ticket creado, todavía no cobrado. No genera movimientos de caja ni descuenta stock.
- `pagado`: pago confirmado. El ticket conserva los productos, cantidades y precios efectivamente cobrados.
- `deuda`: venta confirmada y pendiente de cobro; requiere el nombre del deudor.
- `anulado`: ticket cancelado. No se elimina el registro ni sus detalles.
- Se permiten `abierto` → `pagado`, `abierto` → `deuda` y `abierto`/`pagado`/`deuda` → `anulado`. El estado `anulado` es terminal; el cobro posterior de deudas está pendiente.
- Para anular se exige un motivo. Un ticket abierto no requiere movimientos compensatorios. Una deuda anulada devuelve el stock descontado y deja de estar pendiente.
- Anular un pago en efectivo requiere caja abierta y saldo suficiente; se devuelve el efectivo en el período actual, incluso si el período original ya cerró, sin modificar cierres anteriores.
- La devolución por transferencia se realiza fuera de la aplicación; quien anula debe confirmar que ya se realizó. La anulación conserva esa confirmación y no modifica el efectivo.
- La anulación conserva el ticket, sus detalles, el medio de pago, el estado anterior y todos los movimientos originales. La anulación y sus devoluciones se registran en una única transacción.
- La confirmación del pago y sus efectos (estado del ticket, movimiento de caja y descuento de stock) deben completarse juntos: si una parte falla, no se confirma ninguna.
- El total se calcula a partir de las líneas del ticket (`cantidad × precio_unitario`); cada detalle conserva el precio vigente al crear el ticket abierto, aunque luego cambie el precio del producto. Los tickets guardados no se editan.
- Toda confirmación requiere una caja con período abierto. Efectivo incrementa su saldo y genera un movimiento; transferencia no modifica el efectivo; deuda tampoco lo modifica y permanece pendiente de cobro.

### Control de stock

- `stock = NULL` significa que el producto no tiene control de stock; la venta no lo decrementa ni se rechaza por disponibilidad.
- `stock = 0` o un valor positivo significa que el stock está controlado. Las cantidades negativas no son válidas.
- La cantidad de cada línea debe ser un entero positivo. Al confirmar el pago, se rechaza la venta si la cantidad solicitada supera el stock disponible de algún producto controlado.
- El stock controlado se descuenta al confirmar una venta pagada o a deuda, no al crear un ticket abierto. Al anular se devuelve únicamente el stock que fue descontado.
- Si el control de stock actual no coincide con el que se utilizó en la venta, o la devolución supera el máximo permitido, la anulación se rechaza para que se revise el producto. Un producto desactivado puede recibir la devolución.
- Cada descuento, devolución o ajuste manual debe dejar un registro en `inventory_history` con producto, cantidad anterior y nueva, tipo de movimiento y usuario; el ticket se asocia cuando corresponda.
- Las actualizaciones de stock y la escritura de su historial deben realizarse en la misma operación para evitar que el saldo y el historial queden desincronizados.
- Los campos antiguos `usuario_id` del historial permanecen opcionales. Las nuevas operaciones se asocian al usuario mediante `activity_log`; no se asigna retroactivamente un autor a las anteriores.

### API de categorías y productos

- `GET /api/categorias` lista las categorías compartidas; `GET /api/categorias?eventoId=1` lista las asociadas a un evento, y `POST /api/categorias` crea una categoría con `nombre` y `descripcion` opcional.
- `GET /api/productos?eventoId=1` lista productos del evento, incluidos los inactivos; `GET /api/productos/{id}` consulta uno.
- `POST /api/productos` crea y `PUT /api/productos/{id}` edita usando `nombre`, `descripcion` opcional, `precio` entero en centavos, `stock` entero o `null`, `categoriaId` y `eventoId`. El evento de un producto no se puede cambiar.
- `PATCH /api/productos/{id}/activo` con `{"activo": false}` lo desactiva sin borrarlo; se puede reactivar con `true`.
- Las pantallas de productos, caja y ventas usan datos persistidos.

### API de tickets

- `GET /api/tickets?eventoId=1` lista tickets del evento; `GET /api/tickets/1` consulta uno con sus detalles.
- `POST /api/tickets` crea un ticket abierto: `{"eventoId":1,"detalle":"opcional","items":[{"productoId":1,"cantidad":2}]}`. Los precios se obtienen del servidor.
- `POST /api/tickets/1/confirmar` recibe `{"metodoPago":"efectivo"}`, `{"metodoPago":"transferencia"}` o `{"metodoPago":"deuda","deudor":"Nombre"}`. La confirmación, el stock, el historial y el ingreso de efectivo se registran en una única transacción.
- Para todos los métodos se requiere caja abierta. Confirmar dos veces el mismo ticket o vender más stock del disponible devuelve conflicto. Los productos deben seguir activos al confirmar.
- `POST /api/tickets/1/anular` recibe `{"motivo":"...","devolucionTransferenciaConfirmada":false}`. Para pagos por transferencia la confirmación debe ser `true`; para los demás debe ser `false` o estar ausente.
- `GET /api/historial?eventoId=1` consulta movimientos de inventario e ingresos y devoluciones de efectivo, identificados por ticket y período. La pantalla Tickets también muestra estos movimientos y el motivo de anulación.
- La pantalla Resumen de ventas suma los tickets del evento seleccionado: efectivo y transferencia pagados, más deuda pendiente. El total vendido incluye esas deudas; se excluyen tickets abiertos y anulados. Los gráficos siguen siendo ilustrativos.

### Administrador local y actividad

- La aplicación escucha por defecto solo en `127.0.0.1`. En la computadora donde se ejecuta, abrí `http://localhost:8080/inicializar` para crear la primera cuenta. Esta página solo funciona desde la computadora local y mientras la tabla `users` no tenga registros.
- Elegí un usuario de 3 a 40 caracteres (letras, números, punto, guion o guion bajo) y una contraseña de al menos 12 caracteres y hasta 72 bytes UTF-8. La contraseña se guarda como hash BCrypt; no existe una contraseña predeterminada.
- Después de crear la cuenta, iniciá sesión con ella. No se crean cuentas adicionales desde la aplicación y solo el rol `admin` puede acceder a las funciones protegidas.
- `GET /api/sesion` informa el usuario de la sesión; `GET /api/actividad` y la opción **Actividad** del menú de usuario muestran operaciones registradas desde la inicialización, incluyendo creación y edición de eventos, categorías y productos, caja, ventas y anulaciones.
- Si ya existen filas en `users`, la inicialización no las modifica ni reemplaza. Solo puede autenticarse un usuario activo con rol `admin` y una contraseña BCrypt válida; resolver cualquier cuenta antigua requiere intervención local sin borrar datos.
- No hay recuperación ni cambio de contraseña en la aplicación. Conservá la contraseña y realizá copias de seguridad de la base. No se ha habilitado ni evaluado el uso por red o Internet; para ese escenario se necesita un diseño de seguridad adicional, incluido HTTPS.

### Integridad de las relaciones de venta

- Un ticket pertenece a un único evento y sus productos deben pertenecer a ese mismo evento.
- Cada detalle pertenece a un ticket y referencia un producto existente; ticket, producto, cantidad y precio unitario son datos requeridos para una línea válida.
- Los registros históricos de tickets, detalles, movimientos de caja e inventario no se borran en cascada. Para preservar auditoría, se anula o desactiva el registro y se conservan las referencias.
- La definición final de `NOT NULL`, restricciones `CHECK`, índices y política de borrado de claves foráneas debe reflejar estas reglas al implementar el esquema.
