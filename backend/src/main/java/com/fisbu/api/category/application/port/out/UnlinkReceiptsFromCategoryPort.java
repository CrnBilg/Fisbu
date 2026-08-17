package com.fisbu.api.category.application.port.out;

// Receipt modülü henüz hexagonal'a taşınmadığı için geçici köprü port'u.
// Receipt migrate olduğunda bu port kaldırılıp Receipt modülünün kendi out-port'u kullanılacak.
public interface UnlinkReceiptsFromCategoryPort {

    void unlinkReceiptsFromCategory(Long categoryId);
}
