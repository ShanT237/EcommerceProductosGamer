# Mapeo Dominio -> API

Cada operación del dominio y el endpoint REST que la expone.

| Operación del dominio | Método HTTP | Endpoint | Respuesta exitosa |
|---|---|---|---|
| `Vendedor.registrar(...)` | POST | `/api/vendedores` | 201 Created |
| `Producto.publicar(...)` | POST | `/api/productos` | 201 Created |
| `Producto.eliminar(...)` | DELETE | `/api/productos/{id}` | 204 No Content |
| `Usuario.registrar(...)` | POST | `/api/usuarios` | 201 Created |
| `Compra.realizar(...)` | POST | `/api/compras` | 201 Created |
| `Compra.confirmar()` | PUT | `/api/compras/{id}/confirmar?usuarioId=` | 200 OK |
| `Compra.cancelar()` | PUT | `/api/compras/{id}/cancelar?usuarioId=` | 200 OK |
| `Compra.solicitarReembolso(...)` | PUT | `/api/compras/{id}/reembolso?usuarioId=` | 200 OK |
| Consultar historial de compras | GET | `/api/compras?usuarioId=` | 200 OK |

## Notas

- `POST /api/usuarios` requiere `nombre`, `correo` y `contrasena`; la contraseña debe tener al menos 8 caracteres, una mayúscula y un número. `POST /api/usuarios` y `POST /api/vendedores` son públicos para permitir el registro; las demás rutas `/api/**`, excepto `/api/auth/**`, requieren `Authorization: Bearer {JWT}`.
- Usuarios de demo para `POST /api/auth/login`: `jose.demo@correo.com` / `JoseGamer2026` y `ana.demo@correo.com` / `AnaGamer2026`.
- `usuarioId` va como parámetro en la URL (`?usuarioId=...`) y se usa para verificar que la compra pertenece a ese usuario.
- Los errores de reglas del dominio responden **400** con un mensaje; los recursos que no existen en `Producto` responden **404**.
- Documentación interactiva: `http://localhost:8080/swagger-ui.html`.