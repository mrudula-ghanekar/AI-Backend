package com.resumehelp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // ✅ Analyze Resume for Candidate/Company Mode
    public String analyzeResume(String resumeText, String role, String mode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        String prompt = "You are an AI resume analyzer. Strictly analyze if this resume fits the role of '" + role + "'. " +
                "Return ONLY this JSON: {\n  \"suited_for_role\": \"Yes or No\",\n  \"strong_points\": [...],\n  \"weak_points\": [...],\n  \"improvement_suggestions\": [...]" +
                (mode.equalsIgnoreCase("company") ? ",\n  \"comparison_score\": \"Compared to 10 resumes, this resume is XX% better.\"" : "") +
                "\n}. No other text!\nResume:\n" + resumeText;

        Map<String, Object> systemMessage = Map.of("role", "system", "content", "You are a helpful AI resume analyzer.");
        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("max_tokens", 1000);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, request, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String aiResponse = message.get("content").toString().trim();

            int jsonStart = aiResponse.indexOf('{');
            int jsonEnd = aiResponse.lastIndexOf('}');
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                return aiResponse.substring(jsonStart, jsonEnd + 1);
            }
            return "{\"error\":\"Invalid AI Response\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"API Error: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ✅ Improve Resume for Candidate
    public String generateImprovedResume(String resumeText, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        String prompt = "You are a professional AI resume editor. Improve this resume for the role '" + role + "'. Return ONLY plain text improved resume:\n\n" + resumeText;

        Map<String, Object> systemMessage = Map.of("role", "system", "content", "You are an expert resume editor.");
        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("max_tokens", 1000);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, request, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return message.get("content").toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error generating improved resume: " + e.getMessage();
        }
    }

    // ✅ Batch Resume Comparison
    public String compareResumesInBatch(List<String> resumeTexts, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        StringBuilder combinedResumes = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combinedResumes.append("Resume ").append(i + 1).append(":\n").append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an expert hiring AI. Analyze the following resumes for the role of '" + role + "'. " +
                "Rank them and return this JSON: {\n  \"best_resume_index\": number,\n  \"best_resume_summary\": string,\n  \"ranking\": [ { \"index\": number, \"score\": number, \"summary\": string } ]\n}. No other text.\n\n" + combinedResumes.toString();

        Map<String, Object> systemMessage = Map.of("role", "system", "content", "You are an AI resume ranking expert.");
        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("max_tokens", 1500);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, request, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return message.get("content").toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error during batch comparison: " + e.getMessage();
        }
    }
}
