package com.fisbu.api.receipt.adapter.in.web;

import java.time.LocalDate;

public class SetReceiptRemindersRequest {
    private LocalDate returnDeadline;
    private LocalDate warrantyExpiryDate;

    public LocalDate getReturnDeadline() { return returnDeadline; }
    public void setReturnDeadline(LocalDate returnDeadline) { this.returnDeadline = returnDeadline; }
    public LocalDate getWarrantyExpiryDate() { return warrantyExpiryDate; }
    public void setWarrantyExpiryDate(LocalDate warrantyExpiryDate) { this.warrantyExpiryDate = warrantyExpiryDate; }
}
