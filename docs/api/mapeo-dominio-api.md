# Mapeo Dominio -> API

Cada operación del dominio y el endpoint REST que la expone.

**Rol** indica quién puede llamar al endpoint: `Público` (sin token), `COMPRADOR`, `VENDEDOR` o `Cualquiera` (cualquier sesión con token válido, sea de comprador o de vendedor). El id del comprador o del vendedor **siempre sale del token JWT**; nunca se envía en el body, en la URL ni como parámetro.

## Cuentas y autenticación

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Usuario.registrar(...)` | POST | `/api/usuarios` | Público | 201 Created |
| Consultar mi perfil (comprador) | GET | `/api/usuarios/me` | COMPRADOR | 200 OK |
| `Usuario.actualizarPerfil(...)` | PUT | `/api/usuarios/me/perfil` | COMPRADOR | 200 OK |
| `Usuario.cambiarContrasena(...)` | PUT | `/api/usuarios/me/contrasena` | COMPRADOR | 204 No Content |
| `Usuario.darDeBaja()` | PUT | `/api/usuarios/me/baja` | COMPRADOR | 200 OK |
| `IniciarSesionUseCase` | POST | `/api/auth/login` | Público | 200 OK |
| `SolicitarRecuperacionContrasenaUseCase` | POST | `/api/auth/olvide-contrasena` | Público | 200 OK |
| `RestablecerContrasenaUseCase` | POST | `/api/auth/restablecer-contrasena` | Público | 200 OK |
| `Vendedor.registrar(...)` | POST | `/api/vendedores` | Público | 201 Created |
| Consultar mi perfil (vendedor) | GET | `/api/vendedores/me` | VENDEDOR | 200 OK |
| `Vendedor.actualizarPerfil(...)` | PUT | `/api/vendedores/me/perfil` | VENDEDOR | 200 OK |
| `Vendedor.cambiarContrasena(...)` | PUT | `/api/vendedores/me/contrasena` | VENDEDOR | 204 No Content |
| `Vendedor.darDeBaja()` | PUT | `/api/vendedores/me/baja` | VENDEDOR | 200 OK |
| `ValoracionVendedor.crear(...)` | POST | `/api/valoraciones-vendedor` | COMPRADOR | 201 Created |

## Productos y catálogo

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Producto.publicar(...)` | POST | `/api/productos` | VENDEDOR | 201 Created |
| `Producto.cambiarPrecio(...)` | PUT | `/api/productos/{id}/precio` | VENDEDOR | 200 OK |
| `Producto.editarInformacion(...)` | PUT | `/api/productos/{id}/informacion` | VENDEDOR | 200 OK |
| `Producto.establecerStock(...)` | PUT | `/api/productos/{id}/stock` | VENDEDOR | 200 OK |
| Subir imagen del producto (Cloudinary) | POST | `/api/productos/{id}/imagen` | VENDEDOR | 200 OK |
| `Producto.pausar()` / `activar()` | PUT | `/api/productos/{id}/visibilidad` | VENDEDOR | 200 OK |
| `Producto.eliminar(...)` | DELETE | `/api/productos/{id}` | VENDEDOR | 204 No Content |
| Buscar productos (paginado) | GET | `/api/productos?texto=&categoria=&gama=&precioMin=&precioMax=&soloPreventa=&page=0&size=20` | Público | 200 OK |
| Consultar mis productos (activos y pausados) | GET | `/api/productos/mios` | VENDEDOR | 200 OK |
| `Producto.aplicarDescuento(...)` | POST | `/api/descuentos` | VENDEDOR | 200 OK |
| Quitar descuento inmediato | DELETE | `/api/descuentos/{productoId}` | VENDEDOR | 200 OK |
| `Promocion.programar(...)` | POST | `/api/promociones` | VENDEDOR | 201 Created |
| Consultar mis promociones | GET | `/api/promociones` | VENDEDOR | 200 OK |
| `Promocion.reprogramar(...)` | PUT | `/api/promociones/{id}` | VENDEDOR | 200 OK |
| Quitar promoción | DELETE | `/api/promociones/{id}` | VENDEDOR | 204 No Content |
| `Preventa.programar(...)` | POST | `/api/preventas` | VENDEDOR | 201 Created |
| `Preventa.reprogramar(...)` | PUT | `/api/preventas/{id}` | VENDEDOR | 200 OK |
| Cancelar preventa (la elimina) | DELETE | `/api/preventas/{id}` | VENDEDOR | 204 No Content |
| Consultar preventas futuras | GET | `/api/preventas` | Público | 200 OK |

