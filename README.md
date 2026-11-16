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
3. [Endpoints principales](#3-endpoints-principales)
4. [Correcciones de la Entrega 1](#4-correcciones-de-la-entrega-1)
5. [¿Quién compra y por qué?](#5-quién-compra-y-por-qué)
6. [¿Qué hace único a sus vendedores?](#6-qué-hace-único-a-sus-vendedores)
7. [Lenguaje Ubicuo (DDD)](#7-lenguaje-ubicuo-ddd)
8. [Reglas de Negocio e Invariantes del Dominio](#8-reglas-de-negocio-e-invariantes-del-dominio)
9. [Casos de Uso del Sistema (60 Casos de Uso)](#9-casos-de-uso-del-sistema-60-casos-de-uso)
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

Cupón de demo: `VERANO26` (15%).

### Cómo autenticarse en Swagger

1. `POST /api/auth/login` con `{"correo": "...", "contrasena": "..."}`.
2. Copiar el valor de `token` (sin comillas).
3. Botón **Authorize** (arriba a la derecha) → pegar el token → confirmar.

Sin token la API responde `401`; con el token del rol equivocado, `403`.

### Flujo de punta a punta sugerido

1. Login como **vendedor** (`demo@gamerzone.com`) → `POST /api/productos` (publicar un producto).
2. Login como **comprador** (`jose.demo@correo.com`) → `POST /api/compras` (comprar el producto).
3. `PUT /api/compras/{id}/confirmar` → la compra queda `COMPLETADA`. (Alternativa con carrito: `POST /api/pedidos` con varios ítems y `PUT /api/pedidos/{id}/confirmar`.)
4. `POST /api/resenas` (reseñar) y `POST /api/valoraciones-vendedor` (valorar al vendedor).
5. **Error de negocio a propósito:** con `ana.demo@correo.com`, `PUT /api/compras/dddddddd-dddd-dddd-dddd-ddddddddddd2/reembolso`. Esa compra de demo supera las 48 horas del plazo, por lo que la regla del dominio rechaza el reembolso con `422`.

---

## 2.1. Frontend, carrito y pantallas

El frontend es una aplicación **Angular 21** (componentes standalone, signals y formularios reactivos) que consume la API del backend.

**Requisitos:** Node 20 o superior y el backend corriendo en `http://localhost:8080` (sección 2).

```powershell
cd frontend
npm install
npm start                      # abre http://localhost:4200
```

La URL de la API está en `frontend/src/app/config.ts` (`API_URL`); hay que cambiarla ahí al desplegar. Para entrar usa los usuarios de prueba de la sección 2: la pantalla de login tiene botones que rellenan las cuentas de demo.

### Carrito de compra simulado (pedido todo o nada)

Un comprador añade productos desde el modal del catálogo con **Añadir al carrito** (también puede usar **Comprar** para una compra directa, que viaja como un pedido de un solo ítem). En `/carrito` ajusta las cantidades, que nunca superan el stock (un producto exclusivo admite una sola unidad), quita ítems, escribe un cupón opcional y pulsa **Pagar (simulado)**.

No hay pasarela de pago: pagar significa crear y confirmar **un único pedido** con `POST /api/pedidos` (todas las líneas del carrito, más el `codigoCupon` si lo hay) y luego `PUT /api/pedidos/{id}/confirmar`. El backend valida todas las líneas antes de tocar el stock: si una sola falla (por ejemplo, sin stock) no se descuenta nada, no se guarda ninguna compra, el cupón no se consume y el carrito queda intacto con el mensaje del backend. Si falla la confirmación, el pedido se cancela para devolver el stock. Mientras se procesa, el botón queda bloqueado ("Procesando…") y un segundo clic se ignora.

**Cupón:** el campo de cupón del carrito y de la compra directa muestra una vista previa del descuento (`GET /api/cupones/{codigo}`, que no gasta ningún uso). El cupón consume **un solo uso por pedido**, sin importar cuántos ítems tenga, y cada comprador puede usar un mismo cupón una sola vez. Si el cupón tiene dueño (lo creó un vendedor), solo descuenta los ítems de ese vendedor; `VERANO26` es global.

**Limitaciones:**

- El carrito vive solo en el navegador: no persiste al recargar la página y se vacía al cerrar sesión o cambiar de usuario.
- El precio del carrito es el que tenía el producto al añadirlo; el precio definitivo lo fija el backend al reservar.

### Publicar un producto (formulario reactivo)

En **Mi tienda → Publicar**, el vendedor llena un formulario reactivo que valida antes de enviar: nombre y descripción obligatorios, precio mayor a cero y stock entero de cero o más. Si hay errores no se envía ninguna petición y cada campo muestra su aviso. Al enviar, el botón queda bloqueado ("Publicando…") y, si el backend rechaza la petición, se muestra su mensaje (por ejemplo `403` si el rol no corresponde o `422` si el vendedor está dado de baja). Una vista previa muestra cómo quedará la tarjeta en el catálogo.

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
*Pago detenido por falta de stock (`422`): el ítem se conserva y el mensaje viene del backend.*

![Formulario de publicación](docs/pantallas/05-publicar-formulario.png)
*Formulario reactivo de publicación con vista previa de la tarjeta.*

![Errores de validación](docs/pantallas/06-publicar-error-validacion.png)
*Avisos por campo con el formulario vacío; no se envía ninguna petición.*

![Mis compras](docs/pantallas/07-mis-compras.png)
*Historial de compras con sus estados y acciones (reseña, valoración, reembolso).*

### Pantallas por módulo

Además de las capturas anteriores, la aplicación tiene estas pantallas; cada una consume los endpoints de la sección 3.

| Ruta o pestaña | Rol | Qué hace |
| :--- | :--- | :--- |
| `/favoritos` | `COMPRADOR` | Lista de deseos. El corazón de las tarjetas del catálogo se carga del servidor, así que sigue marcado después de recargar la página. |
| `/notificaciones` y campana de la barra | `COMPRADOR` | Avisos de descuento de los productos favoritos. La campana muestra el contador de no leídas; al abrir una se marca como leída. |
| `/combos` | Cualquier sesión | Combos con sus productos, precio con y sin descuento y ahorro. El comprador elige la cantidad y compra; el vendedor no ve el botón de compra. |
| `/regalos` | `COMPRADOR` | Progreso hacia el próximo regalo (una vez cada 5 compras completadas), regalos disponibles, elegir un regalo, confirmar la recepción y metas de recompensa activas como información. |
| `/vendedor` → **Cupones** | `VENDEDOR` | Crea cupones propios (código, porcentaje, vencimiento, usos máximos) y muestra cuántos usos lleva cada uno. |
| `/vendedor` → **Combos** | `VENDEDOR` | Crea combos con dos o más productos propios y agrega o quita productos (mínimo dos). |
| `/vendedor` → **Preventas** | `VENDEDOR` | Programa una preventa con fecha de lanzamiento futura y lista los próximos lanzamientos. En el catálogo, la tarjeta del producto muestra la insignia "Preventa · lanzamiento dd/mm" y Comprar y Añadir al carrito quedan deshabilitados hasta la fecha. |
| `/vendedor` → **Regalos** | `VENDEDOR` | Ofrece un producto o combo propio como regalo y marca como enviados los que los compradores eligieron. |
| `/vendedor` → **Metas** | `VENDEDOR` | Crea metas de recompensa, cambia su umbral o las desactiva. |
| `/vendedor` → **Resumen** | `VENDEDOR` | Ventas e ingresos, métricas de reseñas, productos más vendidos y alertas de **stock bajo**. |
| `/vendedor` → **Mis productos** | `VENDEDOR` | Lista todos sus productos, incluidos los pausados (`GET /api/productos/mios`). Permite editar, pausar o activar, eliminar y quitar el descuento inmediato; con un descuento activo no se puede cambiar el precio. |
| `/vendedor` → **Promociones** | `VENDEDOR` | Aplica un descuento inmediato y gestiona las promociones de temporada: programar, cambiar porcentaje y fechas, o quitarlas (no pueden solaparse en el mismo producto). |
| `/vendedor` → **Publicar** | `VENDEDOR` | Formulario reactivo de publicación (ver más arriba). |
| `/` | Público | Portada con una escena 3D (teclado, nudo de neón y partículas) y accesos al catálogo, al registro y a la sesión. |
| `/registro` | Público | Crea una cuenta de comprador ("Quiero comprar") o de vendedor ("Quiero vender", con tipo y documento). |
| `/perfil` | `COMPRADOR` | Edita nombre y correo, y cambia la contraseña (la nueva debe tener 8 o más caracteres y ser distinta de la actual). |

### Ensayo de punta a punta

El recorrido completo (publicar, comprar con carrito, errores de negocio y de seguridad) está en [`docs/ensayo-punta-a-punta.md`](docs/ensayo-punta-a-punta.md), con el resultado esperado de cada paso. La colección de Postman equivalente para la API está en [`docs/postman/flujo-completo.json`](docs/postman/flujo-completo.json) (carpetas A a E; la E recorre wishlist, notificaciones, promociones, combos, preventas, regalos y metas).

---

## 3. Endpoints principales

| Método y ruta | Rol | Qué hace |
| :--- | :--- | :--- |
| `POST /api/auth/login` | Público | Inicia sesión y devuelve el JWT |
| `POST /api/usuarios` | Público | Registra un comprador |
| `POST /api/vendedores` | Público | Registra un vendedor |
| `PUT /api/usuarios/me/perfil` · `PUT /api/usuarios/me/contrasena` | `COMPRADOR` | Edita nombre y correo · cambia la contraseña |
| `PUT /api/vendedores/me/baja` | `VENDEDOR` | Da de baja al vendedor (baja lógica) |
| `GET /api/productos` | Autenticado | Catálogo paginado con búsqueda por texto y filtros |
| `POST /api/productos` | `VENDEDOR` | Publica un producto |
| `PUT /api/productos/{id}/precio` | `VENDEDOR` | Modifica el precio |
| `PUT /api/productos/{id}/informacion` | `VENDEDOR` | Edita la información del producto |
| `PUT /api/productos/{id}/visibilidad` | `VENDEDOR` | Pausa o activa la publicación |
| `DELETE /api/productos/{id}` | `VENDEDOR` | Eliminación lógica de su producto |
| `GET /api/productos/mios` | `VENDEDOR` | Todos sus productos, activos y pausados (no los eliminados), con su promoción vigente |
| `POST /api/descuentos` · `DELETE /api/descuentos/{productoId}` | `VENDEDOR` | Aplica o retira el descuento inmediato de su producto (aplicarlo notifica a las wishlists) |
| `POST /api/promociones` · `GET /api/promociones` | `VENDEDOR` | Programa una promoción de temporada · lista las de sus productos |
| `PUT /api/promociones/{id}` · `DELETE /api/promociones/{id}` | `VENDEDOR` | Cambia el porcentaje y la vigencia · la quita |
| `POST /api/compras` | `COMPRADOR` | Realiza una compra |
| `GET /api/compras` | `COMPRADOR` | Historial de compras |
| `PUT /api/compras/{id}/confirmar` | `COMPRADOR` | Confirma la compra |
| `PUT /api/compras/{id}/cancelar` | `COMPRADOR` | Cancela la compra |
| `PUT /api/compras/{id}/reembolso` | `COMPRADOR` | Solicita reembolso (48 h, sin descarga) |
| `POST /api/pedidos` | `COMPRADOR` | Crea una operación para todo el carrito (cupón opcional) |
| `PUT /api/pedidos/{id}/confirmar` | `COMPRADOR` | Confirma el pedido completo |
| `PUT /api/pedidos/{id}/cancelar` | `COMPRADOR` | Cancela el pedido completo y restituye todo el stock |
| `PUT /api/pedidos/{id}/reembolso` | `COMPRADOR` | Reembolsa el pedido completo |
| `POST /api/resenas` | `COMPRADOR` | Reseña una compra completada |
| `POST /api/valoraciones-vendedor` | `COMPRADOR` | Valora al vendedor |
| `POST /api/puntos/redimir` · `GET /api/puntos/historial` | `COMPRADOR` | Redime puntos · historial de puntos ganados y redimidos |
| `GET /api/wishlists` | `COMPRADOR` | Productos favoritos visibles del usuario (`[]` si no tiene wishlist) |
| `POST /api/wishlists` · `DELETE /api/wishlists/{productoId}` | `COMPRADOR` | Añade o quita de la wishlist |
| `GET /api/notificaciones` | `COMPRADOR` | Notificaciones del usuario, la más reciente primero |
| `PUT /api/notificaciones/{id}/leida` | `COMPRADOR` | Marca una notificación como leída (`422` si es de otro usuario) |
| `POST /api/cupones` | `VENDEDOR` | Crea un cupón propio (`409` si el código ya existe) |
| `GET /api/cupones` | `VENDEDOR` | Cupones del vendedor con sus usos |
| `GET /api/cupones/{codigo}` | `COMPRADOR` | Vista previa de un cupón, sin gastar un uso |
| `GET /api/combos` | Autenticado | Combos con todos sus productos visibles, con precio con y sin descuento |
| `GET /api/combos/mios` | `VENDEDOR` | Todos los combos del vendedor |
| `POST /api/combos` · `POST /api/combos/{id}/comprar` | `VENDEDOR` · `COMPRADOR` | Crea un combo · compra un combo como una sola operación |
| `PUT /api/combos/{id}` · `DELETE /api/combos/{id}` | `VENDEDOR` | Modifica nombre, descripción y descuento · elimina el combo (`422` si tiene regalos pendientes) |
| `PUT /api/combos/{id}/productos/{productoId}` · `DELETE ...` | `VENDEDOR` | Agrega o quita un producto del combo (mínimo dos) |
| `GET /api/preventas` | Autenticado | Preventas con lanzamiento futuro, por fecha |
| `POST /api/preventas` | `VENDEDOR` | Programa una preventa |
| `POST /api/regalos` | `VENDEDOR` | Ofrece un producto o combo propio como regalo |
| `GET /api/regalos/disponibles` · `/mios` · `/progreso` | `COMPRADOR` | Regalos por elegir, regalos del comprador y progreso de compras |
| `GET /api/regalos/por-enviar` | `VENDEDOR` | Regalos elegidos de sus productos o combos |
| `PUT /api/regalos/{id}/asignar` · `/enviar` · `/entrega` | `COMPRADOR` · `VENDEDOR` (dueño) · `COMPRADOR` | Elegir, enviar y confirmar la entrega de un regalo |
| `GET /api/metas-recompensa` | Autenticado | Metas activas por umbral ascendente |
| `POST /api/metas-recompensa` · `PUT /api/metas-recompensa/{id}/umbral` · `PUT /api/metas-recompensa/{id}/desactivar` | `VENDEDOR` | Gestiona las metas de recompensa |
| `GET /api/reportes/ventas` | `VENDEDOR` | Reporte de ventas del vendedor |
| `GET /api/reportes/mas-vendidos` | Autenticado | Ranking de más vendidos (JOIN + GROUP BY) |
| `GET /api/reportes/stock-bajo` · `GET /api/reportes/metricas-resenas` | `VENDEDOR` | Alertas de inventario crítico · métricas de sus reseñas |
| `GET /api/reportes/global` | Autenticado | Consolidado del marketplace |

La lista completa está en Swagger y en [`docs/api/mapeo-dominio-api.md`](docs/api/mapeo-dominio-api.md).

---

## 4. Correcciones de la Entrega 1

| Observación que nos dieron | Qué cambiamos | Dónde se ve |
| :--- | :--- | :--- |
| Las entidades del dominio tenían el **constructor público**: cualquiera podía crear un objeto sin pasar por las validaciones y dejarlo en estado inválido. | Los constructores pasaron a `private`. La creación se hace solo con métodos de fábrica estáticos que validan las invariantes (`Producto.publicar`, `Usuario.registrar`, `Compra.realizar`, `Wishlist.crear`, `Vendedor.registrar`, `Combo.crear`, `Preventa.programar`...). | Rama `fix/corrections` (PR #18). Commits del 11/10: `e61f066` (Producto), `04e75f5` (Usuario), `0b14a99` (Compra), `5376af7` (Wishlist), `e1b7def` (Vendedor), `40ba7ed` (Combo), `8c2f707` (Preventa). Código actual en `domain/entity/`. |

**Por qué es mejor:** la regla de negocio queda en un solo punto, dentro del propio objeto del dominio, y es imposible saltársela desde un caso de uso o un controller.

**Consecuencia para la persistencia:** como el constructor es privado, el mapper JPA no puede crear la entidad directamente. Por eso se agregó un método `reconstruir(...)` en cada entidad (commits del 19 al 21/10), que rehidrata el objeto desde la base de datos sin abrir el constructor al resto del código.

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
| **Wishlist** | Lista de deseos personal para seguimiento de precios, promociones y disponibilidad de stock. |
| **Notificación** | Alerta que recibe el usuario cuando un producto de su Wishlist baja de precio (descuento). |
| **Combo** | Agrupación promocional de 2 o más productos comercializados bajo una sola entidad con descuento. |
| **Usuario** | Persona registrada que compra, reseña, acumula y redime puntos. Se llama *Usuario* en todo el código. |
| **Vendedor** | Entidad comercial (Persona Natural o Empresa) registrada para publicar y vender productos. |
| **Puntos de Fidelización** | Unidades de beneficio otorgadas a compradores tras publicar reseñas de compras completadas. |
| **Regalo** | Beneficio otorgado por el sistema al usuario tras alcanzar metas de compra acumuladas. |
| **Pedido** | Operación que agrupa las líneas del carrito (o de un combo) bajo un mismo `pedidoId`; se crea, confirma, cancela o reembolsa completa. |
| **Cupón** | Código de descuento porcentual con vencimiento y usos máximos. Consume un uso por pedido y cada comprador lo usa una sola vez; puede tener un vendedor dueño. |
| **Promoción de temporada** | Descuento porcentual de un producto con fechas de inicio y fin. No cambia el precio base y no se solapa con otra del mismo producto. |
| **Descuento inmediato** | Rebaja porcentual que el vendedor aplica ya a un producto; guarda el precio original, notifica a las wishlists y se puede quitar para volver al precio anterior. |
| **Meta de recompensa** | Umbral de compras completadas ligado a un producto de regalo. Es informativa: la asignación de regalos usa la regla fija de 5 compras. |
| **Exclusividad** | Condición de un producto especial: cada usuario puede comprarlo una sola vez y de a una unidad. |
| **Unidades** | Cantidad disponible de un producto para su venta (nunca negativa; cero significa agotado). |
 
---

## 8. Reglas de Negocio e Invariantes del Dominio

| # | Regla | Dónde se garantiza | Estado |
|---|---|---|---|
| 1 | Un producto nunca tiene stock negativo; se restituye al cancelar o reembolsar. | `Producto.reducirStock` / `aumentarStock` | ✅ |
| 2 | Solo se reseña una compra `COMPLETADA`; una reseña por usuario y producto. | `Compra.registrarResena`, `Resena.crear`, `SubirResenaUseCase` | ✅ |
| 3 | No se elimina un producto con compras activas (`PENDIENTE` o `COMPLETADA`); la eliminación es lógica. | `Producto.eliminar`, `EliminarProductoUseCase` | ✅ |
| 4 | Un producto en preventa no se compra antes de su fecha de lanzamiento. | `Preventa.validarAcceso`, `RealizarCompraUseCase` | ✅ |
| 5 | Un combo tiene dos o más productos distintos y su descuento es menor a 100%. | `Combo.crear`, `agregarProducto`, `quitarProducto`, `CrearComboUseCase`, `ModificarProductosComboUseCase` | ✅ |
| 6 | Los puntos se asignan solo después de reseñar, y una sola vez. | `Compra.asignarPuntos`, `SubirResenaUseCase` | ✅ |
| 7 | Un producto no se repite en la Wishlist ni se agregan productos eliminados. | `Wishlist.agregarProducto`, `AnadirAWishlistUseCase` | ✅ |
| 8 | Solo se notifica a quienes tienen el producto en su Wishlist, y solo cuando hay descuento. | `Producto.aplicarDescuento`, `AplicarDescuentoUseCase`, `NotificarDescuentoWishlistUseCase` | ✅ |
| 9 | Un producto exclusivo se compra una sola vez por usuario y de a una unidad. | `Producto.validarCompraExclusiva` | ✅ |
| 10 | Un regalo solo se elige para quien no posee el producto, y sigue DISPONIBLE → ELEGIDO → ENVIADO → ENTREGADO; solo lo envía el vendedor dueño del producto o combo. | `Regalo`, `EnviarRegaloUseCase`, `ConfirmarEntregaRegaloUseCase` | ✅ |
| 11 | Un pedido se crea todo o nada: se validan todas las líneas antes de descontar stock. | `CrearPedidoUseCase` | ✅ |
| 12 | Un cupón consume un uso por pedido (no uno por ítem) y cada comprador lo usa una sola vez. | `Cupon.registrarUso`, `CrearPedidoUseCase`, `CuponRepository.fueUsadoPor` | ✅ |

### Reglas adicionales que el código garantiza

- El reembolso solo procede si la compra está `COMPLETADA`, dentro de **48 horas**, sin descarga y sin puntos asignados.
- `REEMBOLSADA`, `CANCELADA` y `EXPIRADA` son estados finales de una compra.
- La fecha de la compra y los ids los genera el servidor, no el cliente.
- Un vendedor dado de baja no puede publicar y sus productos no se pueden comprar.
- Un producto pausado o eliminado no se puede comprar; solo el vendedor dueño modifica, edita, pausa o programa promociones de su producto.
- Una `Promocion` tiene vigencia (fechas inclusive), no se solapa con otra del mismo producto y no cambia el precio base.
- Un cupón es válido hasta su fecha de vencimiento y mientras tenga usos; se aplica una sola vez al pedido o combo, solo a compras `PENDIENTE` (el precio se congela al confirmar). Consume **un uso por pedido**, sin importar cuántas líneas tenga, y cada comprador puede usarlo una sola vez. Un cupón sin dueño es global (`VERANO26`); uno con dueño solo descuenta los ítems de su vendedor y se rechaza (`422`) si ningún ítem del pedido es suyo.
- Un pedido es atómico: con lista vacía, cantidad menor o igual a cero, el mismo producto repetido, o un cupón vencido, agotado, ya usado o inexistente, no se descuenta stock, no se guarda ninguna compra y el cupón no se consume.
- Un producto con descuento inmediato activo no cambia de precio (hay que quitar el descuento antes); aplicar un descuento a un producto que ya tiene uno, o quitarlo si no tiene, responde `422`. Quitar el descuento no notifica a las wishlists.
- Modificar una promoción de temporada exige las mismas reglas que al crearla (porcentaje válido, fechas coherentes, sin solaparse con otra del producto), solo la modifica el vendedor dueño y no sobre productos eliminados. Quitarla no cambia el precio base.
- Un combo solo lo modifica o elimina su vendedor dueño (`422` si no lo es); no se elimina mientras exista un regalo sin cerrar que lo referencie. Su descuento va de 0 a menos de 100.
- `GET /api/productos/mios` lista los productos del vendedor, activos y pausados, sin los eliminados; el catálogo público solo muestra los visibles.
- Las líneas de un pedido no se confirman ni se cancelan por separado (`422`): esas acciones se hacen sobre el pedido completo. El reembolso sí puede pedirse por línea o por pedido.
- Solo el vendedor dueño del producto o combo de un regalo puede enviarlo (`422` si no lo es), y solo ve en "por enviar" los regalos elegidos de lo suyo.
- Una notificación solo la marca como leída su dueño (`422` si es de otro usuario; `404` si no existe).
- Una preventa cuyo lanzamiento es hoy ya no cuenta como futura: `GET /api/preventas` solo lista las de fecha estrictamente posterior, igual que el filtro del catálogo.
- `GET /api/wishlists` devuelve `[]` si el usuario no tiene wishlist y omite los productos eliminados o pausados.
- En combos, el cupón se aplica después del descuento del combo y no se acumula con promociones individuales; los cupones de vendedor solo aplican a combos de ese vendedor.
- Comprar un combo valida todos sus productos antes de descontar stock y crea una única operación con nombre e historial propios; confirmar, cancelar o reembolsar afecta todas sus líneas.
- “Mis compras” presenta cada pedido o combo como una operación; al expandirla muestra sus productos, mientras el pago, la cancelación y el reembolso se hacen sobre el grupo completo.
- Una `MetaRecompensa` exige un umbral de compras positivo y se desactiva una sola vez (nunca se borra). Las metas se gestionan y se muestran, pero no gobiernan la asignación de regalos, que usa la regla fija de 5 compras (`Usuario.COMPRAS_POR_REGALO`).
- El vendedor se valora una vez por compra completada. La comisión del marketplace es 5% de los ingresos completados.
- Las contraseñas (de compradores y vendedores) exigen 8+ caracteres, mayúscula y número, y solo se guarda su hash (BCrypt). La sesión usa JWT HS256 con el rol en el token (`COMPRADOR` o `VENDEDOR`) y `/api/**` exige token salvo `/api/auth/**` y los `POST` de registro.
- El id del comprador o vendedor sale siempre del token, nunca del body ni de la URL; cada ruta valida el rol con `@PreAuthorize`.
- La búsqueda de catálogo es paginada (`size` entre 1 y 100) y solo lista productos visibles (no eliminados ni pausados).
- Los errores de dominio responden 422, la duplicidad 409 y las validaciones de datos 400; 401 y 403 devuelven un cuerpo JSON.
---

## 9. Casos de Uso del Sistema (60 Casos de Uso)

**Leyenda:** ✅ Implementado (caso de uso y pruebas) · 🟡 Parcial · ⏳ Especificado, pendiente de implementar.

### 📈 Resumen de avance

| Módulo | ✅ Implementados | 🟡 Parciales | ⏳ Especificados | Total |
| :--- | :---: | :---: | :---: | :---: |
| Compras y Checkout | 12 | 0 | 0 | 12 |
| Productos y Catálogo | 16 | 0 | 0 | 16 |
| Combos Promocionales | 5 | 0 | 0 | 5 |
| Reseñas, Puntos y Recompensas | 7 | 0 | 0 | 7 |
| Lista de Deseos (Wishlist) y Notificaciones | 6 | 0 | 0 | 6 |
| Usuarios, Vendedores y Autenticación | 5 | 0 | 0 | 5 |
| Consultas, Métricas y Reportes | 9 | 0 | 0 | 9 |
| **Total** | **60** | **0** | **0** | **60** |

### 🛒 Módulo 1: Compras y Checkout
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-01` | **Realizar Compra** | ✅ Implementado | Comprador | Registra la orden `PENDIENTE`, reserva stock y valida producto no eliminado, usuario y vendedor activos, exclusividad y fecha de preventa, usando la fecha del servidor (`RealizarCompraUseCase`). |
| `CU-09` | **Confirmar Compra** | ✅ Implementado | Comprador / Sistema | Valida el pago y pasa la orden a `COMPLETADA` (`ConfirmarCompraUseCase`). |
| `CU-12` | **Cancelar Compra** | ✅ Implementado | Comprador | Pasa la orden a `CANCELADA` y devuelve automáticamente el stock retenido al producto (`CancelarCompraUseCase`). |
| `CU-13` | **Solicitar Reembolso** | ✅ Implementado | Comprador | Solicita reembolso dentro de 48 horas, sin descarga y sin puntos asignados; pasa a `REEMBOLSADA` y restituye el stock (`SolicitarReembolsoUseCase`). |
| `CU-16` | **Comprar Combo** | ✅ Implementado | Comprador | Reserva todos los productos como una sola operación, con descuento de combo y cupón opcional; cancelar, confirmar y reembolsar actúan sobre el combo completo (`ComprarComboUseCase`). |
| `CU-17` | **Aplicar Cupón de Descuento** | ✅ Implementado | Comprador | Aplica un código al pedido del carrito o combo; en combos se descuenta sobre el precio ya rebajado y no se acumula con promociones de producto. Consume un uso por operación y cada comprador puede usar un mismo cupón una sola vez (`CrearPedidoUseCase`, `ComprarComboUseCase`). |
| `CU-38` | **Crear Pedido (Carrito)** | ✅ Implementado | Comprador | Convierte el carrito en un pedido: una compra `PENDIENTE` por línea con el mismo `pedidoId`. Valida todas las líneas antes de descontar stock (todo o nada), rechaza lista vacía, cantidades no positivas y productos repetidos, y aplica la promoción vigente y el cupón opcional (`CrearPedidoUseCase`, `ItemPedido`). |
| `CU-39` | **Confirmar Pedido** | ✅ Implementado | Comprador | Pasa todas las líneas del pedido a `COMPLETADA` en una sola operación; si alguna es ajena o no está `PENDIENTE`, no cambia ninguna (`ConfirmarPedidoUseCase`). |
| `CU-40` | **Cancelar Pedido** | ✅ Implementado | Comprador | Cancela todas las líneas y restituye el stock; si alguna no está `PENDIENTE`, no cambia ninguna (`CancelarPedidoUseCase`). |
| `CU-41` | **Reembolsar Pedido** | ✅ Implementado | Comprador | Reembolsa todas las líneas de un pedido o combo y restituye el stock, tras validar todas antes de modificar ninguna (`SolicitarReembolsoPedidoUseCase`). |
| `CU-49` | **Crear Cupón** | ✅ Implementado | Vendedor | Crea un cupón propio con código único (se guarda en mayúsculas), porcentaje entre 0 y 100, vencimiento no pasado y usos máximos; solo descuenta los productos del vendedor (`CrearCuponUseCase`). |
| `CU-50` | **Consultar Cupones** | ✅ Implementado | Vendedor / Comprador | El vendedor lista sus cupones con los usos; el comprador ve una vista previa por código sin gastar un uso (`404` si no existe; `422` si está vencido, agotado o ya lo usó) (`ConsultarCuponesVendedorUseCase`, `ConsultarCuponUseCase`). |

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
| `CU-51` | **Consultar Preventas** | ✅ Implementado | Comprador / Vendedor | Lista las preventas con lanzamiento estrictamente posterior a hoy, ordenadas por fecha (`ConsultarPreventasUseCase`). |
| `CU-53` | **Consultar Mis Productos** | ✅ Implementado | Vendedor | Lista todos los productos del vendedor, activos y pausados (solo excluye los eliminados), por nombre y con su promoción vigente; es el listado del panel para poder reactivar lo pausado (`ConsultarMisProductosUseCase`, `GET /api/productos/mios`). |
| `CU-54` | **Consultar Promociones del Vendedor** | ✅ Implementado | Vendedor | Promociones de temporada (pasadas, vigentes y futuras) de sus productos no eliminados, de la más reciente a la más antigua (`ConsultarPromocionesDelVendedorUseCase`). |
| `CU-55` | **Consultar Promociones Vigentes** | ✅ Implementado | Sistema | Devuelve la promoción vigente hoy de cada producto que se muestra (catálogo, favoritos), para que el comprador vea el mismo precio que se le cobrará al reservar (`ConsultarPromocionesVigentesUseCase`). |
| `CU-56` | **Modificar Promoción** | ✅ Implementado | Vendedor | Cambia porcentaje y vigencia de una promoción de temporada con las mismas reglas que al crearla (porcentaje válido, fechas coherentes y sin solaparse con otra del producto); solo el vendedor dueño y con el producto no eliminado (`ModificarPromocionUseCase`). |
| `CU-57` | **Quitar Promoción** | ✅ Implementado | Vendedor | Elimina una promoción vigente, futura o vencida; el precio base del producto no cambia porque la promoción nunca lo tocó (`QuitarPromocionUseCase`). |
| `CU-58` | **Quitar Descuento** | ✅ Implementado | Vendedor | Retira el descuento inmediato y el producto recupera el precio que tenía antes; `422` si no tiene descuento activo. No notifica a las wishlists, porque solo se avisa cuando el precio baja (`QuitarDescuentoUseCase`, `DELETE /api/descuentos/{productoId}`). |

### 🎁 Módulo 3: Combos Promocionales
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-07` | **Crear Combo Gamer** | ✅ Implementado | Vendedor | Agrupa 2 o más productos del vendedor, sin duplicados y con descuento menor a 100% (`CrearComboUseCase`, `Combo.crear`). |
| `CU-23` | **Modificar Productos de Combo** | ✅ Implementado | Vendedor | Añade o remueve productos garantizando el mínimo de 2 unidades (`ModificarProductosComboUseCase`). |
| `CU-42` | **Consultar Combos** | ✅ Implementado | Comprador / Vendedor | El comprador ve los combos cuyos productos están todos visibles; el vendedor ve todos los suyos, con el precio con y sin descuento (`ConsultarCombosUseCase`). |
| `CU-59` | **Modificar Combo** | ✅ Implementado | Vendedor | El vendedor dueño cambia nombre, descripción y descuento (de 0 a menos de 100) de su combo (`ModificarComboUseCase`). |
| `CU-60` | **Eliminar Combo** | ✅ Implementado | Vendedor | Solo el vendedor dueño; se rechaza (`422`) si hay un regalo sin cerrar (disponible, elegido o enviado) que lo referencie (`EliminarComboUseCase`). |

### ⭐ Módulo 4: Reseñas, Puntos y Recompensas
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-02` | **Subir Reseña y Asignar Puntos** | ✅ Implementado | Comprador | Publica reseña sobre compra completada, valida 1 reseña por usuario/producto y asigna puntos (`SubirResenaUseCase`). |
| `CU-08` | **Redimir Puntos por Beneficios** | ✅ Implementado | Comprador | Canjea puntos acumulados con validación de saldo positivo (`RedimirPuntosUseCase`, `Usuario.redimirPuntos`). |
| `CU-10` | **Asignar Regalo por Meta** | ✅ Implementado | Comprador / Sistema | Asigna un regalo disponible al usuario por cada 5 compras `COMPLETADA`, siempre que no posea ya el producto (o alguno de los del combo) (`AsignarRegaloPorMetaUseCase`). |
| `CU-24` | **Enviar / Confirmar Entrega Regalo** | ✅ Implementado | Vendedor / Comprador | Administra el cambio de estado del regalo: el envío exige que esté elegido y la entrega la confirma solo el destinatario, y el envío solo lo hace el vendedor dueño del producto o combo (`EnviarRegaloUseCase`, `ConfirmarEntregaRegaloUseCase`). |
| `CU-43` | **Crear Regalo** | ✅ Implementado | Vendedor | Ofrece un producto o un combo propio como regalo (exactamente uno de los dos, no eliminado) (`CrearRegaloUseCase`). |
| `CU-44` | **Consultar Regalos** | ✅ Implementado | Comprador / Vendedor | Regalos disponibles, los que el comprador eligió y los elegidos por enviar de cada vendedor (`ConsultarRegalosDisponiblesUseCase`, `ConsultarMisRegalosUseCase`, `ConsultarRegalosPorEnviarUseCase`). |
| `CU-45` | **Consultar Progreso de Regalos** | ✅ Implementado | Comprador | Compras completadas, regalos ganados y compras que faltan para el siguiente, con la misma regla de 5 compras de CU-10 (`ConsultarProgresoRegalosUseCase`). |

### ❤️ Módulo 5: Lista de Deseos (Wishlist) y Notificaciones
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-03` | **Añadir a Wishlist** | ✅ Implementado | Comprador | Guarda productos en la wishlist evitando duplicados y rechazando eliminados (`AnadirAWishlistUseCase`). |
| `CU-14` | **Notificar Descuento en Wishlist** | ✅ Implementado | Sistema | Genera notificaciones por descuento para quienes tienen el producto en su Wishlist (`NotificarDescuentoWishlistUseCase`). |
| `CU-15` | **Quitar de Wishlist** | ✅ Implementado | Comprador | Elimina un producto específico de la lista de deseos (`QuitarDeWishlistUseCase`). |
| `CU-46` | **Consultar Wishlist** | ✅ Implementado | Comprador | Devuelve los productos favoritos visibles en el orden de la lista; sin wishlist devuelve lista vacía y omite los productos eliminados o pausados (`ConsultarWishlistUseCase`). |
| `CU-47` | **Consultar Notificaciones** | ✅ Implementado | Comprador | Notificaciones del usuario, la más reciente primero (`ConsultarNotificacionesUseCase`). |
| `CU-48` | **Marcar Notificación como Leída** | ✅ Implementado | Comprador | `404` si no existe y `422` si pertenece a otro usuario (`MarcarNotificacionLeidaUseCase`, `Notificacion.asegurarPerteneceA`). |

### 👤 Módulo 6: Usuarios, Vendedores y Autenticación
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-11` | **Registrar Usuario / Vendedor** | ✅ Implementado | Público | Registro de compradores (correo único) y de vendedores (documento único, cédula o NIT) (`RegistrarUsuarioUseCase`, `RegistrarVendedorUseCase`). |
| `CU-25` | **Dar de Baja Vendedor** | ✅ Implementado | Vendedor | Baja lógica del vendedor conservando la historia comercial (`DarDeBajaVendedorUseCase`). |
| `CU-26` | **Editar Perfil y Seguridad** | ✅ Implementado | Usuario | Modificación de nombre, correo y contraseña. (`EditarPerfilUseCase`, `CambiarContrasenaUseCase`). |
| `CU-27` | **Iniciar Sesión (JWT)** | ✅ Implementado | Todos | Autenticación con token JWT seguro para el acceso a las APIs. (`IniciarSesionUseCase`, `AuthController`). |
| `CU-28` | **Valorar Vendedor** | ✅ Implementado | Comprador | Calificación directa del servicio y atención brindada por el vendedor. (`ValorarVendedorUseCase`). |

### 📊 Módulo 7: Consultas, Métricas y Reportes
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-29` | **Reporte de Ventas por Vendedor** | ✅ Implementado | Vendedor | Balance de ingresos, unidades vendidas y comisiones. (`ReporteVentasVendedorUseCase`). |
| `CU-30` | **Reporte de Productos Más Vendidos** | ✅ Implementado | Cualquier sesión | Escalafón de productos con mayor volumen de venta y calificación, agrupado y ordenado en la base de datos. (`ReporteMasVendidosUseCase`). |
| `CU-31` | **Consultar Historial de Compras** | ✅ Implementado | Comprador | Muestra las compras del usuario, la más reciente primero (`ConsultarHistorialComprasUseCase`). |
| `CU-32` | **Consultar Historial de Puntos** | ✅ Implementado | Comprador | Detalle de saldo de puntos ganados y redimidos. (`ConsultarHistorialPuntosUseCase`). |
| `CU-33` | **Gestionar Metas de Recompensas** | ✅ Implementado | Vendedor (no existe rol ADMIN) | Configura umbrales de compras ligados a un producto regalo. Las metas se muestran pero no cambian la regla de asignación de CU-10. (`GestionarMetasRecompensaUseCase`). |
| `CU-34` | **Alertas de Stock Bajo** | ✅ Implementado | Vendedor | Reporte de inventario crítico o agotado para reabastecimiento. (`AlertasStockBajoUseCase`). |
| `CU-35` | **Métricas de Reseñas** | ✅ Implementado | Vendedor | Dashboard de satisfacción del cliente y calificaciones. (`MetricasResenasUseCase`). |
| `CU-36` | **Reporte Global del Marketplace** | ✅ Implementado | Cualquier sesión | Consolidado ejecutivo del volumen total de operaciones. (`ReporteGlobalUseCase`). |
| `CU-52` | **Consultar Metas de Recompensa** | ✅ Implementado | Cualquier sesión | Metas activas ordenadas por umbral ascendente; son informativas (`ConsultarMetasUseCase`). |
 
---

## 10. Arquitectura del Software (Clean Architecture)

El backend en Java 21 / Spring Boot implementa **Clean Architecture** estructurado en capas desacopladas.

### Raíces de agregado para la sustentación

- **Producto**: raíz del agregado de catálogo e inventario. Protege invariantes como stock no negativo, baja lógica y visibilidad. El vendedor se referencia por `vendedorId`; no se carga como parte de este agregado.
- **Compra**: raíz del agregado de compra. Controla cantidad, precio unitario congelado y transiciones de estado, además de las reglas de reseña, puntos y reembolso. Referencia `usuarioId` y `productoId` sin contener esos agregados. Las líneas de un pedido o combo comparten un `pedidoId` opcional; el pedido no es un agregado aparte, sino la operación que los casos de uso `CrearPedido`, `ConfirmarPedido` y `CancelarPedido` aplican a todas sus líneas a la vez.
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
│   ├── usecase/             # Lógica de Orquestación (60 casos de uso; `ItemPedido` es el record con el que el carrito entra a CrearPedido)
│   │   ├── Compras:   RealizarCompra, ConfirmarCompra, CancelarCompra, SolicitarReembolso, ConsultarHistorialCompras
│   │   ├── Pedidos:   CrearPedido, ConfirmarPedido, CancelarPedido, SolicitarReembolsoPedido
│   │   ├── Productos: PublicarProducto, EliminarProducto, AplicarDescuento, ProgramarPreventa, ConsultarPreventas,
│   │   │              ConsultarMisProductos, QuitarDescuento, ConsultarPromocionesDelVendedor, ConsultarPromocionesVigentes,
│   │   │              ModificarPromocion, QuitarPromocion
│   │   ├── Combos:    CrearCombo, ModificarProductosCombo, ConsultarCombos, ModificarCombo, EliminarCombo
│   │   ├── Reseñas y recompensas: SubirResena, RedimirPuntos, CrearRegalo, EnviarRegalo, ConfirmarEntregaRegalo, AsignarRegaloPorMeta,
│   │   │                          ConsultarRegalosDisponibles, ConsultarMisRegalos, ConsultarRegalosPorEnviar, ConsultarProgresoRegalos
│   │   ├── Wishlist:  AnadirAWishlist, QuitarDeWishlist, ConsultarWishlist, NotificarDescuentoWishlist, ConsultarNotificaciones, MarcarNotificacionLeida
│   │   ├── Cuentas:   RegistrarUsuario, RegistrarVendedor, DarDeBajaVendedor, IniciarSesion, EditarPerfil, CambiarContrasena, ValorarVendedor
│   │   ├── Catálogo:  ModificarProducto, EditarInformacionProducto, CambiarVisibilidadProducto, BuscarProductos, ConfigurarDescuentoPromocional
│   │   ├── Promos:    CrearCupon, ConsultarCuponesVendedor, ConsultarCupon, ComprarCombo, GestionarMetasRecompensa, ConsultarMetas, ConsultarHistorialPuntos
│   │   └── Reportes:  ReporteVentasVendedor, ReporteMasVendidos, AlertasStockBajo, MetricasResenas, ReporteGlobal
│   ├── port/out/            # Puertos de salida de consulta (ReporteMasVendidosPort)
│   └── dto/                 # Data Transfer Objects (request y response)
└── infrastructure/          # Capa de Infraestructura (Spring Boot / Controller / Persistence)
    ├── rest/                # Controladores REST (uno por recurso), mappers y GlobalExceptionHandler
    ├── persistence/         # Repositorios JPA (H2 / MariaDB) y en memoria, con sus mappers de persistencia
    ├── security/            # JwtEmisorToken, JwtAuthenticationFilter, BCryptHashContrasena y RespuestaErrorSeguridad
    └── config/              # ClockConfig, SecurityConfig (JWT y CORS) y OpenApiConfig
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

### Limitaciones

- **No existe un rol ADMIN.** Las metas de recompensa quedan restringidas a `VENDEDOR`: cualquier vendedor puede crearlas, cambiarlas o desactivarlas, aunque conceptualmente son de administración. El envío de regalos (CU-24) sí exige ser el vendedor dueño del producto o combo.
- Las metas de recompensa se gestionan y se muestran, pero **no gobiernan** la asignación de regalos: `AsignarRegaloPorMetaUseCase` usa la regla fija de 5 compras (`Usuario.COMPRAS_POR_REGALO`).
- `GET /api/reportes/mas-vendidos` y `GET /api/reportes/global` están abiertos a cualquier sesión autenticada (son estadísticas del marketplace).
- El carrito del frontend no persiste (ver la sección [2.1](#21-frontend-carrito-y-pantallas)); el pago sí es todo o nada porque viaja como un único pedido.
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
| 6. Pedido todo o nada, cupón de un solo uso y módulos de consulta | `CrearPedidoUseCaseTest`, `ConfirmarPedidoUseCaseTest`, `CancelarPedidoUseCaseTest`, `PedidoControllerTest`, `CuponControllerTest`, `ConsultarMetasUseCaseTest`, `MetaRecompensaControllerTest`, `integracion/FlujoCompletoIntegracionTest` |

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

El frontend le muestra al usuario el `mensaje` que manda el backend, así que fijamos con pruebas de integración los códigos y mensajes que consume. En `FlujoCompletoIntegracionTest`: comprar más unidades que el stock responde `422` con "No hay suficiente stock disponible" y el stock del producto no cambia en la base; comprar sin token responde `401`; y publicar con el nombre vacío y un precio negativo responde `400` con un mensaje que nombra el campo. Los mismos casos están en la carpeta B de la colección de Postman (`docs/postman/flujo-completo.json`). En el frontend, las pruebas con Vitest cubren `mensajeDeError`, el carrito (incluido el pedido único, el cupón y el doble clic) y el formulario de publicación (validación, botón bloqueado y errores del servidor).

### 6. Pedido todo o nada, cupón y módulos de consulta

Al pasar el carrito a un pedido, las pruebas fijan las garantías que antes no existían. En `CrearPedidoUseCaseTest`: un pedido cuyo último ítem no tiene stock no cambia ningún stock ni guarda ninguna compra; un cupón de tres ítems consume exactamente un uso; un cupón de otro vendedor, vencido, agotado, ya usado o inexistente rechaza el pedido sin tocar el stock; y la lista vacía, la cantidad cero o el producto repetido se rechazan. `ConfirmarPedidoUseCaseTest` y `CancelarPedidoUseCaseTest` comprueban que un pedido ajeno o con una línea que no está `PENDIENTE` no cambia ninguna línea. En `FlujoCompletoIntegracionTest` se recorren esos casos de punta a punta (`pedidoConVERANO26ConsumeUnSoloUso`, `pedidoConUnItemSinStockRetorna422YNoDescuentaStock`, `vendedorCreaCuponYElCompradorLoUsaEnUnPedido`) y la carpeta D de la colección de Postman repite el recorrido contra la API.

Los módulos que solo consultaban datos ganaron sus casos de uso de lectura, cada uno con una prueba con repositorios en memoria y otra de controller (`401` sin token y, cuando corresponde, `403` y errores de negocio): `ConsultarWishlistUseCaseTest`, `ConsultarNotificacionesUseCaseTest`, `MarcarNotificacionLeidaUseCaseTest`, `ConsultarCombosUseCaseTest`, `ConsultarPreventasUseCaseTest`, `ConsultarMetasUseCaseTest` y las pruebas de regalos y cupones. En el frontend, cada módulo (wishlist, notificaciones, cupones, combos, preventas, regalos y metas) tiene pruebas con Vitest de su servicio o de su componente.

Los casos de uso de promociones, descuentos, combos y productos del vendedor tienen su prueba con repositorios en memoria: `ConsultarMisProductosUseCaseTest`, `ConsultarPromocionesDelVendedorUseCaseTest`, `ConsultarPromocionesVigentesUseCaseTest`, `ModificarPromocionUseCaseTest`, `QuitarPromocionUseCaseTest`, `QuitarDescuentoUseCaseTest`, `ModificarComboUseCaseTest` y `EliminarComboUseCaseTest`.

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
