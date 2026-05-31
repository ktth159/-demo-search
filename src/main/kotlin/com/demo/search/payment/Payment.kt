package com.demo.search.payment

import jakarta.persistence.*

@Entity
@Table(name = "payments")
class Payment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true)
    val orderId: String,            // 주문 ID (중복 결제 방지용 UNIQUE)

    val paymentKey: String,         // 토스페이먼츠가 발급한 결제 키
    val amount: Int,

    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.PAID,

    val method: String? = null,     // 카드, 가상계좌, 간편결제 등
)

enum class PaymentStatus {
    PAID,       // 결제 완료
    CANCELLED,  // 취소
}
