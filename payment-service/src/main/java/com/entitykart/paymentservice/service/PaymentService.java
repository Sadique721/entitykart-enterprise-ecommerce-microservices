package com.entitykart.paymentservice.service;

import com.entitykart.paymentservice.client.OrderServiceClient;
import com.entitykart.paymentservice.dto.PaymentProcessedEvent;
import com.entitykart.paymentservice.dto.PaymentRequest;
import com.entitykart.paymentservice.entity.PaymentEntity;
import com.entitykart.paymentservice.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.authorize.Environment;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.CreditCardType;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.PaymentType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionResponse;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderServiceClient orderServiceClient;

    @Value("${authorize.net.api-login-id}")
    private String apiLoginId;

    @Value("${authorize.net.transaction-key}")
    private String transactionKey;

    @Value("${authorize.net.environment:sandbox}")
    private String environment;

    // ─── Card Payment (Authorize.Net sandbox or mock) ─────────────────────────

    @Transactional
    public PaymentEntity processCardPayment(PaymentRequest request) {
        boolean isMockMode = "test".equalsIgnoreCase(environment)
                || "mock".equalsIgnoreCase(environment)
                || apiLoginId == null
                || apiLoginId.contains("dummy")
                || apiLoginId.contains("your");

        if (isMockMode) {
            PaymentEntity payment = new PaymentEntity();
            payment.setOrderId(request.getOrderId());
            payment.setAmount(request.getAmount());
            payment.setPaymentMode(PaymentEntity.PaymentMode.CARD);
            payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
            payment.setTransactionRef("MOCK_CARD_" + System.currentTimeMillis());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setGatewayResponseText("Simulated Approved");
            return saveAndPublish(payment, request.getCustomerEmail(), request.getCustomerName());
        }

        configureAuthorizeNet();

        MerchantAuthenticationType merchantAuth = new MerchantAuthenticationType();
        merchantAuth.setName(apiLoginId);
        merchantAuth.setTransactionKey(transactionKey);
        ApiOperationBase.setMerchantAuthentication(merchantAuth);

        CreditCardType creditCard = new CreditCardType();
        creditCard.setCardNumber(request.getCardNumber().replaceAll("\\s", ""));
        creditCard.setExpirationDate(formatExpiry(request.getExpiryMonth(), request.getExpiryYear()));
        creditCard.setCardCode(request.getCvv());

        PaymentType paymentType = new PaymentType();
        paymentType.setCreditCard(creditCard);

        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        txnRequest.setPayment(paymentType);
        txnRequest.setAmount(BigDecimal.valueOf(request.getAmount()));

        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(merchantAuth);
        apiRequest.setTransactionRequest(txnRequest);

        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMode(PaymentEntity.PaymentMode.CARD);

        if (response != null && response.getMessages().getResultCode() == MessageTypeEnum.OK) {
            applySuccessfulGatewayResponse(payment, response.getTransactionResponse());
            log.info("Payment SUCCESS for order: {}", request.getOrderId());
        } else {
            payment.setPaymentStatus(PaymentEntity.PaymentStatus.FAILED);
            payment.setGatewayResponseText(getGatewayFailureMessage(response));
            log.error("Payment FAILED for order: {}", request.getOrderId());
        }

        return saveAndPublish(payment, request.getCustomerEmail(), request.getCustomerName());
    }

    // ─── UPI / COD — Offline / generic payment ────────────────────────────────

    @Transactional
    public PaymentEntity processOfflinePayment(Long orderId, Double amount, String paymentMode) {
        return processOfflinePayment(orderId, amount, paymentMode, null, null);
    }

    @Transactional
    public PaymentEntity processOfflinePayment(Long orderId, Double amount, String paymentMode,
                                                String customerEmail, String customerName) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.valueOf(paymentMode));

        String prefix;
        switch (paymentMode.toUpperCase()) {
            case "UPI":         prefix = "UPI_";  break;
            case "NET_BANKING": prefix = "NB_";   break;
            case "WALLET":      prefix = "WLT_";  break;
            case "EMI":         prefix = "EMI_";  break;
            case "COD":         prefix = "COD_PENDING_" + orderId + "_"; break;
            default:            prefix = "OFFLINE_"; break;
        }
        payment.setTransactionRef(prefix + System.currentTimeMillis());

        if ("COD".equals(paymentMode.toUpperCase())) {
            // COD stays PENDING until order is DELIVERED
            payment.setPaymentStatus(PaymentEntity.PaymentStatus.PENDING);
        } else {
            payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
        }

        return saveAndPublish(payment, customerEmail, customerName);
    }

    // ─── Net Banking ──────────────────────────────────────────────────────────

    @Transactional
    public PaymentEntity processNetBankingPayment(Long orderId, Double amount, String bankName,
                                                   String customerEmail, String customerName) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.NET_BANKING);
        payment.setTransactionRef("NB_" + (bankName != null ? bankName.toUpperCase() : "BANK") + "_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText("Net Banking via " + bankName + " — Simulated Success");
        log.info("Net Banking payment for order {} via bank {}", orderId, bankName);
        return saveAndPublish(payment, customerEmail, customerName);
    }

    // ─── Wallet ───────────────────────────────────────────────────────────────

    @Transactional
    public PaymentEntity processWalletPayment(Long orderId, Double amount, String walletType,
                                               String customerEmail, String customerName) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.WALLET);
        payment.setTransactionRef("WLT_" + (walletType != null ? walletType.toUpperCase() : "WALLET") + "_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText("Wallet payment via " + walletType + " — Simulated Success");
        log.info("Wallet payment for order {} via {}", orderId, walletType);
        return saveAndPublish(payment, customerEmail, customerName);
    }

    // ─── EMI ─────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentEntity processEmiPayment(Long orderId, Double amount, String cardNumber,
                                            Integer emiTenure, String customerEmail, String customerName) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.EMI);
        payment.setTransactionRef("EMI_" + emiTenure + "M_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText("EMI payment — " + emiTenure + " months — Simulated Success");
        log.info("EMI payment for order {} with {} month tenure", orderId, emiTenure);
        return saveAndPublish(payment, customerEmail, customerName);
    }

    // ─── COD Transaction Assignment (called when order status = DELIVERED) ───

    @Transactional
    public PaymentEntity assignCodTransaction(Long orderId) {
        PaymentEntity payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    PaymentEntity p = new PaymentEntity();
                    p.setOrderId(orderId);
                    p.setPaymentMode(PaymentEntity.PaymentMode.COD);
                    return p;
                });

        payment.setTransactionRef("COD_DELIVERED_" + orderId + "_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponseText("COD collected on delivery");

        PaymentEntity saved = paymentRepository.save(payment);
        // Update order payment status to PAID
        orderServiceClient.updateOrderPaymentStatus(orderId, "PAID");
        log.info("COD transaction assigned for delivered order: {}", orderId);
        return saved;
    }

    // ─── Read operations ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentEntity getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    @Transactional(readOnly = true)
    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private PaymentEntity saveAndPublish(PaymentEntity payment, String customerEmail, String customerName) {
        PaymentEntity saved = paymentRepository.save(payment);

        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId(saved.getOrderId());
        event.setStatus(saved.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS ? "SUCCESS" : "FAILED");
        event.setTransactionRef(saved.getTransactionRef());
        event.setCustomerEmail(customerEmail);
        event.setCustomerName(customerName);
        event.setAmount(saved.getAmount());

        if (saved.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS) {
            orderServiceClient.updateOrderPaymentStatus(saved.getOrderId(), "PAID");
        } else if (saved.getPaymentStatus() == PaymentEntity.PaymentStatus.FAILED) {
            orderServiceClient.updateOrderPaymentStatus(saved.getOrderId(), "UNPAID");
        }
        // PENDING (COD) — don't update order status until delivery

        kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event);
        return saved;
    }

    private void configureAuthorizeNet() {
        if ("production".equalsIgnoreCase(environment)) {
            ApiOperationBase.setEnvironment(Environment.PRODUCTION);
        } else {
            ApiOperationBase.setEnvironment(Environment.SANDBOX);
        }
    }

    private void applySuccessfulGatewayResponse(PaymentEntity payment, TransactionResponse result) {
        if (result != null && result.getMessages() != null) {
            payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(result.getTransId());
            payment.setTransactionRef(result.getTransId());
            payment.setPaymentDate(LocalDateTime.now());
            return;
        }

        payment.setPaymentStatus(PaymentEntity.PaymentStatus.FAILED);
        if (result != null && result.getErrors() != null && !result.getErrors().getError().isEmpty()) {
            payment.setGatewayResponseCode(result.getErrors().getError().get(0).getErrorCode());
            payment.setGatewayResponseText(result.getErrors().getError().get(0).getErrorText());
        }
    }

    private String getGatewayFailureMessage(CreateTransactionResponse response) {
        if (response != null && response.getMessages() != null && !response.getMessages().getMessage().isEmpty()) {
            return response.getMessages().getMessage().get(0).getText();
        }
        return "Gateway error";
    }

    private String formatExpiry(String expiryMonth, String expiryYear) {
        String month = expiryMonth.length() == 1 ? "0" + expiryMonth : expiryMonth;
        String year = expiryYear.length() == 2 ? "20" + expiryYear : expiryYear;
        return year + "-" + month;
    }
}
