// Package declaration — utility class for sending OTP emails via SMTP (with console fallback)
package com.items.util;

// InputStream loads mail.properties from the classpath
import java.io.InputStream;
// Properties holds the SMTP configuration (host, port, user, password)
import java.util.Properties;
// Random generates the 6-digit OTP code
import java.util.Random;

// javax.mail classes for composing and sending SMTP messages
import javax.mail.Authenticator;           // Authenticates with the SMTP server
import javax.mail.Message;                  // Represents an email message
import javax.mail.MessagingException;       // Checked exception for mail errors
import javax.mail.PasswordAuthentication;   // Holds username + password for SMTP auth
import javax.mail.Session;                  // Email session configured with SMTP properties
import javax.mail.Transport;                // Sends the message via the transport protocol
import javax.mail.internet.InternetAddress; // Email address (from, to)
import javax.mail.internet.MimeMessage;     // MIME-format email message

/**
 * Utility for one-time password (OTP) email delivery.
 *
 * How it works:
 *   1. Loads SMTP credentials from mail.properties (WEB-INF/classes/mail.properties)
 *   2. Generates a random 6-digit OTP code (100000–999999)
 *   3. ALWAYS prints the OTP to the console (for development/debugging)
 *   4. If SMTP is configured, sends a styled HTML email via Gmail's SMTP server
 *   5. If SMTP fails OR is not configured, silently continues (OTP is still visible in console)
 *
 * Error handling: ALL exceptions are caught internally (catch(Throwable)) so
 * a mail failure never breaks the registration flow. The user can still see
 * the OTP in the server console.
 *
 * SMTP configuration (mail.properties):
 *   EMAIL_HOST=smtp.gmail.com
 *   EMAIL_PORT=587
 *   EMAIL_USER=your-email@gmail.com
 *   EMAIL_PASS=your-app-password
 *   EMAIL_FROM=your-email@gmail.com
 *   EMAIL_FROM_NAME=ItemManager
 */
public class EmailUtil {

    // Shared Random instance for OTP generation (thread-local would be safer, but this is simpler)
    private static final Random RANDOM = new Random();
    // Configuration properties loaded from mail.properties once at class load time
    private static final Properties CONFIG = new Properties();

    /**
     * Static initializer — runs once when the class is first loaded.
     * Loads mail.properties from the classpath (WEB-INF/classes/).
     * If the file is missing or can't be read, CONFIG will be empty
     * and the sendOtpEmail method will fall back to console-only.
     */
    static {
        try (InputStream in = EmailUtil.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if(in != null) CONFIG.load(in);
            else System.err.println("EmailUtil: mail.properties not found in classpath.");
        } catch(Exception e) {
            System.err.println("EmailUtil: Failed to load mail.properties: " + e.getMessage());
        }
    }

    /** Private constructor — utility class should not be instantiated. */
    private EmailUtil() {}

    /**
     * Generates a random 6-digit OTP code.
     * The code is in the range 100000–999999 (always exactly 6 digits).
     *
     * @return a 6-character string representing the OTP
     */
    public static String generateOtp() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Sends an OTP email to the specified recipient.
     *
     * This method is designed to NEVER throw an exception — every operation
     * from SMTP session creation to message sending is wrapped in a single
     * catch(Throwable) block. This ensures the registration flow never breaks
     * due to mail configuration issues.
     *
     * The OTP is ALWAYS printed to the system console, regardless of whether
     * SMTP is configured or the email was sent successfully. This is essential
     * for development environments.
     *
     * @param toEmail  the recipient's email address
     * @param otpCode  the 6-digit OTP code
     * @param fullName the recipient's full name (for the email greeting)
     */
    public static void sendOtpEmail(String toEmail, String otpCode, String fullName) {
        // Always print the OTP to console — this works even when SMTP is not configured
        System.out.println("========================================");
        System.out.println("OTP for " + fullName + " (" + toEmail + "): " + otpCode);
        System.out.println("========================================");

        // Everything else is wrapped in catch(Throwable) to prevent crashes
        try {
            // Load SMTP configuration from mail.properties
            String host = CONFIG.getProperty("EMAIL_HOST");
            String port = CONFIG.getProperty("EMAIL_PORT", "587");
            String user = CONFIG.getProperty("EMAIL_USER");
            String pass = CONFIG.getProperty("EMAIL_PASS");
            String from = CONFIG.getProperty("EMAIL_FROM", user);
            String fromName = CONFIG.getProperty("EMAIL_FROM_NAME", "ItemManager");

            // If SMTP is not configured (missing host, user, or pass), skip email sending
            // The OTP is still visible in the console from the print above
            if(host == null || user == null || pass == null) {
                System.out.println("EmailUtil: SMTP not configured — OTP only printed to console.");
                return;
            }

            // Configure JavaMail session properties for Gmail SMTP
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");              // Enable SMTP authentication
            props.put("mail.smtp.starttls.enable", "true");   // Use TLS encryption
            props.put("mail.smtp.host", host);                // SMTP server hostname
            props.put("mail.smtp.port", port);                // SMTP port (587 for TLS)

            // Create a mail session with SMTP authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });

            // Compose the email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, fromName));       // Sender address + display name
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail)); // Recipient
            message.setSubject("Verify your ItemManager account");      // Subject line

