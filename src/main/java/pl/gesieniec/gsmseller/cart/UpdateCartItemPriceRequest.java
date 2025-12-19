package pl.gesieniec.gsmseller.cart;

import java.math.BigDecimal;

public record UpdateCartItemPriceRequest(
    BigDecimal price
) {}
