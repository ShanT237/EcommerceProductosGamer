# Mapeo Dominio -> API

Cada operación del dominio y el endpoint REST que la expone.

## Cuentas y autenticación

| Operación del dominio | Método HTTP | Endpoint | Respuesta exitosa |
|---|---|---|---|
| `Usuario.registrar(...)` | POST | `/api/usuarios` | 201 Created |
| `Usuario.actualizarPerfil(...)` | PUT | `/api/usuarios/{id}/perfil` | 200 OK |
| `Usuario.definirContrasena(...)` | PUT | `/api/usuarios/{id}/contrasena` | 200 OK |
| `IniciarSesionUseCase` | POST | `/api/auth/login` | 200 OK |
| `Vendedor.registrar(...)` | POST | `/api/vendedores` | 201 Created |
| `Vendedor.darDeBaja()` | PUT | `/api/vendedores/{id}/baja` | 200 OK |
| `ValoracionVendedor.crear(...)` | POST | `/api/valoraciones-vendedor` | 201 Created |

## Productos y catálogo

| Operación del dominio | Método HTTP | Endpoint | Respuesta exitosa |
|---|---|---|---|
| `Producto.publicar(...)` | POST | `/api/productos` | 201 Created |
| `Producto.cambiarPrecio(...)` | PUT | `/api/productos/{id}/precio` | 200 OK |
| `Producto.editarInformacion(...)` | PUT | `/api/productos/{id}/informacion` | 200 OK |
| `Producto.pausar()` / `activar()` | PUT | `/api/productos/{id}/visibilidad` | 200 OK |
| `Producto.eliminar(...)` | DELETE | `/api/productos/{id}` | 204 No Content |
| Filtrar y buscar productos | GET | `/api/productos` | 200 OK |
| `Producto.aplicarDescuento(...)` | POST | `/api/descuentos` | 200 OK |
| `Promocion.programar(...)` | POST | `/api/promociones` | 201 Created |
| `Preventa.programar(...)` | POST | `/api/preventas` | 201 Created |

## Compras

| Operación del dominio | Método HTTP | Endpoint | Respuesta exitosa |
|---|---|---|---|
| `Compra.realizar(...)` | POST | `/api/compras` | 201 Created |
| `Compra.confirmar()` | PUT | `/api/compras/{id}/confirmar?usuarioId=` | 200 OK |
| `Compra.cancelar()` | PUT | `/api/compras/{id}/cancelar?usuarioId=` | 200 OK |
| `Compra.solicitarReembolso(...)` | PUT | `/api/compras/{id}/reembolso?usuarioId=` | 200 OK |
| `Compra.aplicarCupon(...)` | PUT | `/api/compras/{id}/cupon` | 200 OK |
| Consultar historial de compras | GET | `/api/compras?usuarioId=` | 200 OK |
| Comprar combo | POST | `/api/combos/{id}/comprar` | 201 Created |

## Combos, wishlist, reseñas y puntos

| Operación del dominio | Método HTTP | Endpoint | Respuesta exitosa |
|---|---|---|---|
| `Combo.crear(...)` | POST | `/api/combos` | 201 Created |
| `Combo.agregarProducto(...)` | PUT | `/api/combos/{id}/productos/{productoId}` | 200 OK |
| `Combo.quitarProducto(...)` | DELETE | `/api/combos/{id}/productos/{productoId}` | 200 OK |
| `Wishlist.agregarProducto(...)` | POST | `/api/wishlists` | 201 Created |
| `Wishlist.quitarProducto(...)` | DELETE | `/api/wishlists/{productoId}?usuarioId=` | 200 OK |
| `Resena.crear(...)` | POST | `/api/resenas` | 201 Created |
| `Usuario.redimirPuntos(...)` | POST | `/api/puntos/redimir` | 200 OK |
| Consultar historial de puntos | GET | `/api/puntos/historial?usuarioId=` | 200 OK |

