package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wishlists")
@Getter @Setter
@NoArgsConstructor
public class WishlistEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wishlist_productos", joinColumns = @JoinColumn(name = "wishlist_id"))
    @Column(name = "producto_id")
    private List<UUID> productos = new ArrayList<>();
}