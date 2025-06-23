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
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI resume expert evaluating a resume for the role of '")
                .append(role).append("'.\n\n")
                .append("### Instructions:\n")
                .append("- Extract the candidate's full name. If not available, return \"Unnamed Candidate\".\n")
                .append("- Assess whether the candidate is suited for the role.\n")
                .append("- Highlight 2–3 strengths based on skills, achievements, or relevance to the role.\n")
                .append("- Suggest 2–3 specific and realistic improvements for the resume.\n");

        if (mode.equalsIgnoreCase("candidate")) {
            prompt.append("- Additionally, provide personalized recommendations to improve their profile including:\n")
                    .append("  - Recommended **online courses** (name + platform)\n")
                    .append("  - Top **YouTube channels** for relevant tutorials\n")
                    .append("  - **Career guides** or roadmaps tailored to the role\n")
                    .append("  - Specific **technical skills** to focus on next (languages, frameworks, tools)\n");
        }

        prompt.append("- Respond with a **single valid JSON object**. Do not include any explanations or extra text.\n\n")
                .append("### JSON Format:\n")
                .append("```json\n{\n")
                .append("  \"status\": \"success\",\n")
                .append("  \"candidate_name\": \"Extracted or fallback name\",\n")
                .append("  \"suited_for_role\": \"Yes\" or \"No\",\n")
                .append("  \"strong_points\": [\"...\", \"...\"],\n")
                .append("  \"improvement_suggestions\": [\"...\", \"...\"]");

        if (mode.equalsIgnoreCase("candidate")) {
            prompt.append(",\n")
                    .append("  \"learning_recommendations\": {\n")
                    .append("    \"online_courses\": [\"Course Name (Platform)\", \"...\"],\n")
                    .append("    \"youtube_channels\": [\"Channel 1\", \"...\"],\n")
                    .append("    \"career_guides\": [\"Guide 1\", \"...\"],\n")
                    .append("    \"tech_skills_to_learn\": [\"Skill 1\", \"...\"]\n")
                    .append("  }");
        }

        prompt.append("\n}\n```")
              .append("\n\n### Resume:\n").append(resumeText);

        return callOpenAI(prompt.toString());
    }

    // ✅ Improve Resume for Candidate
    public String generateImprovedResume(String resumeText, String role) {
        String prompt = "You are an AI resume optimizer. Refine the following resume for the role of '" + role + "'. " +
                "Ensure it's ATS-friendly, concise, and achievement-focused.\n\n" +
                "### Instructions:\n" +
                "- Reword and format the resume using bullet points where necessary.\n" +
                "- Add quantifiable accomplishments if appropriate.\n" +
                "- Do not remove important details.\n" +
                "- Respond ONLY with valid JSON.\n\n" +
                "### JSON Format:\n" +
                "```json\n{\n" +
                "  \"status\": \"success\",\n" +
                "  \"improved_resume\": \"...full improved resume text...\"\n" +
                "}\n```" +
                "\n\n### Original Resume:\n" + resumeText;

        return callOpenAI(prompt);
    }

    // ✅ Batch Compare Resumes for Company Mode
    public String compareResumesInBatch(List<String> resumeTexts, List<String> fileNames, String role) {
        StringBuilder combinedResumes = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combinedResumes.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI hiring expert analyzing multiple resumes for the role of '" + role + "'.\n\n" +
                "### Instructions:\n" +
                "- Extract full name of each candidate (fallback to filename).\n" +
                "- Rank candidates by experience, skills, and relevance to the role.\n" +
                "- Output valid JSON sorted by score descending.\n\n" +
                "### JSON Format:\n" +
                "```json\n{\n" +
                "  \"status\": \"success\",\n" +
                "  \"ranking\": [\n" +
                "    { \"index\": 1, \"file_name\": \"resume1.pdf\", \"candidate_name\": \"...\", \"score\": 87, \"summary\": \"...\" }\n" +
                "  ]\n" +
                "}\n```" +
                "\n\n### Resumes:\n" + combinedResumes;

        return callOpenAI(prompt);
    }

    // ✅ Batch Compare With JD
    public String compareResumesInBatchWithJD(List<String> resumeTexts, List<String> fileNames, String jobDescription, String userEmail) {
        StringBuilder combinedResumes = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combinedResumes.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI talent evaluator for recruiter " + userEmail + ", reviewing resumes against the following job:\n\n" +
                "### Job Description:\n" + jobDescription + "\n\n" +
                "### Instructions:\n" +
                "- Extract name or use file name if not found.\n" +
                "- Score resumes (0–100) based on job match.\n" +
                "- Return valid JSON array sorted by score.\n\n" +
                "### JSON Format:\n" +
                "```json\n[\n" +
                "  { \"file_name\": \"abc.pdf\", \"candidate_name\": \"John Doe\", \"score\": 87, \"summary\": \"Strong React/Node skills\" }\n" +
                "]\n```" +
                "\n\n### Resumes:\n" + combinedResumes;

        return callOpenAI(prompt);
    }

    // ✅ Extract JSON using regex
    private String extractJson(String aiResponse) {
        Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        return matcher.find() ? matcher.group() : "{\"error\":\"Invalid AI Response\"}";
    }

    // ✅ OpenAI API caller
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
}