## Compras

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `Compra.realizar(...)` | POST | `/api/compras` | COMPRADOR | 201 Created |
| `Compra.confirmar()` | PUT | `/api/compras/{id}/confirmar` | COMPRADOR | 200 OK |
| `Compra.cancelar()` | PUT | `/api/compras/{id}/cancelar` | COMPRADOR | 200 OK |
| `Compra.solicitarReembolso(...)` | PUT | `/api/compras/{id}/reembolso` | COMPRADOR | 200 OK |
| `Compra.marcarDescargado()` (solo productos `DIGITAL`) | PUT | `/api/compras/{id}/descarga` | COMPRADOR | 200 OK |
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
| Consultar combos (solo con todos sus productos visibles) | GET | `/api/combos` | Público | 200 OK |
| Consultar mis combos | GET | `/api/combos/mios` | VENDEDOR | 200 OK |
| `Combo.crear(...)` | POST | `/api/combos` | VENDEDOR | 201 Created |
| `Combo.modificar(...)` | PUT | `/api/combos/{id}` | VENDEDOR | 200 OK |
| Eliminar combo | DELETE | `/api/combos/{id}` | VENDEDOR | 204 No Content |
| `Combo.agregarProducto(...)` | PUT | `/api/combos/{id}/productos/{productoId}` | VENDEDOR | 200 OK |
| `Combo.quitarProducto(...)` | DELETE | `/api/combos/{id}/productos/{productoId}` | VENDEDOR | 200 OK |
| Consultar wishlist | GET | `/api/wishlists` | COMPRADOR | 200 OK |
| `Wishlist.agregarProducto(...)` | POST | `/api/wishlists` | COMPRADOR | 201 Created |
| `Wishlist.quitarProducto(...)` | DELETE | `/api/wishlists/{productoId}` | COMPRADOR | 200 OK |
| Consultar notificaciones (compradores y vendedores) | GET | `/api/notificaciones` | Cualquiera | 200 OK |
| `Notificacion.marcarComoLeida()` | PUT | `/api/notificaciones/{id}/leida` | Cualquiera | 200 OK |
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
| `Regalo.cancelarRegalo()` (solo el vendedor dueño, antes de `ENTREGADO`) | PUT | `/api/regalos/{id}/cancelar` | VENDEDOR | 200 OK |
| `Regalo.confirmarEntrega(...)` | PUT | `/api/regalos/{id}/entrega` | COMPRADOR | 200 OK |
| Consultar metas activas | GET | `/api/metas-recompensa` | Cualquiera | 200 OK |
| `MetaRecompensa.crear(...)` | POST | `/api/metas-recompensa` | VENDEDOR | 201 Created |
| `MetaRecompensa.actualizarUmbral(...)` | PUT | `/api/metas-recompensa/{id}/umbral` | VENDEDOR | 200 OK |
| `MetaRecompensa.desactivar()` | PUT | `/api/metas-recompensa/{id}/desactivar` | VENDEDOR | 200 OK |

## Chat comprador–vendedor y asistente de IA

| Operación del dominio | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| `IniciarConversacionChatUseCase` | POST | `/api/chat/conversaciones` | COMPRADOR | 200 OK |
| `ListarConversacionesChatUseCase` | GET | `/api/chat/conversaciones` | COMPRADOR y VENDEDOR | 200 OK |
| `ConsultarHistorialChatUseCase` | GET | `/api/chat/conversaciones/{id}/mensajes` | COMPRADOR y VENDEDOR | 200 OK |
| `EnviarMensajeChatUseCase` | POST | `/api/chat/conversaciones/{id}/mensajes` | COMPRADOR y VENDEDOR | 201 Created |
| `MarcarLeidosChatUseCase` | PUT | `/api/chat/conversaciones/{id}/leidos` | COMPRADOR y VENDEDOR | 200 OK |
| `CanalChatEnVivo` (SSE) | GET | `/api/chat/stream` | COMPRADOR y VENDEDOR | 200 OK (`text/event-stream`) |
| `ChatAsistenteCompradorUseCase` | POST | `/api/asistente/comprador` | COMPRADOR | 200 OK |
| `ChatAsistenteVendedorUseCase` | POST | `/api/asistente/vendedor` | VENDEDOR | 200 OK |

