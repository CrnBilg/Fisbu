package com.fisbu.api.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fisbu.api.category.application.port.in.CreateCategoryUseCase.CreateCategoryCommand;
import com.fisbu.api.category.application.port.in.DeleteCategoryUseCase.DeleteCategoryCommand;
import com.fisbu.api.category.application.port.in.UpdateCategoryUseCase.UpdateCategoryCommand;
import com.fisbu.api.category.application.port.out.DeleteBudgetsByCategoryPort;
import com.fisbu.api.category.application.port.out.DeleteCategoryPort;
import com.fisbu.api.category.application.port.out.FindCategoryByNamePort;
import com.fisbu.api.category.application.port.out.LoadCategoriesPort;
import com.fisbu.api.category.application.port.out.LoadCategoryPort;
import com.fisbu.api.category.application.port.out.ResolveUserIdPort;
import com.fisbu.api.category.application.port.out.SaveCategoryPort;
import com.fisbu.api.category.application.port.out.UnlinkReceiptsFromCategoryPort;
import com.fisbu.api.category.domain.Category;
import com.fisbu.api.category.domain.exception.CategoryAccessDeniedException;
import com.fisbu.api.category.domain.exception.CategoryNameAlreadyExistsException;
import com.fisbu.api.category.domain.exception.CategoryNotFoundException;
import com.fisbu.api.category.domain.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final String EMAIL = "test@fisbu.com";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long CATEGORY_ID = 10L;

    @Mock
    private ResolveUserIdPort resolveUserIdPort;
    @Mock
    private LoadCategoriesPort loadCategoriesPort;
    @Mock
    private LoadCategoryPort loadCategoryPort;
    @Mock
    private FindCategoryByNamePort findCategoryByNamePort;
    @Mock
    private SaveCategoryPort saveCategoryPort;
    @Mock
    private DeleteCategoryPort deleteCategoryPort;
    @Mock
    private DeleteBudgetsByCategoryPort deleteBudgetsByCategoryPort;
    @Mock
    private UnlinkReceiptsFromCategoryPort unlinkReceiptsFromCategoryPort;

    private CategoryService newService() {
        return new CategoryService(resolveUserIdPort, loadCategoriesPort, loadCategoryPort, findCategoryByNamePort,
                saveCategoryPort, deleteCategoryPort, deleteBudgetsByCategoryPort, unlinkReceiptsFromCategoryPort);
    }

    private Category category(Long id, Long userId, String name) {
        return new Category(id, userId, name, "#4CAF50");
    }

    @Test
    void getCategories_returnsCategoriesForResolvedUser() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadCategoriesPort.loadByUserId(USER_ID)).thenReturn(List.of(category(CATEGORY_ID, USER_ID, "Market")));

        List<Category> result = newService().getCategories(EMAIL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Market");
    }

    @Test
    void getCategories_throws_whenUserNotFound() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().getCategories(EMAIL)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void createCategory_savesNewCategory_whenNameIsFree() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(findCategoryByNamePort.findByUserIdAndNameIgnoreCase(USER_ID, "Market")).thenReturn(Optional.empty());
        when(saveCategoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Category result = newService().createCategory(new CreateCategoryCommand(EMAIL, "Market", "#4CAF50"));

        assertThat(result.name()).isEqualTo("Market");
        assertThat(result.userId()).isEqualTo(USER_ID);
    }

    @Test
    void createCategory_throws_whenNameAlreadyTaken() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(findCategoryByNamePort.findByUserIdAndNameIgnoreCase(USER_ID, "Market"))
                .thenReturn(Optional.of(category(CATEGORY_ID, USER_ID, "Market")));

        assertThatThrownBy(() -> newService().createCategory(new CreateCategoryCommand(EMAIL, "Market", "#000")))
                .isInstanceOf(CategoryNameAlreadyExistsException.class);
        verify(saveCategoryPort, never()).save(any());
    }

    @Test
    void updateCategory_allowsKeepingItsOwnName() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID, USER_ID, "Market")));
        when(findCategoryByNamePort.findByUserIdAndNameIgnoreCase(USER_ID, "Market"))
                .thenReturn(Optional.of(category(CATEGORY_ID, USER_ID, "Market")));
        when(saveCategoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Category result = newService().updateCategory(new UpdateCategoryCommand(EMAIL, CATEGORY_ID, "Market", "#111"));

        assertThat(result.color()).isEqualTo("#111");
    }

    @Test
    void updateCategory_throws_whenRenamingToAnotherExistingCategoryName() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID, USER_ID, "Market")));
        when(findCategoryByNamePort.findByUserIdAndNameIgnoreCase(USER_ID, "Eğlence"))
                .thenReturn(Optional.of(category(99L, USER_ID, "Eğlence")));

        assertThatThrownBy(() -> newService()
                .updateCategory(new UpdateCategoryCommand(EMAIL, CATEGORY_ID, "Eğlence", "#111")))
                .isInstanceOf(CategoryNameAlreadyExistsException.class);
    }

    @Test
    void updateCategory_throws_whenCategoryBelongsToAnotherUser() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadCategoryPort.loadById(CATEGORY_ID))
                .thenReturn(Optional.of(category(CATEGORY_ID, OTHER_USER_ID, "Market")));

        assertThatThrownBy(() -> newService()
                .updateCategory(new UpdateCategoryCommand(EMAIL, CATEGORY_ID, "Market", "#111")))
                .isInstanceOf(CategoryAccessDeniedException.class);
    }

    @Test
    void updateCategory_throws_whenCategoryDoesNotExist() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService()
                .updateCategory(new UpdateCategoryCommand(EMAIL, CATEGORY_ID, "Market", "#111")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deleteCategory_deletesBudgetsAndUnlinksReceiptsBeforeDeletingCategory() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadCategoryPort.loadById(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID, USER_ID, "Market")));

        newService().deleteCategory(new DeleteCategoryCommand(EMAIL, CATEGORY_ID));

        verify(deleteBudgetsByCategoryPort).deleteBudgetsByCategory(CATEGORY_ID);
        verify(unlinkReceiptsFromCategoryPort).unlinkReceiptsFromCategory(CATEGORY_ID);
        verify(deleteCategoryPort).deleteById(CATEGORY_ID);
    }

    @Test
    void deleteCategory_throws_whenCategoryBelongsToAnotherUser() {
        when(resolveUserIdPort.resolveUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
        when(loadCategoryPort.loadById(CATEGORY_ID))
                .thenReturn(Optional.of(category(CATEGORY_ID, OTHER_USER_ID, "Market")));

        assertThatThrownBy(() -> newService().deleteCategory(new DeleteCategoryCommand(EMAIL, CATEGORY_ID)))
                .isInstanceOf(CategoryAccessDeniedException.class);
        verify(deleteCategoryPort, never()).deleteById(eq(CATEGORY_ID));
    }
}
