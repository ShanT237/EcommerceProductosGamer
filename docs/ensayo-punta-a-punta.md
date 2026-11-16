# Ensayo de punta a punta

Guion para recorrer el sistema completo (backend + frontend) antes de entregar. Sirve como lista de verificación: marca la última columna cuando el resultado esperado se cumpla. Si un paso falla, se corrige antes de entregar.

**Requisitos:** Java 21, Node 20+ y una terminal para cada proceso. La base es H2 en memoria, así que cada arranque del backend parte de los datos de `data.sql` (sin favoritos, notificaciones, combos, preventas, regalos ni metas: los crean los pasos 14 a 31).

> Todos los pasos (1 a 31) fueron ejecutados y verificados (✔).

**Usuarios de prueba**

| Rol | Correo | Contraseña |
| :--- | :--- | :--- |
| `VENDEDOR` | `demo@gamerzone.com` | `GamerZone2026` |
| `COMPRADOR` | `jose.demo@correo.com` | `JoseGamer2026` |
| `COMPRADOR` | `ana.demo@correo.com` | `AnaGamer2026` |

**Datos de demo que usa el guion:** el vendedor GamerZone tiene «Teclado mecánico RGB» (stock 8) y «Mouse gamer inalámbrico» (stock 19); el cupón global `VERANO26` (15 %) vence el 31/12/2026.

**Consejo:** para ver el botón en estado "Publicando…" o "Procesando…" abre las herramientas del navegador (F12), pestaña **Network**, y cambia la velocidad a *Slow 3G*. En red local la petición termina en milisegundos.

## A. Compra, pedido y seguridad

| Paso | Quién | Acción | Resultado esperado | ✔ |
| :---: | :--- | :--- | :--- | :---: |
| 1 | Sistema | Arrancar el backend (`cd backend` y `.\gradlew.bat bootRun`) y el frontend (`cd frontend` y `npm start`). | La consola del backend muestra `Started BackendApplication` y `http://localhost:4200` carga la portada. | ✔ |
| 2 | Vendedor | Ir a **Ingresar** y entrar con `demo@gamerzone.com`. | Entra y queda en `/vendedor` (Mi tienda). | ✔ |
| 3 | Vendedor | Pestaña **Publicar**, dejar el formulario vacío y pulsar **Publicar producto**. | Aparecen "El nombre es obligatorio", "La descripción es obligatoria" y "El precio debe ser mayor a cero". La pestaña Network no muestra ninguna petición. | ✔ |
| 4 | Vendedor | Publicar "Teclado de ensayo", descripción cualquiera, precio 150000, stock 2. | El botón dice "Publicando…" y está bloqueado durante la petición. La respuesta es `201`, sale el toast "¡Producto publicado!" y se abre la pestaña **Mis productos** con el teclado. | ✔ |
| 4a | Vendedor | **Opcional, solo con Cloudinary configurado (`CLOUDINARY_*`):** en **Publicar**, elegir una imagen PNG/JPEG de hasta 5 MB y publicar otro producto de prueba. | `POST /api/productos` (`201`) seguido de `POST /api/productos/{id}/imagen`; la tarjeta del catálogo muestra la imagen. Sin credenciales o si la subida falla, el producto se publica con la ilustración de respaldo y se informa el error. | ✔ |
| 4b | Vendedor | Publicar "Mouse de ensayo", precio 80000, stock 1 (se usa en el paso 10). | Mismo resultado que el paso 4. | ✔ |
| 4c | Vendedor | Publicar "Auriculares de ensayo", precio 200000, stock 5 (se usan en los pasos 25 a 31). | Mismo resultado que el paso 4. | ✔ |
| 5 | Comprador | Salir, entrar con `jose.demo@correo.com` y luego escribir `/vendedor` en la barra de direcciones. | Entra al catálogo (`/catalogo`) y `/vendedor` redirige a `/`. | ✔ |
| 6 | Comprador | En el catálogo, abrir "Teclado de ensayo" y pulsar **Añadir al carrito** con cantidad 2. Añadir además 1 unidad de "Teclado mecánico RGB". | La barra de navegación muestra el contador del carrito en 3. | ✔ |
| 7 | Comprador | Abrir `/carrito` y pulsar `+` en el teclado de ensayo. | El `+` queda deshabilitado en 2 (el stock del teclado). Las cantidades no superan el stock. | ✔ |
| 8 | Comprador | Pulsar **Pagar (simulado)**. | El botón dice "Procesando…" y está bloqueado. La pestaña Network muestra **un solo** `POST /api/pedidos` (`201`, con los dos ítems en `items`) y **un solo** `PUT /api/pedidos/{id}/confirmar` (`200`). Navega a `/mis-compras`, donde el pedido aparece como una operación `COMPLETADA` que, al expandirla, muestra sus dos productos. | ✔ |
| 9 | Comprador | Volver al catálogo y abrir "Teclado de ensayo" (stock 0). | Muestra "Producto agotado" y **Añadir al carrito** y **Comprar** están deshabilitados. | ✔ |
| 10 | Comprador | Error de stock real y todo-o-nada, con dos pestañas del navegador: en la primera añadir "Teclado mecánico RGB" y "Mouse de ensayo" (stock 1) al carrito; en la segunda comprar "Mouse de ensayo" con **Comprar** y **Pagar**; volver a la primera y pulsar **Pagar (simulado)**. | Toast de error con "No hay suficiente stock disponible" (`422`). **Ningún** ítem se compró: el carrito conserva los dos productos, el stock del "Teclado mecánico RGB" no cambió en el catálogo y el botón vuelve a habilitarse. | ✔ |
| 11 | Comprador | En `/mis-compras`, **★ Reseñar** y **Valorar vendedor** sobre una compra `COMPLETADA`. | Ambas responden `201` y salen los toasts de éxito. | ✔ |
| 12 | Comprador | Entrar como `ana.demo@correo.com` y pedir **Reembolso** de la compra de demo `dddddddd-dddd-dddd-dddd-ddddddddddd2`. | `422` por superar el plazo de 48 horas; el toast muestra el mensaje del backend y la compra sigue `COMPLETADA`. | ✔ |
| 13 | Anónimo | En las herramientas del navegador, **Application → Local Storage**, borrar `gamerzone.token` y recargar `/carrito`. | Redirige a `/login`. | ✔ |

