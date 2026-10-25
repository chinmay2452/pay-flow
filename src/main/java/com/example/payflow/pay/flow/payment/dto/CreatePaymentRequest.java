package com.example.payflow.pay.flow.payment.dto;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount
) {}
