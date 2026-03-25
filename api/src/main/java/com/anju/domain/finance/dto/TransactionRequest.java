package com.anju.domain.finance.dto;

import java.math.BigDecimal;

public class TransactionRequest {
    private String transactionNumber;
    private Long appointmentId;
    private Long payerId;
    private BigDecimal amount;
    private String type;
    private String currency;
    private String remark;

    public String getTransactionNumber() { return transactionNumber; }
    public void setTransactionNumber(String transactionNumber) { this.transactionNumber = transactionNumber; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getPayerId() { return payerId; }
    public void setPayerId(Long payerId) { this.payerId = payerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
