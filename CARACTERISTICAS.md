# Caracteristicas del proyecto

## Objetivo

Desarrollar un sistema de caja para eventos de una iglesia, como peñas y bingos, que permita registrar la venta de comida y llevar un control basico del inventario y de la caja.

## Entorno de uso

- La aplicacion se desarrollara en la computadora principal y se ejecutara en una notebook Acer Aspire 3.
- La notebook tiene un procesador Intel Core i5-1135G7 y 8 GB de RAM.
- La interfaz se abrira desde un navegador en la misma notebook.
- El funcionamiento sera local, sin necesidad de conexion a internet durante el evento.
- El backend se ejecutara en la notebook y utilizara una base de datos local.
- Se contempla distribuir la aplicacion como un archivo JAR, con un entorno Java compatible y un medio sencillo para iniciarla.
- La base de datos se guardara separada del JAR para facilitar las actualizaciones y copias de seguridad.

## Tecnologias

- **Backend:** Java y Spring Boot.
- **Frontend:** HTML, CSS, JavaScript y una plantilla de Bootstrap modificada.
- **Base de datos:** SQLite, almacenada localmente.
- **Compilacion y dependencias:** Maven.
- **Recursos frontend:** Bootstrap y los demas recursos necesarios deben estar disponibles localmente para funcionar sin internet.

## Configuracion de Spring Initializr

Segun la captura de configuracion:

| Opcion | Valor visible |
| --- | --- |
| Proyecto | Maven |
| Lenguaje | Java |
| Spring Boot | 4.2.0 |
| Empaquetado | JAR |
| Configuracion | Properties |
| Java | 17 |
| Group | `com.example` |
| Artifact | `demo` |
| Package name | `com.example.demo` |

Se propuso usar Java 21 y los nombres `com.iglesia`, `caja` y `com.iglesia.caja` para Group, Artifact y Package name, respectivamente. **Queda pendiente confirmar si se adoptaron estos valores o se eligieron otros.** La version de Java elegida debera estar disponible en la computadora de desarrollo y ser compatible con el entorno de ejecucion de la notebook.

## Dependencias agregadas

| Dependencia | Proposito |
| --- | --- |
| Spring Web | Gestionar solicitudes HTTP y la comunicacion con el navegador. |
| JDBC API | Acceder a la base de datos mediante JDBC y `JdbcTemplate`. |
| Validation | Validar los datos recibidos por la aplicacion. |
| SQLite Driver | Permitir la conexion JDBC a SQLite. |
| Spring Boot DevTools | Facilitar el desarrollo mediante reinicios y otras herramientas. |
| Lombok | Reducir codigo repetitivo en las clases Java. |
| Spring Security | Gestionar autenticacion y permisos cuando se definan. |
| Thymeleaf | Generar paginas HTML desde el servidor, si se decide utilizarlo. |

SQLite Driver aparece en la seleccion, por lo que no seria necesario agregarlo manualmente si ya quedo incluido en el proyecto generado.

Se utilizara JDBC con `JdbcTemplate` como mecanismo principal de acceso a datos. JPA y su dialecto para SQLite se retiraron para mantener una configuracion mas sencilla. Queda pendiente decidir si se usara Thymeleaf o paginas HTML y JavaScript que consuman el backend. Spring Security requiere configurar el acceso; al agregarlo puede proteger las rutas por defecto.

## Componentes de la plantilla

El menu lateral y la barra superior del index se definen en `plantilla/js/menu-lateral.js` y `plantilla/js/barra-superior.js`. Para reutilizarlos en nuevas paginas ubicadas junto al index, incluir `js/menu-lateral.js` directamente dentro de `#wrapper` y, dentro de `#content`, `js/eventos-sesion.js` seguido por `js/barra-superior.js`, antes de cargar `js/sb-admin-2.min.js`. Los componentes requieren JavaScript y recursos locales de la plantilla. El menu marca la pagina activa segun su nombre de archivo.

