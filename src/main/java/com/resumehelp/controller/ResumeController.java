package com.resumehelp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumehelp.service.OpenAIService;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ResumeController {

    @Autowired
    private OpenAIService openAIService;

    private final Tika tika = new Tika();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Analyze resume by raw text
    @PostMapping("/analyze")
    public ResponseEntity<Object> analyzeResume(
            @RequestParam("resumeText") String resumeText,
            @RequestParam("role") String role,
            @RequestParam("mode") String mode) {

        String result = openAIService.analyzeResume(resumeText, role, mode);
        return parseJsonResponse(result);
    }

    // Analyze uploaded resume file
    @PostMapping("/analyze-file")
    public ResponseEntity<Object> analyzeResumeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("role") String role,
            @RequestParam("mode") String mode) {

        try {
            String resumeText = tika.parseToString(file.getInputStream());
            String result = openAIService.analyzeResume(resumeText, role, mode);
            return parseJsonResponse(result);
        } catch (IOException | TikaException e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", "Failed to parse resume file: " + e.getMessage())
            );
        }
    }

    // Improve resume by text
    @PostMapping("/improve")
    public ResponseEntity<Object> improveResume(
            @RequestParam("resumeText") String resumeText,
            @RequestParam("role") String role) {

        String result = openAIService.generateImprovedResume(resumeText, role);
        return parseJsonResponse(result);
    }

    // Compare batch of resumes by text
    @PostMapping("/compare-batch")
    public ResponseEntity<Object> compareBatch(
            @RequestParam("resumes") List<String> resumeTexts,
            @RequestParam("fileNames") List<String> fileNames,
            @RequestParam("role") String role) {

        String result = openAIService.compareResumesInBatch(resumeTexts, fileNames, role);
        return parseJsonResponse(result);
    }

    // Compare batch of resumes with job description
    @PostMapping("/compare-with-jd")
    public ResponseEntity<Object> compareWithJD(
            @RequestParam("resumes") List<String> resumeTexts,
            @RequestParam("fileNames") List<String> fileNames,
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam("email") String email) {

        String result = openAIService.compareResumesInBatchWithJD(resumeTexts, fileNames, jobDescription, email);
        return parseJsonResponse(result);
    }

    // Helper to parse JSON safely
    private ResponseEntity<Object> parseJsonResponse(String result) {
        try {
            Object json = objectMapper.readValue(result, Object.class);
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", "Invalid JSON response: " + e.getMessage())
            );
        }
    }
}
