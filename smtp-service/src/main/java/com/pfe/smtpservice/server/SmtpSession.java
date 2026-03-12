package com.pfe.smtpservice.server;


import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Session SMTP complète avec un client.
 * INCHANGÉ par rapport à l'original — la machine à états RFC 5321 est correcte.
 */
@Slf4j
public class SmtpSession implements Runnable {

    private enum State { CONNECTED, GREETED, MAIL_FROM, RCPT_TO, DATA, DONE }

    private final Socket socket;
    private final String serverHostname;
    private final MailDeliveryHandler deliveryHandler;

    private BufferedReader reader;
    private PrintWriter writer;
    private State state = State.CONNECTED;
    private EmailMessage currentEmail;

    public SmtpSession(Socket socket, String hostname, MailDeliveryHandler handler) {
        this.socket = socket;
        this.serverHostname = hostname;
        this.deliveryHandler = handler;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            sendResponse("220 " + serverHostname + " ESMTP SmtpServer/1.0 Ready");

            String line;
            while ((line = reader.readLine()) != null) {
                log.debug(">>> RECU : {}", line);
                processCommand(line.trim());
                if (state == State.DONE) break;
            }
        } catch (IOException e) {
            log.error("Erreur session SMTP : {}", e.getMessage());
        } finally {
            closeSession();
        }
    }

    private void processCommand(String line) throws IOException {
        String upper = line.toUpperCase();
        if (upper.startsWith("EHLO") || upper.startsWith("HELO")) handleEhlo(line);
        else if (upper.startsWith("MAIL FROM:")) handleMailFrom(line);
        else if (upper.startsWith("RCPT TO:"))   handleRcptTo(line);
        else if (upper.equals("DATA"))            handleData();
        else if (upper.equals("QUIT"))            handleQuit();
        else if (upper.equals("RSET"))            handleRset();
        else if (upper.equals("NOOP"))            sendResponse("250 Ok");
        else sendResponse("500 Command not recognized: " + line);
    }

    private void handleEhlo(String line) {
        if (state != State.CONNECTED) {
            sendResponse("503 Bad sequence — already greeted"); return;
        }
        String clientDomain = line.substring(5).trim();
        sendResponse("250-" + serverHostname + " Hello " + clientDomain);
        sendResponse("250-SIZE 10240000");
        sendResponse("250-8BITMIME");
        sendResponse("250 SMTPUTF8");
        state = State.GREETED;
        currentEmail = new EmailMessage();
    }

    private void handleMailFrom(String line) {
        if (state != State.GREETED) {
            sendResponse("503 Bad sequence — send EHLO first"); return;
        }
        String from = extractAddress(line.substring(10));
        if (from == null || from.isBlank()) {
            sendResponse("501 Syntax error — MAIL FROM:<address>"); return;
        }
        currentEmail.setFrom(from);
        state = State.MAIL_FROM;
        sendResponse("250 Ok — Sender <" + from + "> accepted");
    }

    private void handleRcptTo(String line) {
        if (state != State.MAIL_FROM && state != State.RCPT_TO) {
            sendResponse("503 Bad sequence — send MAIL FROM first"); return;
        }
        String rcpt = extractAddress(line.substring(8));
        if (rcpt == null || rcpt.isBlank()) {
            sendResponse("501 Syntax error — RCPT TO:<address>"); return;
        }
        currentEmail.addRecipient(rcpt);
        state = State.RCPT_TO;
        sendResponse("250 Ok — Recipient <" + rcpt + "> accepted");
    }

    private void handleData() throws IOException {
        if (state != State.RCPT_TO) {
            sendResponse("503 Bad sequence — send RCPT TO first"); return;
        }
        sendResponse("354 Start input, end with <CRLF>.<CRLF>");

        StringBuilder bodyBuilder = new StringBuilder();
        boolean readingHeaders = true;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(".")) break;
            if (line.startsWith(".")) line = line.substring(1);
            if (readingHeaders) {
                if (line.isEmpty()) readingHeaders = false;
                else currentEmail.addRawHeader(line);
            } else {
                bodyBuilder.append(line).append("\n");
            }
        }

        currentEmail.setBody(bodyBuilder.toString());
        currentEmail.setReceivedTimestamp(System.currentTimeMillis());

        String messageId = deliveryHandler.deliver(currentEmail);
        sendResponse("250 Ok — Message queued as <" + messageId + ">");
        state = State.GREETED;
    }

    private void handleQuit() {
        sendResponse("221 " + serverHostname + " Bye");
        state = State.DONE;
    }

    private void handleRset() {
        currentEmail = new EmailMessage();
        state = State.GREETED;
        sendResponse("250 Ok — Session reset");
    }

    private void sendResponse(String response) {
        log.debug("<<< ENVOI : {}", response);
        writer.print(response + "\r\n");
        writer.flush();
    }

    private String extractAddress(String s) {
        int start = s.indexOf('<');
        int end   = s.indexOf('>');
        if (start >= 0 && end > start) return s.substring(start+1, end).trim();
        return s.trim();
    }

    private void closeSession() {
        try { socket.close(); } catch (IOException ignored) {}
        log.info("Session SMTP fermée.");
    }
}
