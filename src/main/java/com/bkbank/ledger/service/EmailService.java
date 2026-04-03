package com.bkbank.ledger.service;

import com.bkbank.ledger.entity.Client;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${mail.from:}")
    private String mailFrom;

    @Value("${mail.reply-to:}")
    private String mailReplyTo;

    public EmailSendResult sendFraudAlertEmail(Client client,
                                               Long fraudAlertId,
                                               String paymentId,
                                               String merchantName,
                                               Double amount,
                                               String currency,
                                               String riskLevel,
                                               String confirmUrl,
                                               String rejectUrl) throws Exception {
        if (!mailEnabled) {
            throw new IllegalStateException("Email fallback is disabled");
        }
        if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
            throw new IllegalArgumentException("Khong tim thay email cua khach hang");
        }
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("MAIL_FROM is missing");
        }

        String recipient = client.getEmail().trim();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom.trim());
        helper.setTo(recipient);
        helper.setSubject("BKBank - Canh bao giao dich nghi ngo");
        helper.setText(
                buildTextBody(client, merchantName, amount, currency, riskLevel, confirmUrl, rejectUrl),
                buildHtmlBody(client, merchantName, amount, currency, riskLevel, fraudAlertId, paymentId, confirmUrl, rejectUrl)
        );
        if (mailReplyTo != null && !mailReplyTo.isBlank()) {
            helper.setReplyTo(mailReplyTo.trim());
        }

        mailSender.send(message);
        log.info("Sent fraud alert email to {} via SMTP", recipient);
        return new EmailSendResult(recipient, "SMTP");
    }

    private String buildTextBody(Client client,
                                 String merchantName,
                                 Double amount,
                                 String currency,
                                 String riskLevel,
                                 String confirmUrl,
                                 String rejectUrl) {
        String amountText = String.format("%.2f %s", amount != null ? amount : 0.0, currency != null ? currency : "USD");
        return String.format(
                "Xin chao %s,%n%nBKBank phat hien giao dich nghi ngo %s tai %s.%nMuc do rui ro: %s.%n%nXac nhan giao dich:%n%s%n%nBao cao khong phai ban:%n%s%n",
                client.getFullName() != null ? client.getFullName() : "quy khach",
                amountText,
                merchantName != null ? merchantName : "merchant khong xac dinh",
                riskLevel != null ? riskLevel : "UNKNOWN",
                confirmUrl != null ? confirmUrl : "N/A",
                rejectUrl != null ? rejectUrl : "N/A"
        );
    }

    private String buildHtmlBody(Client client,
                                 String merchantName,
                                 Double amount,
                                 String currency,
                                 String riskLevel,
                                 Long fraudAlertId,
                                 String paymentId,
                                 String confirmUrl,
                                 String rejectUrl) {
        String amountText = String.format("%.2f %s", amount != null ? amount : 0.0, currency != null ? currency : "USD");
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #172033;">
                  <h2 style="margin-bottom: 8px;">BKBank - Canh bao giao dich nghi ngo</h2>
                  <p>Xin chao %s,</p>
                  <p>He thong BKBank vua phat hien mot giao dich nghi ngo tren the/tai khoan cua ban.</p>
                  <ul>
                    <li><strong>So tien:</strong> %s</li>
                    <li><strong>Merchant:</strong> %s</li>
                    <li><strong>Muc do rui ro:</strong> %s</li>
                    <li><strong>Fraud alert:</strong> %s</li>
                    <li><strong>Payment ID:</strong> %s</li>
                  </ul>
                  <p style="margin: 20px 0 8px;">Hay chon mot trong hai thao tac duoi day:</p>
                  <div style="margin: 16px 0;">
                    <a href="%s" style="display:inline-block;background:#047857;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:12px;font-weight:700;margin-right:12px;">Day la giao dich cua toi</a>
                    <a href="%s" style="display:inline-block;background:#b91c1c;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:12px;font-weight:700;">Day khong phai giao dich cua toi</a>
                  </div>
                  <p style="font-size: 13px; color: #475569;">Neu nut trong email khong hoat dong, ban co the copy cac link sau:</p>
                  <p style="font-size: 13px; color: #475569; word-break: break-all; margin: 0 0 6px;"><strong>Confirm:</strong> %s</p>
                  <p style="font-size: 13px; color: #475569; word-break: break-all; margin: 0 0 16px;"><strong>Reject:</strong> %s</p>
                  <p>BKBank</p>
                </div>
                """.formatted(
                client.getFullName() != null ? escapeHtml(client.getFullName()) : "quy khach",
                escapeHtml(amountText),
                escapeHtml(merchantName != null ? merchantName : "merchant khong xac dinh"),
                escapeHtml(riskLevel != null ? riskLevel : "UNKNOWN"),
                fraudAlertId != null ? fraudAlertId.toString() : "N/A",
                escapeHtml(paymentId != null ? paymentId : "N/A"),
                escapeHtml(confirmUrl != null ? confirmUrl : "#"),
                escapeHtml(rejectUrl != null ? rejectUrl : "#"),
                escapeHtml(confirmUrl != null ? confirmUrl : "N/A"),
                escapeHtml(rejectUrl != null ? rejectUrl : "N/A")
        );
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record EmailSendResult(String recipient, String providerMessageId) {
    }
}