## Reportes

| Caso de uso | Método HTTP | Endpoint | Rol | Respuesta exitosa |
|---|---|---|---|---|
| CU-29 Ventas por vendedor | GET | `/api/reportes/ventas` | VENDEDOR | 200 OK |
| CU-30 Productos más vendidos | GET | `/api/reportes/mas-vendidos?vendedorId=&top=` | Cualquiera | 200 OK |
| CU-34 Alertas de stock bajo | GET | `/api/reportes/stock-bajo?umbral=` | VENDEDOR | 200 OK |
| CU-35 Métricas de reseñas | GET | `/api/reportes/metricas-resenas` | VENDEDOR | 200 OK |
| CU-36 Reporte global | GET | `/api/reportes/global` | Cualquiera | 200 OK |

## Procesos automáticos (sin endpoint)

Estos casos de uso los dispara un `@Scheduled` y no se pueden invocar por HTTP.

| Caso de uso | Disparador | Qué hace |
|---|---|---|
| `ExpirarComprasPendientesUseCase` (`ComprasScheduler`) | Cada 5 minutos (`0 */5 * * * *`); no corre al arrancar | Pasa a `EXPIRADA` toda compra `PENDIENTE` con más de 10 minutos. Si es línea de un pedido o combo, expira todas las líneas que sigan `PENDIENTE`. Restituye el stock de cada línea y avisa de stock repuesto; no restituye cupones. |
| `NotificarPreventasDisponiblesUseCase` (`PreventasScheduler`) | Todos los días a las 00:05 (`0 5 0 * * *`) y una vez al arrancar la aplicación | Genera `PREVENTA_DISPONIBLE` para quienes tienen en su wishlist un producto cuya preventa se lanza hoy. Es idempotente: no repite el aviso por usuario y producto. |

## Cuerpos y parámetros

