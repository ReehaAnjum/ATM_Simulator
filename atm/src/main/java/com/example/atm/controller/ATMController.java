package com.example.atm.controller;

import com.example.atm.model.Account;
import com.example.atm.service.ATMService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.example.atm.model.DenominationRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/atm")
@CrossOrigin(origins = "*")   // ENABLE CORS FOR FRONTEND
public class ATMController {

    private final ATMService service;

    public ATMController(ATMService service) {
        this.service = service;
    }

    // ---------------- LOGIN ----------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String pin) {
        Account acc = service.login(pin);

        if (acc == null) {
            return ResponseEntity
                    .status(404)
                    .body("User does not exist");
        }
        return ResponseEntity.ok(acc);
    }

    // ---------------- CHECK BALANCE ----------------
    @GetMapping("/balance")
    public ResponseEntity<Double> balance(@RequestParam String pin) {
        Account acc = service.login(pin);

        if (acc == null) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.ok(acc.getBalance());
    }

    // ---------------- DEPOSIT ----------------
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(
            @RequestParam String pin,
            @RequestParam double amount) {

        Account acc = service.login(pin);
        if (acc == null) {
            return ResponseEntity.status(404).body("User does not exist");
        }

        return ResponseEntity.ok(service.deposit(acc, amount));
    }

    // ---------------- WITHDRAW ----------------
    @PostMapping("/withdraw")
        public ResponseEntity<?> withdraw(
            @RequestParam String pin,
            @RequestParam double amount,
            @RequestBody(required = false) DenominationRequest body) {

        Account acc = service.login(pin);
        if (acc == null) {
            return ResponseEntity.status(404).body("User does not exist");
        }

        Account updated;
        if (body != null && body.getDenominations() != null && !body.getDenominations().isEmpty()) {
            updated = service.withdraw(acc, amount, body.getDenominations());
        } else {
            updated = service.withdraw(acc, amount);
        }
        if (updated == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Invalid denomination or insufficient balance");
        }

        return ResponseEntity.ok(updated);
    }

    // ---------------- MINI STATEMENT (JSON) ----------------
    @GetMapping("/statement")
    public ResponseEntity<?> statement(@RequestParam String pin) {
        Account acc = service.login(pin);

        if (acc == null) {
            return ResponseEntity.status(404).body("User does not exist");
        }
        return ResponseEntity.ok(acc.getTransactions());
    }

    // ---------------- MINI STATEMENT (PDF) ----------------
    @GetMapping("/statement/pdf")
    public ResponseEntity<byte[]> downloadStatementPDF(
            @RequestParam String pin) {

        Account acc = service.login(pin);
        if (acc == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Mini Statement"));
            document.add(new Paragraph("User: " + acc.getUsername()));
            document.add(new Paragraph(" "));

            // include last 5 transactions (most recent). If fewer, pad with empty lines
            List<String> txns = acc.getTransactions();
            int show = 5;
            int start = Math.max(0, txns.size() - show);
            for (int i = start; i < txns.size(); i++) {
                document.add(new Paragraph(txns.get(i)));
            }
            for (int i = txns.size(); i < show; i++) {
                document.add(new Paragraph(" "));
            }

            document.close();

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header(
                        "Content-Disposition",
                        "attachment; filename=mini-statement.pdf"
                    )
                    .body(out.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
