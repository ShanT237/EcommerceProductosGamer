# Diagrama de Agregados — Invariantes

## Agregado: Producto

**Raíz de agregado:** `Producto`
**Dentro del límite:** `Gama` (enum), `Exclusividad` (enum)
**Fuera del agregado:** `Vendedor` (referenciado por `vendedorId`). Además, `Producto` es referenciado por otros agregados, ej. `Combo`, `Regalo`, `Compra`, `Preventa`, `Wishlist`.

### Invariantes

- Todo producto pertenece a un vendedor: el `vendedorId` es obligatorio y no cambia.
- El stock de un producto no puede ser negativo.
- Un producto no puede eliminarse si tiene compras activas sujetas a reembolso — solo eliminación lógica (soft delete).
- Un producto exclusivo solo puede ser comprado una vez por el mismo usuario.
- El precio de un producto debe ser siempre positivo.
- Un producto exclusivo solo puede comprarse una vez por usuario y de a una unidad.
- Un descuento debe ser mayor a 0 y menor a 100, y no puede aplicarse a un producto eliminado.

---

## Agregado: Vendedor

**Raíz de agregado:** `Vendedor`
**Dentro del límite:** `TipoVendedor` (enum: persona natural o empresa)
**Fuera del agregado:** ninguna referencia directa (es referenciado por otros, ej. `Producto`, por `vendedorId`). Es independiente de `Usuario` (comprador).

### Invariantes

- Un vendedor siempre tiene un tipo (persona natural o empresa) y un documento de identificación (cédula o NIT según el tipo).
- El nombre del vendedor no puede estar vacío y su correo siempre debe ser válido.
- Un vendedor dado de baja no puede publicar productos.
- Un vendedor solo puede darse de baja una vez — la baja es lógica, para conservar el historial de sus ventas.

---

## Agregado: Regalo

**Raíz de agregado:** `Regalo`
**Dentro del límite:** `EstadoRegalo` (enum)
**Fuera del agregado:** `Usuario`, `Producto`, `Combo` (referenciados por id)

### Invariantes

- Un regalo no puede enviarse si no se ha elegido el usuario destinatario.
- Un regalo solo puede elegirse una vez, y nunca para un usuario que ya posee ese producto.
- Un regalo no puede marcarse como entregado si no fue enviado, ni sin la confirmación de su destinatario.
- Un regalo ENTREGADO o CANCELADO no puede cambiar de estado.
- Un regalo está asociado a un producto o a un combo, nunca a ambos ni a ninguno.

---

## Agregado: Compra

**Raíz de agregado:** `Compra`
**Dentro del límite:** `EstadoCompra` (enum), `Precio` (precio unitario congelado al momento de la compra)
**Fuera del agregado:** `Usuario`, `Producto` (referenciados por id)

### Invariantes

- Una compra nace PENDIENTE y solo puede confirmarse una vez.
- El precio unitario queda fijo al crear la compra; cambios posteriores del producto no la afectan.
- Solo se reseña una compra COMPLETADA, y máximo una vez.
- Los puntos solo se asignan después de reseñar, y una sola vez.
- Una compra solo se reembolsa si está COMPLETADA, dentro de 48 horas, sin descarga y sin puntos asignados.
- Una compra REEMBOLSADA, CANCELADA o EXPIRADA no puede cambiar de estado.