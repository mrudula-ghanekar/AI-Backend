package com.resumehelp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@Service
public class ResumeService {

    // Function to extract text from PDF resume
    public String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();

        if (filename != null && filename.endsWith(".pdf")) {
            return extractPdfText(file.getInputStream());
        } else {
            throw new Exception("❌ Unsupported file type. Please upload a PDF file.");
        }
    }

    // Helper method to extract text from PDF
    private String extractPdfText(InputStream inputStream) throws Exception {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
