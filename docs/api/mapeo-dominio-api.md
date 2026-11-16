# Mapeo Dominio -> API

Cada operación del dominio y el endpoint REST que la expone.

**Rol** indica quién puede llamar al endpoint: `Público` (sin token), `COMPRADOR`, `VENDEDOR` o `Cualquiera` (cualquier sesión con token válido). El id del comprador o del vendedor **siempre sale del token JWT**; nunca se envía en el body, en la URL ni como parámetro.

## Cuentas y autenticación

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Usuario.registrar(...)` | POST | `/api/usuarios` | Público | 201 Created |
| `Usuario.actualizarPerfil(...)` | PUT | `/api/usuarios/me/perfil` | COMPRADOR | 200 OK |
| `Usuario.definirContrasena(...)` | PUT | `/api/usuarios/me/contrasena` | COMPRADOR | 204 No Content |
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
| Subir imagen del producto (Cloudinary) | POST | `/api/productos/{id}/imagen` | VENDEDOR | Por confirmar en Swagger |
| `Producto.pausar()` / `activar()` | PUT | `/api/productos/{id}/visibilidad` | VENDEDOR | 200 OK |
| `Producto.eliminar(...)` | DELETE | `/api/productos/{id}` | VENDEDOR | 204 No Content |
| Buscar productos (paginado) | GET | `/api/productos?texto=&categoria=&gama=&precioMin=&precioMax=&soloPreventa=&page=0&size=20` | Cualquiera | 200 OK |
| Consultar mis productos (activos y pausados) | GET | `/api/productos/mios` | VENDEDOR | 200 OK |
| `Producto.aplicarDescuento(...)` | POST | `/api/descuentos` | VENDEDOR | 200 OK |
| Quitar descuento inmediato | DELETE | `/api/descuentos/{productoId}` | VENDEDOR | 200 OK |
| `Promocion.programar(...)` | POST | `/api/promociones` | VENDEDOR | 201 Created |
| Consultar mis promociones | GET | `/api/promociones` | VENDEDOR | 200 OK |
| `Promocion.reprogramar(...)` | PUT | `/api/promociones/{id}` | VENDEDOR | 200 OK |
| Quitar promoción | DELETE | `/api/promociones/{id}` | VENDEDOR | 204 No Content |
| `Preventa.programar(...)` | POST | `/api/preventas` | VENDEDOR | 201 Created |
| Consultar preventas futuras | GET | `/api/preventas` | Cualquiera | 200 OK |

## Compras

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Compra.realizar(...)` | POST | `/api/compras` | COMPRADOR | 201 Created |
| `Compra.confirmar()` | PUT | `/api/compras/{id}/confirmar` | COMPRADOR | 200 OK |
| `Compra.cancelar()` | PUT | `/api/compras/{id}/cancelar` | COMPRADOR | 200 OK |
| `Compra.solicitarReembolso(...)` | PUT | `/api/compras/{id}/reembolso` | COMPRADOR | 200 OK |
| Consultar historial de compras | GET | `/api/compras` | COMPRADOR | 200 OK |
| Crear pedido (carrito) con cupón opcional | POST | `/api/pedidos` | COMPRADOR | 201 Created |
| Confirmar pedido o combo | PUT | `/api/pedidos/{id}/confirmar` | COMPRADOR | 200 OK |
| Cancelar pedido o combo | PUT | `/api/pedidos/{id}/cancelar` | COMPRADOR | 200 OK |
| Reembolsar pedido o combo completo | PUT | `/api/pedidos/{id}/reembolso` | COMPRADOR | 200 OK |
| Comprar combo (cupón opcional) | POST | `/api/combos/{id}/comprar` | COMPRADOR | 201 Created |