## Regalos y metas de recompensa

| Operación del dominio | Método HTTP | Endpoint | Respuesta exitosa |
|---|---|---|---|
| `Regalo.elegir(...)` (asignar por meta) | PUT | `/api/regalos/{id}/asignar` | 200 OK |
| `Regalo.enviar()` | PUT | `/api/regalos/{id}/enviar` | 200 OK |
| `Regalo.confirmarEntrega(...)` | PUT | `/api/regalos/{id}/entrega` | 200 OK |
| `MetaRecompensa.crear(...)` | POST | `/api/metas-recompensa` | 201 Created |
| `MetaRecompensa.actualizarUmbral(...)` | PUT | `/api/metas-recompensa/{id}/umbral` | 200 OK |
| `MetaRecompensa.desactivar()` | PUT | `/api/metas-recompensa/{id}/desactivar` | 200 OK |

## Reportes

| Caso de uso | Método HTTP | Endpoint | Respuesta exitosa |
|---|---|---|---|
| CU-29 Ventas por vendedor | GET | `/api/reportes/ventas?vendedorId=` | 200 OK |
| CU-30 Productos más vendidos | GET | `/api/reportes/mas-vendidos?vendedorId=&top=` | 200 OK |
| CU-34 Alertas de stock bajo | GET | `/api/reportes/stock-bajo?vendedorId=&umbral=` | 200 OK |
| CU-35 Métricas de reseñas | GET | `/api/reportes/metricas-resenas?vendedorId=` | 200 OK |
| CU-36 Reporte global | GET | `/api/reportes/global` | 200 OK |

## Cuerpos y parámetros

| Endpoint | Cuerpo (JSON) o parámetros |
|---|---|
| `PUT /api/productos/{id}/precio` | `vendedorId`, `nuevoPrecio` |
| `PUT /api/productos/{id}/informacion` | `vendedorId`, `nombre`, `descripcion` |
| `PUT /api/productos/{id}/visibilidad` | `vendedorId`, `activo` (`true` activa, `false` pausa) |
| `GET /api/productos` | Filtros opcionales por query: `texto`, `categoria`, `gama`, `precioMin`, `precioMax`, `soloPreventa` |
| `PUT /api/compras/{id}/cupon` | `usuarioId`, `codigo` |
| `PUT /api/regalos/{id}/asignar` | `usuarioId` |
| `PUT /api/regalos/{id}/enviar` | Sin cuerpo |
| `PUT /api/regalos/{id}/entrega` | `usuarioId` (solo el destinatario confirma) |

## Notas

- `POST /api/usuarios` requiere `nombre`, `correo` y `contrasena`; la contraseña debe tener al menos 8 caracteres, una mayúscula y un número. `POST /api/usuarios` y `POST /api/vendedores` son públicos para permitir el registro; las demás rutas `/api/**`, excepto `/api/auth/**`, requieren `Authorization: Bearer {JWT}`.
- Usuarios de demo para `POST /api/auth/login`: `jose.demo@correo.com` / `JoseGamer2026` y `ana.demo@correo.com` / `AnaGamer2026`.
- `usuarioId` y `vendedorId` van en el body o como parámetro en la URL. Sirven para verificar pertenencia, pero no se cruzan con el JWT (no hay roles).
- Errores: las reglas del dominio responden **400** con un mensaje; un recurso que no existe responde **404**; una violación de integridad o un conflicto de concurrencia responde **409**; sin token válido responde **401**.
- `GET /api/reportes/mas-vendidos` sin `vendedorId` lista todo el marketplace. `top` es 10 por defecto y `umbral` de stock bajo es 5 por defecto.
- Los reportes, las metas de recompensa y los regalos son operaciones de admin o sistema, pero hoy no hay roles: cualquier usuario con token válido puede usarlos.
- Los parámetros de los endpoints que no están en la tabla de cuerpos están en Swagger: `http://localhost:8080/swagger-ui.html`.