| Endpoint | Cuerpo (JSON) o parámetros |
|---|---|
| `POST /api/usuarios` | `nombre`, `correo`, `contrasena` |
| `PUT /api/usuarios/me/perfil` | `nombre`, `correo` |
| `PUT /api/usuarios/me/contrasena` | `contrasenaActual`, `contrasenaNueva` (8 a 72 caracteres) |
| `PUT /api/usuarios/me/baja` | Sin cuerpo |
| `POST /api/auth/login` | `correo`, `contrasena` (devuelve `token`) |
| `POST /api/auth/olvide-contrasena` | `correo`. Responde `200` con `mensaje`, exista o no la cuenta; si existe y está activa, envía el enlace por correo (un correo por minuto por cuenta) |
| `POST /api/auth/restablecer-contrasena` | `token` (el del enlace del correo) y `contrasenaNueva` (8 a 72 caracteres, con mayúscula y número). `422` si el enlace no es válido, venció (30 min) o ya se usó, o si la contraseña no cumple las reglas |
| `POST /api/chat/conversaciones` | `tipo` (`PRODUCTO` u `ORDEN`), `referenciaId` (el producto o el pedido) y `vendedorId` opcional (solo para órdenes con varios vendedores; con `PRODUCTO` se ignora). Devuelve la conversación existente si ya había una |
| `GET /api/chat/conversaciones/{id}/mensajes` | Query opcional: `antesDe` (id de mensaje, página hacia atrás), `despuesDe` (id de mensaje, para ponerse al día) y `tamano` (30 por defecto). No se pueden pedir `antesDe` y `despuesDe` a la vez (`422`). Respuesta en orden ascendente, con `hayMas` |
| `POST /api/chat/conversaciones/{id}/mensajes` | `clienteId` (UUID generado por el cliente, hace idempotente el reintento) y `contenido` (1 a 2000 caracteres) |
| `PUT /api/chat/conversaciones/{id}/leidos` | Sin cuerpo; responde cuántos mensajes marcó |
| `POST /api/asistente/comprador` · `/vendedor` | `mensaje` (hasta 1000 caracteres) y `historial` opcional (hasta 10 turnos con `rol` = `user` o `assistant` y `contenido` de hasta 2000 caracteres); devuelve `respuesta`. Errores del proveedor: `429`, `502`, `503`, `504` |
| `POST /api/vendedores` | `nombre`, `correo`, `tipo`, `documento`, `contrasena` |
| `PUT /api/vendedores/me/perfil` | `nombre`, `correo` |
| `PUT /api/vendedores/me/contrasena` | `contrasenaActual`, `contrasenaNueva` (8 a 72 caracteres) |
| `PUT /api/vendedores/me/baja` | Sin cuerpo |
| `POST /api/productos` | `nombre`, `descripcion`, `precio`, `stock`, `gama`, `exclusividad` y `tipo` opcional (`FISICO` por defecto o `DIGITAL`) |
| `PUT /api/productos/{id}/precio` | `nuevoPrecio` |
| `PUT /api/productos/{id}/informacion` | `nombre`, `descripcion` |
| `PUT /api/productos/{id}/stock` | `stock` (entero ≥ 0): **fija** el stock, no lo suma |
| `POST /api/productos/{id}/imagen` | `multipart/form-data` con el campo `archivo` (PNG o JPEG, máx. 5 MB); no es JSON |
| `PUT /api/productos/{id}/visibilidad` | `activo` (`true` activa, `false` pausa) |
| `GET /api/productos` | Filtros opcionales por query: `texto`, `categoria`, `gama`, `precioMin`, `precioMax`, `soloPreventa`. Paginación: `page` (desde 0, por defecto 0) y `size` (1–100, por defecto 20) |
| `POST /api/compras` | `productoId`, `cantidad` |
| `PUT /api/compras/{id}/descarga` | Sin cuerpo |
| `POST /api/resenas` | `idCompra`, `valorCalificacion` (1 a 5) y `comentario` (obligatorio, máx. 500) |
| `POST /api/valoraciones-vendedor` | `compraId`, `estrellas` (1 a 5) y `comentario` opcional (máx. 500) |
| `POST /api/wishlists` | `productoId` |
| `POST /api/pedidos` | `items` (1 a 50 líneas con `productoId` y `cantidad` > 0, sin productos repetidos) y `codigoCupon` opcional (máx. 50 caracteres) |
| `POST /api/combos/{id}/comprar` | `cantidad` y `codigoCupon` opcional |
| `POST /api/cupones` | `codigo` (máx. 50), `porcentaje` (> 0 y < 100), `vence` (fecha `AAAA-MM-DD`, no pasada) y `usosMaximos` (> 0) |
| `POST /api/combos` | `nombre`, `descripcion`, `descuento` y `productoIds` (2 o más) |
| `PUT /api/combos/{id}` | `nombre`, `descripcion`, `descuento` |
| `POST /api/regalos` | `productoId` o `comboId` (exactamente uno, propio del vendedor) |
| `POST /api/preventas` | `productoId`, `fechaLanzamiento` (futura) y `descripcion` |
| `PUT /api/preventas/{id}` | `fechaLanzamiento` (futura) y `descripcion` |
| `DELETE /api/preventas/{id}` | Sin cuerpo |
| `POST /api/promociones` | `productoId`, `porcentaje` (> 0 y < 100), `desde`, `hasta` |
| `PUT /api/promociones/{id}` | `porcentaje`, `desde`, `hasta` |
| `POST /api/descuentos` | `productoId`, `porcentaje` (> 0 y < 100) |
| `POST /api/metas-recompensa` | `comprasRequeridas`, `productoRegaloId` |
| `PUT /api/metas-recompensa/{id}/umbral` | `nuevoUmbral` |
| `POST /api/puntos/redimir` | `cantidad` |
| `PUT /api/regalos/{id}/asignar` | Sin cuerpo |
| `PUT /api/regalos/{id}/enviar` | Sin cuerpo |
| `PUT /api/regalos/{id}/cancelar` | Sin cuerpo (solo el vendedor dueño) |
| `PUT /api/regalos/{id}/entrega` | Sin cuerpo (solo el destinatario confirma) |
| `GET /api/reportes/stock-bajo` | Query opcional `umbral` (5 por defecto) |
| `GET /api/reportes/mas-vendidos` | Query opcionales `vendedorId` (filtro de consulta) y `top` (10 por defecto) |

