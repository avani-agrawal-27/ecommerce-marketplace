package com.ecommerce.marketplace.order.dto;

import com.ecommerce.marketplace.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull
        OrderStatus status
) {
}