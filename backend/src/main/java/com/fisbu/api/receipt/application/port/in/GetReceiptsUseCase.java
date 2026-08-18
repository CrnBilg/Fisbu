package com.fisbu.api.receipt.application.port.in;

import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

public interface GetReceiptsUseCase {

    List<Receipt> getReceipts(String email);
}
