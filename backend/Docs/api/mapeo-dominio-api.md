# Mapeo Dominio -> API

Cada operación del dominio y el endpoint REST que la expone.

**Rol** indica quién puede llamar al endpoint: `Público` (sin token), `COMPRADOR`, `VENDEDOR` o `Cualquiera` (cualquier sesión con token válido). El id del comprador o del vendedor **siempre sale del token JWT**; nunca se envía en el body, en la URL ni como parámetro.

## Cuentas y autenticación

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Usuario.registrar(...)` | POST | `/api/usuarios` | Público | 201 Created |
| `Usuario.actualizarPerfil(...)` | PUT | `/api/usuarios/me/perfil` | COMPRADOR | 200 OK |
| `Usuario.definirContrasena(...)` | PUT | `/api/usuarios/me/contrasena` | COMPRADOR | 200 OK |
| `IniciarSesionUseCase` | POST | `/api/auth/login` | Público | 200 OK |
| `Vendedor.registrar(...)` | POST | `/api/vendedores` | Público | 201 Created |
| `Vendedor.darDeBaja()` | PUT | `/api/vendedores/me/baja` | VENDEDOR | 200 OK |
| `ValoracionVendedor.crear(...)` | POST | `/api/valoraciones-vendedor` | COMPRADOR | 201 Created |

## Productos y catálogo

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Producto.publicar(...)` | POST | `/api/productos` | VENDEDOR | 201 Created |
| `Producto.cambiarPrecio(...)` | PUT | `/api/productos/{id}/precio` | VENDEDOR | 200 OK |
| `Producto.editarInformacion(...)` | PUT | `/api/productos/{id}/informacion` | VENDEDOR | 200 OK |
| `Producto.pausar()` / `activar()` | PUT | `/api/productos/{id}/visibilidad` | VENDEDOR | 200 OK |
| `Producto.eliminar(...)` | DELETE | `/api/productos/{id}` | VENDEDOR | 204 No Content |
| Buscar productos paginados | GET | `/api/productos?texto=&categoria=&gama=&precioMin=&precioMax=&soloPreventa=&page=0&size=20` | Cualquiera | 200 OK |
| `Producto.aplicarDescuento(...)` | POST | `/api/descuentos` | VENDEDOR | 200 OK |
| `Promocion.programar(...)` | POST | `/api/promociones` | VENDEDOR | 201 Created |
| `Preventa.programar(...)` | POST | `/api/preventas` | VENDEDOR | 201 Created |

## Compras

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Compra.realizar(...)` | POST | `/api/compras` | COMPRADOR | 201 Created |
| `Compra.confirmar()` | PUT | `/api/compras/{id}/confirmar` | COMPRADOR | 200 OK |
| `Compra.cancelar()` | PUT | `/api/compras/{id}/cancelar` | COMPRADOR | 200 OK |
| `Compra.solicitarReembolso(...)` | PUT | `/api/compras/{id}/reembolso` | COMPRADOR | 200 OK |
| `Compra.aplicarCupon(...)` | PUT | `/api/compras/{id}/cupon` | COMPRADOR | 200 OK |
| Consultar historial de compras | GET | `/api/compras` | COMPRADOR | 200 OK |
| Comprar combo | POST | `/api/combos/{id}/comprar` | COMPRADOR | 201 Created |

## Combos, wishlist, reseñas y puntos

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Combo.crear(...)` | POST | `/api/combos` | VENDEDOR | 201 Created |
| `Combo.agregarProducto(...)` | PUT | `/api/combos/{id}/productos/{productoId}` | VENDEDOR | 200 OK |
| `Combo.quitarProducto(...)` | DELETE | `/api/combos/{id}/productos/{productoId}` | VENDEDOR | 200 OK |
| `Wishlist.agregarProducto(...)` | POST | `/api/wishlists` | COMPRADOR | 201 Created |
| `Wishlist.quitarProducto(...)` | DELETE | `/api/wishlists/{productoId}` | COMPRADOR | 200 OK |
| `Resena.crear(...)` | POST | `/api/resenas` | COMPRADOR | 201 Created |
| `Usuario.redimirPuntos(...)` | POST | `/api/puntos/redimir` | COMPRADOR | 200 OK |
| Consultar historial de puntos | GET | `/api/puntos/historial` | COMPRADOR | 200 OK |

