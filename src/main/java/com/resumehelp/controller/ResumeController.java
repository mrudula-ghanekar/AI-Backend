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
@RequestMapping("/api")
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
                resumeTexts.add(new String(file.getBytes(), StandardCharsets.UTF_8));
                fileNames.add(file.getOriginalFilename());
            }

            if (files != null) {
                for (MultipartFile multiFile : files) {
                    resumeTexts.add(new String(multiFile.getBytes(), StandardCharsets.UTF_8));
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
}
