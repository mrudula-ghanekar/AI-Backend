package com.resumehelp.controller;

import com.resumehelp.service.OpenAIService;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ResumeController {

    @Autowired
    private OpenAIService openAIService;

    private final Tika tika = new Tika();

    // Analyze resume by raw text
    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeResume(
            @RequestParam("resumeText") String resumeText,
            @RequestParam("role") String role,
            @RequestParam("mode") String mode) {

        String result = openAIService.analyzeResume(resumeText, role, mode);
        return ResponseEntity.ok(result);
    }

    // Analyze uploaded resume file
    @PostMapping("/analyze-file")
    public ResponseEntity<String> analyzeResumeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("role") String role,
            @RequestParam("mode") String mode) {

        try {
            String resumeText = tika.parseToString(file.getInputStream());
            String result = openAIService.analyzeResume(resumeText, role, mode);
            return ResponseEntity.ok(result);
        } catch (IOException | TikaException e) {
            return ResponseEntity.internalServerError().body("Failed to parse resume file: " + e.getMessage());
        }
    }

    // Improve resume by text
    @PostMapping("/improve")
    public ResponseEntity<String> improveResume(
            @RequestParam("resumeText") String resumeText,
            @RequestParam("role") String role) {

        String result = openAIService.generateImprovedResume(resumeText, role);
        return ResponseEntity.ok(result);
    }

    // Compare batch of resumes by text
    @PostMapping("/compare-batch")
    public ResponseEntity<String> compareBatch(
            @RequestParam("resumes") List<String> resumeTexts,
            @RequestParam("fileNames") List<String> fileNames,
            @RequestParam("role") String role) {

        String result = openAIService.compareResumesInBatch(resumeTexts, fileNames, role);
        return ResponseEntity.ok(result);
    }

    // Compare batch of resumes with job description
    @PostMapping("/compare-with-jd")
    public ResponseEntity<String> compareWithJD(
            @RequestParam("resumes") List<String> resumeTexts,
            @RequestParam("fileNames") List<String> fileNames,
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam("email") String email) {

        String result = openAIService.compareResumesInBatchWithJD(resumeTexts, fileNames, jobDescription, email);
        return ResponseEntity.ok(result);
    }
}
