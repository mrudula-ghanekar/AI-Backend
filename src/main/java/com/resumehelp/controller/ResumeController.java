package com.resumehelp.controller;

import com.resumehelp.service.OpenAIService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api") // Base path for all APIs
public class ResumeController {

    @Autowired
    private OpenAIService openAIService;

    // ✅ Analyze Resume for Candidate/Company Mode (Supports Single & Multiple Files)
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeResume(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam("role") String role,
            @RequestParam("mode") String mode) {

        try {
            List<String> resumeTexts = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            // ✅ Handle Single File (Candidate Mode)
            if (file != null) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    resumeTexts.add(stripper.getText(document));
                    fileNames.add(file.getOriginalFilename());
                }
            }

            // ✅ Handle Multiple Files (Company Mode)
            if (files != null && !files.isEmpty()) {
                for (MultipartFile multiFile : files) {
                    try (PDDocument document = PDDocument.load(multiFile.getInputStream())) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        resumeTexts.add(stripper.getText(document));
                        fileNames.add(multiFile.getOriginalFilename());
                    }
                }
            }

            if (resumeTexts.isEmpty()) {
                return ResponseEntity.badRequest().body("{\"error\": \"No valid resumes uploaded.\"}");
            }

            // ✅ Process resumes based on mode
            String analysis;
            if ("company".equalsIgnoreCase(mode)) {
                analysis = openAIService.compareResumesInBatch(resumeTexts, fileNames, role);
            } else {
                analysis = openAIService.analyzeResume(resumeTexts.get(0), role, mode);
            }

            return ResponseEntity.ok(analysis);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\": \"❌ Failed to process resume(s): " + e.getMessage() + "\"}");
        }
    }

    // ✅ Improve Resume for Candidate Mode
    @PostMapping("/improve")
    public ResponseEntity<String> improveResume(@RequestParam("file") MultipartFile file,
                                                @RequestParam("role") String role) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String resumeText = stripper.getText(document);
            System.out.println("✅ Extracted Resume Text for Improvement:\n" + resumeText);

            String improvedResume = openAIService.generateImprovedResume(resumeText, role);
            return ResponseEntity.ok(improvedResume);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\": \"❌ Failed to improve resume: " + e.getMessage() + "\"}");
        }
    }

    // ✅ Batch Compare Resumes for Company Mode (Handles Multiple Files & Names)
    @PostMapping("/compare-batch")
    public ResponseEntity<String> compareBatchResumes(@RequestParam("files") List<MultipartFile> files,
                                                      @RequestParam("role") String role) {
        try {
            List<String> resumeTexts = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            for (MultipartFile file : files) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String resumeText = stripper.getText(document);
                    resumeTexts.add(resumeText);
                    fileNames.add(file.getOriginalFilename());
                }
            }
            
            String comparisonResult = openAIService.compareResumesInBatch(resumeTexts, fileNames, role);
            return ResponseEntity.ok(comparisonResult);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\": \"❌ Failed to process batch resumes: " + e.getMessage() + "\"}");
        }
    }

    // ✅ API Health Check
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("✅ ResumeHelp API is working!");
    }

    // ✅ Welcome Page
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("🚀 Welcome to ResumeHelp API! Use /api/health to check API status.");
    }
}
