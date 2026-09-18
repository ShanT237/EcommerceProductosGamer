# Diagrama de Agregado — Regalo

## Invariantes

- Un regalo no podrá ser enviado si no se ha elegido el usuario al que se envía.
- Un regalo no podrá ser elegido varias veces a un usuario.
- Un regalo no podrá ser entregado si no se ha enviado.
- Un regalo no podrá ser enviado si el usuario no confirma la recepción del mismo.
- Cuando un regalo sea entregado no podrá cambiar de estado.
- Un regalo no puede ser elegido a un usuario que ya posea ese producto.
