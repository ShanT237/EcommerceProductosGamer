# Diseño de E-commerce Gamer

## 1. ¿Quién compra y por qué?

El marketplace está dirigido principalmente a usuarios interesados en productos y servicios relacionados con el mundo gaming:

* **Gamers:** buscan productos que mejoren su experiencia de juego, como periféricos, sillas y accesorios.
* **Streamers:** buscan productos funcionales y visualmente atractivos que complementen sus espacios de streaming.
* **Jugadores casuales:** buscan productos que proporcionen mayor comodidad y una mejor experiencia de juego.

---

## 2. ¿Qué hace único a sus vendedores?

El sistema funciona como un **marketplace**, por lo que diferentes vendedores independientes pueden registrarse y publicar su propio catálogo de productos.

Los vendedores pueden ofrecer diferentes categorías dentro del nicho gaming, entre ellas:

* 🖱️ **Periféricos:** mouses, teclados, audífonos, controles, etc.
* 🪑 **Mobiliario:** sillas, escritorios y accesorios para el espacio gaming.
* 🎮 **Juegos físicos:** videojuegos y ediciones especiales.
* 💻 **Licencias digitales:** códigos y licencias para videojuegos o servicios relacionados.
* 🎁 **Combos:** conjuntos de dos o más productos ofrecidos como una unidad comercial.

De esta manera, **no todos los vendedores tienen que ofrecer los mismos productos**, sino que cada uno puede especializarse en diferentes categorías o manejar un catálogo variado.

---

## 3. Lenguaje ubicuo


| Término          | Significado                                                                                                        |
| ---------------- | ------------------------------------------------------------------------------------------------------------------ |
| **Gama**         | Clasificación de un producto según sus características y nivel, por ejemplo, gama baja, media o alta.              |
| **Preventa**     | Producto que puede ser adquirido antes de su fecha oficial de lanzamiento o disponibilidad.                        |
| **Unidades**     | Cantidad disponible de un producto para su venta.                                                                  |
| **Wishlist**     | Lista personal donde el usuario guarda productos que desea comprar posteriormente.                                 |
| **Combo**        | Conjunto de dos o más productos vendidos como una oferta o paquete.                                                |
| **Usuario**      | Persona registrada en la plataforma que puede realizar compras y utilizar las funcionalidades del marketplace.     |
| **Puntos**       | Beneficios acumulables que el usuario obtiene al cumplir determinadas acciones dentro de la plataforma.            |
| **Regalo**       | Producto o beneficio que un usuario puede recibir como recompensa por cumplir determinadas metas.                  |
| **Exclusividad** | Condición aplicada a productos especiales que limita su compra a una determinada cantidad o condición por usuario. |

---

## 4. Reglas de negocio innegociables

Las siguientes reglas representan condiciones que el sistema **nunca debe permitir incumplir**:

1. **Un producto no puede tener stock negativo.**
   La cantidad disponible de unidades siempre debe ser igual o superior a cero.

2. **Un comprador no puede calificar un producto sin haberlo comprado.**
   Las reseñas y calificaciones solamente pueden ser realizadas por usuarios que hayan adquirido el producto.

3. **Un vendedor no puede eliminar un producto que tenga compras activas.**
   En estos casos, el producto debe manejarse mediante una eliminación lógica para conservar el historial de las transacciones.

4. **Un usuario no puede tener acceso a un producto de preventa antes de la fecha estipulada.**
   El acceso o disponibilidad del producto debe respetar su fecha oficial de lanzamiento.

5. **Un combo debe estar formado por dos o más productos.**
   No es válido registrar un combo que contenga únicamente un producto.

6. **Un usuario solamente recibe los puntos correspondientes después de realizar una reseña del producto comprado.**
   Los puntos asociados a una compra no deben asignarse antes de que se cumpla esta condición.

7. **Un usuario no puede agregar el mismo producto más de una vez a su wishlist.**
   Si el producto ya está registrado en su wishlist, el sistema debe impedir que se agregue nuevamente.

8. **Un usuario solamente debe recibir una notificación de descuento cuando el producto se encuentre en su wishlist.**
   Las notificaciones de este tipo deben estar relacionadas con productos guardados por el usuario.

9. **Un usuario solamente puede comprar un producto exclusivo una vez.**
   Una vez realizada la compra de un producto marcado como exclusivo, el mismo usuario no podrá adquirirlo nuevamente.

10. **Un usuario que complete una meta de compras podrá ser seleccionado para recibir un regalo.**
    Al alcanzar una meta establecida por el marketplace, el usuario podrá participar en el mecanismo de selección de recompensas correspondiente.