La barra muestra "Usuario" como etiqueta generica; la configuracion de usuario aun no esta disponible. Cerrar sesion lleva a la confirmacion de Spring Security. Las paginas de la plantilla anteriores conservan sus propios menus hasta que se adapten.

La pagina de inicio muestra el evento seleccionado, el estado de su caja y accesos rapidos. El informe ilustrativo anterior se encuentra en `plantilla/resumen-ventas.html`: sus importes y graficos son ficticios, no corresponden al evento seleccionado ni a ventas reales; el grafico de evolucion usa datos independientes. La generacion de informes aun no esta disponible.

La pagina `plantilla/ingresar-venta.html` es una demostracion interactiva con productos y tickets ficticios. El stock se comparte entre los ejemplos, excepto los tickets cancelados. Los cambios solo permanecen en la pagina abierta y se pierden al recargarla; no se guardan en la base de datos ni registran ventas o pagos.

La pagina de eventos y la barra superior usan datos de demostracion en `sessionStorage`: se comparten entre paginas de la misma pestaña y duran su sesion, pero no se guardan en la base de datos. El evento seleccionado puede estar abierto, cerrado o cancelado; su estado es independiente del estado de caja. La apertura y el cierre de caja se gestionan desde la pagina Caja, no desde Eventos. Esta seleccion todavia no afecta a las ventas de ejemplo.

La pagina de productos tambien es una demostracion en la sesion de la pestaña: cada producto pertenece al evento seleccionado y usa una de sus categorias. El precio es obligatorio, admite hasta dos decimales y se conserva en centavos enteros; los productos de sesiones anteriores sin precio se muestran como pendientes hasta editarlos. Un stock vacio significa sin control, mientras que cero indica que no hay unidades. No se puede quitar una categoria del evento mientras tenga productos asociados. Estos productos no estan conectados con la pantalla de ventas de ejemplo.

La pagina de caja registra aportes iniciales, apertura, cierre y reapertura de ejemplo para cada evento en la sesion de la pestaña. La reapertura parte del efectivo contado en el cierre anterior. Los importes de ventas son ficticios, se agregan solo al primer periodo y no provienen de tickets; el historial no es una auditoria real. Si una caja quedo abierta en una version anterior sin apertura registrada, debe registrarse su apertura inicial antes de cerrarla.

La pagina `plantilla/copias-seguridad.html` muestra una propuesta de interfaz para crear y restaurar copias. Sus controles estan desactivados: no crea, verifica ni restaura archivos. Los datos de demostracion de la pestaña no se guardan en la base de datos y no forman parte de sus respaldos.

## Configuracion local de SQLite

- La base de datos se configura por defecto como `caja.db` en el directorio desde el que se inicia la aplicacion, fuera del JAR. Para evitar crear otra base por accidente, iniciar siempre la aplicacion desde el mismo directorio.
- La ubicacion puede cambiarse mediante la variable de entorno `SPRING_DATASOURCE_URL`, por ejemplo `jdbc:sqlite:C:/caja/datos/caja.db`. El directorio indicado debe existir.
- Las tablas se crearan mediante SQL; la aplicacion no utiliza Hibernate para crear o modificar el esquema.
- La prueba de contexto utiliza una base separada en `target/caja-test.db`.

## Seguridad durante el desarrollo

- Spring Security protege las rutas y muestra una pagina de inicio de sesion. Por defecto, el usuario es `user` y la contrasena se genera al arrancar y se muestra en la consola.
- Para usar credenciales propias durante una sesion de desarrollo, se pueden definir las variables de entorno `SPRING_SECURITY_USER_NAME` y `SPRING_SECURITY_USER_PASSWORD` antes de iniciar la aplicacion. No guardar contrasenas en el proyecto.
- Esta configuracion es solo inicial: quedan pendientes los usuarios, roles y permisos necesarios para los eventos.

## Respaldo y restauracion

El JAR incluye comandos para crear y verificar copias consistentes sin instalar `sqlite3`. Las rutas de ejemplo son relativas al directorio desde el que se ejecuta el comando; usar la ubicacion real si la base esta en otro lugar. El archivo de origen debe existir y el directorio de destino tambien.

