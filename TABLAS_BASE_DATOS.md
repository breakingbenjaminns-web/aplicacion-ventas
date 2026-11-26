# Control de tablas de la base de datos

Propuesta inicial de tablas. Marcar cada casilla cuando la tabla esté completada y actualizar sus campos si se agregan o modifican durante el desarrollo.

## Tablas

- [ ] **USUARIOS (users)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre | TEXT NOT NULL UNIQUE | Nombre de usuario |
  | contrasena | TEXT NOT NULL | Contraseña almacenada de forma segura |
  | email | TEXT | Correo electrónico |
  | rol | TEXT DEFAULT 'operador' | Por ejemplo, `admin` u `operador` |
  | activo | BOOLEAN DEFAULT 1 | Indica si el usuario está activo |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |

- [ ] **EVENTOS (events)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre | TEXT NOT NULL | Nombre del evento |
  | descripcion | TEXT | Descripción |
  | fecha | DATE | Fecha del evento |
  | estado | TEXT DEFAULT 'pendiente' | `pendiente`, `activo`, `cerrado` o `cancelado` |
  | usuario_id | INTEGER FOREIGN KEY → users.id | Usuario asociado |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |

- [ ] **CATEGORIAS (categories)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre | TEXT NOT NULL | Nombre de la categoría |
  | descripcion | TEXT | Descripción |

- [ ] **EVENTO_CATEGORIA (event_categories)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | evento_id | INTEGER NOT NULL FOREIGN KEY → events.id | Evento asociado |
  | categoria_id | INTEGER NOT NULL FOREIGN KEY → categories.id | Categoría asociada |

  La clave primaria es la combinación de `evento_id` y `categoria_id`, para evitar asociaciones duplicadas.

- [ ] **PRODUCTOS (products)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | nombre | TEXT NOT NULL | Nombre del producto |
  | descripcion | TEXT | Descripción |
  | precio | INTEGER NOT NULL | Precio en centavos |
  | stock | INTEGER | `NULL`: sin control; `0`: sin unidades; mayor a `0`: unidades disponibles |
  | categoria_id | INTEGER FOREIGN KEY → categories.id | Categoría del producto |
  | evento_id | INTEGER FOREIGN KEY → events.id | Evento al que pertenece |
  | activo | BOOLEAN DEFAULT 1 | Indica si el producto está activo |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |

- [ ] **CAJA (box)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | evento_id | INTEGER FOREIGN KEY → events.id UNIQUE | Evento asociado |
  | estado | TEXT DEFAULT 'cerrada' | `cerrada` o `abierta` |
  | saldo_inicial | INTEGER | Importe en centavos |
  | saldo_actual | INTEGER | Importe en centavos |
  | fecha_apertura | DATETIME | Fecha de apertura |
  | fecha_cierre | DATETIME | Fecha de cierre |
  | usuario_apertura_id | INTEGER FOREIGN KEY → users.id | Usuario que abrió la caja |
  | usuario_cierre_id | INTEGER FOREIGN KEY → users.id | Usuario que cerró la caja |

- [ ] **TICKETS (tickets)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | numero_ticket | TEXT UNIQUE NOT NULL | Número del ticket |
  | evento_id | INTEGER FOREIGN KEY → events.id | Evento asociado |
  | usuario_id | INTEGER FOREIGN KEY → users.id | Usuario asociado |
  | estado | TEXT DEFAULT 'abierto' | `abierto`, `pagado` o `anulado` |
  | total | INTEGER NOT NULL | Total en centavos |
  | metodo_pago | TEXT | Por ejemplo, `efectivo` o `tarjeta` |
  | fecha_creacion | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha de creación |
  | fecha_pago | DATETIME | Fecha de pago |

- [ ] **DETALLES_TICKET (ticket_details)**

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
  | caja_id | INTEGER FOREIGN KEY → box.id | Caja asociada |
  | tipo | TEXT NOT NULL | `ingreso`, `egreso` o `ajuste` |
  | descripcion | TEXT | Descripción |
  | monto | INTEGER NOT NULL | Importe en centavos, positivo o negativo |
  | usuario_id | INTEGER FOREIGN KEY → users.id | Usuario asociado |
  | referencia | TEXT | Identificador del ticket u otra referencia |
  | fecha | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha del movimiento |

- [ ] **HISTORIAL_INVENTARIO (inventory_history)**

  | Campo | Tipo y restricciones | Descripción |
  | --- | --- | --- |
  | id | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador |
  | producto_id | INTEGER FOREIGN KEY → products.id | Producto asociado |
  | cantidad_anterior | INTEGER | Cantidad anterior |
  | cantidad_nueva | INTEGER | Cantidad nueva |
  | tipo_movimiento | TEXT | `venta`, `ajuste` o `entrada` |
  | ticket_id | INTEGER FOREIGN KEY → tickets.id | Ticket asociado, opcional |
  | usuario_id | INTEGER FOREIGN KEY → users.id | Usuario asociado |
  | fecha | DATETIME DEFAULT CURRENT_TIMESTAMP | Fecha del movimiento |

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
| Evento → Tickets | Uno a muchos | Un evento puede tener varios tickets. |
| Ticket → Detalles de ticket | Uno a muchos | Un ticket puede tener varias líneas de detalle. |
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

Para un producto con evento y categoría asignados, la pareja debe existir en `event_categories`. La forma de garantizarlo se definirá al implementar el esquema.

Salvo donde se indica `NOT NULL`, las claves foráneas de la propuesta admiten `NULL`; por lo tanto, esas asociaciones son opcionales por ahora. La obligatoriedad de cada relación queda pendiente de definir.

La referencia de `box_movements` es un campo de texto, no una clave foránea a `tickets`. Para conservar el historial de aperturas y cierres de caja, queda pendiente evaluar una tabla de períodos de caja.

## Orden sugerido de implementación

Implementar cada etapa como una funcionalidad completa, con sus tablas, backend y pruebas; después se puede conectar la pantalla correspondiente.

- [ ] **1. Definir reglas y relaciones básicas:** acordar categorías compartidas, estados de tickets y control de stock, y actualizar esta propuesta.
- [ ] **2. Eventos:** crear la tabla, el modelo, repositorio, servicio y controller para crear y consultar eventos y cambiar su estado.
- [ ] **3. Categorías y productos:** implementar categorías compartidas, su asociación con eventos y los productos de cada evento, con precio en centavos y stock opcional.
- [ ] **4. Caja:** implementar apertura, cierre y reapertura; evaluar períodos de caja para conservar el historial.
- [ ] **5. Tickets y detalles:** crear ventas con productos, cantidades y precios; confirmar el pago y actualizar el stock y la caja en una misma operación.
- [ ] **6. Anulaciones e historial:** registrar anulaciones y los movimientos de inventario y caja que correspondan, sin borrar el historial de ventas.
- [ ] **7. Usuarios y permisos:** definir roles y asociar las operaciones a usuarios; resolver la seguridad antes de usar el sistema en un evento real.
- [ ] **8. Copias de seguridad e informes:** integrar los respaldos existentes en la interfaz y generar informes con datos reales; decidir si hace falta una tabla de historial de copias.
