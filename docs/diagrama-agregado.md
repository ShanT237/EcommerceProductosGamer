# Diagrama de Agregados — Invariantes

## Agregado: Producto

**Raíz de agregado:** `Producto`
**Dentro del límite:** `Gama` (enum), `Exclusividad` (enum)
**Fuera del agregado:** `Vendedor` (referenciado por `vendedorId`). Además, `Producto` es referenciado por otros agregados, ej. `Combo`, `Regalo`, `Compra`, `Preventa`, `Wishlist`.

### Invariantes

- Todo producto pertenece a un vendedor: el `vendedorId` es obligatorio y no cambia.
- El stock de un producto no puede ser negativo.
- Un producto no puede eliminarse si tiene compras activas sujetas a reembolso — solo eliminación lógica (soft delete).
- El precio de un producto debe ser siempre positivo.
- Un producto exclusivo solo puede comprarse una vez por usuario y de a una unidad.
- Un descuento debe ser mayor a 0 y menor a 100, y no puede aplicarse a un producto eliminado ni a uno que ya tiene un descuento activo.
- Con un descuento activo el precio no se puede cambiar: primero se quita el descuento, y el producto vuelve al precio que tenía (quitarlo sin descuento activo se rechaza).
- Un producto pausado o eliminado no se puede comprar; solo su vendedor dueño lo modifica, edita o pausa.
- La categoría del producto es obligatoria.

---

## Agregado: Usuario

**Raíz de agregado:** `Usuario` (el comprador)
**Dentro del límite:** `Puntos` (saldo, objeto de valor), `Contrasena` (se guarda solo el hash)
**Fuera del agregado:** `Compra`, `Resena`, `Wishlist`, `Regalo` (referencian al usuario por id)

### Invariantes

- El nombre no puede estar vacío y el correo debe ser válido.
- Los puntos a acumular o redimir deben ser positivos, y no se redime más que el saldo disponible.
- Un usuario inactivo no opera (`asegurarActivo`).
- Para cambiar la contraseña hay que acertar la actual, y la nueva debe ser distinta; nunca se guarda en claro.
- Solo puede elegir un regalo quien tenga regalos ganados por compras completadas: uno por cada `Usuario.COMPRAS_POR_REGALO` (5) compras `COMPLETADA`, descontando los ya asignados.

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
- Un regalo no guarda su vendedor: el dueño es el del producto o combo al que apunta, y solo ese vendedor puede enviarlo (lo verifica el caso de uso `EnviarRegalo`).

---

## Agregado: Compra

**Raíz de agregado:** `Compra`
**Dentro del límite:** `EstadoCompra` (enum), `Precio` (precio unitario congelado al momento de la compra)
**Fuera del agregado:** `Usuario`, `Producto` (referenciados por id). El `pedidoId` es una referencia opcional a la operación a la que pertenece la línea: no existe un agregado `Pedido`, los casos de uso aplican la acción a todas las líneas que lo comparten.

### Invariantes

- Una compra nace PENDIENTE y solo puede confirmarse una vez.
- El precio unitario queda fijo respecto al producto; cambios posteriores del producto no la afectan. Un cupón puede rebajarlo una sola vez, y solo mientras la compra está PENDIENTE.
- Solo se reseña una compra COMPLETADA, y máximo una vez.
- Los puntos solo se asignan después de reseñar, y una sola vez.
- Una compra solo se reembolsa si está COMPLETADA, dentro de 48 horas, sin descarga y sin puntos asignados.
- Una compra REEMBOLSADA, CANCELADA o EXPIRADA no puede cambiar de estado.
- Las líneas de un pedido comparten `pedidoId`. Un pedido se crea, confirma o cancela completo: una línea con `pedidoId` no se confirma ni se cancela sola (`Compra.asegurarNoEsDeUnPedido`). El reembolso sí puede pedirse por línea o por pedido.
- Un pedido se crea todo o nada: si una línea falla, ninguna compra se guarda, ningún stock cambia y el cupón no se consume.

---

## Agregado: Combo