            // Set the HTML content (styled email body)
            message.setContent(buildHtmlBody(fullName, otpCode), "text/html; charset=UTF-8");

            // Send the message via SMTP
            Transport.send(message);
            System.out.println("EmailUtil: OTP email sent successfully to " + toEmail);

        } catch(Throwable t) {
            // Catch EVERYTHING — including NoClassDefFoundError, NoSuchMethodError, etc.
            // Print the error to stderr so developers can see what went wrong
            // But NEVER let this exception propagate to the caller
            System.err.println("EmailUtil: Failed — " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * Builds a professionally styled HTML email body for the OTP verification message.
     * Uses inline CSS (no external stylesheets) for maximum email client compatibility.
     *
     * @param fullName the recipient's name for the greeting
     * @param otpCode  the 6-digit code to display prominently
     * @return a complete HTML document as a string
     */
    private static String buildHtmlBody(String fullName, String otpCode) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head><meta charset=\"UTF-8\"><style>" +
                "body{margin:0;padding:0;background:#f3f4f6;font-family:'Segoe UI',Tahoma,sans-serif}" +
                ".container{max-width:480px;margin:40px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.08)}" +
                ".header{background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:32px;text-align:center}" +
                ".header h1{margin:0;color:#fff;font-size:20px;font-weight:700}" +
                ".header p{margin:6px 0 0;color:rgba(255,255,255,.8);font-size:14px}" +
                ".body{padding:32px}" +
                ".body h2{margin:0 0 8px;font-size:18px;color:#1f2937}" +
                ".body p{margin:0 0 20px;color:#6b7280;font-size:14px;line-height:1.6}" +
                ".otp-box{background:#f3f4f6;border-radius:12px;padding:20px;text-align:center;margin:20px 0}" +
                ".otp-box .code{font-size:36px;font-weight:800;letter-spacing:8px;color:#6366f1;font-family:monospace}" +
                ".otp-box .expiry{font-size:12px;color:#9ca3af;margin-top:8px}" +
                ".footer{padding:20px 32px;border-top:1px solid #f3f4f6;text-align:center}" +
                ".footer p{margin:0;font-size:12px;color:#9ca3af}" +
            "</style></head>" +
            "<body>" +
                "<div class=\"container\">" +
                    "<div class=\"header\">" +
                        "<h1>Verify Your Email</h1>" +
                        "<p>ItemManager Account</p>" +
                    "</div>" +
                    "<div class=\"body\">" +
                        "<h2>Hello " + escapeHtml(fullName) + ",</h2>" +
                        "<p>Thank you for registering! Use the code below to verify your email address and activate your account.</p>" +
                        "<div class=\"otp-box\">" +
                            "<div class=\"code\">" + otpCode + "</div>" +
                            "<div class=\"expiry\">Valid for 5 minutes</div>" +
                        "</div>" +
                        "<p>If you didn't create an account, you can safely ignore this email.</p>" +
                    "</div>" +
                    "<div class=\"footer\">" +
                        "<p>&copy; 2026 ItemManager. All rights reserved.</p>" +
                    "</div>" +
                "</div>" +
            "</body>" +
            "</html>";
    }

    /**
     * Escapes HTML special characters to prevent injection in the email body.
     * This is a security measure — user-provided names are escaped before being
     * inserted into the HTML template.
     *
     * @param s the raw string to escape
     * @return the escaped string (safe for HTML embedding)
     */
    private static String escapeHtml(String s) {
        if(s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }
}
