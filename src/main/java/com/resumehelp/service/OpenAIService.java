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

    public String analyzeResume(String resumeText, String role, String mode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an intelligent AI career advisor and resume evaluator.\n");
        prompt.append("Analyze the following resume in the context of the role: '").append(role).append("'.\n");

        prompt.append("\n### Instructions:\n");
        prompt.append("- Extract full name (FirstName LastName), or return \"Unnamed Candidate\" if not found.\n");
        prompt.append("- Determine if the resume fits the role.\n");
        prompt.append("- Highlight strong points: matching skills, relevant experience, certifications.\n");
        prompt.append("- Highlight weak points: irrelevant experience, missing key skills.\n");

        if (mode.equalsIgnoreCase("candidate")) {
            prompt.append("- Offer career suggestions based on the resume content, such as:\n");
            prompt.append("  - Online courses\n");
            prompt.append("  - YouTube channels\n");
            prompt.append("  - Career guides or roadmaps\n");
            prompt.append("  - Roles that match current skills (e.g., 'You have skills X and Y, consider applying for Z instead of ").append(role).append("')\n");
            prompt.append("  - Specific tools or languages to learn\n");
        } else {
            prompt.append("- Include a comparison score to evaluate this candidate against others.\n");
            prompt.append("- Give improvement suggestions to better fit the role.\n");
        }

        prompt.append("- Output only a valid JSON object.\n\n");

        prompt.append("### JSON Format:\n");
        prompt.append("{\n");
        prompt.append("  \"status\": \"success\",\n");
        prompt.append("  \"candidate_name\": \"Extracted Name or Unnamed Candidate\",\n");
        prompt.append("  \"suited_for_role\": \"Yes\" or \"No\",\n");
        prompt.append("  \"strong_points\": [\"point1\", \"point2\"],\n");
        prompt.append("  \"weak_points\": [\"point1\", \"point2\"],\n");

        if (mode.equalsIgnoreCase("candidate")) {
            prompt.append("  \"recommendations\": {\n");
            prompt.append("    \"online_courses\": [\"course1\", \"course2\"],\n");
            prompt.append("    \"youtube_channels\": [\"channel1\", \"channel2\"],\n");
            prompt.append("    \"career_guides\": [\"guide1\", \"guide2\"],\n");
            prompt.append("    \"alternative_roles\": [\"role suggestions\"],\n");
            prompt.append("    \"skills_to_learn\": [\"skill1\", \"skill2\"]\n");
            prompt.append("  }\n");
        } else {
            prompt.append("  \"comparison_score\": \"This resume ranks XX% better than other applicants.\",\n");
            prompt.append("  \"improvement_suggestions\": [\"suggestion1\", \"suggestion2\"]\n");
        }

        prompt.append("}\n\n");
        prompt.append("### Resume Content:\n").append(resumeText);

        return callOpenAI(prompt.toString());
    }

    public String generateImprovedResume(String resumeText, String role) {
        String prompt = "You are an AI resume optimizer. Enhance this resume for the role: '" + role + "'.\n\n" +
                "### Instructions:\n" +
                "- Use clear formatting and quantifiable results.\n" +
                "- Optimize with strong bullet points, keywords, and readability.\n" +
                "- Output JSON only.\n\n" +
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

        String prompt = "You are an AI recruiter evaluating resumes for the role '" + role + "'.\n\n" +
                "### Instructions:\n" +
                "- Extract name or fallback to file name.\n" +
                "- Score (0–100) based on skills/experience match.\n" +
                "- Sort by highest to lowest score.\n\n" +
                "### Output:\n" +
                "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"ranking\": [\n" +
                "    {\n" +
                "      \"index\": number,\n" +
                "      \"file_name\": \"resume1.pdf\",\n" +
                "      \"candidate_name\": \"Name or fallback\",\n" +
                "      \"score\": 92,\n" +
                "      \"summary\": \"Short summary\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "### Resumes:\n" + combined;

        return callOpenAI(prompt);
    }

    public String compareResumesInBatchWithJD(List<String> resumeTexts, List<String> fileNames, String jobDescription, String userEmail) {
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combined.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI recruiter comparing resumes to this JD:\n" +
                jobDescription + "\n\n" +
                "### Instructions:\n" +
                "- Score each resume (0–100) based on JD match.\n" +
                "- Extract name or fallback to file name.\n" +
                "- Justify with a one-line summary.\n" +
                "- Return JSON sorted ASCENDING by score (best to worst).\n\n" +
                "[\n" +
                "  {\n" +
                "    \"file_name\": \"resume.pdf\",\n" +
                "    \"candidate_name\": \"Extracted Name\",\n" +
                "    \"score\": 92,\n" +
                "    \"summary\": \"Strong match in skills A and B.\"\n" +
                "  }\n" +
                "]\n\n" +
                "### Resumes:\n" + combined;

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
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String aiResponse = message.get("content").toString().trim();

            System.out.println("🧠 AI Raw Response: " + aiResponse);
            return extractJson(aiResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"API Error: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String extractJson(String aiResponse) {
        Pattern pattern = Pattern.compile("\\{.*\\}|\\[.*\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        return matcher.find() ? matcher.group() : "{\"error\":\"Invalid AI Response\"}";
    }
}
