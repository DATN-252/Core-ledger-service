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
            throw new IllegalArgumentException("Không tìm thấy email của khách hàng");
        }
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("MAIL_FROM is missing");
        }

        String recipient = client.getEmail().trim();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom.trim());
        helper.setTo(recipient);
        helper.setSubject("BKBank - Cảnh báo giao dịch nghi ngờ");
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
                "Xin chào %s,%n%nBKBank phát hiện giao dịch nghi ngờ %s tại %s.%nMức độ rủi ro: %s.%n%nXác nhận giao dịch:%n%s%n%nBáo cáo không phải bạn:%n%s%n",
                client.getFullName() != null ? client.getFullName() : "quy khach",
                amountText,
                merchantName != null ? merchantName : "merchant không xác định",
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
                <div style="margin:0;padding:36px 16px;background:linear-gradient(180deg,#eef4ff 0%%,#f8fbff 42%%,#eef7f2 100%%);font-family:Arial,sans-serif;color:#172033;">
                  <div style="max-width:680px;margin:0 auto;background:#ffffff;border:1px solid #d9e4f5;border-radius:30px;overflow:hidden;box-shadow:0 24px 60px rgba(20,35,90,0.10);">
                    <div style="padding:32px 32px 22px;background:radial-gradient(circle at top right,rgba(255,255,255,0.16),transparent 28%%),linear-gradient(135deg,#123a8f,#1f5dd8 58%%,#2c8fff);color:#ffffff;">
                      <div style="display:inline-block;background:rgba(255,255,255,0.14);border:1px solid rgba(255,255,255,0.18);backdrop-filter:blur(8px);padding:7px 12px;border-radius:999px;font-size:12px;letter-spacing:0.12em;text-transform:uppercase;font-weight:700;margin-bottom:14px;">BKBank Security</div>
                      <h2 style="margin:0;font-size:32px;line-height:1.18;">Cảnh báo giao dịch nghi ngờ</h2>
                      <p style="margin:14px 0 0;font-size:15px;line-height:1.75;max-width:560px;opacity:0.96;">Chúng tôi vừa phát hiện một giao dịch cần bạn xác nhận ngay để bảo vệ thẻ và tài khoản khỏi rủi ro gian lận.</p>
                    </div>
                    <div style="padding:30px 32px 32px;">
                      <p style="margin:0 0 16px;font-size:17px;line-height:1.65;">Xin chào <strong>%s</strong>,</p>
                      <div style="background:linear-gradient(180deg,#fbfdff,#f6faff);border:1px solid #d9e7fb;border-radius:22px;padding:20px 22px;margin-bottom:22px;box-shadow:inset 0 1px 0 rgba(255,255,255,0.7);">
                        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:14px 18px;">
                          <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Số tiền</div><div style="font-size:22px;font-weight:800;color:#0f172a;">%s</div></div>
                          <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Mức độ rủi ro</div><div style="display:inline-block;background:#fff1f2;color:#be123c;border:1px solid #fecdd3;border-radius:999px;padding:8px 12px;font-size:13px;font-weight:800;">%s</div></div>
                          <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Đơn vị chấp nhận thẻ</div><div style="font-size:16px;font-weight:700;color:#1e293b;">%s</div></div>
                          <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Mã cảnh báo</div><div style="font-size:16px;font-weight:700;color:#1e293b;">#%s</div></div>
                          <div style="grid-column:1 / -1;"><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Mã giao dịch</div><div style="font-size:15px;font-weight:700;color:#1e293b;word-break:break-all;">%s</div></div>
                        </div>
                      </div>
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.8;color:#334155;">Nếu đây là giao dịch của bạn, hãy xác nhận để hệ thống đóng cảnh báo. Nếu không phải bạn, hãy báo ngay để ngân hàng tiếp tục khóa thẻ và xử lý rủi ro.</p>
                      <div style="margin:22px 0 20px;">
                        <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#0b8f69,#15b887);color:#ffffff;text-decoration:none;padding:16px 24px;border-radius:16px;font-family:Arial,'Segoe UI',sans-serif;font-size:15px;line-height:1.35;font-weight:700;white-space:nowrap;box-shadow:0 14px 28px rgba(21,184,135,0.28);margin:0 14px 14px 0;">Xác nhận giao dịch</a>
                        <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#c81e1e,#ef4444);color:#ffffff;text-decoration:none;padding:16px 24px;border-radius:16px;font-family:Arial,'Segoe UI',sans-serif;font-size:15px;line-height:1.35;font-weight:700;white-space:nowrap;box-shadow:0 14px 28px rgba(239,68,68,0.24);margin:0 14px 14px 0;">Báo giao dịch gian lận</a>
                      </div>
                      <div style="background:linear-gradient(180deg,#fff8ef,#fff4e8);border:1px solid #fdc88a;border-radius:18px;padding:15px 16px 16px;margin-bottom:18px;">
                        <div style="font-weight:800;margin-bottom:6px;color:#9a3412;">Lưu ý bảo mật</div>
                        <div style="font-size:14px;line-height:1.65;color:#7c2d12;">Các liên kết xác nhận chỉ có hiệu lực trong thời gian ngắn và chỉ sử dụng được một lần.</div>
                      </div>
                      <p style="font-size:13px;color:#64748b;word-break:break-all;margin:0 0 6px;"><strong>Liên kết xác nhận:</strong> %s</p>
                      <p style="font-size:13px;color:#64748b;word-break:break-all;margin:0 0 18px;"><strong>Liên kết báo gian lận:</strong> %s</p>
                      <div style="padding-top:18px;border-top:1px solid #e2e8f0;font-size:14px;color:#64748b;">BKBank</div>
                    </div>
                  </div>
                </div>
                """.formatted(
                client.getFullName() != null ? escapeHtml(client.getFullName()) : "quý khách",
                escapeHtml(amountText),
                escapeHtml(riskLevel != null ? riskLevel : "UNKNOWN"),
                escapeHtml(merchantName != null ? merchantName : "merchant không xác định"),
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
