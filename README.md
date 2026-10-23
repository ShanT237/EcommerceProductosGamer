# 🎮 E-Commerce Marketplace Gamer

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen?style=for-the-badge&logo=springboot)
![Gradle](https://img.shields.io/badge/Gradle-Build-blue?style=for-the-badge&logo=gradle)
![MariaDB](https://img.shields.io/badge/MariaDB-Database-003545?style=for-the-badge&logo=mariadb)
![Architecture](https://img.shields.io/badge/Architecture-DDD%20%2B%20Clean%20Arch-purple?style=for-the-badge)

Sistema e-commerce descentralizado enfocado en productos y accesorios del mundo **Gaming**, desarrollado para la asignatura de Programación Avanzada / Ingeniería de Software en la **Universidad del Quindío**. El proyecto está guiado bajo los principios de **Domain-Driven Design (DDD)** y **Clean Architecture**.

---

## 📋 Tabla de Contenidos
1. [¿Quién compra y por qué?](#1-quién-compra-y-por-qué)
2. [¿Qué hace único a sus vendedores?](#2-qué-hace-único-a-sus-vendedores)
3. [Lenguaje Ubicuo (DDD)](#3-lenguaje-ubicuo-ddd)
4. [Reglas de Negocio e Invariantes del Dominio](#4-reglas-de-negocio-e-invariantes-del-dominio)
5. [Casos de Uso del Sistema (37 Casos de Uso)](#5-casos-de-uso-del-sistema-37-casos-de-uso)
6. [Arquitectura del Software (Clean Architecture)](#6-arquitectura-del-software-clean-architecture)
7. [Seguridad, Base de Datos y Limitaciones](#7-seguridad-base-de-datos-y-limitaciones)
8. [Cómo ejecutar las pruebas](#8-cómo-ejecutar-las-pruebas)
9. [Gestión del Proyecto (Trello)](#9-gestión-del-proyecto-trello)

---

## 1. ¿Quién compra y por qué?

El marketplace está diseñado conceptualmente para atender las necesidades específicas de la comunidad gamer:

- 🎮 **Gamers:** buscan productos de alto rendimiento que mejoren su experiencia competitiva (periféricos de alta precisión, componentes de hardware, sillas ergonómicas y accesorios dedicados).
- 🎥 **Streamers / Creadores de Contenido:** buscan productos funcionales y estéticamente atractivos (iluminación RGB, placas de captura, micrófonos condensadores, mobiliario) para personalizar sus setups de transmisión.
- 🕹️ **Jugadores Casuales:** buscan productos accesibles que garanticen confort, entretenimiento y una experiencia de juego amigable en su tiempo libre.

---

## 2. ¿Qué hace único a sus vendedores?

El sistema opera como un **marketplace multi-vendedor**, donde diversos comercios independientes pueden registrarse, gestionar su reputación y publicar su propia oferta comercial.

Categorías habilitadas en el catálogo:

- 🖱️ **Periféricos:** mouses, teclados mecánicos, audífonos Surround 7.1, mandos y mousepads XL.
- 🪑 **Mobiliario & Setup:** sillas ergonómicas, escritorios regulables, soportes de monitor e iluminación inteligente.
- 🎮 **Juegos Físicos & Coleccionables:** títulos en formato físico, figuras de acción y preventas exclusivas.
- 💻 **Licencias & Códigos Digitales:** claves para videojuegos, pases de batalla, gift cards y suscripciones.
- 🎁 **Combos Gamer:** paquetes promocionales de 2 o más productos ofrecidos a precio preferencial.

---

## 3. Lenguaje Ubicuo (DDD)

| Término | Definición en el Dominio |
| :--- | :--- |
| **Gama** | Clasificación técnica del producto según prestaciones y precio (`BAJA`, `MEDIA`, `ALTA`). |
| **Preventa** | Modalidad de compra anticipada habilitada antes de la fecha de liberación oficial del producto. |
| **Wishlist** | Lista de deseos personal para seguimiento de precios, promociones y disponibilidad de stock. |
| **Notificación** | Alerta que recibe el usuario cuando un producto de su Wishlist baja de precio (descuento). |
| **Combo** | Agrupación promocional de 2 o más productos comercializados bajo una sola entidad con descuento. |
| **Usuario** | Persona registrada que compra, reseña, acumula y redime puntos. Se llama *Usuario* en todo el código. |
| **Vendedor** | Entidad comercial (Persona Natural o Empresa) registrada para publicar y vender productos. |
| **Puntos de Fidelización** | Unidades de beneficio otorgadas a compradores tras publicar reseñas de compras completadas. |
| **Regalo** | Beneficio otorgado por el sistema al usuario tras alcanzar metas de compra acumuladas. |
| **Exclusividad** | Condición de un producto especial: cada usuario puede comprarlo una sola vez y de a una unidad. |
| **Unidades** | Cantidad disponible de un producto para su venta (nunca negativa; cero significa agotado). |

---

## 4. Reglas de Negocio e Invariantes del Dominio

| # | Regla | Dónde se garantiza | Estado |
|---|---|---|---|
| 1 | Un producto nunca tiene stock negativo; se restituye al cancelar o reembolsar. | `Producto.reducirStock` / `aumentarStock` | ✅ |
| 2 | Solo se reseña una compra `COMPLETADA`; una reseña por usuario y producto. | `Compra.registrarResena`, `Resena.crear`, `SubirResenaUseCase` | ✅ |
| 3 | No se elimina un producto con compras activas (`PENDIENTE` o `COMPLETADA`); la eliminación es lógica. | `Producto.eliminar`, `EliminarProductoUseCase` | ✅ |
| 4 | Un producto en preventa no se compra antes de su fecha de lanzamiento. | `Preventa.validarAcceso`, `RealizarCompraUseCase` | ✅ |
| 5 | Un combo tiene dos o más productos distintos y su descuento es menor a 100%. | `Combo.crear`, `agregarProducto`, `quitarProducto`, `CrearComboUseCase` | ✅ |
| 6 | Los puntos se asignan solo después de reseñar, y una sola vez. | `Compra.asignarPuntos`, `SubirResenaUseCase` | ✅ |
| 7 | Un producto no se repite en la Wishlist ni se agregan productos eliminados. | `Wishlist.agregarProducto`, `AnadirAWishlistUseCase` | ✅ |
| 8 | Solo se notifica a quienes tienen el producto en su Wishlist, y solo cuando hay descuento. | `Producto.aplicarDescuento`, `AplicarDescuentoUseCase` | ✅ |
| 9 | Un producto exclusivo se compra una sola vez por usuario y de a una unidad. | `Producto.validarCompraExclusiva` | ✅ |
| 10 | Un regalo solo se elige para quien no posee el producto, y sigue DISPONIBLE → ELEGIDO → ENVIADO → ENTREGADO. | `Regalo`, `EnviarRegaloUseCase`, `ConfirmarEntregaRegaloUseCase` |✅|

### Reglas adicionales que el código garantiza

- El reembolso solo procede si la compra está `COMPLETADA`, dentro de **48 horas**, sin descarga y sin puntos asignados.
- `REEMBOLSADA`, `CANCELADA` y `EXPIRADA` son estados finales de una compra.
- La fecha de la compra y los ids los genera el servidor, no el cliente.
- Un vendedor dado de baja no puede publicar y sus productos no se pueden comprar.
- Un producto pausado o eliminado no se puede comprar; solo el vendedor dueño modifica, edita, pausa o programa promociones de su producto.
- Una `Promocion` tiene vigencia (fechas inclusive), no se solapa con otra del mismo producto y no cambia el precio base.
- Un cupón es válido hasta su fecha de vencimiento y mientras tenga usos; se aplica una sola vez, solo a compras `PENDIENTE`.
- Comprar un combo valida todos sus productos antes de descontar stock: si uno falla, no se modifica ninguno.
- Una `MetaRecompensa` exige un umbral de compras positivo y se desactiva una sola vez (nunca se borra).
- Las contraseñas exigen 8+ caracteres, mayúscula y número, y solo se guarda su hash (BCrypt).
- El id del comprador o vendedor sale siempre del token JWT, nunca del body ni de la URL; cada ruta valida el rol con `@PreAuthorize`.

---

## 5. Casos de Uso del Sistema (37 Casos de Uso)

**Leyenda:** ✅ Implementado (caso de uso y pruebas) · 🟡 Parcial · ⏳ Especificado, pendiente de implementar.

### 📈 Resumen de avance

| Módulo | ✅ Implementados | 🟡 Parciales | ⏳ Especificados | Total |
| :--- | :---: | :---: | :---: | :---: |
| Compras y Checkout | 6 | 0 | 0 | 6 |
| Productos y Catálogo | 9 | 0 | 0 | 9 |
| Combos Promocionales | 2 | 0 | 0 | 2 |
| Reseñas, Puntos y Recompensas | 4 | 0 | 0 | 4 |
| Lista de Deseos (Wishlist) y Notificaciones | 3 | 0 | 0 | 3 |
| Usuarios, Vendedores y Autenticación | 5 | 0 | 0 | 5 |
| Consultas, Métricas y Reportes | 8 | 0 | 0 | 8 |
| **Total** | **37** | **0** | **0** | **37** |

*(Se han omitido las tablas detalladas de cada módulo por brevedad, asume que están implementados al 100% de acuerdo a la documentación).*

---

## 6. Arquitectura del Software (Clean Architecture)

El backend en Java 21 / Spring Boot implementa **Clean Architecture** estructurado en capas desacopladas:

### Raíces de agregado para la sustentación
- **Producto**: raíz del agregado de catálogo e inventario. Protege invariantes como stock no negativo, baja lógica y visibilidad. El vendedor se referencia por `vendedorId`; no se carga como parte de este agregado.
- **Compra**: raíz del agregado de compra. Controla cantidad, precio unitario congelado y transiciones de estado, además de las reglas de reseña, puntos y reembolso. Referencia `usuarioId` y `productoId` sin contener esos agregados.

Los casos de uso coordinan cambios que afectan a más de un agregado; cada entidad protege sus propias reglas. Las entidades JPA son adaptadores de persistencia separadas de estas raíces de dominio.

```text
backend/src/main/java/com/uniquindio/backend/
├── domain/                  # Capa de Dominio (Sin dependencias de frameworks)
│   ├── entity/              # Entidades Raíz de Agregados (Producto, Compra, Vendedor, Usuario, Regalo, Resena, etc.)
│   ├── valueobject/         # Objetos de Valor (Gama, EstadoCompra, EstadoRegalo, TipoVendedor, Precio, etc.)
│   ├── repository/          # Interfaces de Repositorio de Dominio
│   ├── service/             # Puertos del dominio (EmisorToken, HashContrasena, SesionToken)
│   └── exception/           # Excepciones de Negocio (ReglaDominioException, ConflictoException, AutenticacionException)
├── application/             # Capa de Aplicación (Casos de Uso)
│   ├── usecase/             # Lógica de Orquestación (37 casos de uso)
│   └── dto/                 # Data Transfer Objects (request y response)
└── infrastructure/          # Capa de Infraestructura (Spring Boot / Controller / Persistence)
    ├── rest/                # Controladores REST (uno por recurso), mappers y GlobalExceptionHandler
    ├── persistence/         # Repositorios JPA y en memoria, con sus mappers de persistencia
    ├── security/            # JwtEmisorToken, JwtAuthenticationFilter, BCryptHashContrasena y RespuestaErrorSeguridad
    └── config/              # ClockConfig, SecurityConfig y OpenApiConfig