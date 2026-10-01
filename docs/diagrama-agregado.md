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

- Un regalo no podrá ser enviado si no se ha elegido el usuario al que se envía.
- Un regalo no podrá ser elegido varias veces a un usuario.
- Un regalo no podrá ser entregado si no se ha enviado.
- Un regalo no podrá ser enviado si el usuario no confirma la recepción del mismo.
- Cuando un regalo sea entregado no podrá cambiar de estado.
- Un regalo no puede ser elegido a un usuario que ya posea ese producto.

---

## Agregado: Compra

**Raíz de agregado:** `Compra`
**Dentro del límite:** `EstadoCompra` (enum), `Precio` (precio unitario congelado al momento de la compra)
**Fuera del agregado:** `Usuario`, `Producto` (referenciados por id)

### Invariantes

- Una compra nace en estado PENDIENTE y solo puede confirmarse una vez (pasa a COMPLETADA).
- El precio unitario de una compra queda fijo al momento de crearla — cambios futuros en el precio del producto no afectan compras ya realizadas.
- Un comprador no puede calificar (reseñar) un producto sin haberlo comprado y sin que la compra esté COMPLETADA.
- Máximo una reseña por compra.
- Los puntos de una compra solo se asignan después de haber registrado la reseña, y solo una vez.
- Una compra solo puede reembolsarse si está COMPLETADA, dentro de un plazo definido y antes de haber descargado el archivo.
- Una compra REEMBOLSADA no puede volver a confirmarse, reseñarse ni reembolsarse de nuevo.