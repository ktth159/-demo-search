package com.demo.search.payment

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payments")
class PaymentController(private val paymentService: PaymentService) {

    // 결제 승인 - 프론트에서 결제창 완료 후 호출
    @PostMapping("/confirm")
    fun confirm(@RequestBody request: PaymentConfirmRequest): ResponseEntity<PaymentConfirmResponse> {
        return ResponseEntity.ok(paymentService.confirm(request))
    }

    // 결제 취소
    @PostMapping("/{paymentKey}/cancel")
    fun cancel(
        @PathVariable paymentKey: String,
        @RequestBody request: CancelRequest,
    ): ResponseEntity<PaymentCancelResponse> {
        return ResponseEntity.ok(paymentService.cancel(paymentKey, request.cancelReason))
    }
}

data class CancelRequest(val cancelReason: String)
