package com.resumehelp.controller;

import com.resumehelp.service.OpenAIService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "https://ai-resume-frontend-mg.vercel.app")  // Replace with your actual frontend URL
public class ResumeController {

    @Autowired
    private OpenAIService openAIService;

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeResume(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam("role") String role,
            @RequestParam("mode") String mode) {

        try {
            List<String> resumeTexts = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            // Check if no file is uploaded
            if (file == null && (files == null || files.isEmpty())) {
                return ResponseEntity.badRequest().body("{\"error\": \"No files uploaded. Please select a resume.\"}");
            }

            // Extract text from the uploaded single file
            if (file != null) {
                resumeTexts.add(extractTextFromFile(file));
                fileNames.add(file.getOriginalFilename());
            }

            // Extract text from the uploaded multiple files
            if (files != null) {
                for (MultipartFile multiFile : files) {
                    resumeTexts.add(extractTextFromFile(multiFile));
                    fileNames.add(multiFile.getOriginalFilename());
                }
            }

            // Call the OpenAI service for either single resume or batch comparison
            String analysis = mode.equalsIgnoreCase("company") ? 
                    openAIService.compareResumesInBatch(resumeTexts, fileNames, role) :
                    openAIService.analyzeResume(resumeTexts.get(0), role, mode);

            return ResponseEntity.ok(analysis);

        } catch (IOException e) {
            // Return an error response if file parsing fails
            return ResponseEntity.status(500).body("{\"error\": \"❌ Failed to process resume(s). Please check the file format and try again.\"}");
        }
    }

    // Method to extract text from PDF or DOCX files
    private String extractTextFromFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename().toLowerCase();
        InputStream inputStream = file.getInputStream();
        String fileText = "";

        // Handling PDF files
        if (fileName.endsWith(".pdf")) {
            PDDocument document = PDDocument.load(inputStream);
            PDFTextStripper stripper = new PDFTextStripper();
            fileText = stripper.getText(document);
            document.close();
        } 
        // Handling DOCX files
        else if (fileName.endsWith(".docx")) {
            XWPFDocument docx = new XWPFDocument(inputStream);
            fileText = docx.getParagraphs().stream()
                    .map(p -> p.getText())
                    .reduce((p1, p2) -> p1 + "\n" + p2)
                    .orElse("");
            docx.close();
        } 
        // Unsupported file type
        else {
            throw new IOException("Unsupported file type. Please upload a PDF or DOCX file.");
        }

        // Return the extracted text as UTF-8 encoded string
        return new String(fileText.getBytes(), StandardCharsets.UTF_8);
    }
}