## Notas

- **Chat y asistente:** las rutas `/api/chat/**` y `/api/asistente/**` exigen token y rol. Solo el comprador inicia una conversación; solo los dos participantes la consultan o escriben (`403` si no participa, `404` si no existe). `GET /api/chat/stream` mantiene una conexión SSE por usuario (timeout y latido configurables) y emite mensajes nuevos y confirmaciones de lectura. El asistente responde `503` si no hay `OPENROUTER_API_KEY`.
- **Recuperación de contraseña:** `POST /api/auth/olvide-contrasena` responde siempre igual, exista o no el correo; el enlace vence a los 30 minutos y es de un solo uso.
- **Correos de notificación:** ocho tipos de notificación se reenvían además por correo (ver README, sección 8); es de mejor esfuerzo y se apaga con `NOTIFICACIONES_CORREO_HABILITADO=false`.
- **Autenticación:** `POST /api/auth/login` devuelve un JWT (HS256, 2 horas por defecto) con el id, el correo y el rol. Todas las rutas `/api/**` exigen `Authorization: Bearer {JWT}`, salvo `/api/auth/**`, `POST /api/usuarios`, `POST /api/vendedores` y las tres consultas del catálogo, que son **públicas** (un visitante puede ver pero no comprar): `GET /api/productos`, `GET /api/combos` y `GET /api/preventas`. El filtro JWT comprueba en cada petición que la cuenta siga activa: una cuenta dada de baja recibe `401` con `codigo: "CUENTA_INACTIVA"` aunque su token no haya expirado.
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
- **Preventas:** `GET /api/preventas` lista solo las de lanzamiento posterior a hoy, ordenadas por fecha ascendente (el mismo día del lanzamiento el producto ya se puede comprar). `PUT /api/preventas/{id}` y `DELETE /api/preventas/{id}` solo los hace el vendedor dueño del producto y solo mientras la preventa no se haya lanzado (después responden `422` «La preventa ya fue lanzada…»); modificar exige las mismas reglas que programar (fecha estrictamente futura, descripción, producto propio y no eliminado). Cancelar elimina la preventa: el producto se puede comprar de inmediato y se puede programar otra más adelante.
- **Regalos:** el estado sigue `DISPONIBLE → ELEGIDO → ENVIADO → ENTREGADO`; `PUT /api/regalos/{id}/cancelar` lo puede hacer el vendedor dueño desde cualquier estado anterior a `ENTREGADO` (otro vendedor o un regalo `ENTREGADO`/`CANCELADO` responde `422`). Si el regalo ya tenía comprador (`ELEGIDO` o `ENVIADO`) se le notifica (`REGALO_CANCELADO`) y recupera su cupo, porque los regalos `CANCELADO` no cuentan como asignados. Se elige un regalo por cada 5 compras `COMPLETADA`; `GET /api/regalos/progreso` devuelve `comprasCompletadas`, `comprasPorRegalo`, `regalosAsignados`, `regalosDisponibles` y `comprasParaSiguiente`. Enviar un regalo que no es de un producto o combo del vendedor responde `422`. Las metas de `/api/metas-recompensa` son informativas y no cambian esa regla.
- **Notificaciones:** las consultan tanto compradores como vendedores (cada uno ve solo las suyas, la más reciente primero). `PUT /api/notificaciones/{id}/leida` responde `404` si no existe y `422` si es de otro usuario. Tipos: comprador (`DESCUENTO`, `STOCK_DISPONIBLE`, `PREVENTA_DISPONIBLE`, `META_ALCANZADA`, `REGALO_ENVIADO`, `REGALO_CANCELADO`) y vendedor (`STOCK_BAJO`, `STOCK_AGOTADO`, `NUEVA_VENTA`, `REEMBOLSO_SOLICITADO`, `NUEVA_RESENA`, `NUEVA_VALORACION`, `REGALO_ELEGIDO`).
- **Stock:** `PUT /api/productos/{id}/stock` fija el stock disponible (por ejemplo al reponer un producto agotado); solo el vendedor dueño y sobre un producto no eliminado. Un valor negativo responde `400`. Si el producto estaba en 0 y vuelve a tener unidades, se notifica `STOCK_DISPONIBLE` a quienes lo tienen en su wishlist.
- **Descarga:** `PUT /api/compras/{id}/descarga` marca como descargada una compra `COMPLETADA` de un producto `DIGITAL` (no entrega ningún archivo). Un producto `FISICO` o una compra no completada responden `422`. Funciona también sobre las líneas de un pedido o combo y sigue permitida si el producto se pausó o eliminó después de pagarse. Tras descargar, el reembolso queda bloqueado (también el del pedido completo).
- **Expiración de compras:** una compra `PENDIENTE` con más de 10 minutos pasa a `EXPIRADA` (estado final) por un proceso automático cada 5 minutos; ver «Procesos automáticos». Confirmar, cancelar o reembolsar una compra expirada responde `422`.
- **Cuentas:** `PUT /api/usuarios/me/baja` y `PUT /api/vendedores/me/baja` son bajas lógicas (se conserva el historial); una segunda baja responde `422`. Iniciar sesión con una cuenta dada de baja y credenciales correctas responde `422`, y las peticiones con un token ya emitido responden `401` (`CUENTA_INACTIVA`). Al editar el perfil de un vendedor el correo no puede ser el de otro vendedor ni el de un comprador (`409`); el tipo y el documento no se editan.
- **Ranking de más vendidos:** agrupa en la base de datos las unidades vendidas de compras `COMPLETADA` (`JOIN`, `GROUP BY` y `SUM`), ordena por unidades (desempate por nombre) e incluye la calificación promedio de cada producto. Sin `vendedorId` lista todo el marketplace; los productos sin ventas no aparecen.
- **Errores** (cuerpo JSON `{timestamp, status, mensaje}`):
  | Código | Cuándo |
  |---|---|
  | 400 | Validación de datos del request (`@Valid`) o JSON inválido; en la subida de imagen: archivo vacío, no PNG/JPEG, de más de 5 MB o de más de 6000 × 6000 px |
  | 401 | Token ausente, inválido o expirado; credenciales incorrectas en el login; cuenta dada de baja con token vigente (el cuerpo trae además `codigo: "CUENTA_INACTIVA"`) |
  | 403 | El rol del token no puede usar la ruta |
  | 404 | El recurso no existe |
  | 409 | Duplicidad (correo, documento, código de cupón, preventa, promoción solapada, reseña o valoración repetida, producto repetido en wishlist o combo), violación de integridad o conflicto de concurrencia |
  | 422 | Una regla de negocio del dominio se violó (stock insuficiente, reembolso fuera de plazo, transición de estado no permitida, paginación fuera de rango, preventa ya lanzada, producto físico sin descarga, etc.) |
  | 502 / 503 | Solo en la subida de imagen: Cloudinary rechazó o no respondió (`502`) o la carga de imágenes no está configurada en el backend (`503`) |

- **No existe un rol ADMIN.** Las operaciones de administración se resolvieron así: las metas de recompensa son de `VENDEDOR` (cualquier vendedor puede gestionar cualquier meta) y el envío de regalos es de `VENDEDOR`, solo del dueño del producto o combo; `mas-vendidos` y el reporte global están abiertos a cualquier sesión autenticada.
- Los parámetros de los endpoints que no están en la tabla de cuerpos están en Swagger.
- **Imágenes:** el producto se publica primero y la imagen se sube después a `POST /api/productos/{id}/imagen`. El backend firma la petición a Cloudinary y guarda solo la URL HTTPS; requiere configurar `CLOUDINARY_*` (ver README, sección 11). La respuesta es `{ productoId, imagenUrl }`; el producto no puede estar eliminado ni ser de otro vendedor. Sin imagen, o si la carga falla, el catálogo usa la ilustración de respaldo.