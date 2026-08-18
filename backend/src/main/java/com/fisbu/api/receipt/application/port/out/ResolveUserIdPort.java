package com.fisbu.api.receipt.application.port.out;

import java.util.Optional;

public interface ResolveUserIdPort {

    Optional<Long> resolveUserIdByEmail(String email);
}
