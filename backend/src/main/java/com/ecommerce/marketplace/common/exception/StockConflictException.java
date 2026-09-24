package com.ecommerce.marketplace.common.exception;

public class StockConflictException extends RuntimeException {

    public StockConflictException(String message) {
        super(message);
    }
}