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
@RequestMapping("/api") // ✅ Base path for all API
@CrossOrigin(origins = "https://ai-resume-frontend-mg.vercel.app/") // ✅ Frontend CORS allowed
public class ResumeController {

    @Autowired
    private OpenAIService openAIService;

    // ✅ 1. Analyze Single Resume for Candidate/Company Mode
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeResume(@RequestParam("file") MultipartFile file,
                                                @RequestParam("role") String role,
                                                @RequestParam("mode") String mode) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String resumeText = stripper.getText(document);
            System.out.println("✅ Extracted Resume Text:\n" + resumeText);

            String analysis = openAIService.analyzeResume(resumeText, role, mode);
            return ResponseEntity.ok(analysis);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\": \"❌ Failed to process resume: " + e.getMessage() + "\"}");
        }
    }

    // ✅ 2. Improve Resume for Candidate Mode
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

    // ✅ 3. Batch Compare Resumes for Company Mode
    @PostMapping("/compare-batch")
    public ResponseEntity<String> compareBatchResumes(@RequestParam("files") List<MultipartFile> files,
                                                      @RequestParam("role") String role) {
        try {
            List<String> resumeTexts = new ArrayList<>();
            for (MultipartFile file : files) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String resumeText = stripper.getText(document);
                    resumeTexts.add(resumeText);
                }
            }
            String comparisonResult = openAIService.compareResumesInBatch(resumeTexts, role);
            return ResponseEntity.ok(comparisonResult);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\": \"❌ Failed to process batch resumes: " + e.getMessage() + "\"}");
        }
    }

    // ✅ 4. API Health Check
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("✅ ResumeHelp API is working!");
    }

    // ✅ 5. Welcome Page
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("🚀 Welcome to ResumeHelp API! Use /api/health to check API status.");
    }
}