## Cupones

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Cupon.crear(...)` | POST | `/api/cupones` | VENDEDOR | 201 Created |
| Consultar mis cupones | GET | `/api/cupones` | VENDEDOR | 200 OK |
| Vista previa de un cupón (no consume usos) | GET | `/api/cupones/{codigo}` | COMPRADOR | 200 OK |

El cupón no se aplica con un endpoint propio: se envía como `codigoCupon` en `POST /api/pedidos` o `POST /api/combos/{id}/comprar` y se consume ahí. `PUT /api/compras/{id}/cupon` ya no existe.

## Combos, wishlist, notificaciones, reseñas y puntos

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| Consultar combos (solo con todos sus productos visibles) | GET | `/api/combos` | Cualquiera | 200 OK |
| Consultar mis combos | GET | `/api/combos/mios` | VENDEDOR | 200 OK |
| `Combo.crear(...)` | POST | `/api/combos` | VENDEDOR | 201 Created |
| `Combo.modificar(...)` | PUT | `/api/combos/{id}` | VENDEDOR | 200 OK |
| Eliminar combo | DELETE | `/api/combos/{id}` | VENDEDOR | 204 No Content |
| `Combo.agregarProducto(...)` | PUT | `/api/combos/{id}/productos/{productoId}` | VENDEDOR | 200 OK |
| `Combo.quitarProducto(...)` | DELETE | `/api/combos/{id}/productos/{productoId}` | VENDEDOR | 200 OK |
| Consultar wishlist | GET | `/api/wishlists` | COMPRADOR | 200 OK |
| `Wishlist.agregarProducto(...)` | POST | `/api/wishlists` | COMPRADOR | 201 Created |
| `Wishlist.quitarProducto(...)` | DELETE | `/api/wishlists/{productoId}` | COMPRADOR | 200 OK |
| Consultar notificaciones | GET | `/api/notificaciones` | COMPRADOR | 200 OK |
| `Notificacion.marcarComoLeida()` | PUT | `/api/notificaciones/{id}/leida` | COMPRADOR | 200 OK |
| `Resena.crear(...)` | POST | `/api/resenas` | COMPRADOR | 201 Created |
| `Usuario.redimirPuntos(...)` | POST | `/api/puntos/redimir` | COMPRADOR | 200 OK |
| Consultar historial de puntos | GET | `/api/puntos/historial` | COMPRADOR | 200 OK |

## Regalos y metas de recompensa

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Regalo.crear(...)` | POST | `/api/regalos` | VENDEDOR | 201 Created |
| Regalos disponibles para elegir | GET | `/api/regalos/disponibles` | COMPRADOR | 200 OK |
| Mis regalos | GET | `/api/regalos/mios` | COMPRADOR | 200 OK |
| Progreso de compras hacia el siguiente regalo | GET | `/api/regalos/progreso` | COMPRADOR | 200 OK |
| Regalos elegidos por enviar | GET | `/api/regalos/por-enviar` | VENDEDOR | 200 OK |
| `Regalo.elegir(...)` (asignar por meta) | PUT | `/api/regalos/{id}/asignar` | COMPRADOR | 200 OK |
| `Regalo.enviar()` (solo el vendedor dueño) | PUT | `/api/regalos/{id}/enviar` | VENDEDOR | 200 OK |
| `Regalo.confirmarEntrega(...)` | PUT | `/api/regalos/{id}/entrega` | COMPRADOR | 200 OK |
| Consultar metas activas | GET | `/api/metas-recompensa` | Cualquiera | 200 OK |
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
| `POST /api/productos/{id}/imagen` | `multipart/form-data` con el campo `archivo` (PNG o JPEG, máx. 5 MB); no es JSON |
| `PUT /api/productos/{id}/visibilidad` | `activo` (`true` activa, `false` pausa) |
| `GET /api/productos` | Filtros opcionales por query: `texto`, `categoria`, `gama`, `precioMin`, `precioMax`, `soloPreventa`. Paginación: `page` (desde 0, por defecto 0) y `size` (1–100, por defecto 20) |
| `POST /api/compras` | `productoId`, `cantidad` |
| `POST /api/wishlists` | `productoId` |
| `POST /api/pedidos` | `items` (1 a 50 líneas con `productoId` y `cantidad` > 0, sin productos repetidos) y `codigoCupon` opcional (máx. 50 caracteres) |
| `POST /api/combos/{id}/comprar` | `cantidad` y `codigoCupon` opcional |
| `POST /api/cupones` | `codigo` (máx. 50), `porcentaje` (> 0 y < 100), `vence` (fecha `AAAA-MM-DD`, no pasada) y `usosMaximos` (> 0) |
| `POST /api/combos` | `nombre`, `descripcion`, `descuento` y `productoIds` (2 o más) |
| `PUT /api/combos/{id}` | `nombre`, `descripcion`, `descuento` |
| `POST /api/regalos` | `productoId` o `comboId` (exactamente uno, propio del vendedor) |
| `POST /api/preventas` | `productoId`, `fechaLanzamiento` (futura) y `descripcion` |
| `POST /api/promociones` | `productoId`, `porcentaje`, `desde`, `hasta` |
| `PUT /api/promociones/{id}` | `porcentaje`, `desde`, `hasta` |
| `POST /api/descuentos` | `productoId`, `porcentaje` |
| `POST /api/metas-recompensa` | `comprasRequeridas`, `productoRegaloId` |
| `PUT /api/metas-recompensa/{id}/umbral` | `nuevoUmbral` |
| `POST /api/puntos/redimir` | `cantidad` |
| `PUT /api/regalos/{id}/asignar` | Sin cuerpo |
| `PUT /api/regalos/{id}/enviar` | Sin cuerpo |
| `PUT /api/regalos/{id}/entrega` | Sin cuerpo (solo el destinatario confirma) |
| `GET /api/reportes/stock-bajo` | Query opcional `umbral` (5 por defecto) |
| `GET /api/reportes/mas-vendidos` | Query opcionales `vendedorId` (filtro de consulta) y `top` (10 por defecto) |

