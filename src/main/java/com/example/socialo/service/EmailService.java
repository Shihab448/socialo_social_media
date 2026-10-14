package com.example.socialo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends branded HTML emails from Socialo.
 * Falls back to clear console output when MAIL_USERNAME is not set (dev mode).
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    // ─────────────────────────────────────────────────────────────
    //  Public send methods
    // ─────────────────────────────────────────────────────────────

    /** 6-digit email verification code sent at signup. */
    public void sendVerificationCode(String toEmail, String fullName, String code) {
        String subject = "Socialo: " + code + " is your verification code";
        String body = branded(
            "Verify your email address",
            "Hi <strong>" + esc(fullName) + "</strong>,<br><br>" +
            "Thanks for joining Socialo! Use the code below to verify your email address.",
            "<div style='font-size:40px;font-weight:800;letter-spacing:12px;" +
            "color:#1d4ed8;text-align:center;background:#eff6ff;border-radius:14px;" +
            "padding:20px 24px;margin:24px 0;'>" + esc(code) + "</div>" +
            "<p style='color:#6b7280;font-size:13px;text-align:center;'>" +
            "This code expires in <strong>15 minutes</strong>. " +
            "Never share it with anyone.<br><br>" +
            "If you did not sign up for Socialo, you can safely ignore this email.</p>"
        );
        send(toEmail, subject, body);
    }

    /** Password reset link (one-use, 30 min). */
    public void sendPasswordReset(String toEmail, String fullName, String token) {
        String link = baseUrl + "/reset-password?token=" + token;
        String subject = "Reset your Socialo password";
        String body = branded(
            "Reset your password",
            "Hi <strong>" + esc(fullName) + "</strong>,<br><br>" +
            "We received a request to reset the password for your Socialo account. " +
            "Click the button below to choose a new password.",
            "<div style='text-align:center;margin:28px 0;'>" +
            "<a href='" + link + "' style='display:inline-block;background:#1d4ed8;color:#fff;" +
            "text-decoration:none;border-radius:12px;padding:14px 32px;font-size:15px;" +
            "font-weight:700;'>Reset Password</a></div>" +
            "<p style='color:#6b7280;font-size:12px;text-align:center;'>" +
            "This link expires in <strong>30 minutes</strong> and can only be used once.<br>" +
            "If you did not request this, ignore this email — your password will not change.</p>"
        );
        send(toEmail, subject, body);
    }

    /** Account suspended — includes appeal link. */
    public void sendSuspensionNotice(String toEmail, String fullName) {
        String appealLink = baseUrl + "/appeal";
        String subject = "Your Socialo account has been suspended";
        String body = branded(
            "Account Suspended",
            "Hi <strong>" + esc(fullName) + "</strong>,<br><br>" +
            "Your Socialo account has been <strong>suspended</strong> following a review by our team.",
            "<div style='text-align:center;margin:28px 0;'>" +
            "<a href='" + appealLink + "' style='display:inline-block;background:#ef4444;color:#fff;" +
            "text-decoration:none;border-radius:12px;padding:14px 32px;font-size:15px;" +
            "font-weight:700;'>Appeal this decision</a></div>" +
            "<p style='color:#6b7280;font-size:12px;text-align:center;'>" +
            "If you believe this was a mistake, submit an appeal with your identity document.</p>"
        );
        send(toEmail, subject, body);
    }

    /** Account restored after appeal approved. */
    public void sendRestorationNotice(String toEmail, String fullName) {
        String subject = "Your Socialo account has been restored";
        String body = branded(
            "Account Restored ✓",
            "Hi <strong>" + esc(fullName) + "</strong>,<br><br>" +
            "Great news! Your Socialo account has been <strong>restored</strong>. " +
            "You can now log in and use Socialo normally.",
            "<div style='text-align:center;margin:28px 0;'>" +
            "<a href='" + baseUrl + "/login' style='display:inline-block;background:#22c55e;color:#fff;" +
            "text-decoration:none;border-radius:12px;padding:14px 32px;font-size:15px;" +
            "font-weight:700;'>Go to Login</a></div>"
        );
        send(toEmail, subject, body);
    }

    /** Appeal rejected notification. */
    public void sendAppealRejected(String toEmail, String fullName, String adminNote) {
        String subject = "Your Socialo appeal has been reviewed";
        String body = branded(
            "Appeal Rejected",
            "Hi <strong>" + esc(fullName) + "</strong>,<br><br>" +
            "We have reviewed your appeal and unfortunately it has been <strong>rejected</strong>." +
            (adminNote != null && !adminNote.isBlank()
                ? "<br><br><strong>Reason:</strong> " + esc(adminNote) : ""),
            "<p style='color:#6b7280;font-size:13px;'>" +
            "If you have further questions, please contact our support team.</p>"
        );
        send(toEmail, subject, body);
    }

    // ─────────────────────────────────────────────────────────────
    //  Branded HTML template (Facebook-style)
    // ─────────────────────────────────────────────────────────────

    private String branded(String title, String greeting, String bodyContent) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>" + esc(title) + "</title></head>" +
            "<body style='margin:0;padding:0;background:#f3f4f6;font-family:Poppins,Arial,sans-serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f3f4f6;padding:32px 16px;'>" +
            "<tr><td align='center'>" +
            // Card
            "<table width='100%' style='max-width:520px;background:#ffffff;border-radius:20px;" +
            "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.08);'>" +
            // Header (dark brand)
            "<tr><td style='background:#02161b;padding:28px 32px;text-align:center;'>" +
            "<div style='display:inline-flex;align-items:center;gap:10px;'>" +
            // Logo circle
            "<div style='width:44px;height:44px;border-radius:50%;background:linear-gradient(135deg,#00e5d4,#1d4ed8);" +
            "display:inline-flex;align-items:center;justify-content:center;" +
            "font-size:20px;color:#fff;font-weight:900;'>S</div>" +
            "<span style='font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;'>Socialo</span>" +
            "</div></td></tr>" +
            // Body
            "<tr><td style='padding:32px 36px 24px;'>" +
            "<h2 style='font-size:20px;font-weight:700;color:#0f172a;margin:0 0 14px;'>" + esc(title) + "</h2>" +
            "<p style='font-size:15px;color:#374151;line-height:1.7;margin:0 0 16px;'>" + greeting + "</p>" +
            bodyContent +
            "</td></tr>" +
            // Footer
            "<tr><td style='background:#f8fafc;padding:18px 36px;border-top:1px solid #e5e7eb;" +
            "text-align:center;'>" +
            "<p style='font-size:12px;color:#9ca3af;margin:0;'>" +
            "This email was sent by <strong style='color:#1d4ed8;'>Socialo</strong>. " +
            "Do not reply to this email. &copy; 2026 Socialo.</p>" +
            "</td></tr>" +
            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    // ─────────────────────────────────────────────────────────────
    //  Internal send
    // ─────────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        if (mailSender == null || fromAddress == null || fromAddress.isBlank()) {
            // ── Console fallback (dev mode — SMTP not configured) ──
            String plainText = htmlBody.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
            System.out.println("\n╔══════════════════════════════════════════════════╗");
            System.out.println("║         SOCIALO EMAIL  [CONSOLE / DEV MODE]      ║");
            System.out.println("╠══════════════════════════════════════════════════╣");
            System.out.printf ("║ To      : %-40s ║%n", to);
            System.out.printf ("║ Subject : %-40s ║%n", subject.length() > 40 ? subject.substring(0,40) : subject);
            System.out.println("╠══════════════════════════════════════════════════╣");
            // Print the readable content in chunks
            int chunkLen = 50;
            String[] words = plainText.split(" ");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                if (line.length() + w.length() + 1 > chunkLen) {
                    System.out.printf("║ %-50s ║%n", line.toString().trim());
                    line = new StringBuilder();
                }
                line.append(w).append(" ");
            }
            if (!line.toString().isBlank()) System.out.printf("║ %-50s ║%n", line.toString().trim());
            System.out.println("╚══════════════════════════════════════════════════╝");
            System.out.println("  → To enable real email: set MAIL_USERNAME and MAIL_PASSWORD");
            System.out.println("  → Get Gmail App Password: Google Account > Security > App passwords\n");
            return;
        }

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromAddress, "Socialo");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            // Anti-spam headers
            mime.setHeader("X-Mailer", "Socialo Mailer");
            mime.setHeader("X-Priority", "1");
            mime.setHeader("Precedence", "bulk");
            mailSender.send(mime);
            System.out.println("[EmailService] ✓ Sent to " + to + " | " + subject);
        } catch (Exception e) {
            System.err.println("[EmailService] ✗ Failed to send to " + to + ": " + e.getMessage());
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