## Regalos y metas de recompensa

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Regalo.elegir(...)` (asignar por meta) | PUT | `/api/regalos/{id}/asignar` | COMPRADOR | 200 OK |
| `Regalo.enviar()` | PUT | `/api/regalos/{id}/enviar` | VENDEDOR | 200 OK |
| `Regalo.confirmarEntrega(...)` | PUT | `/api/regalos/{id}/entrega` | COMPRADOR | 200 OK |
| `MetaRecompensa.crear(...)` | POST | `/api/metas-recompensa` | VENDEDOR | 201 Created |
| `MetaRecompensa.actualizarUmbral(...)` | PUT | `/api/metas-recompensa/{id}/umbral` | VENDEDOR | 200 OK |
| `MetaRecompensa.desactivar()` | PUT | `/api/metas-recompensa/{id}/desactivar` | VENDEDOR | 200 OK |

## Reportes

| Caso de uso | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| CU-29 Ventas por vendedor | GET | `/api/reportes/ventas` | VENDEDOR | 200 OK |
| CU-30 Productos más vendidos | GET | `/api/reportes/mas-vendidos?vendedorId=&top=` | Cualquiera | 200 OK |
| CU-34 Alertas de stock bajo | GET | `/api/reportes/stock-bajo?umbral=` | VENDEDOR | 200 OK |
| CU-35 Métricas de reseñas | GET | `/api/reportes/metricas-resenas` | VENDEDOR | 200 OK |
| CU-36 Reporte global | GET | `/api/reportes/global` | Cualquiera | 200 OK |

## Cuerpos y parámetros

| Endpoint | Cuerpo (JSON) o parámetros |
|---|---|
| `POST /api/usuarios` | `nombre`, `correo`, `contrasena` |
| `PUT /api/usuarios/me/perfil` | `nombre`, `correo` |
| `PUT /api/usuarios/me/contrasena` | `contrasenaActual`, `contrasenaNueva` |
| `POST /api/auth/login` | `correo`, `contrasena` (devuelve `token`) |
| `POST /api/vendedores` | `nombre`, `correo`, `tipo`, `documento`, `contrasena` |
| `PUT /api/vendedores/me/baja` | Sin cuerpo |
| `POST /api/productos` | `nombre`, `descripcion`, `precio`, `stock`, `gama`, `exclusividad` |
| `PUT /api/productos/{id}/precio` | `nuevoPrecio` |
| `PUT /api/productos/{id}/informacion` | `nombre`, `descripcion` |
| `PUT /api/productos/{id}/visibilidad` | `activo` (`true` activa, `false` pausa) |
| `GET /api/productos` | Filtros query opcionales: `texto`, `categoria`, `gama`, `precioMin`, `precioMax`, `soloPreventa`. Paginación: `page` (desde 0), `size` (1–100) |
| `POST /api/compras` | `productoId`, `cantidad` |
| `POST /api/wishlists` | `productoId` |
| `PUT /api/compras/{id}/cupon` | `codigo` |
| `POST /api/combos/{id}/comprar` | `cantidad` |
| `PUT /api/regalos/{id}/asignar` | Sin cuerpo |
| `PUT /api/regalos/{id}/enviar` | Sin cuerpo |
| `PUT /api/regalos/{id}/entrega` | Sin cuerpo |
| `GET /api/reportes/stock-bajo` | Query opcional `umbral` (5 por defecto) |
| `GET /api/reportes/mas-vendidos` | Query opcionales `vendedorId` (filtro de consulta) y `top` (10 por defecto) |

## Notas y Reglas de Negocio

- **Autenticación:** `POST /api/auth/login` devuelve un JWT (HS256, 2 horas por defecto) con el id, el correo y el rol. Todas las rutas `/api/**` exigen `Authorization: Bearer {JWT}`, salvo `/api/auth/**`, `POST /api/usuarios` y `POST /api/vendedores`.
- **Roles:** `COMPRADOR` y `VENDEDOR`. Un rol que no corresponde a la ruta recibe 403. Los controladores toman el id directamente del `Authentication` (Security Context); cualquier `usuarioId` o `vendedorId` enviado en el body o en la URL se ignora.
- **Contraseñas:** Mínimo 8 caracteres, una mayúscula y un número; se guardan con BCrypt.
- **Usuarios de demo** (`POST /api/auth/login`):
    - Compradores: `jose.demo@correo.com` / `JoseGamer2026` y `ana.demo@correo.com` / `AnaGamer2026`.
    - Vendedor: `demo@gamerzone.com` / `GamerZone2026`.
- **Swagger:** En `http://localhost:8080/swagger-ui.html`, ejecuta el login, copia el token y pégalo en el botón **Authorize** (sin el prefijo `Bearer`).
- **Paginación y Búsqueda:** La búsqueda responde con `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`. Los filtros y la paginación se ejecutan de manera optimizada en JPQL.
- **Ranking:** El reporte de más vendidos agrupa en base de datos las unidades vendidas de compras `COMPLETADA` mediante `JOIN`, `GROUP BY` y `SUM`.
- **Administración:** No existe un rol ADMIN. Las metas de recompensa y envíos de regalos son del `VENDEDOR`; los reportes globales están abiertos a cualquier sesión autenticada.

**Errores** (cuerpo JSON estándar `{timestamp, status, mensaje}`):

| Código | Cuándo |
|---|---|
| 400 | Validación de datos del request (`@Valid`), JSON inválido, o reglas del dominio no cumplidas |
| 401 | Token ausente, inválido, expirado, o credenciales incorrectas en el login |
| 403 | El rol del token no tiene permisos para usar la ruta |
| 404 | El recurso solicitado no existe |
| 409 | Duplicidad (correo, documento, preventa, promoción solapada, reseña, valoración, producto repetido en wishlist/combo), violación de integridad o conflicto de concurrencia |
| 422 | Una regla de negocio del dominio se violó (stock insuficiente, reembolso fuera de plazo, transición de estado no permitida, etc.) |