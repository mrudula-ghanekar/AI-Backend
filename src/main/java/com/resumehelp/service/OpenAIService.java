package com.resumehelp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.regex.*;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // ✅ Analyze Resume for Candidate/Company Mode
    public String analyzeResume(String resumeText, String role, String mode) {
        String prompt = "You are an AI resume analyzer. Your task is to evaluate a given resume for the role of '" + role + "'. " +
                "\n\n### Instructions:" +
                "\n- Extract the **full name** of the candidate from the resume." +
                "\n- Return **ONLY** a valid JSON object (**no explanations, no extra text**)." +
                "\n- Ensure JSON **strictly follows** this format:" +
                "\n```json\n{" +
                "\"status\": \"success\"," +
                "\"candidate_name\": \"Full Name\"," +
                "\"suited_for_role\": \"Yes or No\"," +
                "\"strong_points\": [\"Bullet Point 1\", \"Bullet Point 2\"]," +
                (mode.equalsIgnoreCase("company") ? "\"comparison_score\": \"This resume ranks XX% better than other applicants.\"," : "") +
                "\"improvement_suggestions\": [\"Bullet Point Suggestion 1\", \"Bullet Point Suggestion 2\"]" +
                "}" +
                "\n```" +
                "\n\n**Resume:**\n" + resumeText;

        return callOpenAI(prompt);
    }

    // ✅ Improve Resume for Candidate
    public String generateImprovedResume(String resumeText, String role) {
        String prompt = "You are an AI resume optimizer. Your job is to refine and enhance resumes for ATS and recruiter screening. " +
                "\n\n### Instructions:" +
                "\n- Improve the given resume for the role of '" + role + "'." +
                "\n- Ensure **concise bullet points, measurable achievements, and ATS-friendly formatting**." +
                "\n- Return **ONLY** valid JSON (**no explanations, no extra text**)." +
                "\n\n### Expected JSON Response:" +
                "\n```json\n{" +
                "\"status\": \"success\"," +
                "\"improved_resume\": \"Updated resume text with optimizations.\"" +
                "}\n```" +
                "\n\n**Resume:**\n" + resumeText;

        return callOpenAI(prompt);
    }

    // ✅ Batch Resume Comparison - Extracts candidate names & ranks properly
    public String compareResumesInBatch(List<String> resumeTexts, List<String> fileNames, String role) {
        StringBuilder combinedResumes = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combinedResumes.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI hiring expert analyzing multiple resumes for the role of '" + role + "'. " +
                "\n\n### Instructions:" +
                "\n- Extract the **full name** of each candidate from the resumes." +
                "\n- Include the **file name** alongside each candidate." +
                "\n- Compare and rank the resumes based on **experience, skills, and role fit**." +
                "\n- Return **ONLY JSON** (**no explanations, no extra text**)." +
                "\n- Ensure ranking is **sorted in descending order** based on the score." +
                "\n\n### Expected JSON Response:" +
                "\n```json\n{" +
                "\"status\": \"success\"," +
                "\"ranking\": [" +
                "{ \"index\": number, \"file_name\": \"original_file_name.pdf\", \"candidate_name\": \"Extracted Name\", \"score\": number, \"summary\": \"Candidate Name (File Name) - Brief analysis of this resume\" }" +
                "]}" +
                "```" +
                "\n\n**Resumes:**\n" + combinedResumes;

        return callOpenAI(prompt);
    }

    // ✅ Extract valid JSON from AI response using regex
    private String extractJson(String aiResponse) {
        Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        return matcher.find() ? matcher.group() : "{\"error\":\"Invalid AI Response\"}";
    }

    // ✅ Common method to call OpenAI API
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
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String aiResponse = message.get("content").toString().trim();

            System.out.println("🧠 AI Raw Response: " + aiResponse);

            return extractJson(aiResponse); // ✅ Extract JSON safely
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"API Error: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