Para crear un respaldo, incluso con la aplicacion abierta:

```powershell
java -jar .\target\caja-0.0.1-SNAPSHOT.jar --crear-respaldo .\caja.db .\respaldo.db
```

Para restaurarlo en un archivo nuevo, detener antes la aplicacion y cualquier otra conexion a la base:

```powershell
java -jar .\target\caja-0.0.1-SNAPSHOT.jar --restaurar-respaldo .\respaldo.db .\caja-restaurada.db
```

Ambos comandos comprueban la integridad de la copia y rechazan destinos existentes o con archivos auxiliares de SQLite. No modifican el archivo de origen. Usar un nombre nuevo para cada copia y guardar una copia protegida en otro dispositivo. Para revisar una restauracion, iniciar la aplicacion con `--spring.datasource.url=jdbc:sqlite:caja-restaurada.db`. El reemplazo definitivo de una base requiere detener todas las conexiones y conservar una copia previa; no borrar archivos `-wal`, `-shm` o `-journal` para forzarlo. Queda pendiente integrar estas operaciones en la interfaz y verificar el procedimiento en la notebook.

## Prueba en la notebook

Estos pasos suponen que la notebook usa Windows. Hay que hacerlos en ese equipo para confirmar que funciona sin internet:

1. Instalar previamente Java 17 compatible y comprobar `java -version` en una terminal. No se necesita Maven en la notebook.
2. Copiar el JAR generado en `target/caja-0.0.1-SNAPSHOT.jar` y `iniciar-caja.cmd` a una misma carpeta local con permisos de escritura. Para conservar datos existentes, trasladar la base mediante un respaldo verificado; no reemplazar datos sin una copia previa.
3. Desconectar internet y abrir `iniciar-caja.cmd`. Mantener la ventana abierta y entrar a `http://localhost:8080` desde el navegador.
4. Iniciar sesion con el usuario `user` y la contrasena generada en la ventana de la aplicacion. Confirmar que se ve la pagina, con sus estilos y la fecha.
5. Con una base de prueba que contenga datos, comprobar el respaldo y la restauracion usando los comandos anteriores y la ruta real del JAR en la notebook. La base puede no crearse hasta que la aplicacion realice una operacion de datos.
6. Detener la aplicacion con `Ctrl+C` en su ventana. Reiniciar y comprobar que los datos de prueba sigan disponibles cuando existan funciones para consultarlos.

El inicio de sesion actual es temporal y no constituye una configuracion definitiva de seguridad para un evento. Si se cambia el nombre o la version del JAR, hay que actualizar el nombre indicado en `iniciar-caja.cmd`.

## Funcionalidades previstas

- Registro de ventas de comida.
- Control basico de productos e inventario.
- Registro y seguimiento de caja.
- Cierre de caja y registro de anulaciones.
- Copias de seguridad de la base de datos.

Los detalles de estas funcionalidades todavia deben definirse. Los importes deben calcularse sin errores de redondeo.

## Validacion e importes

- Para los importes se utilizara `BigDecimal` en Java y unidades menores enteras (por ejemplo, centavos) en SQLite, no `float`, `double` ni columnas `REAL`.
- Como criterio inicial se consideran dos decimales; la conversion a unidades enteras debe rechazar valores que no puedan representarse exactamente. La moneda, los limites y las reglas de redondeo quedan pendientes de definir.
- La dependencia de validacion se comprueba con datos de ejemplo. Las validaciones reales se definiran para cada operacion al desarrollar las funcionalidades.

## Pendiente de definir

- Nombre definitivo del proyecto y configuracion final de Spring Initializr.
- Si se utilizara una sola caja o varias computadoras durante un evento.
- Productos, manejo de stock, medios de pago y datos necesarios para el cierre.
- Roles, usuarios y permisos.
- Automatizacion de copias de seguridad y restauracion dentro de la aplicacion.
- Verificar inicio, funcionamiento sin internet y recuperacion de datos en la notebook.