## B. Favoritos y notificaciones

| Paso | Quién | Acción | Resultado esperado | ✔ |
| :---: | :--- | :--- | :--- | :---: |
| 14 | Comprador (Jose) | Entrar como `jose.demo@correo.com`. En el catálogo, marcar el corazón de "Mouse gamer inalámbrico", abrir `/favoritos` y luego recargar la página (F5) en el catálogo. | El producto aparece en Favoritos y el contador de la barra sube a 1. Tras F5 el corazón **sigue marcado** (la pestaña Network muestra `GET /api/wishlists`). | ✔ |
| 15 | Vendedor | Entrar como `demo@gamerzone.com`, **Mis productos** → oferta inmediata del 10 % en "Mouse gamer inalámbrico". | `200`; el producto muestra el precio rebajado. | ✔ |
| 16 | Comprador (Jose) | Entrar de nuevo y mirar la campana de la barra. Abrir `/notificaciones` y pulsar la notificación. | La campana muestra 1 sin leer. Al pulsar se envía `PUT /api/notificaciones/{id}/leida` (`200`) y el contador baja a 0. | ✔ |

## C. Cupones

| Paso | Quién | Acción | Resultado esperado | ✔ |
| :---: | :--- | :--- | :--- | :---: |
| 17 | Vendedor | **Cupones** → crear `ENSAYO10`, 10 %, vence mañana, 5 usos. Repetir con el mismo código y luego con vencimiento de ayer. | El primero responde `201` y aparece con 0/5 usos. El duplicado responde `409` y el vencimiento pasado `422`; el toast muestra el mensaje del backend. | ✔ |
| 18 | Comprador (Ana) | Entrar como `ana.demo@correo.com`, añadir al carrito 1 "Teclado mecánico RGB" y 1 "Mouse gamer inalámbrico", escribir `ENSAYO10` y pulsar **Aplicar**; luego **Pagar (simulado)**. | **Aplicar** muestra el descuento estimado (`GET /api/cupones/ENSAYO10`, `200`). Se ve un solo `POST /api/pedidos` con `codigoCupon` y el pedido queda `COMPLETADA` con el total rebajado. | ✔ |
| 19 | Vendedor | **Cupones**. | `ENSAYO10` muestra **1/5 usos**, aunque el pedido tuvo dos ítems (un uso por pedido). | ✔ |
| 20 | Comprador (Ana) | Añadir otro producto al carrito, escribir `ENSAYO10` y pulsar **Aplicar**. | `422` "Ya usaste este cupón…": un cupón se usa una sola vez por comprador. El carrito y el stock quedan intactos. | ✔ |
| 21 | Comprador (Ana) | Escribir un código que no existe (`NOEXISTE`) y pulsar **Aplicar**. | `404` "El cupón no existe". | ✔ |

