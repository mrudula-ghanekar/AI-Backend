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

    public String analyzeResume(String resumeText, String role, String mode) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an honest and intelligent AI career advisor and resume evaluator.\n");
        prompt.append("Strictly analyze the resume below ONLY for the role: '").append(role).append("'.\n\n");

        prompt.append("### TASK:\n")
              .append("1. Strictly compare required skills for the role vs skills in the resume.\n")
              .append("2. If resume is a close or perfect match, return \"suited_for_role\": \"Yes\". Else, return \"No\".\n")
              .append("3. Extract candidate name. If not available, return \"Unnamed Candidate\".\n")
              .append("4. Provide:\n")
              .append("   - strong_points: only the skills/projects/tools that are relevant to the role\n")
              .append("   - weak_points: missing or misaligned areas for the role\n");

        if ("candidate".equalsIgnoreCase(mode)) {
            prompt.append("5. If the resume is NOT suited for the role, suggest alternative roles where the candidate’s skills are a better fit.\n")
                  .append("6. Provide recommendations:\n")
                  .append("   - online_courses\n")
                  .append("   - youtube_channels\n")
                  .append("   - career_guides\n")
                  .append("   - alternative_roles\n")
                  .append("   - skills_to_learn\n");
        } else {
            prompt.append("5. Provide a percentile-based comparison score.\n")
                  .append("6. Give improvement suggestions.\n");
        }

        prompt.append("\n⚠️ Output must ONLY be valid JSON with this format:\n\n")
              .append("{\n")
              .append("  \"status\": \"success\",\n")
              .append("  \"candidate_name\": \"Extracted Name or Unnamed Candidate\",\n")
              .append("  \"suited_for_role\": \"Yes\" or \"No\",\n")
              .append("  \"strong_points\": [\"...\"],\n")
              .append("  \"weak_points\": [\"...\"],\n");

        if ("candidate".equalsIgnoreCase(mode)) {
            prompt.append("  \"improvement_suggestions\": [\"...\"],\n")
                  .append("  \"recommendations\": {\n")
                  .append("    \"online_courses\": [\"...\"],\n")
                  .append("    \"youtube_channels\": [\"...\"],\n")
                  .append("    \"career_guides\": [\"...\"],\n")
                  .append("    \"alternative_roles\": [\"...\"],\n")
                  .append("    \"skills_to_learn\": [\"...\"]\n")
                  .append("  }\n");
        } else {
            prompt.append("  \"comparison_score\": \"Ranks higher than XX% of applicants\",\n")
                  .append("  \"improvement_suggestions\": [\"...\"]\n");
        }

        prompt.append("}\n\n### Resume:\n").append(resumeText);

        return callOpenAI(prompt.toString());
    }

    public String generateImprovedResume(String resumeText, String role) {
        String prompt = "You are an AI resume optimizer. Improve the following resume for the role: '" + role + "'.\n\n" +
                "- Format it clearly with bullet points.\n" +
                "- Improve readability and ATS score.\n" +
                "- Use role-specific keywords.\n\n" +
                "Return only valid JSON:\n" +
                "{ \"status\": \"success\", \"improved_resume\": \"...\" }\n\n" +
                "### Resume:\n" + resumeText;

        return callOpenAI(prompt);
    }

    public String compareResumesInBatch(List<String> resumeTexts, List<String> fileNames, String role) {
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combined.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI recruiter evaluating candidates for the role: '" + role + "'.\n" +
                "- Extract name or fallback to file name.\n" +
                "- Score (0–100) based on relevance and experience.\n" +
                "- Output only valid JSON:\n" +
                "{ \"status\": \"success\", \"ranking\": [ { \"index\": 0, \"file_name\": \"...\", \"candidate_name\": \"...\", \"score\": 87, \"summary\": \"...\" } ] }\n\n"
                + "### Resumes:\n" + combined;

        return callOpenAI(prompt);
    }

    public String compareResumesInBatchWithJD(List<String> resumeTexts, List<String> fileNames, String jobDescription, String userEmail) {
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combined.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI recruiter comparing the following resumes against this job description:\n\n"
                + jobDescription + "\n\n"
                + "- Score each resume (0–100) based on match.\n"
                + "- Output sorted JSON array:\n"
                + "[ { \"file_name\": \"...\", \"candidate_name\": \"...\", \"score\": 91, \"summary\": \"...\" } ]\n\n"
                + "### Resumes:\n" + combined;

        return callOpenAI(prompt);
    }

    private String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4");
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, request, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");

            if (choices == null || choices.isEmpty()) {
                return "{\"error\":\"Empty response from OpenAI\"}";
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String aiResponse = String.valueOf(message.get("content")).trim();

            System.out.println("🧠 Raw AI Response:\n" + aiResponse);
            return extractJson(aiResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"API Error: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String extractJson(String aiResponse) {
        aiResponse = aiResponse.trim();

        int arrayStart = aiResponse.indexOf('[');
        int arrayEnd = aiResponse.lastIndexOf(']');
        if (arrayStart != -1 && arrayEnd != -1 && arrayEnd > arrayStart) {
            String json = aiResponse.substring(arrayStart, arrayEnd + 1);
            System.out.println("📥 Extracted JSON Array:\n" + json);
            return json;
        }

        int objStart = aiResponse.indexOf('{');
        int objEnd = aiResponse.lastIndexOf('}');
        if (objStart != -1 && objEnd != -1 && objEnd > objStart) {
            String json = aiResponse.substring(objStart, objEnd + 1);
            System.out.println("📥 Extracted JSON Object:\n" + json);
            return json;
        }

        System.out.println("❌ Couldn't extract JSON. Raw response:\n" + aiResponse);
        return "{\"error\":\"Invalid AI JSON structure\"}";
    }
}
