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
        String prompt = "You are an AI resume analyzer. Analyze if this resume fits the role of '" + role + "'. "
                + "Return ONLY a valid JSON object without any extra text in this exact format: "
                + "{"
                + "\"suited_for_role\": \"Yes or No\"," 
                + "\"strong_points\": [\"Point 1\", \"Point 2\"],"
                + (mode.equalsIgnoreCase("company") ? "\"comparison_score\": \"This resume is XX% better compared to others.\"," : "")
                + "\"improvement_suggestions\": [\"Suggestion 1\", \"Suggestion 2\"]"
                + "}";

        return callOpenAI(prompt);
    }

    // ✅ Improve Resume for Candidate
    public String generateImprovedResume(String resumeText, String role) {
        String prompt = "You are a professional AI resume editor. Improve this resume for the role '" + role + "'. "
                + "Return ONLY a structured JSON response: {\"improved_resume\": \"Updated Resume Text\"}"
                + "\n\nResume:\n" + resumeText;

        return callOpenAI(prompt);
    }

    // ✅ Batch Resume Comparison (Now with structured ranking response)
    public String compareResumesInBatch(List<String> resumeTexts, List<String> fileNames, String role) {
        StringBuilder combinedResumes = new StringBuilder();

        for (int i = 0; i < resumeTexts.size(); i++) {
            combinedResumes.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI hiring expert. Analyze and rank the following resumes for the role of '" + role + "'. "
                + "Return ONLY valid JSON in this format: "
                + "{"
                + "\"best_resume_index\": number, "
                + "\"best_resume_summary\": \"string\", "
                + "\"ranking\": [ "
                + "{ \"index\": number, \"file_name\": \"original_file_name.pdf\", \"score\": number, \"summary\": \"summary of resume\" }"
                + "]"
                + "}";

        return callOpenAI(prompt);
    }

    // ✅ Common method to call OpenAI API
    private String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4");
        body.put("messages", List.of(Map.of("role", "system", "content", prompt)));
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, request, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String aiResponse = message.get("content").toString().trim();

            // ✅ Debug print to check raw AI response in logs
            System.out.println("🧠 AI Raw Response: " + aiResponse);

            // ✅ Clean and extract only JSON
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
}
