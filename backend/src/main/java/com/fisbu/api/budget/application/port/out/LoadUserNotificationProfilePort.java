package com.fisbu.api.budget.application.port.out;

import java.util.Optional;

// User modülü henüz hexagonal'a taşınmadığı için geçici köprü port'u.
public interface LoadUserNotificationProfilePort {

    Optional<UserNotificationProfile> loadProfileByUserId(Long userId);
}
