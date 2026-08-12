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

import java.time.Year;

/**
 * Email sending service (originally from notification-service).
 * Handles async SMTP delivery and builds HTML email templates
 * for all notification types.
 *
 * <p>Design system:
 * <ul>
 *   <li>order/account = Indigo gradient (#4338CA → #6366F1 / #7C3AED)</li>
 *   <li>shipped       = Sky gradient  (#0EA5E9 → #6366F1)</li>
 *   <li>success / delivered = Emerald (#059669 → #4338CA)</li>
 *   <li>cancelled / failed  = Rose    (#DC2626 → #F59E0B)</li>
 *   <li>in-progress / return = Amber  (#F59E0B → #EF4444)</li>
 * </ul>
 * Table-based layout with solid-colour fallbacks for Outlook compatibility.
 * Every template is a full {@code <!DOCTYPE html>} document (not a fragment).
 *
 * <p>Links: all internal URLs use {@code #!/} hash-bang format to match the
 * AngularJS router ({@code $locationProvider.hashPrefix('!')} mode).
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

    @jakarta.annotation.PostConstruct
    void validateMailConfig() {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.error("🚨 spring.mail.username is EMPTY — MAIL_USERNAME env var is not set. " +
                    "All outgoing emails will silently fail until this is fixed.");
        } else {
            log.info("Mail sender configured as: {}", fromEmail);
        }
    }

    // ─── Core send helper ─────────────────────────────────────────────────────

    /**
     * Sends an HTML email asynchronously (fire-and-forget).
     * Uses {@code @Async} so the Kafka listener thread is never blocked by SMTP.
     * Return type must be {@code void} for {@code @Async} (Spring proxy).
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

    // ─── Admin report with attachments ───────────────────────────────────────

    public void sendReportWithAttachments(String to, String reportType, byte[] excelData, byte[] wordData) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("📊 EntityKart Admin Report — " + reportType.toUpperCase());
            helper.setText(buildAdminReportEmail(reportType), true);

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

    // ─── Template builders ────────────────────────────────────────────────────

    public String buildWelcomeEmail(String customerName) {
        String safe = esc(customerName);
        return shell(
            "Welcome to EntityKart, " + safe,
            "#7C3AED", "#DB2777",
            "WELCOME", "🎉",
            "Welcome Aboard!", "Great things are just a click away.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>We're thrilled to have you on board! Explore thousands of products across every category "
            + "— from everyday essentials to the latest launches.</p>"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"perks\"><tr>"
            + "<td><span class=\"picon\">🚚</span>Fast delivery</td>"
            + "<td><span class=\"picon\">🔄</span>Easy returns</td>"
            + "<td><span class=\"picon\">🔒</span>Secure payments</td>"
            + "</tr></table>"
            + btn("Start Shopping", frontendUrl + "/#!/products", "#4338CA")
        );
    }

    public String buildPasswordResetEmail(String customerName, String token) {
        String safe = esc(customerName);
        return shell(
            "Your password reset code",
            "#4338CA", "#7C3AED",
            "ACCOUNT SECURITY", "🔒",
            "Reset Your Password", "Use the code below to continue.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>We received a request to reset your password. Use the verification code below to continue, "
            + "or tap the button to go straight to the reset page.</p>"
            + "<div class=\"codebox\">" + esc(token) + "</div>"
            + "<p>This code expires in <strong>15 minutes</strong>. "
            + "If you didn't request this, no action is needed — your password will stay unchanged.</p>"
            + btn("Reset Password", frontendUrl + "/#!/reset-password", "#F59E0B")
        );
    }

    public String buildOrderPlacedEmail(String customerName, Long orderId, Double total) {
        String safe = esc(customerName);
        return shell(
            "Order #" + orderId + " received",
            "#4338CA", "#6366F1",
            "ORDER UPDATE", "🧾",
            "Order Received", "We're getting things moving.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>Thanks for shopping with us! We've received your order and it's now being processed.</p>"
            + receipt(
                row("Order ID", "#" + orderId),
                row("Amount",   formatAmount(total)),
                pillRow("Status", "RECEIVED", "background:#E0E7FF;color:#3730A3;"))
            + btn("View Order", frontendUrl + "/#!/orders", "#F59E0B")
        );
    }

    public String buildOrderConfirmedEmail(String customerName, Long orderId, Double total) {
        String safe = esc(customerName);
        return shell(
            "Order #" + orderId + " confirmed",
            "#4338CA", "#7C3AED",
            "ORDER UPDATE", "✅",
            "Order Confirmed", "Your order is being prepared for shipment.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>Good news — your order has been confirmed and is being packed for shipment.</p>"
            + receipt(
                row("Order ID", "#" + orderId),
                row("Amount",   formatAmount(total)),
                pillRow("Status", "CONFIRMED", "background:#E0E7FF;color:#3730A3;"))
            + btn("Track Order", frontendUrl + "/#!/orders", "#F59E0B")
        );
    }

    public String buildOrderShippedEmail(String customerName, Long orderId, Double total) {
        String safe = esc(customerName);
        return shell(
            "Order #" + orderId + " is on its way",
            "#0EA5E9", "#6366F1",
            "ORDER UPDATE", "🚚",
            "On Its Way!", "Your package has left our warehouse.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>Your order has shipped and is on its way to you. "
            + "Keep your phone handy — our delivery partner may reach out before arrival.</p>"
            + receipt(
                row("Order ID", "#" + orderId),
                row("Amount",   formatAmount(total)),
                pillRow("Status", "SHIPPED", "background:#E0F2FE;color:#075985;"))
            + btn("Track Shipment", frontendUrl + "/#!/orders", "#F59E0B")
        );
    }

    public String buildOrderDeliveredEmail(String customerName, Long orderId, Double total) {
        String safe = esc(customerName);
        return shell(
            "Order #" + orderId + " has been delivered",
            "#059669", "#4338CA",
            "ORDER UPDATE", "🥳",
            "Delivered!", "We hope you love it.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>Your order has been delivered. We hope it's everything you expected!</p>"
            + receipt(
                row("Order ID",    "#" + orderId),
                row("Amount Paid", formatAmount(total)),
                pillRow("Status",  "DELIVERED", "background:#DCFCE7;color:#166534;"))
            + "<p style=\"text-align:center;margin-top:20px;\">How was it?</p>"
            + "<div class=\"stars\">★★★★★</div>"
            + btn("Write a Review", frontendUrl + "/#!/orders", "#F59E0B")
        );
    }

    public String buildOrderCancelledEmail(String customerName, Long orderId, Double total) {
        String safe = esc(customerName);
        return shell(
            "Your order #" + orderId + " has been cancelled",
            "#DC2626", "#F59E0B",
            "ORDER UPDATE", "❌",
            "Order Cancelled", "We're sorry to see this happen.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>Your order <strong>#" + orderId + "</strong> has been cancelled.</p>"
            + receipt(
                row("Order ID", "#" + orderId),
                row("Amount",   formatAmount(total)),
                pillRow("Status", "CANCELLED", "background:#FEE2E2;color:#991B1B;"))
            + "<div class=\"notice\" style=\"background:#FFF7ED;color:#92400E;border-color:#F59E0B;\">"
            + "If payment was made, a refund of <strong>" + formatAmount(total) + "</strong> will be processed "
            + "within <strong>5–7 business days</strong>. If you did not request this cancellation, "
            + "please contact our support team immediately.</div>"
            + btn("Shop Again", frontendUrl + "/#!/products", "#4338CA")
        );
    }

    /**
     * Return Initiated — sent as soon as the customer submits a return request.
     * Note: orderId is used here as the return identifier (return-service context).
     */
    public String buildOrderReturnedEmail(String customerName, Long orderId, Double total) {
        String safe = esc(customerName);
        return shell(
            "Your return request for #" + orderId + " has been received",
            "#F59E0B", "#EF4444",
            "RETURN REQUEST", "📦",
            "Return Initiated", "We've received your request.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>Your return request for order <strong>#" + orderId + "</strong> has been submitted "
            + "successfully and is now under review.</p>"
            + receipt(
                row("Order ID",   "#" + orderId),
                row("Refund Amt", formatAmount(total)),
                pillRow("Status", "UNDER REVIEW", "background:#FEF9C3;color:#854D0E;"))
            + "<div class=\"notice\" style=\"background:#FFF7ED;color:#92400E;border-color:#F59E0B;\">"
            + "Our team typically reviews returns within <strong>24–48 hours</strong>. "
            + "You'll be notified once a decision is made.</div>"
            + btn("View Return Status", frontendUrl + "/#!/orders", "#F59E0B")
        );
    }

    public String buildReturnStatusEmail(String customerName, Long returnId, String status,
                                          Double refundAmount, String rejectionReason) {
        String safe = esc(customerName);
        boolean approved  = "APPROVED".equalsIgnoreCase(status);
        boolean refunded  = "REFUNDED".equalsIgnoreCase(status);
        boolean rejected  = "REJECTED".equalsIgnoreCase(status);
        boolean positive  = approved || refunded;

        String g1    = positive ? "#059669" : "#DC2626";
        String g2    = positive ? "#0EA5E9" : "#9CA3AF";
        String icon  = positive ? "✅" : (rejected ? "🚫" : "📦");
        String h1    = positive
                          ? (refunded ? "Refund Processed!" : "Return Approved!")
                          : "Return Not Approved";
        String sub   = positive
                          ? (refunded ? "Money is on its way." : "Your refund is being prepared.")
                          : "We're sorry for the inconvenience.";
        String preheader = "Update on your return request #" + returnId;

        String pillStyle = positive
                ? "background:#DCFCE7;color:#166534;"
                : "background:#FEE2E2;color:#991B1B;";

        StringBuilder body = new StringBuilder();
        body.append("<p class=\"greeting\">Hi ").append(safe).append(",</p>");

        if (refunded) {
            body.append("<p>Your refund has been successfully processed! The amount will reflect in your "
                    + "original payment method within <strong>5–7 business days</strong>.</p>");
            body.append(receipt(
                row("Return ID",     "#" + returnId),
                row("Refund Amount", formatAmount(refundAmount)),
                pillRow("Status",    "REFUNDED", pillStyle)));
            body.append("<div class=\"notice\" style=\"background:#F0FDF4;color:#166534;border-color:#22C55E;\">"
                    + "Refund initiated to your original payment method. Bank processing times may vary.</div>");
            body.append(btn("Shop Again", frontendUrl + "/#!/products", "#059669"));

        } else if (approved) {
            body.append("<p>Great news! Your return request has been <strong>approved</strong>. "
                    + "Please ship the item back as instructed.</p>");
            body.append(receipt(
                row("Return ID",  "#" + returnId),
                row("Refund Amt", formatAmount(refundAmount)),
                pillRow("Status", "APPROVED", pillStyle)));
            body.append("<div class=\"notice\" style=\"background:#F0FDF4;color:#166534;border-color:#22C55E;\">"
                    + "Once we receive and inspect the item, your refund will be credited within "
                    + "<strong>5–7 business days</strong>.</div>");
            body.append(btn("View Return Status", frontendUrl + "/#!/orders", "#059669"));

        } else { // REJECTED or unknown
            body.append("<p>After reviewing your return request <strong>#").append(returnId)
                .append("</strong>, we were unable to approve it at this time.</p>");
            body.append(receipt(
                row("Return ID", "#" + returnId),
                pillRow("Status", status.toUpperCase(), pillStyle)));
            if (rejectionReason != null && !rejectionReason.isBlank()) {
                body.append("<div class=\"notice\" style=\"background:#FFF1F2;color:#9F1239;border-color:#F43F5E;\">"
                        + "<strong>Reason:</strong> ").append(esc(rejectionReason)).append("</div>");
            }
            body.append("<p>If you believe this decision was made in error, "
                    + "please contact our support team within 7 days.</p>");
            body.append(btn("Contact Support", frontendUrl + "/#!/orders", "#4338CA"));
        }

        return shell(preheader, g1, g2, "RETURN UPDATE", icon, h1, sub, body.toString());
    }

    public String buildPaymentSuccessEmail(String customerName, Long orderId, String txnRef, Double amount) {
        String safe = esc(customerName);
        return shell(
            "Payment confirmed for order #" + orderId,
            "#059669", "#0EA5E9",
            "PAYMENT", "✅",
            "Payment Successful!", "Your transaction is complete.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>Your payment has been processed successfully. Thank you for your purchase!</p>"
            + receipt(
                row("Order ID",        "#" + orderId),
                row("Amount Paid",     formatAmount(amount)),
                row("Transaction Ref", esc(txnRef)),
                pillRow("Status",      "SUCCESS", "background:#DCFCE7;color:#166534;"))
            + btn("View Order", frontendUrl + "/#!/orders", "#059669")
        );
    }

    public String buildPaymentFailedEmail(String customerName, Long orderId) {
        String safe = esc(customerName);
        return shell(
            "Payment could not be processed — order #" + orderId,
            "#DC2626", "#F59E0B",
            "PAYMENT ALERT", "⚠️",
            "Payment Failed", "Don't worry — your cart is safe.",
            "<p class=\"greeting\">Hi " + safe + ",</p>"
            + "<p>Unfortunately, we couldn't process your payment for order <strong>#" + orderId + "</strong>. "
            + "This can happen due to insufficient funds, card limits, or a temporary network issue.</p>"
            + receipt(
                row("Order ID", "#" + orderId),
                pillRow("Status", "FAILED", "background:#FEE2E2;color:#991B1B;"))
            + "<div class=\"notice\" style=\"background:#FFF1F2;color:#9F1239;border-color:#F43F5E;\">"
            + "Please retry with the same card or try a different payment method. "
            + "Your order is saved and ready to complete.</div>"
            + btn("Retry Payment", frontendUrl + "/#!/orders", "#DC2626")
        );
    }

    // ─── Private: admin report body ──────────────────────────────────────────

    private String buildAdminReportEmail(String reportType) {
        String safeType = esc(reportType.toUpperCase());
        return shell(
            "Your EntityKart admin report is ready",
            "#4338CA", "#7C3AED",
            "ADMIN DASHBOARD", "📊",
            "Report Ready", "Your requested data export is attached.",
            "<p class=\"greeting\">Hi Admin,</p>"
            + "<p>Your requested <strong>" + safeType + "</strong> report has been generated "
            + "and is attached to this email.</p>"
            + receipt(
                row("Report Type", safeType),
                row("Format",      "Excel + Word"))
            + "<div class=\"notice\" style=\"background:#EEF2FF;color:#3730A3;border-color:#6366F1;\">"
            + "Attachments: <strong>" + reportType.toLowerCase() + "_report.xlsx</strong> and "
            + "<strong>" + reportType.toLowerCase() + "_report.doc</strong></div>"
            + btn("Go to Dashboard", frontendUrl + "/#!/admin/dashboard", "#4338CA")
        );
    }

    // ─── Private: HTML builder utilities ─────────────────────────────────────

    /** Full {@code <!DOCTYPE html>} email document shell. Outlook-safe (table layout). */
    private String shell(String preheader,
                         String grad1, String grad2,
                         String eyebrow, String badgeEmoji,
                         String heading, String subHeading,
                         String bodyHtml) {
        int year = Year.now().getValue();
        return "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "<meta charset=\"UTF-8\" />\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n"
            + "<meta name=\"color-scheme\" content=\"light\" />\n"
            + "<title>EntityKart</title>\n"
            + "<style>" + CSS + "</style>\n"
            + "</head>\n"
            + "<body>\n"
            + "<!--[if mso]>\n"
            + "<table role=\"presentation\" width=\"600\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\"><tr><td>\n"
            + "<![endif]-->\n"
            + "<span style=\"display:none;max-height:0;overflow:hidden;opacity:0;\">" + esc(preheader) + "</span>\n"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"wrapper\"><tr><td>\n"
            + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"container\" align=\"center\"><tr><td style=\"padding:0;\">\n"
            + "<div class=\"topbar\">🛍️ EntityKart</div>\n"
            + "<div class=\"hero\" style=\"background-color:" + grad1 + ";background-image:linear-gradient(135deg," + grad1 + "," + grad2 + ");\">\n"
            + "<div class=\"eyebrow\">" + eyebrow + "</div>\n"
            + "<div class=\"badge\">" + badgeEmoji + "</div>\n"
            + "<h1>" + heading + "</h1>\n"
            + "<p class=\"sub\">" + subHeading + "</p>\n"
            + "</div>\n"
            + "<div class=\"content\">" + bodyHtml + "</div>\n"
            + "<div class=\"divider\"></div>\n"
            + "<div class=\"footer\">\n"
            + "<div class=\"word\">🛍️ EntityKart</div>\n"
            + "<p class=\"tagline\">Shop smart. Shop happy.</p>\n"
            + "<div class=\"social\">"
            + "<a href=\"#\">f</a><a href=\"#\">in</a><a href=\"#\">ig</a><a href=\"#\">x</a>"
            + "</div>\n"
            + "<div class=\"legal\">\n"
            + "EntityKart Pvt. Ltd., Bengaluru, India<br/>\n"
            + "You're receiving this email because you have an account with EntityKart.<br/>\n"
            + "<a href=\"" + frontendUrl + "/#!/profile\">Manage email preferences</a>\n"
            + "&nbsp;&middot;&nbsp; &copy; " + year + " EntityKart. All rights reserved.\n"
            + "</div>\n"
            + "</div>\n"
            + "</td></tr></table>\n"
            + "</td></tr></table>\n"
            + "<!--[if mso]>\n"
            + "</td></tr></table>\n"
            + "<![endif]-->\n"
            + "</body>\n"
            + "</html>\n";
    }

    /** Shared CSS string — table-based, Outlook-safe, mobile-responsive. */
    private static final String CSS =
        "@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@600;700&display=swap');\n"
        + "*{box-sizing:border-box;}\n"
        + "body{margin:0;padding:0;background:#EEF1FB;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-text-size-adjust:100%;}\n"
        + "img{border:0;line-height:100%;outline:none;text-decoration:none;}\n"
        + "table{border-collapse:collapse;}\n"
        + "a{text-decoration:none;}\n"
        + ".wrapper{width:100%;background:#EEF1FB;padding:32px 12px;}\n"
        + ".container{max-width:600px;background:#FFFFFF;border-radius:20px;overflow:hidden;box-shadow:0 12px 32px rgba(31,41,89,0.10);}\n"
        + ".topbar{padding:16px 28px;font-family:'Poppins','Segoe UI',Arial,sans-serif;font-weight:700;font-size:14px;color:#4338CA;border-bottom:1px solid #F1F3FA;}\n"
        + ".hero{padding:36px 32px 40px;text-align:center;}\n"
        + ".eyebrow{font-size:11px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:rgba(255,255,255,0.78);margin-bottom:14px;}\n"
        + ".badge{width:68px;height:68px;line-height:68px;border-radius:50%;background:rgba(255,255,255,0.20);font-size:30px;display:inline-block;margin-bottom:16px;}\n"
        + ".hero h1{margin:0 0 8px;font-family:'Poppins','Segoe UI',Arial,sans-serif;font-weight:700;font-size:24px;color:#FFFFFF;}\n"
        + ".hero .sub{margin:0;font-size:14px;color:rgba(255,255,255,0.88);}\n"
        + ".content{padding:34px 32px 8px;}\n"
        + ".content p{margin:0 0 14px;font-size:15px;line-height:1.65;color:#374151;}\n"
        + ".greeting{font-size:16px;color:#111827;font-weight:600;margin-bottom:14px;}\n"
        + ".receipt{border:1.5px dashed #D8DEEC;border-radius:14px;background:#F8FAFF;padding:4px 20px;margin:22px 0;}\n"
        + ".receipt-row td{padding:10px 0;font-size:14px;border-bottom:1px solid #EDF0FA;}\n"
        + ".receipt-row:last-child td{border-bottom:none;}\n"
        + ".receipt-row .label{color:#6B7280;}\n"
        + ".receipt-row .value{color:#111827;font-weight:700;text-align:right;}\n"
        + ".pill{display:inline-block;padding:4px 14px;border-radius:999px;font-size:11px;font-weight:800;letter-spacing:0.4px;text-transform:uppercase;}\n"
        + ".notice{border-radius:12px;padding:14px 16px 14px 18px;font-size:13.5px;line-height:1.55;margin:18px 0;border-left:4px solid currentColor;}\n"
        + ".codebox{background:#F3F4F9;border:1.5px dashed #C7CEE6;border-radius:14px;padding:20px;text-align:center;"
        + "font-family:'Courier New',monospace;font-size:26px;font-weight:700;letter-spacing:4px;color:#111827;margin:22px 0;}\n"
        + ".stars{text-align:center;font-size:22px;letter-spacing:4px;color:#F59E0B;margin:2px 0 4px;}\n"
        + ".btn-wrap{text-align:center;margin:26px 0 6px;}\n"
        + ".btn{display:inline-block;padding:14px 36px;border-radius:999px;color:#FFFFFF !important;"
        + "text-decoration:none;font-weight:700;font-size:14px;font-family:'Poppins','Segoe UI',Arial,sans-serif;}\n"
        + ".perks{width:100%;margin:18px 0 6px;}\n"
        + ".perks td{text-align:center;padding:10px 4px;font-size:12px;color:#6B7280;width:33.33%;}\n"
        + ".perks .picon{font-size:22px;display:block;margin-bottom:4px;}\n"
        + ".divider{border-top:1px solid #EEF1F6;margin:14px 32px 0;}\n"
        + ".footer{padding:26px 32px 32px;text-align:center;}\n"
        + ".footer .word{font-family:'Poppins','Segoe UI',Arial,sans-serif;font-weight:700;color:#4338CA;font-size:15px;margin-bottom:8px;}\n"
        + ".footer .tagline{margin:0;font-size:12px;color:#9CA3AF;}\n"
        + ".social{margin:16px 0 4px;}\n"
        + ".social a{display:inline-block;width:30px;height:30px;line-height:30px;border-radius:50%;"
        + "background:#EEF1FB;color:#4338CA;text-align:center;font-size:12px;font-weight:800;margin:0 4px;}\n"
        + ".legal{color:#9CA3AF;font-size:11px;line-height:1.7;margin-top:10px;}\n"
        + ".legal a{color:#9CA3AF;text-decoration:underline;}\n"
        + "@media only screen and (max-width:600px){\n"
        + ".hero{padding:28px 20px 30px;}\n"
        + ".content{padding:26px 20px 4px;}\n"
        + ".footer{padding:22px 20px 28px;}\n"
        + ".hero h1{font-size:21px;}\n"
        + ".codebox{font-size:21px;letter-spacing:3px;padding:16px;}\n"
        + ".divider{margin:14px 20px 0;}\n"
        + "}";

    /** Builds a dashed receipt box from one or more row strings. */
    private String receipt(String... rows) {
        StringBuilder sb = new StringBuilder(
            "<div class=\"receipt\"><table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        for (String r : rows) sb.append(r);
        sb.append("</table></div>");
        return sb.toString();
    }

    /** Plain text receipt row. */
    private String row(String label, String value) {
        return "<tr class=\"receipt-row\"><td class=\"label\">" + label
             + "</td><td class=\"value\">" + value + "</td></tr>";
    }

    /** Receipt row with a coloured pill badge for the value. */
    private String pillRow(String label, String value, String pillStyle) {
        return "<tr class=\"receipt-row\"><td class=\"label\">" + label
             + "</td><td class=\"value\"><span class=\"pill\" style=\"" + pillStyle + "\">"
             + value + "</span></td></tr>";
    }

    /** CTA button — table-wrapped for Outlook VML compatibility. */
    private String btn(String label, String href, String color) {
        return "<div class=\"btn-wrap\">"
             + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\"><tr>"
             + "<td style=\"border-radius:999px;background-color:" + color + ";\">"
             + "<a href=\"" + href + "\" class=\"btn\" style=\"background-color:" + color + ";\">"
             + label + "</a></td></tr></table></div>";
    }

    /** Formats a nullable Double as ₹X,XXX.XX; returns "—" when null. */
    private String formatAmount(Double amount) {
        if (amount == null) return "&mdash;";
        return "&#8377;" + String.format("%,.2f", amount);
    }

    /**
     * Escapes HTML special characters to prevent XSS when user-supplied
     * strings (name, token, reason) are embedded in the template.
     */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
