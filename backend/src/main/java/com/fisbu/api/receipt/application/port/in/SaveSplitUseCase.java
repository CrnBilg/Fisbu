package com.fisbu.api.receipt.application.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.fisbu.api.receipt.domain.Receipt;

public interface SaveSplitUseCase {

    Receipt saveSplit(String email, Long receiptId, List<SplitParticipant> participants);

    record SplitParticipant(String name, BigDecimal amount) {
    }
}
