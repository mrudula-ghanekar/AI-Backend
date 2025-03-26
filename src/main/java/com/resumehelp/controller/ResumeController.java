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
@CrossOrigin(origins = "https://your-frontend-url.com")  // Replace with your actual frontend URL
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

            if (file == null && (files == null || files.isEmpty())) {
                return ResponseEntity.badRequest().body("{\"error\": \"No files uploaded. Please select a resume.\"}");
            }

            if (file != null) {
                resumeTexts.add(extractTextFromFile(file));
                fileNames.add(file.getOriginalFilename());
            }

            if (files != null) {
                for (MultipartFile multiFile : files) {
                    resumeTexts.add(extractTextFromFile(multiFile));
                    fileNames.add(multiFile.getOriginalFilename());
                }
            }

            String analysis = mode.equalsIgnoreCase("company") ?
                    openAIService.compareResumesInBatch(resumeTexts, fileNames, role) :
                    openAIService.analyzeResume(resumeTexts.get(0), role, mode);

            return ResponseEntity.ok(analysis);

        } catch (IOException e) {
            return ResponseEntity.status(500).body("{\"error\": \"❌ Failed to process resume(s).\"}");
        }
    }

    private String extractTextFromFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename().toLowerCase();
        InputStream inputStream = file.getInputStream();
        String fileText = "";

        if (fileName.endsWith(".pdf")) {
            PDDocument document = PDDocument.load(inputStream);
            PDFTextStripper stripper = new PDFTextStripper();
            fileText = stripper.getText(document);
            document.close();
        } else if (fileName.endsWith(".docx")) {
            XWPFDocument docx = new XWPFDocument(inputStream);
            fileText = docx.getParagraphs().stream()
                    .map(p -> p.getText())
                    .reduce((p1, p2) -> p1 + "\n" + p2)
                    .orElse("");
            docx.close();
        } else {
            throw new IOException("Unsupported file type.");
        }

        return new String(fileText.getBytes(), StandardCharsets.UTF_8);
    }
}
