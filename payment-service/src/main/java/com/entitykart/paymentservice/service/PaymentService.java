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

    @Transactional
    public PaymentEntity processCardPayment(PaymentRequest request) {
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

        return saveAndPublish(payment);
    }

    @Transactional
    public PaymentEntity processOfflinePayment(Long orderId, Double amount, String paymentMode) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMode(PaymentEntity.PaymentMode.valueOf(paymentMode));
        payment.setTransactionRef("OFFLINE_" + System.currentTimeMillis());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        return saveAndPublish(payment);
    }

    @Transactional(readOnly = true)
    public PaymentEntity getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    private PaymentEntity saveAndPublish(PaymentEntity payment) {
        PaymentEntity saved = paymentRepository.save(payment);

        if (saved.getPaymentStatus() == PaymentEntity.PaymentStatus.SUCCESS) {
            orderServiceClient.updateOrderPaymentStatus(saved.getOrderId(), "PAID");
            kafkaTemplate.send(
                    PAYMENT_EVENTS_TOPIC,
                    new PaymentProcessedEvent(saved.getOrderId(), "SUCCESS", saved.getTransactionRef()));
        } else {
            orderServiceClient.updateOrderPaymentStatus(saved.getOrderId(), "UNPAID");
            kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, new PaymentProcessedEvent(saved.getOrderId(), "FAILED", null));
        }

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

    @Transactional(readOnly = true)
    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }
}
