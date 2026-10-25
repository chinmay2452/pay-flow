package com.example.payflow.pay.flow.payment;

import com.example.payflow.pay.flow.payment.dto.CreatePaymentRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @PostMapping
    public String createPayment(
            @RequestBody CreatePaymentRequest request) {

        return "Payment request received for amount: "
                + request.amount();
    }
}