package com.demo.search.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class PaymentPageController(private val paymentService: PaymentService) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 결제 성공 시 토스페이먼츠가 리다이렉트하는 URL
    // ?paymentKey=...&orderId=...&amount=... 쿼리파라미터로 전달됨
    @GetMapping("/payment/success")
    @ResponseBody
    fun success(
        @RequestParam paymentKey: String,
        @RequestParam orderId: String,
        @RequestParam amount: Int,
    ): String {
        log.debug("결제 성공 콜백 orderId={}", orderId)

        return try {
            val result = paymentService.confirm(
                PaymentConfirmRequest(
                    paymentKey = paymentKey,
                    orderId = orderId,
                    amount = amount,
                )
            )
            """
            <html><body style="font-family:sans-serif; max-width:400px; margin:60px auto; text-align:center">
            <h2>✅ 결제 완료!</h2>
            <p>주문번호: ${result.orderId}</p>
            <p>결제수단: ${result.method}</p>
            <p>금액: ${"%,d".format(result.amount)}원</p>
            <a href="/payment.html">← 다시 테스트</a>
            </body></html>
            """.trimIndent()
        } catch (e: Exception) {
            log.error("결제 승인 실패", e)
            """
            <html><body style="font-family:sans-serif; max-width:400px; margin:60px auto; text-align:center">
            <h2>❌ 결제 승인 실패</h2>
            <p>${e.message}</p>
            <a href="/payment.html">← 다시 테스트</a>
            </body></html>
            """.trimIndent()
        }
    }

    // 결제 실패 시 리다이렉트
    @GetMapping("/payment/fail")
    @ResponseBody
    fun fail(
        @RequestParam code: String,
        @RequestParam message: String,
        @RequestParam(required = false) orderId: String?,
    ): String {
        log.debug("결제 실패 code={}, message={}", code, message)
        return """
        <html><body style="font-family:sans-serif; max-width:400px; margin:60px auto; text-align:center">
        <h2>❌ 결제 실패</h2>
        <p>사유: $message</p>
        <p>코드: $code</p>
        <a href="/payment.html">← 다시 테스트</a>
        </body></html>
        """.trimIndent()
    }
}
