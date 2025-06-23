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

        prompt.append("You are an intelligent AI career advisor and resume evaluator.\n");
        prompt.append("Analyze the following resume in the context of the role: '").append(role).append("'.\n\n");

        prompt.append("### Instructions:\n")
              .append("- Extract full name (FirstName LastName), or return \"Unnamed Candidate\" if not found.\n")
              .append("- Determine if the resume fits the role.\n")
              .append("- Highlight strong points (skills, experience, certifications).\n")
              .append("- Highlight weak points (missing skills, unrelated experience).\n");

        if ("candidate".equalsIgnoreCase(mode)) {
            prompt.append("- Offer personalized career recommendations:\n")
                  .append("  - Online courses\n")
                  .append("  - YouTube channels\n")
                  .append("  - Career guides\n")
                  .append("  - Alternative roles matching current skills\n")
                  .append("  - Skills/tools to learn\n");
        } else {
            prompt.append("- Include a comparison score (percentile-based).\n")
                  .append("- Give suggestions to improve fit for the role.\n");
        }

        prompt.append("- Output only a valid JSON object. No explanation. No markdown. No headers.\n\n");

        prompt.append("### JSON Format:\n{\n")
              .append("  \"status\": \"success\",\n")
              .append("  \"candidate_name\": \"Extracted Name or Unnamed Candidate\",\n")
              .append("  \"suited_for_role\": \"Yes\" or \"No\",\n")
              .append("  \"strong_points\": [\"point1\", \"point2\"],\n")
              .append("  \"weak_points\": [\"point1\", \"point2\"],\n");

        if ("candidate".equalsIgnoreCase(mode)) {
            prompt.append("  \"recommendations\": {\n")
                  .append("    \"online_courses\": [\"course1\", \"course2\"],\n")
                  .append("    \"youtube_channels\": [\"channel1\", \"channel2\"],\n")
                  .append("    \"career_guides\": [\"guide1\", \"guide2\"],\n")
                  .append("    \"alternative_roles\": [\"role1\", \"role2\"],\n")
                  .append("    \"skills_to_learn\": [\"skill1\", \"skill2\"]\n")
                  .append("  }\n");
        } else {
            prompt.append("  \"comparison_score\": \"This resume ranks XX% better than other applicants.\",\n")
                  .append("  \"improvement_suggestions\": [\"suggestion1\", \"suggestion2\"]\n");
        }

        prompt.append("}\n\n### Resume Content:\n").append(resumeText);

        return callOpenAI(prompt.toString());
    }

    public String generateImprovedResume(String resumeText, String role) {
        String prompt = "You are an AI resume optimizer. Improve the following resume for the role: '" + role + "'.\n\n" +
                "### Instructions:\n" +
                "- Format clearly and include measurable results.\n" +
                "- Improve readability and optimize for ATS.\n" +
                "- Use role-specific keywords and strong bullet points.\n" +
                "- Output only valid JSON. No explanation. No markdown.\n\n" +
                "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"improved_resume\": \"Full optimized resume content\"\n" +
                "}\n\n" +
                "### Original Resume:\n" + resumeText;

        return callOpenAI(prompt);
    }

    public String compareResumesInBatch(List<String> resumeTexts, List<String> fileNames, String role) {
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combined.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI recruiter evaluating candidates for the role: '" + role + "'.\n\n" +
                "### Instructions:\n" +
                "- Extract name or fallback to file name.\n" +
                "- Score (0–100) based on relevance and experience.\n" +
                "- Sort in descending order of score.\n" +
                "- Output only valid JSON. No explanation. No markdown.\n\n" +
                "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"ranking\": [\n" +
                "    {\n" +
                "      \"index\": 0,\n" +
                "      \"file_name\": \"resume1.pdf\",\n" +
                "      \"candidate_name\": \"John Doe\",\n" +
                "      \"score\": 87,\n" +
                "      \"summary\": \"Relevant backend experience and Spring Boot expertise.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n### Resumes:\n" + combined;

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
                + jobDescription + "\n\n" +
                "### Instructions:\n" +
                "- Score each resume (0–100) based on relevance.\n" +
                "- Extract full name or fallback to file name.\n" +
                "- Justify score with a one-line summary.\n" +
                "- Return only a JSON array sorted by score descending.\n" +
                "- Output strictly valid JSON array. No markdown. No explanation.\n\n" +
                "[\n" +
                "  {\n" +
                "    \"file_name\": \"resume1.pdf\",\n" +
                "    \"candidate_name\": \"Jane Doe\",\n" +
                "    \"score\": 91,\n" +
                "    \"summary\": \"Excellent match: strong Java, REST, Spring experience.\"\n" +
                "  }\n" +
                "]\n\n### Resumes:\n" + combined;

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

        // ❌ REMOVED invalid: body.put("response_format", "json");

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
