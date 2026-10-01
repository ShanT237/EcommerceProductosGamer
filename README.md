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
5. [Casos de Uso del Sistema (36 Casos de Uso)](#5-casos-de-uso-del-sistema-36-casos-de-uso)
6. [Diseño de Agregados y Dominio (DDD)](#6-diseño-de-agregados-y-dominio-ddd)
7. [Arquitectura del Software (Clean Architecture)](#7-arquitectura-del-software-clean-architecture)
8. [Guía de Instalación y Ejecución](#8-guía-de-instalación-y-ejecución)

---

## 1. ¿Quién compra y por qué?

El marketplace está diseñado conceptualmente para atender las necesidades específicas de la comunidad gamer:

* 🎮 **Gamers:** buscan productos de alto rendimiento que mejoren su experiencia competitiva (periféricos de alta precisión, componentes de hardware, sillas ergonómicas y accesorios dedicados).
* 🎥 **Streamers / Creadores de Contenido:** buscan productos funcionales y estéticamente atractivos (iluminación RGB, placas de captura, micrófonos condensadores, mobiliario) para personalizar sus setups de transmisión.
* 🕹️ **Jugadores Casuales:** buscan productos accesibles que garanticen confort, entretenimiento y una experiencia de juego amigable en su tiempo libre.

---

## 2. ¿Qué hace único a sus vendedores?

El sistema opera como un **marketplace multi-vendedor**, donde diversos comercios independientes pueden registrarse, gestionar su reputación y publicar su propia oferta comercial.

Categorías habilitadas en el catálogo:

* 🖱️ **Periféricos:** mouses, teclados mecánicos, audífonos Surround 7.1, mandos y mousepads XL.
* 🪑 **Mobiliario & Setup:** sillas ergonómicas, escritorios regulables, soportes de monitor e iluminación inteligente.
* 🎮 **Juegos Físicos & Coleccionables:** títulos en formato físico, figuras de acción y preventas exclusivas.
* 💻 **Licencias & Códigos Digitales:** claves para videojuegos, pases de batalla, gift cards y suscripciones.
* 🎁 **Combos Gamer:** paquetes promocionales de 2 o más productos ofrecidos a precio preferencial.

---

## 3. Lenguaje Ubicuo (DDD)

| Término | Definición en el Dominio |
| :--- | :--- |
| **Gama** | Clasificación técnica del producto según prestaciones y precio (`BAJA`, `MEDIA`, `ALTA`, `ENTUSIASTA`). |
| **Preventa** | Modalidad de compra anticipada habilitada antes de la fecha de liberación oficial del producto. |
| **Stock / Inventario** | Unidades físicas o licencias digitales disponibles en inventario (`stock >= 0`). |
| **Wishlist** | Lista de deseos personal para seguimiento de precios, promociones y disponibilidad de stock. |
| **Notificación** | Alerta generada para el comprador sobre cambios de precio o reposición de stock en productos guardados en su Wishlist. |
| **Combo** | Agrupación promocional de 2 o más productos comercializados bajo una sola entidad con descuento. |
| **Comprador / Usuario** | Entidad compradora registrada que realiza transacciones, evalúa productos, redime y acumula puntos. |
| **Vendedor** | Entidad comercial (Persona Natural o Empresa) registrada para publicar y vender productos. |
| **Puntos de Fidelización** | Unidades de beneficio otorgadas a compradores tras publicar reseñas de compras completadas. |
| **Regalo / Recompensa** | Beneficio otorgado por el sistema al usuario tras alcanzar metas de compra acumuladas. |
| **Exclusividad** | Restricción de compra que limita la adquisición a máximo una unidad por comprador. |
| **Eliminación Lógica** | Desactivación del estado visible de una entidad preservando la trazabilidad histórica de transacciones. |

---

## 4. Reglas de Negocio e Invariantes del Dominio

### Reglas Innegociables (100% Implementadas en Código)
1. **Stock No Negativo:** Un producto nunca puede tener stock inferior a cero (`stock >= 0`). Restitución de stock garantizada al cancelar o reembolsar compras.
2. **Reseña Verificada:** Solo se permite calificar y reseñar productos con compras previas en estado `COMPLETADA`. Máximo 1 reseña por usuario y producto.
3. **Integridad de Productos con Compras:** No se puede eliminar físicamente un producto con historial de ventas; se debe usar **eliminación lógica**.
4. **Fechas de Preventa:** Ningún comprador puede acceder a productos en preventa antes de la fecha estipulada.
5. **Estructura de Combos:** Un combo debe estar compuesto obligatoriamente por **mínimo dos (2) productos** distintos pertenecientes a un vendedor.
6. **Acreditación de Puntos:** Los puntos de fidelización solo se asignan al registrar una reseña de una compra confirmada. Se habilita también la redención de puntos (`redimirPuntos`).
7. **Unicidad en Wishlist:** Un comprador no puede añadir duplicados del mismo producto a su Wishlist ni agregar productos eliminados.
8. **Notificaciones Dirigidas (Wishlist):** Notificaciones automáticas por descuento o reposición de stock enviadas a usuarios que tengan el producto en su Wishlist (`NotificarCambioWishlistUseCase`).
9. **Límite Exclusivo:** Productos marcados como exclusivos solo pueden ser comprados una única vez por el mismo usuario.
10. **Metas de Recompensa:** Los regalos solo se asignan a usuarios que hayan cumplido las metas acumuladas de compra exigidas.

---

## 5. Casos de Uso del Sistema (36 Casos de Uso)

### 🛒 Módulo 1: Compras y Checkout
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-01` | **Realizar Compra** | ✅ Implementado | Comprador | Registra orden `PENDIENTE`, reserva stock y valida producto no eliminado, usuario activo y vendedor activo (`RealizarCompraUseCase`). |
| `CU-09` | **Confirmar Compra** | ✅ Implementado | Comprador / Sistema | Valida el pago y pasa la orden a `COMPLETADA` (`ConfirmarCompraUseCase`). |
| `CU-12` | **Cancelar Compra** | ✅ Implementado | Comprador | Pasa la orden a `CANCELADA` y devuelve automáticamente el stock retenido al producto (`CancelarCompraUseCase`). |
| `CU-13` | **Solicitar Reembolso** | ✅ Implementado | Comprador | Solicita reembolso dentro del plazo legal, pasa a `REEMBOLSADA` y restituye el stock al inventario (`SolicitarReembolsoUseCase`). |
| `CU-16` | **Comprar Combo** | ⏳ Especificado | Comprador | Permite adquirir un combo verificando stock de todos sus integrantes. |
| `CU-17` | **Aplicar Cupón de Descuento** | ⏳ Especificado | Comprador | Aplica un código promocional al total del pago previo al checkout. |

### 📦 Módulo 2: Productos y Catálogo
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-04` | **Publicar Producto** | ✅ Implementado | Vendedor | Crea un nuevo producto especificando gama, precio, stock y atributos (`PublicarProductoUseCase`). |
| `CU-05` | **Modificar Producto** | ⏳ Especificado | Vendedor | Actualiza precio, descripción o disponibilidad de preventa. |
| `CU-06` | **Eliminar Producto** | ✅ Implementado | Vendedor | Desactiva producto preservando trazabilidad; rechaza baja si hay compras activas (`EliminarProductoUseCase`). |
| `CU-18` | **Gestionar / Aumentar Stock** | ✅ Implementado | Vendedor | Incrementa el número de unidades disponibles en inventario (`Producto.aumentarStock`). |
| `CU-19` | **Editar Información General** | ⏳ Especificado | Vendedor | Actualiza detalles secundarios e imágenes del producto. |
| `CU-20` | **Filtrar y Buscar Productos** | ⏳ Especificado | Comprador | Búsqueda avanzada por gama, categoría, precio y preventas. |
| `CU-21` | **Activar / Pausar Publicación** | ⏳ Especificado | Vendedor | Alterna visibilidad del producto en el catálogo sin borrarlo. |
| `CU-22` | **Configurar Descuento Promocional** | ⏳ Especificado | Vendedor | Establece porcentajes de descuento temporales en un producto. |

### 🎁 Módulo 3: Combos Promocionales
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-07` | **Crear Combo Gamer** | ✅ Implementado (Dominio) | Vendedor | Agrupa 2 o más productos sin duplicados y con descuento en la entidad `Combo`. |
| `CU-23` | **Modificar Productos de Combo**| ✅ Implementado (Dominio) | Vendedor | Añade o remueve productos garantizando el mínimo de 2 unidades. |

### ⭐ Módulo 4: Reseñas, Puntos y Recompensas
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-02` | **Subir Reseña y Asignar Puntos** | ✅ Implementado | Comprador | Publica reseña sobre compra completada, valida 1 reseña por usuario/producto y asigna puntos (`SubirResenaUseCase`). |
| `CU-08` | **Redimir Puntos por Beneficios** | ✅ Implementado (Dominio) | Comprador | Canjea puntos acumulados con validación de saldo positivo (`Usuario.redimirPuntos`). |
| `CU-10` | **Asignar Regalo por Meta** | ⏳ Especificado | Admin / Sistema | Concede regalos del catálogo al cumplir metas acumuladas de compra. |
| `CU-24` | **Enviar / Confirmar Entrega Regalo**| ⏳ Especificado | Admin / Sistema | Administra la logística y cambio de estado del regalo asignado. |

### ❤️ Módulo 5: Lista de Deseos (Wishlist) y Notificaciones
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-03` | **Añadir a Wishlist** | ✅ Implementado | Comprador | Guarda productos en wishlist evitando duplicados y rechazando eliminados (`AnadirAWishlistUseCase`). |
| `CU-14` | **Notificar Cambio Wish List** | ✅ Implementado | Sistema | Genera notificaciones automáticas por cambio de precio o reabastecimiento de stock (`NotificarCambioWishlistUseCase`). |
| `CU-15` | **Quitar de Wishlist** | ✅ Implementado (Dominio) | Comprador | Elimina un producto específico de la lista de deseos (`Wishlist.quitarProducto`). |

### 👤 Módulo 6: Usuarios, Vendedores y Autenticación
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-11` | **Registrar Usuario / Vendedor** | ⏳ Especificado | Público | Registro de cuentas de compradores o perfiles de vendedor. |
| `CU-25` | **Dar de Baja Vendedor** | ✅ Implementado (Dominio) | Admin / Vendedor | Desactiva la cuenta del vendedor conservando la historia comercial (`Vendedor.darDeBaja`). |
| `CU-26` | **Editar Perfil y Seguridad** | ⏳ Especificado | Usuario | Modificación de datos personales, dirección y contraseña. |
| `CU-27` | **Iniciar Sesión (JWT)** | ⏳ Especificado | Todos | Autenticación con token JWT seguro para el acceso a las APIs. |
| `CU-28` | **Valorar Vendedor** | ⏳ Especificado | Comprador | Calificación directa del servicio y atención brindada por el vendedor. |

### 📊 Módulo 7: Consultas, Métricas y Reportes
| Código | Caso de Uso | Estado Código | Actor | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `CU-29` | **Reporte de Ventas por Vendedor** | ⏳ Especificado | Vendedor | Balance de ingresos, unidades vendidas y comisiones. |
| `CU-30` | **Reporte de Productos Más Vendidos**| ⏳ Especificado | Admin / Vendedor | Escalafón de productos con mayor volumen de venta y calificación. |
| `CU-31` | **Consultar Historial de Compras** | ⏳ Especificado | Comprador | Muestra órdenes históricas, comprobantes y estados de entrega. |
| `CU-32` | **Consultar Historial de Puntos** | ⏳ Especificado | Comprador | Detalle de saldo de puntos ganados y redimidos. |
| `CU-33` | **Gestionar Metas de Recompensas** | ⏳ Especificado | Admin | Configura umbrales de compras para desbloquear regalos. |
| `CU-34` | **Alertas de Stock Bajo** | ⏳ Especificado | Vendedor | Reporte de inventario crítico o agotado para reabastecimiento. |
| `CU-35` | **Métricas de Reseñas** | ⏳ Especificado | Admin / Vendedor | Dashboard de satisfacción del cliente y calificaciones. |
| `CU-36` | **Reporte Global del Marketplace** | ⏳ Especificado | Admin | Consolidado ejecutivo del volumen total de operaciones. |

---

## 6. Diseño de Agregados y Dominio (DDD)

El modelo de dominio se compone de los agregados principales implementados en `domain/entity`:

```mermaid
classDiagram
    class Producto {
        +UUID id
        +UUID vendedorId
        +String nombre
        +double precio
        +int stock
        +Gama gama
        +Exclusividad exclusividad
        +boolean eliminado
        +reducirStock()
        +aumentarStock()
        +eliminar()
    }

    class Vendedor {
        +UUID id
        +String nombre
        +String email
        +TipoVendedor tipo
        +String documento
        +boolean activo
        +darDeBaja()
    }

    class Compra {
        +UUID id
        +UUID usuarioId
        +UUID productoId
        +Precio precioUnitario
        +int cantidad
        +EstadoCompra estado
        +confirmar()
        +cancelar()
        +expirar()
        +solicitarReembolso()
    }

    class Regalo {
        +UUID id
        +UUID usuarioId
        +UUID productoId
        +EstadoRegalo estado
    }

    class Usuario {
        +UUID id
        +String nombre
        +String correo
        +int puntos
        +boolean activo
        +acumularPuntos()
        +redimirPuntos()
        +desactivar()
    }

    class Notificacion {
        +UUID id
        +UUID usuarioId
        +UUID productoId
        +TipoNotificacion tipo
        +String mensaje
        +boolean leida
        +marcarComoLeida()
    }

    class Wishlist {
        +UUID id
        +UUID usuarioId
        +List~UUID~ productos
        +agregarProducto()
        +quitarProducto()
    }

    class Combo {
        +UUID id
        +UUID vendedorId
        +String nombre
        +double descuento
        +List~UUID~ productos
        +agregarProducto()
        +quitarProducto()
        +calcularPrecioTotal()
    }

    Vendedor "1" -- "N" Producto : publica
    Vendedor "1" -- "N" Combo : crea
    Compra "N" -- "1" Producto : referencia
    Compra "N" -- "1" Usuario : realiza
    Wishlist "1" -- "1" Usuario : pertenece a
    Notificacion "N" -- "1" Usuario : notifica a
    Regalo "N" -- "1" Usuario : asignado a
```

---

## 7. Arquitectura del Software (Clean Architecture)

El backend en Java 21 / Spring Boot implementa **Clean Architecture** estructurado en capas desacopladas:

```
backend/src/main/java/com/uniquindio/backend/
├── domain/                  # Capa de Dominio (Sin dependencias de frameworks)
│   ├── entity/              # Entidades Raíz de Agregados (Producto, Compra, Vendedor, Usuario, Regalo, Resena, Wishlist, Combo, Preventa, Notificacion)
│   ├── valueobject/         # Objetos de Valor (Gama, EstadoCompra, EstadoRegalo, TipoVendedor, Precio, Exclusividad, TipoNotificacion)
│   ├── repository/          # Interfaces de Repositorio de Dominio
│   └── exception/           # Excepciones de Negocio
├── application/             # Capa de Aplicación (Casos de Uso)
│   ├── usecase/             # Lógica de Orquestación (RealizarCompra, ConfirmarCompra, CancelarCompra, SolicitarReembolso, PublicarProducto, EliminarProducto, SubirResena, AnadirAWishlist, NotificarCambioWishlist)
│   └── dto/                 # Data Transfer Objects (DTOs)
└── infrastructure/          # Capa de Infraestructura (Spring Boot / Controller / Persistence)
    ├── controller/          # Controladores REST API
    └── persistence/         # Repositorios en Memoria y Persistencia JPA / MariaDB
```

---

## 8. Guía de Instalación y Ejecución

### Prerrequisitos
* **Java Development Kit (JDK):** Versión 21 o superior.
* **Base de Datos:** MariaDB / MySQL.
* **Gestor de Construcción:** Gradle (incluido en el proyecto vía Wrapper `./gradlew`).

### Pasos de Configuración

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/ShanT237/EcommerceProductosGamer.git
   cd EcommerceProductosGamer/backend
   ```

2. **Compilar el proyecto:**
   ```bash
   ./gradlew build
   ```

3. **Ejecutar las pruebas unitarias e integración:**
   ```bash
   ./gradlew test
   ```

4. **Iniciar el servidor backend:**
   ```bash
   ./gradlew bootRun
   ```

El servidor Spring Boot iniciará por defecto en `http://localhost:8080`.
