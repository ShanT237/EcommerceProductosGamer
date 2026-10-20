package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.AnadirWishlistResponse;
import com.uniquindio.backend.domain.entity.Wishlist;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {
    public AnadirWishlistResponse toResponse(Wishlist wishlist) {
        return AnadirWishlistResponse.desde(wishlist);
    }
}