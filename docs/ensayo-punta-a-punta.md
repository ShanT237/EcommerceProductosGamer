# Ensayo de punta a punta

Guion para recorrer el sistema completo (backend + frontend) antes de entregar. Sirve como lista de verificación: marca la última columna cuando el resultado esperado se cumpla. Si un paso falla, se corrige antes de entregar.

**Requisitos:** Java 21, Node 20+ y una terminal para cada proceso. La base es H2 en memoria, así que cada arranque del backend parte de los datos de `data.sql`.

**Usuarios de prueba**

| Rol | Correo | Contraseña |
| :--- | :--- | :--- |
| `VENDEDOR` | `demo@gamerzone.com` | `GamerZone2026` |
| `COMPRADOR` | `jose.demo@correo.com` | `JoseGamer2026` |
| `COMPRADOR` | `ana.demo@correo.com` | `AnaGamer2026` |

**Consejo:** para ver el botón en estado "Publicando…" o "Procesando…" abre las herramientas del navegador (F12), pestaña **Network**, y cambia la velocidad a *Slow 3G*. En red local la petición termina en milisegundos.

| Paso | Quién | Acción | Resultado esperado | ✔ |
| :---: | :--- | :--- | :--- | :---: |
| 1 | Sistema | Arrancar el backend (`cd backend` y `.\gradlew.bat bootRun`) y el frontend (`cd frontend` y `npm start`). | La consola del backend muestra `Started BackendApplication` y `http://localhost:4200` carga la portada. | ✔ |
| 2 | Vendedor | Ir a **Ingresar** y entrar con `demo@gamerzone.com`. | Entra y queda en `/vendedor` (Mi tienda). | ✔ |
| 3 | Vendedor | Pestaña **Publicar**, dejar el formulario vacío y pulsar **Publicar producto**. | Aparecen "El nombre es obligatorio", "La descripción es obligatoria" y "El precio debe ser mayor a cero". La pestaña Network no muestra ninguna petición. | ✔ |
| 4 | Vendedor | Publicar "Teclado de ensayo", descripción cualquiera, precio 150000, stock 2. | El botón dice "Publicando…" y está bloqueado durante la petición. La respuesta es `201`, sale el toast "¡Producto publicado!" y se abre la pestaña **Mis productos** con el teclado. | ✔ |
| 4b | Vendedor | Publicar "Mouse de ensayo", precio 80000, stock 1 (se usa en el paso 10). | Mismo resultado que el paso 4. | ✔ |
| 5 | Comprador | Salir, entrar con `jose.demo@correo.com` y luego escribir `/vendedor` en la barra de direcciones. | Entra al catálogo (`/catalogo`) y `/vendedor` redirige a `/`. | ✔ |
| 6 | Comprador | En el catálogo, abrir "Teclado de ensayo" y pulsar **Añadir al carrito** con cantidad 2. Añadir además 1 unidad de otro producto con stock. | La barra de navegación muestra el contador del carrito en 3. | ✔ |
| 7 | Comprador | Abrir `/carrito` y pulsar `+` en el teclado. | El `+` queda deshabilitado en 2 (el stock del teclado). Las cantidades no superan el stock. | ✔ |
| 8 | Comprador | Pulsar **Pagar (simulado)**. | El botón dice "Procesando…" y está bloqueado. Se ve **un solo** `POST /api/pedidos` (`201`) con todos los ítems y un `PUT /api/pedidos/{id}/confirmar` (`200`), sin importar cuántos ítems tenga el carrito. Navega a `/mis-compras` y las compras aparecen `COMPLETADA`. | ✔ |
| 9 | Comprador | Volver al catálogo y abrir "Teclado de ensayo" (stock 0). | Muestra "Producto agotado" y **Añadir al carrito** y **Comprar** están deshabilitados. | ✔ |
| 10 | Comprador | Error de stock real, con dos pestañas del navegador: en la primera añadir "Mouse de ensayo" (stock 1) al carrito; en la segunda comprarlo con **Comprar** y **Pagar**; volver a la primera y pulsar **Pagar (simulado)**. | Toast de error con "No hay suficiente stock disponible" (`422`) y el nombre del producto. El pedido es todo o nada: no se descontó el stock de ningún otro ítem, el carrito conserva todos los ítems y el botón vuelve a habilitarse. | ✔ |
| 11 | Comprador | En `/mis-compras`, **★ Reseñar** y **Valorar vendedor** sobre una compra `COMPLETADA`. | Ambas responden `201` y salen los toasts de éxito. | ✔ |
| 12 | Comprador | Entrar como `ana.demo@correo.com` y pedir **Reembolso** de la compra de demo `dddddddd-dddd-dddd-dddd-ddddddddddd2`. | `422` por superar el plazo de 48 horas; el toast muestra el mensaje del backend y la compra sigue `COMPLETADA`. | ✔ |
| 13 | Comprador | En el catálogo, marcar un producto como favorito (corazón) y recargar la página (F5). Abrir `/favoritos`. | El corazón sigue marcado tras recargar y `/favoritos` lista el producto. Quitarlo desde esa pantalla lo saca de la lista. | |
| 14 | Vendedor | En **Mi tienda → Cupones**, crear `GAMER10` (10 %, vence en una fecha futura, 3 usos). | `201`, el cupón aparece en "Mis cupones" como vigente con "0 de 3 usos". | |
| 15 | Comprador | Añadir 2 productos del vendedor al carrito, escribir `GAMER10`, pulsar **Aplicar** y luego **Pagar (simulado)**. | **Aplicar** muestra el descuento estimado. Se ve un solo `POST /api/pedidos` con `codigoCupon`. En **Cupones** del vendedor el cupón pasa a "1 de 3 usos" (un uso por pedido, no por ítem). Repetir el pago con `GAMER10` responde `422` (cada comprador lo usa una vez). | |
| 16 | Vendedor / Comprador | El comprador marca un producto como favorito. El vendedor le aplica una oferta inmediata. El comprador abre la campana. | La campana muestra el contador de no leídas. Al abrir la notificación se hace `PUT /api/notificaciones/{id}/leida` y el contador baja. | |
| 17 | Vendedor / Comprador | El vendedor crea un combo con 2 productos propios (**Mi tienda → Combos**). El comprador abre `/combos` y lo compra con cantidad 1. | El combo muestra precio con y sin descuento. La compra queda en **Mis compras** como una sola operación. | |
| 18 | Vendedor | En **Mi tienda → Preventas**, programar la preventa de un producto con fecha de mañana. Como comprador, abrir el catálogo. | La tarjeta muestra "Preventa · lanzamiento dd/mm" y **Comprar** y **Añadir al carrito** están deshabilitados. Una fecha de hoy o pasada no envía la petición. | |
| 19 | Vendedor / Comprador | El vendedor crea un regalo con un producto propio (**Mi tienda → Regalos**). El comprador abre `/regalos`: con menos de 5 compras completadas **Elegir** está deshabilitado; con 5 o más lo elige. El vendedor lo ve en "Por enviar" y lo marca como enviado. El comprador confirma la recepción. | El regalo pasa por ELEGIDO → ENVIADO → ENTREGADO. Otro vendedor no puede enviarlo (`422`). | |
| 20 | Vendedor / Comprador | El vendedor crea una meta de 5 compras con un producto regalo (**Mi tienda → Metas**) y cambia su umbral a 8. El comprador abre `/regalos`. | La meta aparece en "Metas de recompensa" con 8 compras. Un umbral de 0 no envía la petición. | |
| 21 | Anónimo | En las herramientas del navegador, **Application → Local Storage**, borrar `gamerzone.token` y recargar `/carrito`. | Redirige a `/login`. | ✔ |
| 22 | Anónimo | Ir a **Crear cuenta** (`/registro`). Registrar un comprador con "Quiero comprar" y un vendedor con "Quiero vender" (tipo y documento). Probar una contraseña de menos de 8 caracteres y otra de 8 o más sin mayúscula ni número. | Ambas cuentas responden `201`. La corta muestra el aviso y no envía la petición; la que no tiene mayúscula y número responde `400` o `422` con el mensaje del backend. Con la cuenta nueva se puede iniciar sesión. | |
| 23 | Comprador | Abrir **Perfil** (`/perfil`), cambiar el nombre y guardar. Luego cambiar la contraseña con una actual incorrecta y después con la correcta. | El nombre queda actualizado (`200`). Con la contraseña actual incorrecta responde `422` con el mensaje del backend; con la correcta responde `204` y se puede volver a ingresar con la nueva. | |
| 24 | Vendedor | En **Mi tienda → Mis productos**, pausar un producto y comprobar que sigue en la lista (marcado como pausado) pero no sale en el catálogo del comprador. Volver a activarlo. | Pausado aparece solo en "Mis productos" (`GET /api/productos/mios`); activado vuelve al catálogo. | |
| 25 | Vendedor | En **Mi tienda → Promociones**, aplicar un descuento inmediato a un producto y luego quitarlo desde "Mis productos". Intentar cambiar el precio del producto mientras tiene el descuento activo. | El descuento se aplica (`200`) y al quitarlo el producto vuelve a su precio anterior. Con el descuento activo el cambio de precio responde `422` ("quítalo antes de cambiar el precio"). | |
| 26 | Vendedor | En **Promociones**, programar una promoción de temporada de 10 % para un producto. Intentar crear otra que se solape con las mismas fechas. Cambiar el porcentaje a 15 % y después quitarla. | La primera responde `201`; la solapada responde `409` ("ya tiene una promoción en ese rango de fechas"). El cambio responde `200` y al quitarla (`204`) el precio base no cambia. | |
| 27 | Vendedor | En **Mi tienda → Combos**, editar el nombre y el descuento de un combo propio y luego eliminarlo. Repetir con un combo que tenga un regalo disponible. | El cambio responde `200` y la eliminación `204`. Con el regalo sin cerrar, eliminar responde `422` y el combo se conserva. | |
| 28 | Vendedor | Abrir **Mi tienda → Resumen**. | Se ven las ventas, las métricas de reseñas, los más vendidos y las alertas de **stock bajo** del vendedor. | |

## Verificaciones automáticas complementarias

| Qué | Comando | Resultado esperado |
| :--- | :--- | :--- |
| Pruebas del backend | `cd backend` y `.\gradlew.bat test` | Todo en verde. |
| Pruebas del frontend | `cd frontend` y `npx ng test --watch=false` | Todo en verde. |
| Compilación del frontend | `cd frontend` y `npx ng build` | Build exitoso. |
| Colección de Postman | Importar `docs/postman/flujo-completo.json` y ejecutar las carpetas A, B, C, D y E en orden | Todos los tests en verde. |
