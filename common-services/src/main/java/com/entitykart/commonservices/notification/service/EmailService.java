package com.entitykart.commonservices.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email sending service (originally from notification-service).
 * Handles async SMTP delivery and builds HTML email templates
 * for all notification types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:9901}")
    private String frontendUrl;

    /**
     * Sends an HTML email asynchronously (fire-and-forget).
     * Uses @Async so the Kafka listener thread is never blocked by SMTP.
     * Note: return type must be void for @Async (Spring proxy works correctly).
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            if (to == null || to.isBlank() || fromEmail == null || fromEmail.isBlank()) {
                log.warn("Email skipped — missing 'to' ({}) or 'from' ({})", to, fromEmail);
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("✅ Email sent → {} | Subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error sending email to {}: {}", to, e.getMessage());
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
            log.info("Report email sent to {} for type {}", to, reportType);
        } catch (Exception e) {
            log.error("Failed to send report email to {}: {}", to, e.getMessage());
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
             + "<p>We'll notify you when your order is confirmed and shipped.</p>"
             + "<a href='" + frontendUrl + "/#/orders' style='background:#4f46e5;color:#fff;padding:10px 20px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>View Order</a>"
             + "<hr style='margin-top:30px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart – Your trusted shopping destination</p>"
             + "</div>";
    }

    public String buildOrderConfirmedEmail(String customerName, Long orderId, Double total) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<div style='background:#4f46e5;padding:20px;border-radius:12px 12px 0 0;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;'>✅ Order Confirmed</h1></div>"
             + "<div style='background:#f8fafc;padding:20px;border-radius:0 0 12px 12px;border:1px solid #e2e8f0;'>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>Great news! Your order <strong>#" + orderId + "</strong> has been <strong>confirmed</strong> and is being prepared for shipment.</p>"
             + "<table style='width:100%;border-collapse:collapse;margin:15px 0;'>"
             + "<tr><td style='padding:8px;color:#6b7280;'>Order ID</td><td style='padding:8px;font-weight:bold;'>#" + orderId + "</td></tr>"
             + "<tr style='background:#f1f5f9;'><td style='padding:8px;color:#6b7280;'>Amount</td><td style='padding:8px;font-weight:bold;'>₹" + String.format("%.2f", total) + "</td></tr>"
             + "<tr><td style='padding:8px;color:#6b7280;'>Status</td><td style='padding:8px;'><span style='background:#dcfce7;color:#166534;padding:3px 10px;border-radius:20px;'>CONFIRMED</span></td></tr>"
             + "</table>"
             + "<a href='" + frontendUrl + "/#/orders' style='background:#4f46e5;color:#fff;padding:12px 24px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>Track Order</a>"
             + "<hr style='margin-top:25px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart</p></div></div>";
    }

    public String buildOrderShippedEmail(String customerName, Long orderId, Double total) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<div style='background:linear-gradient(135deg,#0ea5e9,#6366f1);padding:20px;border-radius:12px 12px 0 0;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;'>🚚 Your Order is On the Way!</h1></div>"
             + "<div style='background:#f8fafc;padding:20px;border-radius:0 0 12px 12px;border:1px solid #e2e8f0;'>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>Exciting news! Your order <strong>#" + orderId + "</strong> has been <strong>shipped</strong> and is on its way to you.</p>"
             + "<table style='width:100%;border-collapse:collapse;margin:15px 0;'>"
             + "<tr><td style='padding:8px;color:#6b7280;'>Order ID</td><td style='padding:8px;font-weight:bold;'>#" + orderId + "</td></tr>"
             + "<tr style='background:#f1f5f9;'><td style='padding:8px;color:#6b7280;'>Amount</td><td style='padding:8px;font-weight:bold;'>₹" + String.format("%.2f", total) + "</td></tr>"
             + "<tr><td style='padding:8px;color:#6b7280;'>Status</td><td style='padding:8px;'><span style='background:#dbeafe;color:#1e40af;padding:3px 10px;border-radius:20px;'>SHIPPED</span></td></tr>"
             + "</table>"
             + "<p style='color:#4b5563;'>Please keep your phone handy — the delivery agent may contact you.</p>"
             + "<a href='" + frontendUrl + "/#/orders' style='background:#0ea5e9;color:#fff;padding:12px 24px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>Track Shipment</a>"
             + "<hr style='margin-top:25px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart</p></div></div>";
    }

    public String buildOrderDeliveredEmail(String customerName, Long orderId, Double total) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<div style='background:linear-gradient(135deg,#16a34a,#4f46e5);padding:20px;border-radius:12px 12px 0 0;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;'>🎉 Order Delivered!</h1></div>"
             + "<div style='background:#f8fafc;padding:20px;border-radius:0 0 12px 12px;border:1px solid #e2e8f0;'>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>Your order <strong>#" + orderId + "</strong> has been <strong>delivered successfully</strong>! We hope you love your purchase. 😊</p>"
             + "<table style='width:100%;border-collapse:collapse;margin:15px 0;'>"
             + "<tr><td style='padding:8px;color:#6b7280;'>Order ID</td><td style='padding:8px;font-weight:bold;'>#" + orderId + "</td></tr>"
             + "<tr style='background:#f1f5f9;'><td style='padding:8px;color:#6b7280;'>Amount Paid</td><td style='padding:8px;font-weight:bold;'>₹" + String.format("%.2f", total) + "</td></tr>"
             + "<tr><td style='padding:8px;color:#6b7280;'>Status</td><td style='padding:8px;'><span style='background:#dcfce7;color:#166534;padding:3px 10px;border-radius:20px;'>DELIVERED ✓</span></td></tr>"
             + "</table>"
             + "<p style='color:#4b5563;'>Had a great experience? Leave a review and help others make informed decisions!</p>"
             + "<a href='" + frontendUrl + "/#/orders' style='background:#16a34a;color:#fff;padding:12px 24px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>Write a Review</a>"
             + "<hr style='margin-top:25px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart – Thank you for shopping with us!</p></div></div>";
    }

    public String buildOrderCancelledEmail(String customerName, Long orderId, Double total) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<div style='background:#dc2626;padding:20px;border-radius:12px 12px 0 0;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;'>❌ Order Cancelled</h1></div>"
             + "<div style='background:#f8fafc;padding:20px;border-radius:0 0 12px 12px;border:1px solid #e2e8f0;'>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>Your order <strong>#" + orderId + "</strong> has been <strong>cancelled</strong>.</p>"
             + "<p>If payment was made, a refund of <strong>₹" + String.format("%.2f", total) + "</strong> will be processed within 5–7 business days.</p>"
             + "<p>If you did not request this cancellation, please contact our support team immediately.</p>"
             + "<a href='" + frontendUrl + "/#/products' style='background:#4f46e5;color:#fff;padding:12px 24px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>Shop Again</a>"
             + "<hr style='margin-top:25px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart</p></div></div>";
    }

    public String buildOrderReturnedEmail(String customerName, Long orderId, Double total) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>"
             + "<div style='background:#f59e0b;padding:20px;border-radius:12px 12px 0 0;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;'>📦 Return Initiated</h1></div>"
             + "<div style='background:#f8fafc;padding:20px;border-radius:0 0 12px 12px;border:1px solid #e2e8f0;'>"
             + "<p>Hi <strong>" + customerName + "</strong>,</p>"
             + "<p>Your order <strong>#" + orderId + "</strong> has been marked as <strong>RETURNED</strong>.</p>"
             + "<p>The refund of <strong>₹" + String.format("%.2f", total) + "</strong> will be processed once the returned item is received and inspected (typically 5–7 business days).</p>"
             + "<a href='" + frontendUrl + "/#/returns' style='background:#f59e0b;color:#fff;padding:12px 24px;"
             + "border-radius:6px;text-decoration:none;display:inline-block;margin-top:10px;'>View Return Status</a>"
             + "<hr style='margin-top:25px;'/><p style='color:#9ca3af;font-size:12px;'>EntityKart</p></div></div>";
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
