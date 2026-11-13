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
| 8 | Comprador | Pulsar **Pagar (simulado)**. | El botón dice "Procesando…" y está bloqueado. Se ven `POST /api/compras` (`201`) y `PUT /api/compras/{id}/confirmar` (`200`) por cada ítem. Navega a `/mis-compras` y las compras aparecen `COMPLETADA`. | ✔ |
| 9 | Comprador | Volver al catálogo y abrir "Teclado de ensayo" (stock 0). | Muestra "Producto agotado" y **Añadir al carrito** y **Comprar** están deshabilitados. | ✔ |
| 10 | Comprador | Error de stock real, con dos pestañas del navegador: en la primera añadir "Mouse de ensayo" (stock 1) al carrito; en la segunda comprarlo con **Comprar** y **Pagar**; volver a la primera y pulsar **Pagar (simulado)**. | Toast de error con "No hay suficiente stock disponible" (`422`) y el nombre del producto. El ítem sigue en el carrito y el botón vuelve a habilitarse. | ✔ |
| 11 | Comprador | En `/mis-compras`, **★ Reseñar** y **Valorar vendedor** sobre una compra `COMPLETADA`. | Ambas responden `201` y salen los toasts de éxito. | ✔ |
| 12 | Comprador | Entrar como `ana.demo@correo.com` y pedir **Reembolso** de la compra de demo `dddddddd-dddd-dddd-dddd-ddddddddddd2`. | `422` por superar el plazo de 48 horas; el toast muestra el mensaje del backend y la compra sigue `COMPLETADA`. | ✔ |
| 13 | Anónimo | En las herramientas del navegador, **Application → Local Storage**, borrar `gamerzone.token` y recargar `/carrito`. | Redirige a `/login`. | ✔ |

## Verificaciones automáticas complementarias

| Qué | Comando | Resultado esperado |
| :--- | :--- | :--- |
| Pruebas del backend | `cd backend` y `.\gradlew.bat test` | Todo en verde. |
| Pruebas del frontend | `cd frontend` y `npx ng test --watch=false` | Todo en verde. |
| Compilación del frontend | `cd frontend` y `npx ng build` | Build exitoso. |
| Colección de Postman | Importar `docs/postman/flujo-completo.json` y ejecutar las carpetas A, B y C en orden | Todos los tests en verde. |