**Raíz de agregado:** `Combo`
**Dentro del límite:** la lista de ids de producto que lo forman
**Fuera del agregado:** `Producto` (referenciado por id) y `Vendedor` (por `vendedorId`)

### Invariantes

- Un combo tiene dos o más productos y sin duplicados; quitar productos nunca lo deja por debajo de dos.
- Agregar un producto que ya está en el combo se rechaza como conflicto (`409`).
- Nombre y descripción son obligatorios, y el descuento va de 0 a menos de 100.
- Solo su vendedor dueño lo modifica, le agrega o quita productos y lo elimina (`asegurarPerteneceA`).
- Un combo no se elimina mientras exista un regalo sin cerrar que lo referencie: el caso de uso `EliminarCombo` lo verifica porque `Regalo` guarda el id del combo sin clave foránea.

---

## Agregado: Wishlist

**Raíz de agregado:** `Wishlist`
**Dentro del límite:** la lista de ids de producto
**Fuera del agregado:** `Usuario` y `Producto` (referenciados por id)

### Invariantes

- Un producto no se repite en la wishlist (`409` si ya está) y no se agregan productos eliminados.
- Quitar un producto que no está en la wishlist se rechaza.
- Solo quienes tienen el producto en su wishlist reciben la notificación de descuento.

---

## Agregado: Preventa

**Raíz de agregado:** `Preventa`
**Dentro del límite:** la fecha de lanzamiento y la descripción
**Fuera del agregado:** `Producto` (referenciado por id)

### Invariantes

- Al programarla, la fecha de lanzamiento es obligatoria y futura, y la descripción no puede estar vacía.
- Un producto tiene como máximo una preventa.
- Un producto en preventa no se compra antes de su fecha: la preventa está disponible desde el día de lanzamiento (`validarAcceso`), y solo cuenta como futura si la fecha es estrictamente posterior a hoy.

---

## Agregado: Resena

**Raíz de agregado:** `Resena`
**Dentro del límite:** `Calificacion` (objeto de valor, de 1 a 5)
**Fuera del agregado:** `Compra`, `Producto` y `Usuario` (referenciados por id)

### Invariantes

- Solo se reseña una compra `COMPLETADA`, con calificación de 1 a 5 y un comentario obligatorio de máximo 500 caracteres.
- Una reseña por usuario y producto.
- El dominio permite una única respuesta del vendedor a la reseña (`responder`), pero la API todavía no expone esa acción.

---

## Agregados de apoyo (Promocion, Cupon, MetaRecompensa, ValoracionVendedor, MovimientoPuntos)

Todos referencian a otros agregados solo por id.

- **Promocion** (raíz): descuento de 0 a 100 exclusivo, con vigencia entre dos fechas inclusive. No se solapa con otra promoción del mismo producto y no modifica el precio base. Se puede reprogramar (porcentaje y fechas, con las mismas reglas) o quitar, y solo lo hace el vendedor dueño del producto.
- **Cupon** (raíz): código único en mayúsculas, descuento de 0 a 100 exclusivo, válido hasta su vencimiento y mientras tenga usos disponibles. Tiene un dueño opcional (`vendedorId`; nulo significa cupón global) y `aplicaA(vendedorDelProducto)` decide a qué productos descuenta. Consume **un uso por pedido**, no uno por ítem. Que un comprador ya lo usó se registra en `cupon_usos` (fuera del agregado), porque cada comprador lo usa una sola vez.
- **MetaRecompensa** (raíz): umbral de compras completadas positivo, ligado a un producto regalo; se desactiva una sola vez, nunca se borra. Es informativa: no gobierna la asignación de regalos, que usa la regla fija de 5 compras.
- **Notificacion** (raíz): aviso de descuento de un producto de la wishlist de un usuario; solo su dueño la marca como leída (`asegurarPerteneceA`).
- **ValoracionVendedor** (raíz): una por compra completada; calificación obligatoria y comentario opcional de máximo 500 caracteres.
- **MovimientoPuntos** (raíz, inmutable): registro de puntos ganados o redimidos con cantidad positiva; el saldo vigente sigue en `Usuario`.
