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
                ? "Day la giao dich cua toi"
                : "Day khong phai giao dich cua toi";
        String actionColor = action.getDecision() == FraudAlertEmailActionDecision.CONFIRM ? "#047857" : "#b91c1c";
        String alertStatus = alert != null && alert.get("status") != null ? escapeHtml(alert.get("status").toString()) : "N/A";
        String customerResponse = alert != null && alert.get("customerResponse") != null ? escapeHtml(alert.get("customerResponse").toString()) : "NO_RESPONSE";
        return layout(
                "Xac nhan giao dich nghi ngo",
                """
                <p style="margin:0 0 12px;">BKBank can ban xac nhan lai mot giao dich nghi ngo.</p>
                <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:16px;margin:16px 0;">
                  <div style="margin-bottom:8px;"><strong>Fraud alert:</strong> #%s</div>
                  <div style="margin-bottom:8px;"><strong>So tien:</strong> %s</div>
                  <div style="margin-bottom:8px;"><strong>Merchant:</strong> %s</div>
                  <div style="margin-bottom:8px;"><strong>The:</strong> %s</div>
                  <div style="margin-bottom:8px;"><strong>Muc do rui ro:</strong> %s</div>
                  <div style="margin-bottom:8px;"><strong>Trang thai case:</strong> %s</div>
                  <div><strong>Phan hoi hien tai:</strong> %s</div>
                </div>
                <p style="margin:0 0 16px;">Lien ket nay het han luc <strong>%s</strong>.</p>
                <form method="post" action="/public/fraud-alert-actions/%s">
                  <button type="submit" style="background:%s;color:#fff;border:none;border-radius:12px;padding:12px 20px;font-size:15px;font-weight:700;cursor:pointer;">
                    %s
                  </button>
                </form>
                """.formatted(
                        action.getFraudAlertId(),
                        escapeHtml(amountText(action.getAmount(), action.getCurrency())),
                        escapeHtml(valueOrDefault(action.getMerchantName(), "Merchant khong xac dinh")),
                        escapeHtml(valueOrDefault(action.getMaskedPan(), "N/A")),
                        escapeHtml(valueOrDefault(action.getRiskLevel(), "UNKNOWN")),
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
                ? "Da xac nhan giao dich"
                : "Da bao cao giao dich khong phai ban";
        String body = action.getDecision() == FraudAlertEmailActionDecision.CONFIRM
                ? "Cam on ban da xac nhan. BKBank da cap nhat case fraud alert nay."
                : "BKBank da ghi nhan phan hoi cua ban. Neu can, the se duoc khoa theo quy trinh hien tai.";
        return layout(title,
                """
                <div style="background:#ecfdf5;border:1px solid #a7f3d0;border-radius:16px;padding:16px;">
                  <p style="margin:0 0 8px;"><strong>%s</strong></p>
                  <p style="margin:0;">%s</p>
                </div>
                """.formatted(escapeHtml(title), escapeHtml(body)));
    }

    private String renderError(String title, String message) {
        return layout(title,
                """
                <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:16px;padding:16px;">
                  <p style="margin:0 0 8px;"><strong>%s</strong></p>
                  <p style="margin:0;">%s</p>
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
                <body style="margin:0;background:#f1f5f9;font-family:Arial,sans-serif;color:#172033;">
                  <div style="max-width:640px;margin:48px auto;padding:0 16px;">
                    <div style="background:#ffffff;border-radius:20px;padding:24px;box-shadow:0 16px 40px rgba(15,23,42,0.08);">
                      <h1 style="margin:0 0 12px;font-size:24px;">%s</h1>
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
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
