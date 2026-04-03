package com.bkbank.ledger.controller;

import com.bkbank.ledger.entity.FraudAlertEmailAction;
import com.bkbank.ledger.entity.enums.FraudAlertEmailActionDecision;
import com.bkbank.ledger.service.FraudAlertEmailActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/public/fraud-alert-actions")
@RequiredArgsConstructor
@Slf4j
public class PublicFraudAlertActionController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FraudAlertEmailActionService fraudAlertEmailActionService;

    @GetMapping("/{token}")
    public ResponseEntity<String> preview(@PathVariable String token) {
        try {
            FraudAlertEmailActionService.EmailActionPreview preview = fraudAlertEmailActionService.getPreview(token);
            return html(renderPreview(preview, token));
        } catch (Exception e) {
            log.warn("Fraud email action preview failed: {}", e.getMessage());
            return html(renderError("Lien ket khong hop le", e.getMessage()));
        }
    }

    @PostMapping("/{token}")
    public ResponseEntity<String> execute(@PathVariable String token) {
        try {
            FraudAlertEmailActionService.EmailActionExecutionResult result = fraudAlertEmailActionService.execute(token);
            return html(renderSuccess(result));
        } catch (Exception e) {
            log.warn("Fraud email action execution failed: {}", e.getMessage());
            return html(renderError("Khong the xu ly yeu cau", e.getMessage()));
        }
    }

    private ResponseEntity<String> html(String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
                .body(body);
    }

    private String renderPreview(FraudAlertEmailActionService.EmailActionPreview preview, String token) {
        FraudAlertEmailAction action = preview.action();
        Map<String, Object> alert = preview.alert();
        String actionLabel = action.getDecision() == FraudAlertEmailActionDecision.CONFIRM
                ? "Đây là giao dịch của tôi"
                : "Đây không phải giao dịch của tôi";
        String actionColor = action.getDecision() == FraudAlertEmailActionDecision.CONFIRM ? "#047857" : "#b91c1c";
        String alertStatus = alert != null && alert.get("status") != null ? escapeHtml(alert.get("status").toString()) : "N/A";
        String customerResponse = alert != null && alert.get("customerResponse") != null ? escapeHtml(alert.get("customerResponse").toString()) : "NO_RESPONSE";
        return layout(
                "Xác nhận giao dịch nghi ngờ",
                """
                <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:16px;flex-wrap:wrap;margin-bottom:18px;">
                  <div>
                    <div style="display:inline-block;background:#e0ecff;color:#1d4ed8;border-radius:999px;padding:7px 12px;font-size:12px;font-weight:800;letter-spacing:0.08em;text-transform:uppercase;margin-bottom:12px;">BKBank Security</div>
                    <p style="margin:0;font-size:16px;line-height:1.8;color:#334155;max-width:560px;">BKBank cần bạn xác nhận lại giao dịch nghi ngờ dưới đây trước khi hệ thống tiếp tục xử lý.</p>
                  </div>
                  <div style="background:#fff1f2;color:#be123c;border:1px solid #fecdd3;border-radius:999px;padding:10px 14px;font-size:13px;font-weight:800;">%s</div>
                </div>
                <div style="background:linear-gradient(180deg,#fbfdff,#f5f9ff);border:1px solid #d9e7fb;border-radius:22px;padding:22px;margin:18px 0 20px;box-shadow:inset 0 1px 0 rgba(255,255,255,0.75);">
                  <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px 18px;">
                    <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Mã cảnh báo</div><div style="font-size:17px;font-weight:800;color:#0f172a;">#%s</div></div>
                    <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Số tiền</div><div style="font-size:24px;font-weight:800;color:#0f172a;">%s</div></div>
                    <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Đơn vị chấp nhận thẻ</div><div style="font-size:16px;font-weight:700;color:#1e293b;">%s</div></div>
                    <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Thẻ</div><div style="font-size:16px;font-weight:700;color:#1e293b;">%s</div></div>
                    <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Trạng thái case</div><div style="font-size:16px;font-weight:700;color:#1e293b;">%s</div></div>
                    <div><div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:6px;">Phản hồi hiện tại</div><div style="font-size:16px;font-weight:700;color:#1e293b;">%s</div></div>
                  </div>
                </div>
                <div style="display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap;margin-bottom:18px;">
                  <p style="margin:0;color:#475569;line-height:1.7;">Liên kết này hết hạn lúc <strong>%s</strong>.</p>
                  <form method="post" action="/public/fraud-alert-actions/%s" style="margin:0;">
                    <button type="submit" style="background:%s;color:#fff;border:none;border-radius:16px;padding:16px 24px;font-family:Arial,'Segoe UI',sans-serif;font-size:15px;line-height:1.35;font-weight:700;white-space:nowrap;cursor:pointer;box-shadow:0 16px 30px rgba(15,23,42,0.14);">
                      %s
                    </button>
                  </form>
                </div>
                """.formatted(
                        escapeHtml(valueOrDefault(action.getRiskLevel(), "UNKNOWN")),
                        action.getFraudAlertId(),
                        escapeHtml(amountText(action.getAmount(), action.getCurrency())),
                        escapeHtml(valueOrDefault(action.getMerchantName(), "Merchant khong xac dinh")),
                        escapeHtml(valueOrDefault(action.getMaskedPan(), "N/A")),
                        alertStatus,
                        customerResponse,
                        action.getExpiresAt().format(DATETIME_FORMATTER),
                        escapeHtml(token),
                        actionColor,
                        escapeHtml(actionLabel)
                )
        );
    }

    private String renderSuccess(FraudAlertEmailActionService.EmailActionExecutionResult result) {
        FraudAlertEmailAction action = result.action();
        String title = action.getDecision() == FraudAlertEmailActionDecision.CONFIRM
                ? "Đã xác nhận giao dịch"
                : "Đã báo cáo giao dịch không phải bạn";
        String body = action.getDecision() == FraudAlertEmailActionDecision.CONFIRM
                ? "Cảm ơn bạn đã xác nhận. BKBank đã cập nhật cảnh báo này và ghi nhận giao dịch là hợp lệ."
                : "BKBank đã ghi nhận phản hồi của bạn. Nếu cần, thẻ sẽ được khóa và case sẽ tiếp tục được xử lý theo quy trình hiện tại.";
        return layout(title,
                """
                <div style="background:linear-gradient(180deg,#effcf7,#e6fbf3);border:1px solid #9de8ca;border-radius:22px;padding:20px;box-shadow:0 18px 36px rgba(16,185,129,0.10);">
                  <div style="display:inline-block;background:#d1fae5;color:#047857;border-radius:999px;padding:7px 12px;font-size:12px;font-weight:800;letter-spacing:0.08em;text-transform:uppercase;margin-bottom:12px;">Xử lý thành công</div>
                  <p style="margin:0 0 10px;font-size:24px;font-weight:800;color:#064e3b;">%s</p>
                  <p style="margin:0;font-size:16px;line-height:1.75;color:#065f46;">%s</p>
                </div>
                """.formatted(escapeHtml(title), escapeHtml(body)));
    }

    private String renderError(String title, String message) {
        return layout(title,
                """
                <div style="background:linear-gradient(180deg,#fff5f5,#fff0f0);border:1px solid #fecaca;border-radius:22px;padding:20px;box-shadow:0 18px 36px rgba(239,68,68,0.08);">
                  <div style="display:inline-block;background:#fee2e2;color:#b91c1c;border-radius:999px;padding:7px 12px;font-size:12px;font-weight:800;letter-spacing:0.08em;text-transform:uppercase;margin-bottom:12px;">Không thể xử lý</div>
                  <p style="margin:0 0 10px;font-size:24px;font-weight:800;color:#7f1d1d;">%s</p>
                  <p style="margin:0;font-size:16px;line-height:1.75;color:#991b1b;">%s</p>
                </div>
                """.formatted(escapeHtml(title), escapeHtml(message)));
    }

    private String layout(String title, String content) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;background:radial-gradient(circle at top,#eef4ff 0%%,#f8fbff 48%%,#eff7f1 100%%);font-family:Arial,sans-serif;color:#172033;">
                  <div style="max-width:680px;margin:48px auto;padding:0 16px;">
                    <div style="background:rgba(255,255,255,0.92);border:1px solid rgba(217,228,245,0.85);backdrop-filter:blur(12px);border-radius:30px;padding:32px;box-shadow:0 28px 64px rgba(20,35,90,0.10);">
                      <h1 style="margin:0 0 14px;font-size:34px;line-height:1.18;color:#0f2d66;">%s</h1>
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(escapeHtml(title), escapeHtml(title), content);
    }

    private String amountText(Double amount, String currency) {
        return String.format("%.2f %s", amount != null ? amount : 0.0, currency != null ? currency : "USD");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