## Notas

- **Autenticación:** `POST /api/auth/login` devuelve un JWT (HS256, 2 horas por defecto) con el id, el correo y el rol. Todas las rutas `/api/**` exigen `Authorization: Bearer {JWT}`, salvo `/api/auth/**`, `POST /api/usuarios` y `POST /api/vendedores`.
- **Roles:** `COMPRADOR` y `VENDEDOR`. Un rol que no corresponde a la ruta recibe 403. Los controllers toman el id del `Authentication`; un `usuarioId` o `vendedorId` enviado en el body o en la URL se ignora.
- **Contraseñas:** mínimo 8 caracteres, una mayúscula y un número; se guardan con BCrypt.
- **CORS:** `/api/**` acepta peticiones desde `localhost:4200` y `localhost:3000` (y sus equivalentes en `127.0.0.1`); el preflight `OPTIONS` se responde sin exigir token.
- **Usuarios de demo** para `POST /api/auth/login`:
  - Compradores: `jose.demo@correo.com` / `JoseGamer2026` y `ana.demo@correo.com` / `AnaGamer2026`.
  - Vendedor: `demo@gamerzone.com` / `GamerZone2026`.
- **Swagger:** en `http://localhost:8080/swagger-ui.html`, ejecuta el login, copia el token y pégalo en el botón **Authorize** (sin el prefijo `Bearer`).
- **Búsqueda paginada:** `GET /api/productos` solo devuelve productos visibles (no eliminados ni pausados). `texto` busca en nombre y descripción sin distinguir mayúsculas; `soloPreventa=true` devuelve los productos cuya preventa aún no ha llegado a su fecha de lanzamiento. La respuesta trae `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`. Los filtros y la paginación se ejecutan en la base de datos (JPQL). Una página o un tamaño fuera de rango responde 422.
- **Operaciones de compra:** las líneas de un pedido o combo comparten `pedidoId`; confirmación, cancelación y reembolso se realizan sobre todas las líneas. El historial muestra el pedido como `producto1 + producto2` y conserva el nombre del combo comprado.
- **Cupones:** un cupón consume **un uso por pedido o combo** (no uno por línea) y cada comprador puede usarlo **una sola vez** (`422` "Ya usaste este cupón"). Con `vendedorId` (cupones creados por un vendedor) solo rebaja los productos de ese vendedor y, si ninguna línea del pedido le aplica, el pedido se rechaza con `422`; sin dueño (como `VERANO26`) aplica a todo. Vencido, agotado o ya usado → `422`; inexistente → `404`; en ambos casos el stock queda intacto. `PedidoResponse` y `ComprarComboResponse` traen `cuponAplicado`.
- **Cupón para combos:** se calcula sobre el precio unitario ya rebajado por el combo, no se combina con promociones de producto y consume un uso por combo.
- **Pedido atómico:** `POST /api/pedidos` valida todas las líneas (producto comprable, preventa, vendedor activo, exclusividad, stock) antes de descontar nada; si una falla, no cambia ningún producto ni se guarda ninguna compra. `POST /api/compras/{id}/confirmar|cancelar|reembolso` sobre una línea de pedido o combo responde `422`: se opera con `/api/pedidos/{id}/…`.
- **Wishlist y combos:** `GET /api/wishlists` devuelve `[]` sin wishlist y omite productos no visibles; `GET /api/combos` omite combos con algún producto pausado o eliminado. `DELETE /api/combos/{id}` responde `422` si el combo tiene regalos pendientes.
- **Preventas:** `GET /api/preventas` lista solo las de lanzamiento posterior a hoy, ordenadas por fecha ascendente (el mismo día del lanzamiento el producto ya se puede comprar).
- **Regalos:** se elige un regalo por cada 5 compras `COMPLETADA`; `GET /api/regalos/progreso` devuelve `comprasCompletadas`, `comprasPorRegalo`, `regalosAsignados`, `regalosDisponibles` y `comprasParaSiguiente`. Enviar un regalo que no es de un producto o combo del vendedor responde `422`. Las metas de `/api/metas-recompensa` son informativas y no cambian esa regla.
- **Notificaciones:** `PUT /api/notificaciones/{id}/leida` responde `404` si no existe y `422` si es de otro usuario.
- **Ranking de más vendidos:** agrupa en la base de datos las unidades vendidas de compras `COMPLETADA` (`JOIN`, `GROUP BY` y `SUM`), ordena por unidades (desempate por nombre) e incluye la calificación promedio de cada producto. Sin `vendedorId` lista todo el marketplace; los productos sin ventas no aparecen.
- **Errores** (cuerpo JSON `{timestamp, status, mensaje}`):
  | Código | Cuándo |
  |---|---|
  | 400 | Validación de datos del request (`@Valid`) o JSON inválido |
  | 401 | Token ausente, inválido o expirado; credenciales incorrectas en el login |
  | 403 | El rol del token no puede usar la ruta |
  | 404 | El recurso no existe |
  | 409 | Duplicidad (correo, documento, código de cupón, preventa, promoción solapada, reseña o valoración repetida, producto repetido en wishlist o combo), violación de integridad o conflicto de concurrencia |
  | 422 | Una regla de negocio del dominio se violó (stock insuficiente, reembolso fuera de plazo, transición de estado no permitida, paginación fuera de rango, etc.) |

- **No existe un rol ADMIN.** Las operaciones de administración se resolvieron así: las metas de recompensa son de `VENDEDOR` (cualquier vendedor puede gestionar cualquier meta) y el envío de regalos es de `VENDEDOR`, solo del dueño del producto o combo; `mas-vendidos` y el reporte global están abiertos a cualquier sesión autenticada.
- Los parámetros de los endpoints que no están en la tabla de cuerpos están en Swagger.
- **Imágenes:** el producto se publica primero y la imagen se sube después a `POST /api/productos/{id}/imagen`. El backend firma la petición a Cloudinary y guarda solo la URL HTTPS; requiere configurar `CLOUDINARY_*` (ver README, sección 11). Sin imagen, o si la carga falla, el catálogo usa la ilustración de respaldo.
