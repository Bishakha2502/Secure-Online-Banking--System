package com.banking.application.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.banking.application.dto.EmailDetails;
import com.banking.application.entity.Transaction;
import com.banking.application.entity.User;
import com.banking.application.repository.TransactionRepository;
import com.banking.application.repository.UserRepository;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class BankStatement {

    private TransactionRepository transactionRepository;
    private UserRepository userRepository;
    private EmailService emailService;

    public List<Transaction> generateStatement(String accountNumber, String startDate, String endDate) {

        try {
            LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
            LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);

            List<Transaction> transactionList = transactionRepository.findAll().stream()
                    .filter(t -> t.getAccountNumber().equals(accountNumber))
                    .filter(t -> !t.getCreatedAt().isBefore(start) && !t.getCreatedAt().isAfter(end))
                    .toList();

            User user = userRepository.findByAccountNumber(accountNumber);

            if (user == null) {
                throw new RuntimeException("User not found");
            }

            String customerName = user.getFirstName() + " " + user.getLastName();

            // Create statements folder
            String folderPath = System.getProperty("user.dir") + File.separator + "statements";
            File folder = new File(folderPath);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            // PDF file path
            String filePath = folderPath + File.separator +
                    "Statement_" + accountNumber + "_" + LocalDate.now() + ".pdf";

            System.out.println("Generating PDF at: " + filePath);

            Document document = new Document(new Rectangle(PageSize.A4));
            OutputStream outputStream = new FileOutputStream(filePath);

            PdfWriter.getInstance(document, outputStream);
            document.open();

            // ================= BANK HEADER =================
            PdfPTable bankTable = new PdfPTable(1);

            PdfPCell bankName = new PdfPCell(new Phrase("The Banking App"));
            bankName.setBorder(0);
            bankName.setBackgroundColor(BaseColor.CYAN);
            bankName.setPadding(10f);

            PdfPCell bankAddress = new PdfPCell(new Phrase("RadhaNagar, Burnpur"));
            bankAddress.setBorder(0);

            bankTable.addCell(bankName);
            bankTable.addCell(bankAddress);

            // ================= CUSTOMER DETAILS =================
            PdfPTable customerTable = new PdfPTable(2);

            customerTable.addCell(createCell("Start Date: " + startDate));
            customerTable.addCell(createCell("Statement of Account"));
            customerTable.addCell(createCell("End Date: " + endDate));
            customerTable.addCell(createCell("Customer: " + customerName));
            customerTable.addCell(createCell("Account Number: " + accountNumber));
            customerTable.addCell(createCell("Email: " + user.getEmail()));

            // ================= TRANSACTION TABLE =================
            PdfPTable transactionTable = new PdfPTable(4);

            transactionTable.addCell(createHeader("DATE"));
            transactionTable.addCell(createHeader("TYPE"));
            transactionTable.addCell(createHeader("AMOUNT"));
            transactionTable.addCell(createHeader("STATUS"));

            for (Transaction t : transactionList) {
                transactionTable.addCell(t.getCreatedAt().toString());
                transactionTable.addCell(t.getTransactionType());
                transactionTable.addCell(t.getAmount().toString());
                transactionTable.addCell(t.getStatus());
            }

            document.add(bankTable);
            document.add(customerTable);
            document.add(transactionTable);

            document.close();
            outputStream.close();

            File pdfFile = new File(filePath);

            if (!pdfFile.exists() || pdfFile.length() == 0) {
                throw new RuntimeException("PDF generation failed");
            }

            log.info("PDF generated successfully");

            // ================= SEND EMAIL WITH PDF =================
            EmailDetails emailDetails = EmailDetails.builder()
                    .recipient(user.getEmail())
                    .subject("Your Bank Statement")
                    .messageBody("Dear Customer,\n\nPlease find attached your account statement.\n\nRegards,\nThe Banking App")
                    .attachment(filePath)
                    .build();

            emailService.sendEmailWithAttachment(emailDetails);

            log.info("Statement email sent successfully");

            return transactionList;

        } catch (Exception e) {
            log.error("Error generating statement", e);
            throw new RuntimeException("Failed to generate statement: " + e.getMessage());
        }
    }

    private PdfPCell createHeader(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return cell;
    }

    private PdfPCell createCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBorder(0);
        return cell;
    }
}