package com.csse3200.game.components.shop;

/** A catalogue's immutable offer. Presentation metadata stays in the existing item catalogue. */
public record ShopOffer(String offerId, ShopProductKind kind, String productId, int goldPrice) {}
