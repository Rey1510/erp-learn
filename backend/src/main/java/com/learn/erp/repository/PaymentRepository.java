package com.learn.erp.repository;

import com.learn.erp.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentNumber(String paymentNumber);
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<Payment> findByStatusAndExpiresAtBefore(String status, java.time.LocalDateTime time);
}
