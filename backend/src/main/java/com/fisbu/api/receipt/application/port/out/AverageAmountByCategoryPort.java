package com.fisbu.api.receipt.application.port.out;

import java.math.BigDecimal;

public interface AverageAmountByCategoryPort {

    BigDecimal averageByCategoryId(Long categoryId);
}
