package com.entitykart.notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:9999}")
    private String frontendUrl;

    /**
     * Sends an HTML email asynchronously.
     * Returns true on success, false on failure (caller handles persistence).
     */
    @Async
    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} | Subject: {}", to, subject);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public void sendReportWithAttachments(String to, String reportType, byte[] excelData, byte[] wordData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("📊 EntityKart Admin Report - " + reportType.toUpperCase());
            helper.setText("<div style='font-family:Arial,sans-serif;'>"
                 + "<h2>EntityKart Admin Report</h2>"
                 + "<p>Attached are the requested Excel and Word reports for <strong>" + reportType.toUpperCase() + "</strong>.</p>"
                 + "<hr/><p style='color:#9ca3af;font-size:12px;'>EntityKart Admin Dashboard</p>"
                 + "</div>", true);

            helper.addAttachment(reportType.toLowerCase() + "_report.xlsx", 
                    new org.springframework.core.io.ByteArrayResource(excelData));
            
            helper.addAttachment(reportType.toLowerCase() + "_report.doc", 
                    new org.springframework.core.io.ByteArrayResource(wordData));

            mailSender.send(message);
            log.info("Report email sent successfully to {} for type {}", to, reportType);
        } catch (Exception e) {
            log.error("Failed to send report email with attachments to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage());
        }
    }

    // ─── Email Template Builders ──────────────────────────────────────────────

    public String buildOrderPlacedEmail(String customerName, Long orderId, Double total) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<h2 style='color:#4f46e5;'>🛒 Order Confirmed!</h2>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>Your order <strong>#" + orderId + "</strong> has been placed successfully.</p>"
             + "<p><strong>Total Amount:</strong> ₹" + String.format("%.2f", total) + "</p>"
             + "<p>We'll notify you when your order is shipped.</p>"
             + "<a href='" + frontendUrl + "/#/orders' style='background:#4f46e5;color:#fff;padding:10px 20px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>View Order</a>"
             + "<hr style='margin-top:30px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart – Your trusted shopping destination</p>"
             + "</div>";
    }

    public String buildPaymentSuccessEmail(String customerName, Long orderId, String txnRef, Double amount) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<h2 style='color:#16a34a;'>✅ Payment Successful</h2>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>Your payment for order <strong>#" + orderId + "</strong> was processed successfully.</p>"
             + "<p><strong>Amount:</strong> ₹" + String.format("%.2f", amount) + "</p>"
             + "<p><strong>Transaction Ref:</strong> " + txnRef + "</p>"
             + "<a href='" + frontendUrl + "/#/orders' style='background:#16a34a;color:#fff;padding:10px 20px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>View Order</a>"
             + "<hr style='margin-top:30px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart</p>"
             + "</div>";
    }

    public String buildPaymentFailedEmail(String customerName, Long orderId) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<h2 style='color:#dc2626;'>❌ Payment Failed</h2>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>Unfortunately, payment for order <strong>#" + orderId + "</strong> could not be processed.</p>"
             + "<p>Please try again or use a different payment method.</p>"
             + "<a href='" + frontendUrl + "/#/cart' style='background:#dc2626;color:#fff;padding:10px 20px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>Retry Payment</a>"
             + "<hr style='margin-top:30px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart</p>"
             + "</div>";
    }

    public String buildReturnStatusEmail(String customerName, Long returnId, String status,
                                          Double refundAmount, String rejectionReason) {
        String color = "APPROVED".equals(status) || "REFUNDED".equals(status) ? "#16a34a" : "#dc2626";
        String icon  = "APPROVED".equals(status) || "REFUNDED".equals(status) ? "✅" : "❌";
        String body = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
                    + "<h2 style='color:" + color + ";'>" + icon + " Return Request " + status + "</h2>"
                    + "<p>Hi <strong>" + customerName + "</strong>,</p>"
                    + "<p>Your return request <strong>#" + returnId + "</strong> has been <strong>" + status + "</strong>.</p>";
        if ("APPROVED".equals(status) || "REFUNDED".equals(status)) {
            body += "<p><strong>Refund Amount:</strong> ₹" + String.format("%.2f", refundAmount) + "</p>";
            if ("REFUNDED".equals(status)) {
                body += "<p>Your refund has been initiated and will reflect within 5–7 business days.</p>";
            }
        } else if ("REJECTED".equals(status) && rejectionReason != null) {
            body += "<p><strong>Reason:</strong> " + rejectionReason + "</p>";
        }
        body += "<a href='" + frontendUrl + "/#/returns' style='background:" + color + ";color:#fff;padding:10px 20px;"
              + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>View Returns</a>"
              + "<hr style='margin-top:30px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart</p>"
              + "</div>";
        return body;
    }

    public String buildWelcomeEmail(String customerName) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;"
             + "background:linear-gradient(135deg,#4f46e5,#7c3aed);border-radius:12px;'>"
             + "<h1 style='color:#fff;text-align:center;'>Welcome to EntityKart! 🎉</h1>"
             + "<div style='background:#fff;border-radius:8px;padding:20px;margin-top:20px;'>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>We're thrilled to have you on board! Start exploring thousands of products.</p>"
             + "<a href='" + frontendUrl + "/#/products' style='background:#4f46e5;color:#fff;padding:12px 24px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>Shop Now</a>"
             + "</div>"
             + "<p style='color:#e0e7ff;font-size:12px;text-align:center;margin-top:20px;'>EntityKart – Shop Smart</p>"
             + "</div>";
    }

    public String buildPasswordResetEmail(String customerName, String token) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<h2 style='color:#6366f1;'>🔑 Password Reset Request</h2>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>We received a request to reset your password. Use the following verification token to complete the reset:</p>"
             + "<div style='background:#f3f4f6;padding:15px;border-radius:6px;font-family:monospace;font-size:18px;"
             + "text-align:center;letter-spacing:1px;font-weight:bold;margin:20px 0;border:1px solid #e5e7eb;color:#111827;'>"
             + token
             + "</div>"
             + "<p>This token is valid for 15 minutes. If you did not make this request, please ignore this email.</p>"
             + "<hr style='margin-top:30px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart</p>"
             + "</div>";
    }
}
