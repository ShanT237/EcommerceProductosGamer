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
- Un producto pausado o eliminado no se puede comprar; solo su vendedor dueño lo modifica, edita o pausa.
- La categoría del producto es obligatoria.

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
- El regalo no guarda su vendedor: el dueño es el del producto o combo al que apunta. Solo ese vendedor puede crearlo y enviarlo.
- Un comprador elige un regalo por cada 5 compras COMPLETADAS (regalos asignados ≤ completadas ÷ 5).

---

## Agregado: Compra

**Raíz de agregado:** `Compra`
**Dentro del límite:** `EstadoCompra` (enum), `Precio` (precio unitario congelado al momento de la compra)
**Fuera del agregado:** `Usuario`, `Producto`, `Combo` (referenciados por id)

### Invariantes

- Una compra nace PENDIENTE y solo puede confirmarse una vez.
- El precio unitario queda fijo respecto al producto; cambios posteriores del producto no la afectan. Un cupón puede rebajarlo una sola vez, y solo mientras la compra está PENDIENTE.
- Una compra puede formar parte de un **pedido** (`pedidoId`): todas las líneas de un pedido comparten el mismo id. Una compra de combo (`comboId` y `comboNombre`, ambos o ninguno) siempre pertenece a un pedido.
- Las líneas de un pedido o combo no se confirman, cancelan ni reembolsan por separado (`asegurarNoEsDeUnPedido`): esas operaciones se hacen sobre el pedido completo.
- Solo se reseña una compra COMPLETADA, y máximo una vez.
- Los puntos solo se asignan después de reseñar, y una sola vez.
- Una compra solo se reembolsa si está COMPLETADA, dentro de 48 horas, sin descarga y sin puntos asignados.
- Una compra REEMBOLSADA, CANCELADA o EXPIRADA no puede cambiar de estado.

---

---

## Operación: Pedido (conjunto de Compra)

**No es una raíz de agregado:** es el conjunto de `Compra` que comparten un `pedidoId` (el carrito pagado o un combo comprado). Cada `Compra` sigue siendo su propia raíz; la coherencia entre líneas la imponen los casos de uso con una transacción.

### Invariantes (garantizadas por los casos de uso, no por una entidad)

- Todo o nada: se validan todas las líneas (producto comprable, preventa, vendedor activo, exclusividad, stock) **antes** de descontar stock; si una falla, no cambia ningún producto ni se guarda ninguna compra.
- Un pedido tiene entre 1 y 50 líneas, con cantidades positivas y sin repetir producto.
- Confirmar o cancelar exige que **todas** las líneas estén PENDIENTES y sean del usuario, comprobado antes de mutar; cancelar o reembolsar restituye el stock de cada línea.
- Un cupón consume **un solo uso** por pedido, sin importar cuántas líneas tenga, y solo rebaja las líneas a las que aplica.

---

## Agregados de apoyo (Promocion, Cupon, MetaRecompensa, ValoracionVendedor, MovimientoPuntos, Combo, Preventa, Wishlist, Notificacion)

Todos referencian a otros agregados solo por id.

- **Promocion** (raíz): descuento de 0 a 100 exclusivo, con vigencia entre dos fechas inclusive. No se solapa con otra promoción del mismo producto y no modifica el precio base.
- **Cupon** (raíz): código único en mayúsculas, descuento de 0 a 100 exclusivo, válido hasta su vencimiento (inclusive) y mientras tenga usos disponibles. Tiene un **dueño opcional** (`vendedorId`): con dueño solo aplica a productos de ese vendedor (`aplicaA`); sin dueño es global. Consume un uso por pedido; la regla «una vez por comprador» depende del historial (`CuponUso`) y la comprueba el caso de uso.
- **MetaRecompensa** (raíz): umbral de compras completadas positivo, ligado a un producto regalo; se desactiva una sola vez, nunca se borra. Es informativa: no gobierna la asignación de regalos (`Usuario.COMPRAS_POR_REGALO`).
- **ValoracionVendedor** (raíz): una por compra completada; calificación obligatoria y comentario opcional de máximo 500 caracteres.
- **MovimientoPuntos** (raíz, inmutable): registro de puntos ganados o redimidos con cantidad positiva; el saldo vigente sigue en `Usuario`.
- **Combo** (raíz): dos o más productos distintos del mismo vendedor y un descuento menor a 100. Solo su vendedor lo modifica o elimina, y no se elimina con regalos pendientes que lo referencien.
- **Preventa** (raíz): un producto con fecha de lanzamiento futura al programarla; el producto se puede comprar desde ese mismo día. Una por producto.
- **Wishlist** (raíz): productos favoritos de un usuario, sin repetidos ni eliminados.
- **Notificacion** (raíz): aviso de descuento para un usuario; solo su dueño la marca como leída.
