package com.fisbu.api.receipt.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase;
import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase.CreateReceiptResult;
import com.fisbu.api.receipt.application.port.in.DeleteReceiptUseCase;
import com.fisbu.api.receipt.application.port.in.ExportReceiptsUseCase;
import com.fisbu.api.receipt.application.port.in.ExportReceiptsUseCase.ExportResult;
import com.fisbu.api.receipt.application.port.in.GetReceiptByIdUseCase;
import com.fisbu.api.receipt.application.port.in.GetReceiptsUseCase;
import com.fisbu.api.receipt.application.port.in.SaveSplitUseCase;
import com.fisbu.api.receipt.application.port.in.SaveSplitUseCase.SplitParticipant;
import com.fisbu.api.receipt.application.port.in.SearchReceiptsUseCase;
import com.fisbu.api.receipt.application.port.in.SetReceiptRemindersUseCase;
import com.fisbu.api.receipt.application.port.in.SuggestCategoryUseCase;
import com.fisbu.api.receipt.application.port.in.SuggestCategoryUseCase.CategorySuggestion;
import com.fisbu.api.receipt.application.port.out.ReceiptPage;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    private final GetReceiptsUseCase getReceiptsUseCase;
    private final SearchReceiptsUseCase searchReceiptsUseCase;
    private final SuggestCategoryUseCase suggestCategoryUseCase;
    private final CreateReceiptUseCase createReceiptUseCase;
    private final GetReceiptByIdUseCase getReceiptByIdUseCase;
    private final DeleteReceiptUseCase deleteReceiptUseCase;
    private final SaveSplitUseCase saveSplitUseCase;
    private final SetReceiptRemindersUseCase setReceiptRemindersUseCase;
    private final ExportReceiptsUseCase exportReceiptsUseCase;
    private final ReceiptWebMapper mapper;

    public ReceiptController(GetReceiptsUseCase getReceiptsUseCase, SearchReceiptsUseCase searchReceiptsUseCase,
                              SuggestCategoryUseCase suggestCategoryUseCase, CreateReceiptUseCase createReceiptUseCase,
                              GetReceiptByIdUseCase getReceiptByIdUseCase, DeleteReceiptUseCase deleteReceiptUseCase,
                              SaveSplitUseCase saveSplitUseCase, SetReceiptRemindersUseCase setReceiptRemindersUseCase,
                              ExportReceiptsUseCase exportReceiptsUseCase, ReceiptWebMapper mapper) {
        this.getReceiptsUseCase = getReceiptsUseCase;
        this.searchReceiptsUseCase = searchReceiptsUseCase;
        this.suggestCategoryUseCase = suggestCategoryUseCase;
        this.createReceiptUseCase = createReceiptUseCase;
        this.getReceiptByIdUseCase = getReceiptByIdUseCase;
        this.deleteReceiptUseCase = deleteReceiptUseCase;
        this.saveSplitUseCase = saveSplitUseCase;
        this.setReceiptRemindersUseCase = setReceiptRemindersUseCase;
        this.exportReceiptsUseCase = exportReceiptsUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ReceiptResponse> getReceipts(@AuthenticationPrincipal UserDetails userDetails) {
        return mapper.toResponseList(getReceiptsUseCase.getReceipts(userDetails.getUsername()));
    }

    // Fiş listesi ekranı: mağaza adına göre arama + kategori filtresi + sayfalama (infinite scroll)
    @GetMapping("/search")
    public PageResponse<ReceiptResponse> searchReceipts(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Long categoryId,
                                                          @RequestParam(defaultValue = "false") boolean uncategorized,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        ReceiptPage result = searchReceiptsUseCase.searchReceipts(
                userDetails.getUsername(), query, categoryId, uncategorized, page, size);
        return PageResponse.of(mapper.toResponseList(result.content()), result.page(), result.size(),
                result.totalElements(), result.totalPages(), result.hasNext());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptResponse createReceipt(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody @Valid ReceiptRequest request) {
        CreateReceiptResult result = createReceiptUseCase.createReceipt(
                mapper.toCommand(userDetails.getUsername(), request));
        return mapper.toResponse(result.receipt(), result.anomalyWarning());
    }

    // Elle fiş eklerken mağaza adına göre kategori önerisi — öneri yoksa 204 döner
    @GetMapping("/category-suggestion")
    public ResponseEntity<CategorySuggestionResponse> suggestCategory(@AuthenticationPrincipal UserDetails userDetails,
                                                                        @RequestParam String storeName) {
        CategorySuggestion suggestion = suggestCategoryUseCase.suggestCategory(userDetails.getUsername(), storeName);
        return suggestion != null
                ? ResponseEntity.ok(new CategorySuggestionResponse(suggestion.categoryId(), suggestion.categoryName()))
                : ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ReceiptResponse getReceiptById(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long id) {
        return mapper.toResponse(getReceiptByIdUseCase.getReceiptById(userDetails.getUsername(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReceipt(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long id) {
        deleteReceiptUseCase.deleteReceipt(userDetails.getUsername(), id);
    }

    @PutMapping("/{id}/split")
    public ReceiptResponse saveSplit(@AuthenticationPrincipal UserDetails userDetails,
                                      @PathVariable Long id,
                                      @RequestBody @Valid SaveSplitRequest request) {
        List<SplitParticipant> participants = request.getParticipants().stream()
                .map(p -> new SplitParticipant(p.getName(), p.getAmount()))
                .toList();
        return mapper.toResponse(saveSplitUseCase.saveSplit(userDetails.getUsername(), id, participants));
    }

    // Garanti/iade hatırlatıcı tarihlerini fiş eklendikten sonra kurar/günceller
    @PutMapping("/{id}/reminders")
    public ReceiptResponse setReminders(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Long id,
                                         @RequestBody SetReceiptRemindersRequest request) {
        return mapper.toResponse(setReceiptRemindersUseCase.setReminders(userDetails.getUsername(), id,
                request.getReturnDeadline(), request.getWarrantyExpiryDate()));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReceipts(@AuthenticationPrincipal UserDetails userDetails,
                                                  @RequestParam String format,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        ExportResult result = exportReceiptsUseCase.exportReceipts(userDetails.getUsername(), format, start, end);
        ContentDisposition disposition = ContentDisposition.attachment().filename(result.filename()).build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(result.content());
    }
}