## D. Combos

| Paso | Quién | Acción | Resultado esperado | ✔ |
| :---: | :--- | :--- | :--- | :---: |
| 22 | Vendedor | **Combos** → crear "Combo ensayo" con un solo producto y pulsar crear; luego con "Teclado mecánico RGB" y "Mouse gamer inalámbrico" y descuento 20 %. | Con un solo producto no se envía ninguna petición. Con dos responde `201` y el combo aparece en la lista del vendedor. | ✔ |
| 23 | Comprador (Jose) | `/combos` → **Comprar combo** con cantidad 1; luego en `/mis-compras` **Confirmar pago** de esa operación. | `POST /api/combos/{id}/comprar` (`201`) y `PUT /api/pedidos/{id}/confirmar` (`200`). La operación aparece con el nombre "Combo ensayo" y sus dos productos, y queda `COMPLETADA`. Con esto Jose suma **5 compras completadas** (2 del paso 8, 1 del paso 10 y 2 del combo). | ✔ |

## E. Preventas

| Paso | Quién | Acción | Resultado esperado | ✔ |
| :---: | :--- | :--- | :--- | :---: |
| 24 | Vendedor | **Preventas** → elegir una fecha pasada (o ninguna) y enviar; luego programar "Auriculares de ensayo" para mañana; repetir sobre el mismo producto. | Sin fecha válida no se envía petición. La primera preventa responde `201`; la segunda sobre el mismo producto, `409`. | ✔ |
| 25 | Comprador (Jose) | En el catálogo, abrir "Auriculares de ensayo". | Muestra la insignia "Preventa · lanzamiento dd/mm" y **Comprar** y **Añadir al carrito** están deshabilitados con un aviso. Con Swagger, `POST /api/pedidos` de ese producto responde `422` y no descuenta stock. | ✔ |

## F. Regalos y metas

| Paso | Quién | Acción | Resultado esperado | ✔ |
| :---: | :--- | :--- | :--- | :---: |
| 26 | Vendedor | **Regalos** → crear un regalo con el producto "Auriculares de ensayo". | `201`; el regalo queda `DISPONIBLE`. Crear un regalo con un producto de otro vendedor (por ejemplo, «Figura coleccionable limitada» de Carlos Pérez, vía Swagger) responde `422`. | ✔ |
| 27 | Comprador (Jose) | Abrir `/regalos`. | El progreso muestra 5 compras completadas y 1 regalo por elegir. | ✔ |
| 28 | Comprador (Jose) | Pulsar **Elegir** en el regalo de los auriculares. | `PUT /api/regalos/{id}/asignar` (`200`); el regalo pasa a `ELEGIDO` y **Elegir** queda deshabilitado (ya no hay regalos por elegir). | ✔ |
| 29 | Vendedor | **Regalos** → lista de regalos por enviar → **Enviar**. | `PUT /api/regalos/{id}/enviar` (`200`) y el regalo sale de la lista. Con el token de otro vendedor (Carlos Pérez) el mismo `PUT` responde `422`. | ✔ |
| 30 | Comprador (Jose) | En `/regalos`, **Confirmar recepción** del regalo `ENVIADO`. | `PUT /api/regalos/{id}/entrega` (`200`); queda `ENTREGADO`. | ✔ |
| 31 | Vendedor / Comprador | **Metas** → crear una meta de 3 compras con "Auriculares de ensayo"; ver `/regalos` como Jose; luego desactivar la meta. | La meta responde `201` y Jose la ve como información en `/regalos`; al desactivarla (`200`) deja de aparecer. La regla de los regalos no cambia (sigue siendo 5 compras). | ✔ |

## Verificaciones automáticas complementarias

| Qué | Comando | Resultado esperado |
| :--- | :--- | :--- |
| Pruebas del backend | `cd backend` y `.\gradlew.bat test` | Todo en verde. |
| Pruebas del frontend | `cd frontend` y `npx ng test --watch=false` | Todo en verde. |
| Compilación del frontend | `cd frontend` y `npx ng build` | Build exitoso. |
| Colección de Postman | Importar `docs/postman/flujo-completo.json` y ejecutar las carpetas A, B, C y D en orden | Todos los tests en verde. |