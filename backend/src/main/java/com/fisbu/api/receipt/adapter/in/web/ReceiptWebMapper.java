package com.fisbu.api.receipt.adapter.in.web;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase;
import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase.CreateReceiptCommand;
import com.fisbu.api.receipt.application.port.in.CreateReceiptUseCase.ReceiptItemCommand;
import com.fisbu.api.receipt.application.port.in.CreateReceiptsBulkUseCase.BulkCreateResult;
import com.fisbu.api.receipt.domain.Receipt;
import com.fisbu.api.receipt.domain.ReceiptItem;

@Component
public class ReceiptWebMapper {

    private static final Logger log = LoggerFactory.getLogger(ReceiptWebMapper.class);

    private final ObjectMapper objectMapper;

    public ReceiptWebMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ReceiptResponse toResponse(Receipt receipt) {
        return toResponse(receipt, null);
    }

    public ReceiptResponse toResponse(Receipt receipt, String anomalyWarning) {
        ReceiptResponse response = new ReceiptResponse();
        response.setId(receipt.id());
        response.setStoreName(receipt.storeName());
        response.setTotalAmount(receipt.totalAmount());
        response.setReceiptDate(receipt.receiptDate());
        response.setImageUrl(receipt.imageUrl());
        response.setRawOcrText(receipt.rawOcrText());
        response.setCreatedAt(receipt.createdAt());
        response.setReturnDeadline(receipt.returnDeadline());
        response.setWarrantyExpiryDate(receipt.warrantyExpiryDate());
        response.setCategoryId(receipt.categoryId());
        response.setCategoryName(receipt.categoryName());
        response.setAnomalyWarning(anomalyWarning);

        List<ReceiptItemResponse> itemResponses = new ArrayList<>();
        if (receipt.items() != null) {
            for (ReceiptItem item : receipt.items()) {
                ReceiptItemResponse itemResponse = new ReceiptItemResponse();
                itemResponse.setId(item.id());
                itemResponse.setProductName(item.productName());
                itemResponse.setUnitPrice(item.unitPrice());
                itemResponse.setQuantity(item.quantity());
                itemResponses.add(itemResponse);
            }
        }
        response.setItems(itemResponses);

        if (receipt.splitDetailsJson() != null) {
            try {
                response.setSplitParticipants(objectMapper.readValue(
                        receipt.splitDetailsJson(), new TypeReference<List<SplitParticipantDto>>() {}));
            } catch (Exception e) {
                log.error("Fiş #{} için splitDetailsJson ayrıştırılamadı: {}", receipt.id(), e.getMessage());
                response.setSplitParticipants(null);
            }
        }

        return response;
    }

    public List<ReceiptResponse> toResponseList(List<Receipt> receipts) {
        List<ReceiptResponse> result = new ArrayList<>();
        for (Receipt receipt : receipts) {
            result.add(toResponse(receipt));
        }
        return result;
    }

    public CreateReceiptCommand toCommand(String email, ReceiptRequest request) {
        List<ReceiptItemCommand> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (ReceiptItemRequest item : request.getItems()) {
                items.add(new ReceiptItemCommand(item.getProductName(), item.getUnitPrice(), item.getQuantity()));
            }
        }

        return new CreateReceiptCommand(email, request.getStoreName(), request.getTotalAmount(),
                request.getReceiptDate(), request.getImageUrl(), request.getRawOcrText(), request.getCategoryId(),
                request.getReturnDeadline(), request.getWarrantyExpiryDate(), request.isAllowDuplicate(), items);
    }

    public BulkReceiptImportResponse toBulkResponse(BulkCreateResult result) {
        BulkReceiptImportResponse response = new BulkReceiptImportResponse();

        List<ReceiptResponse> created = new ArrayList<>();
        for (CreateReceiptUseCase.CreateReceiptResult item : result.created()) {
            created.add(toResponse(item.receipt(), item.anomalyWarning()));
        }
        response.setCreated(created);

        List<BulkImportError> failed = new ArrayList<>();
        for (var error : result.failed()) {
            failed.add(new BulkImportError(error.index(), error.error()));
        }
        response.setFailed(failed);

        return response;
    }
}
