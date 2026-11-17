# 🎮 E-Commerce Marketplace Gamer

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?style=for-the-badge&logo=springboot)
![Gradle](https://img.shields.io/badge/Gradle-Build-blue?style=for-the-badge&logo=gradle)
![MariaDB](https://img.shields.io/badge/MariaDB-Database-003545?style=for-the-badge&logo=mariadb)
![Architecture](https://img.shields.io/badge/Architecture-DDD%20%2B%20Clean%20Arch-purple?style=for-the-badge)

Sistema e-commerce descentralizado enfocado en productos y accesorios del mundo **Gaming**, desarrollado para la asignatura de Programación Avanzada / Ingeniería de Software en la **Universidad del Quindío**. El proyecto está guiado bajo los principios de **Domain-Driven Design (DDD)** y **Clean Architecture**.
 
---

## 📋 Tabla de Contenidos
1. [Integrantes y nicho](#1-integrantes-y-nicho)
2. [Cómo ejecutar el proyecto y usuarios de prueba](#2-cómo-ejecutar-el-proyecto-y-usuarios-de-prueba)
  - [2.1 Frontend, carrito y pantallas](#21-frontend-carrito-y-pantallas)
    - [Mapa de pantallas por rol](#mapa-de-pantallas-por-rol)
    - [Carrito, pedido y cupones](#carrito-pedido-y-cupones)
    - [Panel del vendedor](#panel-del-vendedor)
3. [Endpoints principales](#3-endpoints-principales)
4. [Correcciones de la Entrega 1](#4-correcciones-de-la-entrega-1)
5. [¿Quién compra y por qué?](#5-quién-compra-y-por-qué)
6. [¿Qué hace único a sus vendedores?](#6-qué-hace-único-a-sus-vendedores)
7. [Lenguaje Ubicuo (DDD)](#7-lenguaje-ubicuo-ddd)
8. [Reglas de Negocio e Invariantes del Dominio](#8-reglas-de-negocio-e-invariantes-del-dominio)
9. [Casos de Uso del Sistema (57 Casos de Uso)](#9-casos-de-uso-del-sistema-57-casos-de-uso)
10. [Arquitectura del Software (Clean Architecture)](#10-arquitectura-del-software-clean-architecture)
11. [Seguridad, Base de Datos y Limitaciones](#11-seguridad-base-de-datos-y-limitaciones)
12. [Cómo ejecutar las pruebas](#12-cómo-ejecutar-las-pruebas)
13. [Pruebas avanzadas](#13-pruebas-avanzadas)
14. [Gestión del Proyecto (Trello)](#14-gestión-del-proyecto-trello)

---

## 1. Integrantes y nicho

**Nicho:** Marketplace Gamer: productos y accesorios gaming (periféricos, mobiliario de setup, juegos físicos, licencias digitales y combos), con múltiples vendedores independientes.

**Integrantes:**

| Integrante | GitHub |
| :--- | :--- |
| Juan David Tapiero | `Juanda2312` |
| Santiago Torres | `ShanT237` |
| José Bedoya | `thelegendsxv` |

---

## 2. Cómo ejecutar el proyecto y usuarios de prueba

**Requisitos:** Java 21. No hace falta instalar Gradle (se usa el wrapper).

```powershell
cd backend
.\gradlew.bat bootRun          # Linux/Mac: ./gradlew bootRun
```

Por defecto usa **H2 en memoria** y carga `data.sql` al arrancar. Cuando aparezca `Started BackendApplication`, abre:

- Swagger: http://localhost:8080/swagger-ui.html
- Consola H2: http://localhost:8080/h2-console (URL JDBC: `jdbc:h2:mem:marketplacedb`, usuario `SA`)

Si el puerto 8080 está ocupado: `.\gradlew.bat bootRun --args="--server.port=8081"`.
Para MariaDB, ver la sección [11](#11-seguridad-base-de-datos-y-limitaciones).

### Usuarios de prueba (perfil H2 por defecto)

| Rol | Nombre | Correo | Contraseña |
| :--- | :--- | :--- | :--- |
| `VENDEDOR` | GamerZone | demo@gamerzone.com | `GamerZone2026` |
| `VENDEDOR` | Carlos Pérez | carlos@correo.com | `CarlosGamer2026` |
| `COMPRADOR` | Jose | jose.demo@correo.com | `JoseGamer2026` |
| `COMPRADOR` | Ana | ana.demo@correo.com | `AnaGamer2026` |

Cupón de demo: `VERANO26` (15 %, global, 100 usos, vence el 31/12/2026). Cada comprador puede usarlo **una sola vez**; al reiniciar el backend (H2 en memoria) los usos se reinician. Pasada su fecha de vencimiento hay que actualizar `data.sql` o crear un cupón propio desde el panel del vendedor.

### Cómo autenticarse en Swagger

1. `POST /api/auth/login` con `{"correo": "...", "contrasena": "..."}`.
2. Copiar el valor de `token` (sin comillas).
3. Botón **Authorize** (arriba a la derecha) → pegar el token → confirmar.

Sin token la API responde `401`; con el token del rol equivocado, `403`.

### Flujo de punta a punta sugerido

1. Login como **vendedor** (`demo@gamerzone.com`) → `POST /api/productos` (publicar un producto).
2. Login como **comprador** (`jose.demo@correo.com`) → `POST /api/compras` (compra suelta de un producto) o, como hace el frontend, `POST /api/pedidos` con uno o varios ítems (`{"items":[{"productoId":"...","cantidad":1}],"codigoCupon":"VERANO26"}`).
3. `PUT /api/compras/{id}/confirmar` (compra suelta) o `PUT /api/pedidos/{id}/confirmar` (pedido): todo queda `COMPLETADA`. Las líneas de un pedido **no** se confirman, cancelan ni reembolsan por separado (`422`).
4. `POST /api/resenas` (reseñar) y `POST /api/valoraciones-vendedor` (valorar al vendedor).
5. **Error de negocio a propósito:** con `ana.demo@correo.com`, `PUT /api/compras/dddddddd-dddd-dddd-dddd-ddddddddddd2/reembolso`. Esa compra de demo supera las 48 horas del plazo, por lo que la regla del dominio rechaza el reembolso con `422`.
6. **Cupón de un solo uso por comprador:** repite el paso 2 con el mismo `codigoCupon` y el mismo comprador → `422` ("Ya usaste este cupón"), sin tocar el stock. Con `GET /api/cupones/VERANO26` (comprador) se ve la vista previa sin consumirlo.

El recorrido completo, con los módulos nuevos (favoritos, notificaciones, combos, preventas, regalos), está en [`docs/ensayo-punta-a-punta.md`](docs/ensayo-punta-a-punta.md).

---

## 2.1. Frontend, carrito y pantallas

El frontend es una aplicación **Angular 21** (componentes standalone, signals y formularios reactivos) que consume la API del backend.

**Requisitos:** Node 20 o superior y el backend corriendo en `http://localhost:8080` (sección 2).

```powershell
cd frontend
npm install
npm start                      # abre http://localhost:4200
```

La URL de la API está en `frontend/src/app/config.ts` (`API_URL`); hay que cambiarla ahí al desplegar. Para entrar usa los usuarios de prueba de la sección 2: la pantalla de login tiene botones que rellenan las cuentas de demo. El token de sesión es lo único que se guarda en el navegador (`localStorage`, clave `gamerzone.token`).

### Mapa de pantallas por rol

Las rutas se protegen con guards (`authGuard`, `rolGuard`, `guestGuard`): sin sesión se redirige a `/login`, y un rol que no corresponde vuelve a la portada.

| Ruta | Quién entra | Qué hace | Endpoints que usa |
| :--- | :--- | :--- | :--- |
| `/` | Todos | Portada | — |
| `/login` · `/registro` | Anónimo | Ingreso (con botones de demo) y registro de comprador | `POST /api/auth/login`, `POST /api/usuarios` |
| `/catalogo` | Cualquier sesión | Catálogo paginado con búsqueda y filtros (texto, categoría, gama, precio, solo preventa). Al abrir un producto: añadir al carrito, **Comprar** (pedido de un ítem, con cupón opcional), corazón de favorito, insignia de **preventa** (Comprar y Añadir al carrito quedan deshabilitados hasta el lanzamiento) y precio con descuento o promoción vigente | `GET /api/productos`, `GET /api/preventas`, `POST /api/pedidos`, wishlist |
| `/combos` | Cualquier sesión (comprar solo `COMPRADOR`) | Combos con sus productos, precio sin y con descuento y ahorro; selector de cantidad y cupón opcional por combo | `GET /api/combos`, `POST /api/combos/{id}/comprar`, `GET /api/cupones/{codigo}` |
| `/carrito` | `COMPRADOR` | Cantidades limitadas por el stock, cupón con vista previa del descuento y **Pagar (simulado)** | `POST /api/pedidos`, `PUT /api/pedidos/{id}/confirmar`, `GET /api/cupones/{codigo}` |
| `/mis-compras` | `COMPRADOR` | Historial agrupado por operación (pedido o combo, con filtro por estado): confirmar pago, cancelar, reembolso, reseñar, valorar al vendedor y redimir puntos | `GET /api/compras`, `PUT /api/pedidos/{id}/…`, `POST /api/resenas`, `POST /api/valoraciones-vendedor`, `POST /api/puntos/redimir`, `GET /api/puntos/historial` |
| `/favoritos` | `COMPRADOR` | Wishlist cargada desde el servidor: el corazón sigue marcado tras recargar la página (F5) | `GET/POST /api/wishlists`, `DELETE /api/wishlists/{productoId}` |
| `/notificaciones` | `COMPRADOR` | Avisos de descuento de la wishlist; la campana de la barra muestra el contador de no leídas y al abrir una se marca como leída | `GET /api/notificaciones`, `PUT /api/notificaciones/{id}/leida` |
| `/regalos` | `COMPRADOR` | Progreso hacia el siguiente regalo (uno por cada 5 compras completadas), regalos disponibles para elegir, mis regalos con confirmación de recepción y las metas activas como información | `GET /api/regalos/progreso`, `/disponibles`, `/mios`, `PUT /api/regalos/{id}/asignar`, `PUT /api/regalos/{id}/entrega`, `GET /api/metas-recompensa` |
| `/perfil` | `COMPRADOR` | Editar nombre y correo, y cambiar contraseña | `PUT /api/usuarios/me/perfil`, `PUT /api/usuarios/me/contrasena` |
| `/vendedor` | `VENDEDOR` | Panel «Mi tienda» (ver abajo) | varios |

### Carrito, pedido y cupones

Un comprador añade productos desde el modal del catálogo con **Añadir al carrito** (también puede usar **Comprar** para una compra directa de un solo ítem). En `/carrito` ajusta las cantidades, que nunca superan el stock (un producto exclusivo admite una sola unidad), quita ítems, aplica un cupón y pulsa **Pagar (simulado)**.

**El carrito se paga como un solo pedido (todo o nada).** No hay pasarela de pago: pagar significa `POST /api/pedidos` con todos los ítems (una compra `PENDIENTE` por línea, todas con el mismo `pedidoId`) y después `PUT /api/pedidos/{id}/confirmar`. El backend valida **todas** las líneas antes de descontar el stock, así que si una falla (stock, preventa, producto pausado, vendedor dado de baja…) no cambia nada y el carrito queda intacto con el mensaje del backend. Si falla la confirmación, el frontend cancela el pedido para devolver el stock. Mientras se procesa, el botón queda bloqueado ("Procesando…") y un segundo clic se ignora.

**Cupones.** El campo de cupón de carrito, catálogo y combos usa `GET /api/cupones/{codigo}` como **vista previa**: no consume nada y muestra un descuento estimado (si el cupón es global, sobre todo el subtotal; si es de un vendedor, solo sobre los ítems de ese vendedor). El cupón se consume al crear el pedido o comprar el combo: **un uso por operación** (no uno por ítem) y **una sola vez por comprador**. Un cupón vencido, agotado, inexistente (`404`), ya usado por el comprador, o que no aplica a ningún producto del pedido, se rechaza sin tocar el stock.

**Limitaciones:**

- El carrito se guarda en el localStorage del navegador, separado por usuario: sobrevive a recargas y al cierre de sesión, y se recupera al volver a entrar con la misma cuenta. No se comparte entre dispositivos ni navegadores.
- Al abrir /carrito se refrescan precio y stock desde el catálogo y las cantidades se ajustan al stock actual. El precio definitivo (promoción vigente y cupón incluidos) lo sigue fijando el backend al reservar.
- La vista previa del cupón es una estimación; el total definitivo es el que devuelve POST /api/pedidos.

### Panel del vendedor

La ruta `/vendedor` (**Mi tienda**) tiene una pestaña por módulo:

| Pestaña | Qué permite | Endpoints |
| :--- | :--- | :--- |
| Resumen | Ingresos y comisión, calificación promedio y reseñas, más vendidos y alertas de stock bajo | `GET /api/reportes/ventas`, `/metricas-resenas`, `/mas-vendidos`, `/stock-bajo` |
| Mis productos | Todos sus productos (activos y pausados): editar, pausar o activar, eliminar, oferta inmediata y quitarla | `GET /api/productos/mios`, `PUT/DELETE /api/productos/{id}…`, `POST/DELETE /api/descuentos` |
| Promociones | Promociones de temporada con vigencia: crear, editar y quitar | `GET/POST /api/promociones`, `PUT/DELETE /api/promociones/{id}` |
| Publicar | Formulario reactivo de publicación, con imagen opcional | `POST /api/productos`, `POST /api/productos/{id}/imagen` |
| Combos | Crear, editar, eliminar y cambiar los productos de sus combos (mínimo dos) | `GET /api/combos/mios`, `POST/PUT/DELETE /api/combos…` |
| Cupones | Crear cupones propios (solo aplican a sus productos) y ver sus usos | `GET/POST /api/cupones` |
| Regalos | Crear regalos de un producto o combo propio y enviar los que los compradores eligieron | `POST /api/regalos`, `GET /api/regalos/por-enviar`, `PUT /api/regalos/{id}/enviar` |
| Preventas | Programar una preventa con fecha de lanzamiento futura (mínimo mañana) | `POST /api/preventas`, `GET /api/preventas` |
| Metas | Crear, cambiar el umbral y desactivar metas de recompensa | `GET/POST /api/metas-recompensa`, `PUT /api/metas-recompensa/{id}/umbral` y `/desactivar` |

### Publicar un producto (formulario reactivo)

En **Mi tienda → Publicar**, el vendedor llena un formulario reactivo que valida antes de enviar: nombre y descripción obligatorios, precio mayor a cero y stock entero de cero o más. Puede añadir una imagen PNG/JPEG de hasta 5 MB; Spring Boot la sube a Cloudinary y guarda solo la URL en el producto. Si la carga falla, el producto queda publicado con la ilustración de respaldo y se informa el error. Si hay errores no se envía ninguna petición y cada campo muestra su aviso. Al enviar, el botón queda bloqueado ("Publicando…") y, si el backend rechaza la petición, se muestra su mensaje (por ejemplo `403` si el rol no corresponde o `422` si el vendedor está dado de baja). Una vista previa muestra cómo quedará la tarjeta en el catálogo.

### Mensajes de error

Todos los errores HTTP pasan por `mensajeDeError`: se muestra el `mensaje` que manda el backend y, si no hay cuerpo, un texto por defecto según el código (`400` datos no válidos, `401` sesión no válida, `403` sin permiso, `404` no encontrado, `409` ya existe, `422` regla de negocio, `500` error del servidor y `0` sin conexión). Nunca se muestra el JSON crudo.

### Pantallas

![Login](docs/pantallas/01-login.png)
*Login con botones para las cuentas de demo.*

![Catálogo](docs/pantallas/02-catalogo.png)
*Catálogo paginado con filtros y búsqueda.*

![Carrito](docs/pantallas/03-carrito.png)
*Carrito con cantidades limitadas por el stock y resumen del pago.*

![Carrito con error de stock](docs/pantallas/04-carrito-error-stock.png)
*Pago detenido por falta de stock (`422`): no se crea ni se descuenta nada, el carrito se conserva y el mensaje viene del backend.*

![Formulario de publicación](docs/pantallas/05-publicar-formulario.png)
*Formulario reactivo de publicación con imagen opcional y vista previa de la tarjeta.*

![Errores de validación](docs/pantallas/06-publicar-error-validacion.png)
*Avisos por campo con el formulario vacío; no se envía ninguna petición.*

![Mis compras](docs/pantallas/07-mis-compras.png)
*Historial de compras agrupado por operación (pedido o combo), con sus estados y acciones (reseña, valoración, reembolso).*

![Favoritos](docs/pantallas/08-favoritos.png)
*Wishlist cargada desde el servidor: el corazón del catálogo sigue marcado tras recargar.*

![Notificaciones](docs/pantallas/09-notificaciones.png)
*Notificaciones de descuento con la campana y el contador de no leídas.*

![Combos](docs/pantallas/10-combos.png)
*Combos con precio sin y con descuento, selector de cantidad y cupón opcional.*

![Carrito con cupón](docs/pantallas/11-carrito-cupon.png)
*Cupón aplicado en el carrito: descuento estimado en el resumen del pedido.*

![Cupones del vendedor](docs/pantallas/12-cupones-vendedor.png)
*Pestaña Cupones del panel del vendedor, con el contador de usos.*

![Preventas](docs/pantallas/13-preventas.png)
*Insignia «Preventa · lanzamiento dd/mm» en el catálogo y compra deshabilitada hasta la fecha.*

![Regalos](docs/pantallas/14-regalos.png)
*Progreso de compras, regalos disponibles, mis regalos y metas activas.*

![Metas de recompensa](docs/pantallas/15-metas-vendedor.png)
*Pestaña Metas del panel del vendedor.*

### Ensayo de punta a punta

El recorrido completo (publicar, comprar con carrito y cupón, errores de negocio y de seguridad, favoritos, notificaciones, combos, preventas y regalos) está en [`docs/ensayo-punta-a-punta.md`](docs/ensayo-punta-a-punta.md), con el resultado esperado de cada paso. La colección de Postman equivalente para la API está en [`docs/postman/flujo-completo.json`](docs/postman/flujo-completo.json).

---

## 3. Endpoints principales

| Método y ruta | Rol | Qué hace |
| :--- | :--- | :--- |
| `POST /api/auth/login` | Público | Inicia sesión y devuelve el JWT |
| `POST /api/usuarios` | Público | Registra un comprador |
| `POST /api/vendedores` | Público | Registra un vendedor |
| `PUT /api/usuarios/me/perfil` · `PUT /api/usuarios/me/contrasena` | `COMPRADOR` | Edita el perfil y cambia la contraseña |
| `PUT /api/vendedores/me/baja` | `VENDEDOR` | Baja lógica del vendedor |
| `GET /api/productos` | Autenticado | Catálogo paginado con búsqueda por texto y filtros (incluye el precio con promoción vigente) |
| `GET /api/productos/mios` | `VENDEDOR` | Todos los productos del vendedor, activos y pausados |
| `POST /api/productos` | `VENDEDOR` | Publica un producto |
| `PUT /api/productos/{id}/precio` | `VENDEDOR` | Modifica el precio |
| `PUT /api/productos/{id}/informacion` | `VENDEDOR` | Edita la información del producto |
| `PUT /api/productos/{id}/visibilidad` | `VENDEDOR` | Pausa o activa la publicación |
| `POST /api/productos/{id}/imagen` | `VENDEDOR` | Sube la imagen del producto (`multipart/form-data`, campo `archivo`; PNG/JPEG de hasta 5 MB) a Cloudinary y guarda la URL |
| `DELETE /api/productos/{id}` | `VENDEDOR` | Eliminación lógica de su producto |
| `POST /api/descuentos` · `DELETE /api/descuentos/{productoId}` | `VENDEDOR` | Aplica o quita el descuento inmediato (el alta notifica a las wishlists) |
| `GET /api/promociones` · `POST /api/promociones` | `VENDEDOR` | Lista y programa promociones de temporada con vigencia |
| `PUT /api/promociones/{id}` · `DELETE /api/promociones/{id}` | `VENDEDOR` | Modifica o quita una promoción |
| `GET /api/preventas` | Autenticado | Preventas con lanzamiento futuro, de la más próxima a la más lejana |
| `POST /api/preventas` | `VENDEDOR` | Programa una preventa de un producto propio |
| `POST /api/compras` | `COMPRADOR` | Realiza una compra suelta |
| `GET /api/compras` | `COMPRADOR` | Historial de compras |
| `PUT /api/compras/{id}/confirmar` | `COMPRADOR` | Confirma la compra (una línea de pedido o combo se rechaza con `422`) |
| `PUT /api/compras/{id}/cancelar` | `COMPRADOR` | Cancela la compra (una línea de pedido o combo se rechaza con `422`) |
| `PUT /api/compras/{id}/reembolso` | `COMPRADOR` | Solicita reembolso (48 h, sin descarga); una línea de pedido o combo se rechaza con `422` |
| `POST /api/pedidos` | `COMPRADOR` | Crea el pedido del carrito: una compra por línea, todo o nada, con cupón opcional (`codigoCupon`) |
| `PUT /api/pedidos/{id}/confirmar` | `COMPRADOR` | Confirma el pedido completo |
| `PUT /api/pedidos/{id}/cancelar` | `COMPRADOR` | Cancela el pedido completo y restituye todo el stock |
| `PUT /api/pedidos/{id}/reembolso` | `COMPRADOR` | Reembolsa el pedido o combo completo |
| `GET /api/cupones/{codigo}` | `COMPRADOR` | Vista previa de un cupón (no consume ningún uso) |
| `POST /api/cupones` · `GET /api/cupones` | `VENDEDOR` | Crea un cupón propio y lista los suyos |
| `GET /api/combos` | Autenticado | Combos con todos sus productos visibles, con precio con y sin descuento |
| `GET /api/combos/mios` | `VENDEDOR` | Todos los combos del vendedor |
| `POST /api/combos` · `PUT /api/combos/{id}` · `DELETE /api/combos/{id}` | `VENDEDOR` | Crea, modifica o elimina un combo propio |
| `PUT /api/combos/{id}/productos/{productoId}` · `DELETE …` | `VENDEDOR` | Agrega o quita un producto del combo (mínimo 2) |
| `POST /api/combos/{id}/comprar` | `COMPRADOR` | Compra el combo como una operación (cantidad y cupón opcional) |
| `POST /api/resenas` | `COMPRADOR` | Reseña una compra completada |
| `POST /api/valoraciones-vendedor` | `COMPRADOR` | Valora al vendedor |
| `POST /api/puntos/redimir` · `GET /api/puntos/historial` | `COMPRADOR` | Redime puntos y consulta el historial |
| `GET /api/wishlists` | `COMPRADOR` | Productos favoritos (solo visibles; `[]` si no hay) |
| `POST /api/wishlists` · `DELETE /api/wishlists/{productoId}` | `COMPRADOR` | Añade o quita de la wishlist |
| `GET /api/notificaciones` · `PUT /api/notificaciones/{id}/leida` | `COMPRADOR` | Lista las notificaciones (más reciente primero) y marca una como leída |
| `GET /api/regalos/disponibles` · `/mios` · `/progreso` | `COMPRADOR` | Regalos que puede elegir, los suyos y su progreso de compras |
| `PUT /api/regalos/{id}/asignar` · `PUT /api/regalos/{id}/entrega` | `COMPRADOR` | Elige un regalo (1 por cada 5 compras completadas) y confirma su recepción |
| `POST /api/regalos` · `GET /api/regalos/por-enviar` · `PUT /api/regalos/{id}/enviar` | `VENDEDOR` | Crea un regalo de un producto o combo propio, ve los elegidos y los envía (solo el vendedor dueño) |
| `GET /api/metas-recompensa` | Autenticado | Metas activas, de menor a mayor umbral |
| `POST /api/metas-recompensa` · `PUT …/{id}/umbral` · `PUT …/{id}/desactivar` | `VENDEDOR` | Gestiona las metas de recompensa |
| `GET /api/reportes/ventas` · `/stock-bajo` · `/metricas-resenas` | `VENDEDOR` | Ventas, alertas de stock y métricas de reseñas del vendedor |
| `GET /api/reportes/mas-vendidos` · `/global` | Autenticado | Ranking de más vendidos (JOIN + GROUP BY) y reporte global |

La lista completa, con códigos de respuesta y cuerpos, está en [`docs/api/mapeo-dominio-api.md`](docs/api/mapeo-dominio-api.md) y en Swagger.

---

## 4. Correcciones de la Entrega 1

| Observación que nos dieron | Qué cambiamos | Dónde se ve |
| :--- | :--- | :--- |
| Las entidades del dominio tenían el **constructor público**: cualquiera podía crear un objeto sin pasar por las validaciones y dejarlo en estado inválido. | Los constructores pasaron a `private`. La creación se hace solo con métodos de fábrica estáticos que validan las invariantes (`Producto.publicar`, `Usuario.registrar`, `Compra.realizar`, `Wishlist.crear`, `Vendedor.registrar`, `Combo.crear`, `Preventa.programar`...). | Rama `fix/corrections` (PR #18). Commits del 11/10: `e61f066` (Producto), `04e75f5` (Usuario), `0b14a99` (Compra), `5376af7` (Wishlist), `e1b7def` (Vendedor), `40ba7ed` (Combo), `8c2f707` (Preventa). Código actual en `domain/entity/`. |

**Por qué es mejor:** la regla de negocio queda en un solo punto, dentro del propio objeto del dominio, y es imposible saltársela desde un caso de uso o un controller.

**Consecuencia para la persistencia:** como el constructor es privado, el mapper JPA no puede crear la entidad directamente. Por eso se agregó un método `reconstruir(...)` en cada entidad (commits del 19 al 21/10), que rehidrata el objeto desde la base de datos sin abrir el constructor al resto del código.

**Entidades posteriores:** las entidades que se añadieron o ampliaron después (`Cupon` con dueño opcional, `Regalo`, `MetaRecompensa`, `Promocion` y `Compra` con `pedidoId` y datos de combo) mantienen el mismo patrón: constructor `private`, fábrica estática que valida (`Cupon.crear`, `Regalo.crear`, `MetaRecompensa.crear`, `Promocion.programar`, `Compra.realizar` con sus sobrecargas) y `reconstruir(...)` solo para la persistencia.

**Value Objects:** los VO (`Precio`, `Puntos`, `Unidades`, `Calificacion`...) son `record` de Java. Su constructor canónico es público, pero es seguro porque el `record` es inmutable y valida sus invariantes en el constructor compacto: un VO inválido nunca llega a existir.

---

## 5. ¿Quién compra y por qué?

El marketplace está diseñado conceptualmente para atender las necesidades específicas de la comunidad gamer:

- 🎮 **Gamers:** buscan productos de alto rendimiento que mejoren su experiencia competitiva (periféricos de alta precisión, componentes de hardware, sillas ergonómicas y accesorios dedicados).
- 🎥 **Streamers / Creadores de Contenido:** buscan productos funcionales y estéticamente atractivos (iluminación RGB, placas de captura, micrófonos condensadores, mobiliario) para personalizar sus setups de transmisión.
- 🕹️ **Jugadores Casuales:** buscan productos accesibles que garanticen confort, entretenimiento y una experiencia de juego amigable en su tiempo libre.
---

## 6. ¿Qué hace único a sus vendedores?

El sistema opera como un **marketplace multi-vendedor**, donde diversos comercios independientes pueden registrarse, gestionar su reputación y publicar su propia oferta comercial.

Categorías habilitadas en el catálogo:

- 🖱️ **Periféricos:** mouses, teclados mecánicos, audífonos Surround 7.1, mandos y mousepads XL.
- 🪑 **Mobiliario & Setup:** sillas ergonómicas, escritorios regulables, soportes de monitor e iluminación inteligente.
- 🎮 **Juegos Físicos & Coleccionables:** títulos en formato físico, figuras de acción y preventas exclusivas.
- 💻 **Licencias & Códigos Digitales:** claves para videojuegos, pases de batalla, gift cards y suscripciones.
- 🎁 **Combos Gamer:** paquetes promocionales de 2 o más productos ofrecidos a precio preferencial.
---

## 7. Lenguaje Ubicuo (DDD)

| Término | Definición en el Dominio |
| :--- | :--- |
| **Gama** | Clasificación técnica del producto según prestaciones y precio (`BAJA`, `MEDIA`, `ALTA`). |
| **Preventa** | Modalidad de compra anticipada habilitada antes de la fecha de liberación oficial del producto. |
| **Wishlist** | Lista de deseos personal para seguimiento de precios, promociones y disponibilidad de stock. En la interfaz se muestra como *Favoritos*. |
| **Notificación** | Alerta que recibe el usuario cuando un producto de su Wishlist baja de precio (descuento). |
| **Combo** | Agrupación promocional de 2 o más productos comercializados bajo una sola entidad con descuento. |
| **Usuario** | Persona registrada que compra, reseña, acumula y redime puntos. Se llama *Usuario* en todo el código. |
| **Vendedor** | Entidad comercial (Persona Natural o Empresa) registrada para publicar y vender productos. |
| **Puntos de Fidelización** | Unidades de beneficio otorgadas a compradores tras publicar reseñas de compras completadas. |
| **Regalo** | Beneficio que un vendedor ofrece (un producto o un combo propio) y que el comprador elige al completar 5 compras. Sigue `DISPONIBLE → ELEGIDO → ENVIADO → ENTREGADO`. |
| **Pedido** | Operación que agrupa varias compras creadas, confirmadas, canceladas o reembolsadas juntas: sus líneas comparten un `pedidoId`. El carrito se paga como un pedido (todo o nada). No existe como entidad aparte: es un conjunto de `Compra`. |
| **Cupón** | Código de descuento porcentual con vencimiento y usos máximos. Es global (sin dueño) o de un vendedor (solo aplica a sus productos). Consume un uso por pedido o combo y cada comprador lo usa una sola vez. |
| **Promoción de temporada** | Descuento porcentual con vigencia entre dos fechas, sin solaparse con otra del mismo producto. No cambia el precio base. |
| **Descuento inmediato** | Rebaja puntual del precio de un producto que notifica a las wishlists que lo contienen; el vendedor puede retirarla. |
| **Meta de recompensa** | Umbral de compras completadas ligado a un producto regalo. Hoy es informativa: no cambia la regla de 5 compras por regalo. |
| **Exclusividad** | Condición de un producto especial: cada usuario puede comprarlo una sola vez y de a una unidad. |
| **Unidades** | Cantidad disponible de un producto para su venta (nunca negativa; cero significa agotado). |

## Tipos de notificación
| Destinatario | Tipo | Cuándo se genera |
|---|---|---|
| Comprador | `DESCUENTO` (ya existía) | El vendedor rebaja un producto de su wishlist |
| Comprador | `STOCK_DISPONIBLE` | Producto de su wishlist estaba en 0 y vuelve a tener stock (cancelación o reembolso) |
| Comprador | `PREVENTA_DISPONIBLE` | Llega la fecha de lanzamiento de una preventa de su wishlist (job diario 00:05 + al arrancar; no duplica) |
| Comprador | `META_ALCANZADA` | Sus compras COMPLETADAS cruzan un múltiplo de 5 (confirmar compra o pedido) |
| Comprador | `REGALO_ENVIADO` | El vendedor marca el regalo como enviado |
| Vendedor | `STOCK_BAJO` | El stock de un producto cruza hacia abajo el umbral (5) |
| Vendedor | `STOCK_AGOTADO` | El stock llega a 0 |
| Vendedor | `NUEVA_VENTA` | Compra individual, pedido o combo con un producto suyo |
| Vendedor | `REEMBOLSO_SOLICITADO` | Reembolso individual o de pedido sobre un producto suyo |
| Vendedor | `NUEVA_RESENA` / `NUEVA_VALORACION` | Reseña de producto / valoración del vendedor |
---

## 8. Reglas de Negocio e Invariantes del Dominio

| # | Regla | Dónde se garantiza | Estado |
|---|---|---|---|
| 1 | Un producto nunca tiene stock negativo; se restituye al cancelar o reembolsar. | `Producto.reducirStock` / `aumentarStock` | ✅ |
| 2 | Solo se reseña una compra `COMPLETADA`; una reseña por usuario y producto. | `Compra.registrarResena`, `Resena.crear`, `SubirResenaUseCase` | ✅ |
| 3 | No se elimina un producto con compras activas (`PENDIENTE` o `COMPLETADA`); la eliminación es lógica. | `Producto.eliminar`, `EliminarProductoUseCase` | ✅ |
| 4 | Un producto en preventa no se compra antes de su fecha de lanzamiento. | `Preventa.validarAcceso`, `RealizarCompraUseCase`, `CrearPedidoUseCase`, `ComprarComboUseCase` | ✅ |
| 5 | Un combo tiene dos o más productos distintos y su descuento es menor a 100%. | `Combo.crear`, `agregarProducto`, `quitarProducto`, `CrearComboUseCase`, `ModificarProductosComboUseCase` | ✅ |
| 6 | Los puntos se asignan solo después de reseñar, y una sola vez. | `Compra.asignarPuntos`, `SubirResenaUseCase` | ✅ |
| 7 | Un producto no se repite en la Wishlist ni se agregan productos eliminados. | `Wishlist.agregarProducto`, `AnadirAWishlistUseCase` | ✅ |
| 8 | Solo se notifica a quienes tienen el producto en su Wishlist, y solo cuando hay descuento. | `Producto.aplicarDescuento`, `AplicarDescuentoUseCase`, `NotificarDescuentoWishlistUseCase` | ✅ |
| 9 | Un producto exclusivo se compra una sola vez por usuario y de a una unidad. | `Producto.validarCompraExclusiva` | ✅ |
| 10 | Un regalo solo se elige para quien no posee el producto, y sigue DISPONIBLE → ELEGIDO → ENVIADO → ENTREGADO. Solo lo envía el vendedor dueño de su producto o combo. | `Regalo`, `EnviarRegaloUseCase`, `ConfirmarEntregaRegaloUseCase` | ✅ |
| 11 | Un cupón consume **un uso por pedido o combo** (no uno por línea), cada comprador lo usa **una sola vez**, y todo se valida antes de descontar stock o consumir el uso. | `Cupon.validarUso`, `Cupon.registrarUso`, `CuponRepository.fueUsadoPor`, `CrearPedidoUseCase`, `ComprarComboUseCase` | ✅ |
| 12 | Un cupón con dueño solo rebaja los productos de ese vendedor; si ninguna línea del pedido le aplica, el pedido se rechaza. Sin dueño (global) aplica a todo. | `Cupon.aplicaA`, `CrearPedidoUseCase`, `ComprarComboUseCase` | ✅ |
| 13 | El pedido es atómico: se validan **todas** las líneas antes de descontar stock; no admite lista vacía, cantidades ≤ 0, el mismo producto repetido ni más de 50 líneas. | `CrearPedidoUseCase`, `CrearPedidoRequest` | ✅ |
| 14 | Las líneas de un pedido o combo no se confirman, cancelan ni reembolsan por separado: la operación se hace sobre el pedido completo y, si una línea no cumple, no cambia ninguna. | `Compra.asegurarNoEsDeUnPedido`, `ConfirmarPedidoUseCase`, `CancelarPedidoUseCase`, `SolicitarReembolsoPedidoUseCase` | ✅ |
| 15 | Un comprador elige un regalo por cada 5 compras `COMPLETADA` (regalos asignados ≤ compras completadas ÷ 5). | `Usuario.asegurarPuedeElegirRegalo`, `AsignarRegaloPorMetaUseCase` | ✅ |
| 16 | Un regalo se crea sobre exactamente un producto (no eliminado) o un combo, y siempre propios del vendedor que lo crea. | `Regalo.crear`, `CrearRegaloUseCase` | ✅ |

### Reglas adicionales que el código garantiza

- El reembolso solo procede si la compra está `COMPLETADA`, dentro de **48 horas**, sin descarga y sin puntos asignados.
- El reembolso de un pedido o combo exige que **todas** sus líneas cumplan esas condiciones; si una no cumple, ninguna cambia. Al reembolsar o cancelar se restituye el stock de cada línea.
- `REEMBOLSADA`, `CANCELADA` y `EXPIRADA` son estados finales de una compra.
- La fecha de la compra y los ids los genera el servidor, no el cliente.
- Un vendedor dado de baja no puede publicar y sus productos no se pueden comprar.
- Un producto pausado o eliminado no se puede comprar; solo el vendedor dueño modifica, edita, pausa o programa promociones de su producto.
- Una `Promocion` tiene vigencia (fechas inclusive), no se solapa con otra del mismo producto y no cambia el precio base.
- Un cupón es válido hasta su fecha de vencimiento (inclusive) y mientras tenga usos; se aplica una sola vez al pedido o combo, solo a compras `PENDIENTE` (el precio se congela al confirmar). Su código es único (sin distinguir mayúsculas), un vendedor activo lo crea con un vencimiento que no esté en el pasado, y `GET /api/cupones/{codigo}` es solo una vista previa que no consume nada (404 si no existe; 422 si está vencido, agotado o el comprador ya lo usó).
- En combos, el cupón se aplica después del descuento del combo y no se acumula con promociones individuales; los cupones de vendedor solo aplican a combos de ese vendedor.
- Comprar un combo valida todos sus productos antes de descontar stock y crea una única operación con nombre e historial propios; confirmar, cancelar o reembolsar afecta todas sus líneas.
- “Mis compras” presenta cada pedido o combo como una operación; al expandirla muestra sus productos, mientras el pago, la cancelación y el reembolso se hacen sobre el grupo completo.
- Combos: solo el vendedor dueño los modifica o elimina; no se elimina un combo con regalos pendientes (disponibles, elegidos o enviados). `GET /api/combos` solo lista combos cuyos productos están **todos** visibles (ni pausados ni eliminados), mientras que `GET /api/combos/mios` muestra todos los del vendedor.
- Wishlist: `GET /api/wishlists` devuelve `[]` si no hay wishlist y omite los productos eliminados o pausados. Solo se notifica el descuento cuando el precio baja: quitar un descuento no genera notificaciones.
- Notificaciones: se listan de la más reciente a la más antigua y solo su dueño puede marcarlas como leídas (`422` si es ajena, `404` si no existe).
- Preventas: `GET /api/preventas` lista solo las de lanzamiento **estrictamente posterior a hoy** (el mismo día del lanzamiento el producto ya se puede comprar), de la más próxima a la más lejana; programar una exige una fecha futura y un producto propio sin preventa previa.
- Promociones y precios: el catálogo, la wishlist y «Mis productos» muestran el precio con la promoción de temporada vigente hoy, el mismo que cobra el backend al reservar; solo el vendedor dueño modifica o quita una promoción.
- Una `MetaRecompensa` exige un umbral de compras positivo, un producto regalo existente y no eliminado, y se desactiva una sola vez (nunca se borra). Las metas **se muestran, pero no gobiernan** la asignación de regalos: esa regla sigue siendo `Usuario.COMPRAS_POR_REGALO = 5`.
- El vendedor se valora una vez por compra completada. La comisión del marketplace es 5% de los ingresos completados.
- Las contraseñas (de compradores y vendedores) exigen 8+ caracteres, mayúscula y número, y solo se guarda su hash (BCrypt). La sesión usa JWT HS256 con el rol en el token (`COMPRADOR` o `VENDEDOR`) y `/api/**` exige token salvo `/api/auth/**` y los `POST` de registro.
- El id del comprador o vendedor sale siempre del token, nunca del body ni de la URL; cada ruta valida el rol con `@PreAuthorize`.
- La búsqueda de catálogo es paginada (`size` entre 1 y 100) y solo lista productos visibles (no eliminados ni pausados).
- Los errores de dominio responden 422, la duplicidad 409 y las validaciones de datos 400; 401 y 403 devuelven un cuerpo JSON.
---

## 9. Casos de Uso del Sistema (57 Casos de Uso)

**Leyenda:** ✅ Implementado (caso de uso y pruebas) · 🟡 Parcial · ⏳ Especificado, pendiente de implementar.

Los códigos `CU-01` a `CU-37` son los de la especificación original; `CU-38` a `CU-57` se añadieron al construir las pantallas y la gestión que faltaban (pedidos, cupones, wishlist, notificaciones, combos, preventas, regalos y metas). Un caso de uso puede agrupar varias clases (el backend tiene 65 clases `*UseCase`; todas aparecen en las tablas).

### 📈 Resumen de avance

| Módulo | ✅ Implementados | 🟡 Parciales | ⏳ Especificados | Total |
| :--- | :---: | :---: | :---: | :---: |
| Compras y Checkout | 12 | 0 | 0 | 12 |
| Productos y Catálogo | 14 | 0 | 0 | 14 |
| Combos Promocionales | 4 | 0 | 0 | 4 |
| Reseñas, Puntos y Recompensas | 8 | 0 | 0 | 8 |
| Lista de Deseos (Wishlist) y Notificaciones | 6 | 0 | 0 | 6 |
| Usuarios, Vendedores y Autenticación | 5 | 0 | 0 | 5 |
| Consultas, Métricas y Reportes | 8 | 0 | 0 | 8 |
| **Total** | **57** | **0** | **0** | **57** |

### 🛒 Módulo 1: Compras y Checkout
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-01` | **Realizar Compra** | ✅ Implementado | Comprador | Registra la orden `PENDIENTE` de un producto, reserva stock y valida producto no eliminado, usuario y vendedor activos, exclusividad y fecha de preventa, usando la fecha del servidor (`RealizarCompraUseCase`). |
| `CU-09` | **Confirmar Compra** | ✅ Implementado | Comprador / Sistema | Valida el pago y pasa la orden a `COMPLETADA`. Una línea de pedido o combo se rechaza (`422`): se confirma con el pedido (CU-39) (`ConfirmarCompraUseCase`). |
| `CU-12` | **Cancelar Compra** | ✅ Implementado | Comprador | Pasa la orden a `CANCELADA` y devuelve automáticamente el stock retenido al producto. Una línea de pedido o combo se cancela con el pedido (CU-40) (`CancelarCompraUseCase`). |
| `CU-13` | **Solicitar Reembolso** | ✅ Implementado | Comprador | Solicita reembolso de una compra suelta dentro de 48 horas, sin descarga y sin puntos asignados; pasa a `REEMBOLSADA` y restituye el stock. Una línea de pedido o combo se reembolsa con la operación (CU-41) (`SolicitarReembolsoUseCase`). |
| `CU-16` | **Comprar Combo** | ✅ Implementado | Comprador | Reserva todos los productos como una sola operación, con descuento de combo y cupón opcional; cancelar, confirmar y reembolsar actúan sobre el combo completo (`ComprarComboUseCase`). |
| `CU-17` | **Aplicar Cupón de Descuento** | ✅ Implementado | Comprador | Aplica un código al pedido del carrito o al combo: consume **un uso por operación** (no uno por línea), cada comprador lo usa **una sola vez** y, si el cupón tiene dueño, solo rebaja los productos de ese vendedor (global: todos). En combos se descuenta sobre el precio ya rebajado y no se acumula con promociones de producto. Se valida antes de descontar stock (`CrearPedidoUseCase`, `ComprarComboUseCase`, `Cupon`). |
| `CU-38` | **Crear Pedido (carrito)** | ✅ Implementado | Comprador | Convierte el carrito en una operación: una compra `PENDIENTE` por línea con un mismo `pedidoId`, la promoción vigente aplicada y el cupón opcional. Valida **todas** las líneas antes de descontar stock (todo o nada); rechaza lista vacía, cantidades ≤ 0 y productos repetidos (`CrearPedidoUseCase`, `ItemPedido`, `PedidoController`). |
| `CU-39` | **Confirmar Pedido** | ✅ Implementado | Comprador / Sistema | Confirma todas las líneas del pedido en una operación; `404` si no existe y `422` si es ajeno o alguna línea no está `PENDIENTE`, verificado antes de cambiar nada (`ConfirmarPedidoUseCase`). |
| `CU-40` | **Cancelar Pedido** | ✅ Implementado | Comprador | Cancela todas las líneas pendientes y restituye el stock de cada producto, verificando antes de mutar (`CancelarPedidoUseCase`). |
| `CU-41` | **Reembolsar Pedido o Combo** | ✅ Implementado | Comprador | Reembolsa todas las líneas de un pedido o combo como una sola operación si cada una cumple el plazo de 48 horas, sin descarga y sin puntos; restituye el stock (`SolicitarReembolsoPedidoUseCase`). |
| `CU-42` | **Crear Cupón** | ✅ Implementado | Vendedor | Un vendedor activo crea un cupón propio (solo aplica a sus productos) con código único, porcentaje entre 0 y 100, vencimiento no pasado y usos máximos (`CrearCuponUseCase`). |
| `CU-43` | **Consultar Cupones** | ✅ Implementado | Vendedor / Comprador | El vendedor lista sus cupones con sus usos; el comprador obtiene la vista previa de un código sin consumirlo (`404` si no existe, `422` si está vencido, agotado o ya usado por él) (`ConsultarCuponesVendedorUseCase`, `ConsultarCuponUseCase`). |

### 📦 Módulo 2: Productos y Catálogo
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-04` | **Publicar Producto** | ✅ Implementado | Vendedor | Crea un nuevo producto especificando gama, precio, stock y atributos; expuesto en `POST /api/productos` (`PublicarProductoUseCase`, `ProductoController`). |
| `CU-05` | **Modificar Producto** | ✅ Implementado | Vendedor | Actualiza precio, descripción o disponibilidad de preventa. (`ModificarProductoUseCase`). |
| `CU-06` | **Eliminar Producto** | ✅ Implementado | Vendedor | Desactiva el producto preservando trazabilidad; rechaza la baja si hay compras activas (`EliminarProductoUseCase`). |
| `CU-18` | **Gestionar / Aumentar Stock** | ✅ Implementado (Dominio) | Vendedor | Incrementa las unidades disponibles en inventario (`Producto.aumentarStock`, usado al cancelar y reembolsar). |
| `CU-19` | **Editar Información General** | ✅ Implementado | Vendedor | Actualiza detalles secundarios e imágenes del producto. (`EditarInformacionProductoUseCase`). |
| `CU-20` | **Filtrar y Buscar Productos** | ✅ Implementado | Comprador | Búsqueda avanzada y paginada por texto, gama, categoría, precio y preventas; filtros y paginación se resuelven en la base de datos. (`BuscarProductosUseCase`). |
| `CU-21` | **Activar / Pausar Publicación** | ✅ Implementado | Vendedor | Alterna la visibilidad del producto en el catálogo sin borrarlo. (`CambiarVisibilidadProductoUseCase`). |
| `CU-22` | **Configurar Descuento Promocional** | ✅ Implementado | Vendedor | Aplica un porcentaje de descuento a un producto y notifica a quienes lo tienen en su Wishlist (`AplicarDescuentoUseCase`). La vigencia temporal la maneja `Promocion` (el precio base no cambia). (`ConfigurarDescuentoPromocionalUseCase`). |
| `CU-37` | **Programar Preventa** | ✅ Implementado | Vendedor | Programa una preventa con fecha de lanzamiento futura, un producto del vendedor y sin preventa duplicada (`ProgramarPreventaUseCase`). `RealizarCompraUseCase` respeta esa fecha (Regla 4). |
| `CU-44` | **Consultar Mis Productos** | ✅ Implementado | Vendedor | Lista todos los productos del vendedor, activos y pausados (solo excluye los eliminados), para poder reactivarlos; incluye la promoción vigente (`ConsultarMisProductosUseCase`). |
| `CU-45` | **Quitar Descuento** | ✅ Implementado | Vendedor | El vendedor dueño retira el descuento inmediato y el producto recupera su precio anterior; no se notifica a las wishlists (`QuitarDescuentoUseCase`). |
| `CU-46` | **Modificar / Quitar Promoción** | ✅ Implementado | Vendedor | Cambia porcentaje y vigencia de una promoción de temporada (mismas reglas que al crearla, sin solaparse) o la elimina; el precio base nunca se tocó (`ModificarPromocionUseCase`, `QuitarPromocionUseCase`). |
| `CU-47` | **Consultar Promociones** | ✅ Implementado | Vendedor / Sistema | El vendedor ve todas sus promociones (pasadas, vigentes y futuras); el catálogo, la wishlist y «Mis productos» resuelven la promoción vigente hoy para mostrar el mismo precio que se cobrará (`ConsultarPromocionesDelVendedorUseCase`, `ConsultarPromocionesVigentesUseCase`). |
| `CU-48` | **Consultar Preventas** | ✅ Implementado | Comprador / Vendedor | Lista las preventas con lanzamiento estrictamente posterior a hoy, de la más próxima a la más lejana; el frontend las indexa por producto para mostrar la insignia y bloquear la compra (`ConsultarPreventasUseCase`). |

### 🎁 Módulo 3: Combos Promocionales
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-07` | **Crear Combo Gamer** | ✅ Implementado | Vendedor | Agrupa 2 o más productos del vendedor, sin duplicados y con descuento menor a 100% (`CrearComboUseCase`, `Combo.crear`). |
| `CU-23` | **Modificar Productos de Combo** | ✅ Implementado | Vendedor | Añade o remueve productos garantizando el mínimo de 2 unidades (`ModificarProductosComboUseCase`). |
| `CU-49` | **Consultar Combos** | ✅ Implementado | Comprador / Vendedor | Para compradores, solo combos cuyos productos están todos visibles, con precio con y sin descuento; el vendedor ve todos los suyos, aunque tengan productos pausados (`ConsultarCombosUseCase`). |
| `CU-50` | **Modificar / Eliminar Combo** | ✅ Implementado | Vendedor | El vendedor dueño cambia nombre, descripción y descuento, o elimina el combo; no se elimina si hay regalos pendientes que lo referencian (`ModificarComboUseCase`, `EliminarComboUseCase`). |

### ⭐ Módulo 4: Reseñas, Puntos y Recompensas
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-02` | **Subir Reseña y Asignar Puntos** | ✅ Implementado | Comprador | Publica reseña sobre compra completada, valida 1 reseña por usuario/producto y asigna puntos (`SubirResenaUseCase`). |
| `CU-08` | **Redimir Puntos por Beneficios** | ✅ Implementado | Comprador | Canjea puntos acumulados con validación de saldo positivo (`RedimirPuntosUseCase`, `Usuario.redimirPuntos`). |
| `CU-10` | **Asignar Regalo por Meta** | ✅ Implementado | Comprador / Sistema | El comprador elige un regalo `DISPONIBLE` por cada 5 compras `COMPLETADA`, siempre que no posea ya el producto (o alguno de los del combo) (`AsignarRegaloPorMetaUseCase`). |
| `CU-24` | **Enviar / Confirmar Entrega Regalo** | ✅ Implementado | Vendedor / Comprador | Administra el cambio de estado del regalo: el envío exige que esté elegido y solo lo hace el vendedor dueño del producto o combo; la entrega la confirma solo el destinatario (`EnviarRegaloUseCase`, `ConfirmarEntregaRegaloUseCase`). |
| `CU-51` | **Crear Regalo** | ✅ Implementado | Vendedor | Un vendedor activo ofrece un regalo (queda `DISPONIBLE`) sobre un producto no eliminado o un combo propio; exactamente uno de los dos (`CrearRegaloUseCase`, `Regalo.crear`). |
| `CU-52` | **Consultar Regalos** | ✅ Implementado | Comprador / Vendedor | Regalos disponibles para elegir, regalos del comprador y regalos elegidos que el vendedor debe enviar (solo los suyos) (`ConsultarRegalosDisponiblesUseCase`, `ConsultarMisRegalosUseCase`, `ConsultarRegalosPorEnviarUseCase`). |
| `CU-53` | **Consultar Progreso de Regalos** | ✅ Implementado | Comprador | Compras completadas, regalos asignados, regalos por elegir y compras que faltan para el siguiente, con la misma regla de 5 compras de CU-10 (`ConsultarProgresoRegalosUseCase`). |
| `CU-54` | **Consultar Metas** | ✅ Implementado | Comprador / Vendedor | Metas de recompensa activas de menor a mayor umbral; son informativas y no gobiernan la asignación de regalos (`ConsultarMetasUseCase`). |

### ❤️ Módulo 5: Lista de Deseos (Wishlist) y Notificaciones
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-03` | **Añadir a Wishlist** | ✅ Implementado | Comprador | Guarda productos en la wishlist evitando duplicados y rechazando eliminados (`AnadirAWishlistUseCase`). |
| `CU-14` | **Notificar Descuento en Wishlist** | ✅ Implementado | Sistema | Genera notificaciones por descuento para quienes tienen el producto en su Wishlist (`NotificarDescuentoWishlistUseCase`). |
| `CU-15` | **Quitar de Wishlist** | ✅ Implementado | Comprador | Elimina un producto específico de la lista de deseos (`QuitarDeWishlistUseCase`). |
| `CU-55` | **Consultar Wishlist** | ✅ Implementado | Comprador | Devuelve los favoritos en el orden en que se añadieron, omitiendo productos eliminados o pausados; `[]` si no hay wishlist. El frontend lo carga para que el corazón persista tras recargar (`ConsultarWishlistUseCase`). |
| `CU-56` | **Consultar Notificaciones** | ✅ Implementado | Comprador | Lista las notificaciones del usuario, la más reciente primero (`ConsultarNotificacionesUseCase`). |
| `CU-57` | **Marcar Notificación como Leída** | ✅ Implementado | Comprador | Solo el dueño marca su notificación como leída: `404` si no existe, `422` si es ajena (`MarcarNotificacionLeidaUseCase`). |

### 👤 Módulo 6: Usuarios, Vendedores y Autenticación
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-11` | **Registrar Usuario / Vendedor** | ✅ Implementado | Público | Registro de compradores (correo único) y de vendedores (documento único, cédula o NIT) (`RegistrarUsuarioUseCase`, `RegistrarVendedorUseCase`). |
| `CU-25` | **Dar de Baja Vendedor** | ✅ Implementado | Admin / Vendedor | Baja lógica del vendedor conservando la historia comercial (`DarDeBajaVendedorUseCase`). |
| `CU-26` | **Editar Perfil y Seguridad** | ✅ Implementado | Usuario | Modificación de nombre, correo y contraseña. (`EditarPerfilUseCase`, `CambiarContrasenaUseCase`). |
| `CU-27` | **Iniciar Sesión (JWT)** | ✅ Implementado | Todos | Autenticación con token JWT seguro para el acceso a las APIs. (`IniciarSesionUseCase`, `AuthController`). |
| `CU-28` | **Valorar Vendedor** | ✅ Implementado | Comprador | Calificación directa del servicio y atención brindada por el vendedor. (`ValorarVendedorUseCase`). |

### 📊 Módulo 7: Consultas, Métricas y Reportes
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-29` | **Reporte de Ventas por Vendedor** | ✅ Implementado | Vendedor | Balance de ingresos, unidades vendidas y comisiones. (`ReporteVentasVendedorUseCase`). |
| `CU-30` | **Reporte de Productos Más Vendidos** | ✅ Implementado | Admin / Vendedor | Escalafón de productos con mayor volumen de venta y calificación, agrupado y ordenado en la base de datos. (`ReporteMasVendidosUseCase`). |
| `CU-31` | **Consultar Historial de Compras** | ✅ Implementado | Comprador | Muestra las compras del usuario, la más reciente primero (`ConsultarHistorialComprasUseCase`). |
| `CU-32` | **Consultar Historial de Puntos** | ✅ Implementado | Comprador | Detalle de saldo de puntos ganados y redimidos. (`ConsultarHistorialPuntosUseCase`). |
| `CU-33` | **Gestionar Metas de Recompensas** | ✅ Implementado | Admin (en la práctica, `VENDEDOR`) | Crea metas, cambia su umbral o las desactiva; el producto regalo debe existir y no estar eliminado. (`GestionarMetasRecompensaUseCase`). |
| `CU-34` | **Alertas de Stock Bajo** | ✅ Implementado | Vendedor | Reporte de inventario crítico o agotado para reabastecimiento. (`AlertasStockBajoUseCase`). |
| `CU-35` | **Métricas de Reseñas** | ✅ Implementado | Admin / Vendedor | Dashboard de satisfacción del cliente y calificaciones. (`MetricasResenasUseCase`). |
| `CU-36` | **Reporte Global del Marketplace** | ✅ Implementado | Admin | Consolidado ejecutivo del volumen total de operaciones. (`ReporteGlobalUseCase`). |
 
---

## 10. Arquitectura del Software (Clean Architecture)

El backend en Java 21 / Spring Boot implementa **Clean Architecture** estructurado en capas desacopladas.

### Raíces de agregado para la sustentación

- **Producto**: raíz del agregado de catálogo e inventario. Protege invariantes como stock no negativo, baja lógica y visibilidad. El vendedor se referencia por `vendedorId`; no se carga como parte de este agregado.
- **Compra**: raíz del agregado de compra. Controla cantidad, precio unitario congelado y transiciones de estado, además de las reglas de reseña, puntos y reembolso. Referencia `usuarioId` y `productoId` sin contener esos agregados.
- **Cupon**: raíz del agregado de descuento por código. Protege el código normalizado, el porcentaje válido, el vencimiento y los usos máximos; con `vendedorId` es de un vendedor y sin él es global. La regla «una vez por comprador» depende del historial y vive en los casos de uso (`CuponRepository.fueUsadoPor`).
- **Pedido**: no es una raíz propia, sino un conjunto de `Compra` que comparten `pedidoId` (el carrito o un combo). Su atomicidad la garantizan los casos de uso con `@Transactional`, validando todas las líneas antes de modificar nada.
  Los casos de uso coordinan cambios que afectan a más de un agregado; cada entidad protege sus propias reglas. Las entidades JPA son adaptadores de persistencia separados de estas raíces de dominio.

```
backend/src/main/java/com/uniquindio/backend/
├── domain/                  # Capa de Dominio (Sin dependencias de frameworks)
│   ├── entity/              # Entidades Raíz de Agregados (Producto, Compra, Vendedor, Usuario, Regalo, Resena, Wishlist, Combo, Preventa, Notificacion, Promocion, Cupon, MetaRecompensa, MovimientoPuntos, ValoracionVendedor)
│   ├── valueobject/         # Objetos de Valor (Gama, EstadoCompra, EstadoRegalo, TipoVendedor, TipoNotificacion, Precio, Puntos, Unidades, Calificacion, Exclusividad, FiltroProducto, Pagina, Contrasena, TipoMovimientoPuntos, Rol)
│   ├── repository/          # Interfaces de Repositorio de Dominio (incluye CatalogoRepository)
│   ├── service/             # Puertos del dominio (EmisorToken, HashContrasena, SesionToken)
│   └── exception/           # Excepciones de Negocio (ReglaDominioException, ConflictoException, AutenticacionException)
├── application/             # Capa de Aplicación (Casos de Uso)
│   ├── usecase/             # Lógica de Orquestación (65 clases *UseCase = 57 casos de uso; ver sección 9)
│   │   ├── Compras:   RealizarCompra, ConfirmarCompra, CancelarCompra, SolicitarReembolso, ConsultarHistorialCompras
│   │   ├── Pedidos:   CrearPedido (+ ItemPedido), ConfirmarPedido, CancelarPedido, SolicitarReembolsoPedido
│   │   ├── Cupones:   CrearCupon, ConsultarCuponesVendedor, ConsultarCupon
│   │   ├── Productos: PublicarProducto, EliminarProducto, ConsultarMisProductos, ProgramarPreventa, ConsultarPreventas
│   │   ├── Catálogo y promos: ModificarProducto, EditarInformacionProducto, CambiarVisibilidadProducto, BuscarProductos,
│   │   │              AplicarDescuento, QuitarDescuento, ConfigurarDescuentoPromocional, ModificarPromocion, QuitarPromocion,
│   │   │              ConsultarPromocionesDelVendedor, ConsultarPromocionesVigentes
│   │   ├── Combos:    CrearCombo, ModificarCombo, ModificarProductosCombo, EliminarCombo, ConsultarCombos (+ ComboConProductos), ComprarCombo
│   │   ├── Reseñas y puntos: SubirResena, RedimirPuntos, ConsultarHistorialPuntos
│   │   ├── Regalos y metas:  CrearRegalo, ConsultarRegalosDisponibles, ConsultarMisRegalos, ConsultarRegalosPorEnviar,
│   │   │              ConsultarProgresoRegalos, AsignarRegaloPorMeta, EnviarRegalo (+ DuenoDeRegalo), ConfirmarEntregaRegalo,
│   │   │              GestionarMetasRecompensa, ConsultarMetas
│   │   ├── Wishlist y notificaciones: AnadirAWishlist, QuitarDeWishlist, ConsultarWishlist, NotificarDescuentoWishlist,
│   │   │              ConsultarNotificaciones, MarcarNotificacionLeida
│   │   ├── Cuentas:   RegistrarUsuario, RegistrarVendedor, DarDeBajaVendedor, IniciarSesion, EditarPerfil, CambiarContrasena, ValorarVendedor
│   │   └── Reportes:  ReporteVentasVendedor, ReporteMasVendidos, AlertasStockBajo, MetricasResenas, ReporteGlobal
│   ├── port/out/            # Puertos de salida de consulta (ReporteMasVendidosPort)
│   └── dto/                 # Data Transfer Objects (request y response)
└── infrastructure/          # Capa de Infraestructura (Spring Boot / Controller / Persistence)
    ├── rest/                # Controladores REST (uno por recurso: Auth, Compra, Pedido, Cupon, Combo, Regalo, Wishlist, Notificacion, Preventa, MetaRecompensa...), mappers y GlobalExceptionHandler
    ├── persistence/         # Repositorios JPA (H2 / MariaDB) y en memoria, con sus mappers de persistencia; CuponUso registra qué comprador usó cada cupón
    ├── security/            # JwtEmisorToken, JwtAuthenticationFilter, BCryptHashContrasena y RespuestaErrorSeguridad
    └── config/              # ClockConfig, SecurityConfig (JWT y CORS) y OpenApiConfig
```

Estructura del frontend (`frontend/src/app/`):

```
├── componentes/   # Pantallas standalone: catalogo, combos, carrito, mis-compras, favoritos, notificaciones, regalos, perfil,
│                  # panel-vendedor y sus pestañas (formulario-publicar, combos-vendedor, cupones-vendedor, regalos-vendedor,
│                  # preventas-vendedor, metas-vendedor), login, registro, inicio y componentes de apoyo (tarjeta-producto,
│                  # confirmacion, arte-producto, escena3d)
├── servicios/     # Un servicio por módulo (auth, wishlist, notificacion, carrito, compra, combo, cupon, regalo, preventa, meta,
│                  # producto, reporte, usuario) y utilidades (api.util con mensajeDeError, formato, toast, confirmacion)
├── core/          # auth.interceptor (agrega el JWT) y guards (authGuard, rolGuard, guestGuard)
├── modelos/       # Interfaces TypeScript de las respuestas de la API
└── app.routes.ts  # Rutas protegidas por guards (sección 2.1)
```
 
---

## 11. Seguridad, Base de Datos y Limitaciones

### Seguridad

La API usa Spring Security con JWT (jjwt, HS256). El login (CU-27) autentica compradores y vendedores y emite un token con el rol (`COMPRADOR` o `VENDEDOR`). `/api/**` exige token, salvo `/api/auth/**` y los `POST` de registro.

Los controllers leen el id del `Authentication` (token), nunca del body ni de la URL, por lo que un usuario no puede actuar sobre recursos de otro. Cada ruta restringe el rol con `@PreAuthorize`: un rol equivocado recibe 403.

CORS está habilitado para el frontend en desarrollo (`localhost:4200` y `localhost:3000`) dentro de la cadena de seguridad, de modo que el preflight `OPTIONS` se responde sin exigir token.

En desarrollo el secreto JWT tiene un valor por defecto; en producción debe definirse la variable de entorno `JWT_SECRET` (mínimo 32 caracteres).

### Base de datos y consultas

El perfil predeterminado usa H2 en memoria para desarrollo y pruebas, con datos de demo cargados desde `data.sql`. Para MariaDB, crear la base `ecommerce_productos_gamer` y arrancar desde `backend` con el perfil `mariadb`.

PowerShell:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3306/ecommerce_productos_gamer"
$env:DB_USERNAME = "usuario"
$env:DB_PASSWORD = "contraseña"
.\gradlew.bat bootRun --args="--spring.profiles.active=mariadb"
```

Bash:

```bash
DB_URL=jdbc:mariadb://localhost:3306/ecommerce_productos_gamer \
DB_USERNAME=usuario DB_PASSWORD=contraseña \
./gradlew bootRun --args='--spring.profiles.active=mariadb'
```

El perfil `mariadb` no carga los datos de demo (`spring.sql.init.mode=never`): los usuarios y vendedores se crean con los endpoints de registro.

El catálogo se busca con `GET /api/productos?texto=mouse&categoria=Perifericos&page=0&size=20`; la respuesta contiene `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`. `GET /api/reportes/mas-vendidos` agrupa ventas completadas en la base de datos.

### Imágenes de producto (Cloudinary)

La carga es opcional. Para habilitarla, configura en `backend/.env` las tres variables `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` y `CLOUDINARY_API_SECRET`, o bien la variable única `CLOUDINARY_URL` con el formato estándar `cloudinary://API_KEY:API_SECRET@CLOUD_NAME`. No guardes estas credenciales en Git ni las envíes desde Angular:

```bash
cd backend
# Crea o edita el archivo .env ignorado por Git y define una de estas dos opciones:
# CLOUDINARY_CLOUD_NAME=...
# CLOUDINARY_API_KEY=...
# CLOUDINARY_API_SECRET=...
# CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME
set -a
. ./.env
set +a
./gradlew bootRun
```

En Windows PowerShell, asigna al entorno la misma opción que configuraste en `.env` antes de iniciar `.\gradlew.bat bootRun`; PowerShell no carga `.env` automáticamente.

El vendedor publica primero el producto y luego sube su imagen a `POST /api/productos/{id}/imagen` como `multipart/form-data` en el campo `archivo`. El backend acepta PNG/JPEG de hasta 5 MB, firma la petición a Cloudinary y persiste la URL HTTPS; el catálogo muestra la imagen o conserva la ilustración si no hay imagen o falla su carga.

### Limitaciones

- **No existe un rol ADMIN.** Las metas de recompensa quedan restringidas a `VENDEDOR`, aunque conceptualmente son de administración; como las metas son globales, **cualquier vendedor puede crear, cambiar o desactivar cualquier meta**. El envío de regalos (CU-24) ya no sufre esto: solo lo hace el vendedor dueño del producto o combo del regalo.
- **Las metas no gobiernan la asignación de regalos.** Se gestionan y se muestran (`GET /api/metas-recompensa`, pantalla de regalos), pero la regla real es un regalo por cada 5 compras completadas (`Usuario.COMPRAS_POR_REGALO`).
- `GET /api/reportes/mas-vendidos` y `GET /api/reportes/global` están abiertos a cualquier sesión autenticada (son estadísticas del marketplace).
- Un cupón es de «una vez por comprador», no «una sola compra en total»: mientras queden usos, otros compradores pueden usarlo. El cupón de demo `VERANO26` vence el 31/12/2026.
- La carga de imágenes depende de configurar Cloudinary (opcional): sin credenciales, o si la subida falla, el producto se publica igual y el catálogo usa la ilustración de respaldo.
- Un usuario comprador no puede darse de baja (solo existe la baja de vendedor) y el rol `VENDEDOR` no puede editar su perfil ni cambiar su contraseña desde la API.
  Los endpoints de cada operación están en [`docs/api/mapeo-dominio-api.md`](docs/api/mapeo-dominio-api.md) y en Swagger (`http://localhost:8080/swagger-ui.html`, botón **Authorize** para pegar el token del login).

---

## 12. Cómo ejecutar las pruebas

```powershell
cd backend
.\gradlew.bat test      # Linux/Mac: ./gradlew test
```

Pruebas del frontend (Vitest) y compilación:

```powershell
cd frontend
npx ng test --watch=false
npx ng build
```
 
---

## 13. Pruebas avanzadas

En esta parte agregamos pruebas para revisar que el backend no solo funcione por encima, sino que también guarde bien la información y responda como debe cuando alguien intenta hacer algo que no puede. Para organizarlas usamos el patrón AAA: primero preparamos los datos, después ejecutamos la acción y al final revisamos si pasó lo que esperábamos. Las pruebas se ejecutan con `./gradlew test`.

### Dónde está cada prueba

| Punto del seguimiento | Clase de prueba |
| :--- | :--- |
| 0. Guardar, consultar y revisar consultas del repositorio | `ConsultasCatalogoJpaTest`, `VendedorRepositoryJpaTest`, `PersistenciaSecundariaJpaTest` |
| 1. Endpoint con seguridad | `ProductoControllerTest`, `ReporteControllerTest` |
| 2. Pruebas con `@DataJpaTest` | `ConsultasCatalogoJpaTest`, `VendedorRepositoryJpaTest`, `PersistenciaSecundariaJpaTest` |
| 3. Prueba de integración | `integracion/FlujoCompletoIntegracionTest` |
| 4. Error 404 y regla de negocio | `EliminarProductoControllerTest` |
| 5. Errores HTTP de compra y publicación (401, 422 y 400) | `integracion/FlujoCompletoIntegracionTest` |
| 6. Pedido atómico y cupones | `CrearPedidoUseCaseTest`, `ConfirmarPedidoUseCaseTest`, `CancelarPedidoUseCaseTest`, `SolicitarReembolsoPedidoUseCaseTest`, `PedidoControllerTest`, `CuponTest`, `CrearCuponUseCaseTest`, `ConsultarCuponUseCaseTest`, `CuponControllerTest`, `CuponRepositoryJpaTest`, `integracion/FlujoCompletoIntegracionTest` |
| 7. Consultas y gestión de los módulos con pantalla | `ConsultarWishlistUseCaseTest`, `WishlistControllerTest`, `ConsultarNotificacionesUseCaseTest`, `MarcarNotificacionLeidaUseCaseTest`, `NotificacionControllerTest`, `ConsultarCombosUseCaseTest`, `ComboRepositoryJpaTest`, `ComboControllerTest`, `ConsultarPreventasUseCaseTest`, `PreventaRepositoryJpaTest`, `PreventaControllerTest`, `CrearRegaloUseCaseTest`, `ConsultarProgresoRegalosUseCaseTest`, `DuenoDeRegaloTest`, `RegaloRepositoryJpaTest`, `RegaloControllerTest`, `ConsultarMetasUseCaseTest`, `MetaRecompensaControllerTest` |

### 0. Pruebas del repositorio

Con estas pruebas revisamos que la información sí se guarde y después se pueda consultar sin que se pierdan los datos. También comprobamos consultas del catálogo y de vendedores usando datos que conocemos, para saber cuántos resultados deberían salir y en qué orden. Esto sirve porque no basta con que el programa diga que guardó algo: también toca mirar que al recuperarlo los datos sigan bien, incluyendo los objetos de valor que usa el dominio. Para esta parte usamos pruebas de persistencia con `@DataJpaTest`, que nos deja probar esa zona sin tener que levantar toda la aplicación.

### 1. Probar un endpoint con seguridad

Probamos `POST /api/productos`, que es una ruta para publicar productos y que debe usar un vendedor. La idea es revisar tres situaciones: si no se manda token, la API responde `401`; si se entra como comprador, que no tiene permiso para publicar, responde `403`; y si se entra como vendedor, la petición se acepta. En este último caso devuelve `201`, porque se está creando un producto nuevo. Para estas pruebas usamos `SesionTest`, que nos ayuda a preparar una sesión con el rol que necesitamos. Así revisamos que la seguridad no dependa solo de que alguien esté conectado, sino también de que tenga el permiso correcto.

### 2. `@DataJpaTest` contra `@SpringBootTest`

Por lo que entendimos, `@DataJpaTest` sirve para probar la parte que maneja los datos, como los repositorios y las consultas, sin cargar todos los componentes del sistema. En cambio, `@SpringBootTest` levanta una parte mucho más completa de la aplicación, por ejemplo los controllers, los casos de uso y la seguridad. Por eso usamos `@DataJpaTest` en las pruebas de repositorio y `@SpringBootTest` para revisar flujos más completos. Cuando trabajamos con Spring Boot 4, vimos que `@DataJpaTest` cambió de paquete y que también hacía falta agregar una dependencia de pruebas aparte. Lo resolvimos agregando `spring-boot-starter-data-jpa-test` al `build.gradle` y usando el import nuevo de la anotación.

### 3. Prueba de integración

Una prueba unitaria revisa una parte pequeña del código por separado, mientras que una prueba de integración mira si varias partes trabajan bien juntas. En `FlujoCompletoIntegracionTest` usamos `@SpringBootTest` y `MockMvc` para recorrer el flujo real desde la petición, pasando por la seguridad, el controller y el caso de uso, hasta llegar al repositorio y a la base de datos H2. No usamos mocks para reemplazar esas partes del flujo. Probamos, por ejemplo, que un vendedor publique un producto y que un comprador pueda comprarlo, confirmar la compra y verla en su historial. También revisamos que un reembolso fuera del tiempo permitido sea rechazado. Estas pruebas sirven para encontrar problemas entre componentes, aunque tardan más que las unitarias, así que conviene usarlas en los procesos más importantes.

### 4. Más casos en el controller

También agregamos casos donde la petición no debería terminar guardando información. Uno revisa qué pasa cuando se intenta eliminar un producto que no existe y la API devuelve un `404`. El otro revisa una regla de negocio que impide eliminar un producto cuando tiene compras activas. En los dos casos usamos `verify(..., never())` para comprobar que el método de guardar no se haya llamado. Esto es importante porque no sería suficiente con que la API devuelva un error: también tenemos que comprobar que no haya hecho un cambio que no debía hacer.

### 5. Errores HTTP de compra y publicación

El frontend le muestra al usuario el `mensaje` que manda el backend, así que fijamos con pruebas de integración los códigos y mensajes que consume. En `FlujoCompletoIntegracionTest`: comprar más unidades que el stock responde `422` con "No hay suficiente stock disponible" y el stock del producto no cambia en la base; comprar sin token responde `401`; y publicar con el nombre vacío y un precio negativo responde `400` con un mensaje que nombra el campo. Los mismos casos están en la carpeta B de la colección de Postman (`docs/postman/flujo-completo.json`). En el frontend, las pruebas con Vitest cubren `mensajeDeError`, el carrito (un solo pedido, cupón, cancelación si falla la confirmación y doble clic), los servicios y pantallas de cada módulo (favoritos, notificaciones, combos, cupones, preventas, regalos y metas) y el formulario de publicación (validación, botón bloqueado y errores del servidor).

### 6. Pedido atómico y cupón de un solo uso

El carrito se paga con un pedido: `POST /api/pedidos` crea una compra por línea y descuenta el stock de todas o de ninguna, por lo que las pruebas comprueban sobre todo lo que **no** debe cambiar cuando algo falla. En `CrearPedidoUseCaseTest` (con repositorios en memoria) comprobamos que si el último ítem no tiene stock no se descuenta ningún producto ni se guarda ninguna compra, que no se admite una lista vacía, cantidades menores o iguales a cero ni el mismo producto repetido, y que con un cupón de tres ítems se consume **exactamente un uso** (`consumeUnSoloUsoConTresItems`). También que un cupón vencido, agotado, inexistente, de otro vendedor o ya usado por el mismo comprador se rechaza sin tocar el stock, y que si falla el stock el cupón no se consume. `CuponRepositoryJpaTest` (`@DataJpaTest`) verifica que el uso por comprador queda registrado y es idempotente.

En `FlujoCompletoIntegracionTest` recorremos el flujo real con `MockMvc` y H2: un pedido de dos productos se confirma en una sola operación; un pedido con un ítem sin stock responde `422` y no descuenta nada; cancelar un pedido restituye el stock y sus líneas no se operan por separado; un pedido ajeno responde `422` y uno inexistente `404`; un pedido sin ítems responde `400`; `VERANO26` consume un solo uso y un vendedor puede crear un cupón que el comprador usa en un pedido. `PedidoControllerTest` fija los códigos `201`, `200`, `400`, `401`, `403`, `404` y `422` del controller.

### 7. Módulos con pantalla (wishlist, notificaciones, combos, preventas, regalos y metas)

Cada módulo sigue la misma plantilla: una prueba del caso de uso con repositorios en memoria, una del repositorio con `@DataJpaTest` cuando se añadió una consulta, y una del controller con `SesionTest` para fijar `401` (sin token), `403` (rol equivocado) y el código de éxito. Algunos casos que fijan reglas: una wishlist vacía devuelve `[]` y no `404`; una notificación ajena responde `422`; un combo con un producto pausado no aparece en el catálogo pero sí en el panel de su vendedor; una preventa con fecha de lanzamiento igual a hoy ya no cuenta como futura; un vendedor no puede crear ni enviar el regalo de un producto ajeno (`DuenoDeRegaloTest`); y el progreso de regalos calcula cuántos puede elegir el comprador y cuántas compras le faltan. En el frontend cada servicio y pantalla nuevos tienen su `*.spec.ts` (por ejemplo `wishlist.service.spec.ts`, `notificacion.service.spec.ts`, `combos.spec.ts`, `regalos.spec.ts`).

## 14. Gestión del Proyecto (Trello)

El avance del proyecto se gestiona en un tablero público de Trello:

👉 **[Marketplace Gamer — Backend & DDD](https://trello.com/b/pmz32QJB/marketplace-gamer-backend-ddd-entrega-1)**

**Listas del tablero**

| Lista | Qué contiene |
| :--- | :--- |
| 📥 Backlog / Por Hacer | Casos de uso y tareas técnicas pendientes. |
| ⚙️ En Proceso | Trabajo en desarrollo. |
| 🔍 En Revisión / Code Review | Trabajo terminado a la espera de revisión del equipo. |
| ✅ Completado | Trabajo implementado y revisado. |

**Etiquetas:** `Caso de Uso`, `Comprador`, `Vendedor` e `Infraestructura`.

**Convención de códigos:** cada tarjeta de caso de uso se llama `[CU-xx] Nombre` y usa el mismo código de la sección 9 de este README (por ejemplo, `[CU-22] Aplicar Descuento a Producto`). Las tareas técnicas llevan un prefijo propio, como `[PERSISTENCIA]`, `[DTO]`, `[Dominio]` o `[Doc]`.
