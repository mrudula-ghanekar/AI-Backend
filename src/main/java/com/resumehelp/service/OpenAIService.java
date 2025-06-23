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

    // ✅ Analyze Resume: Candidate & Company Mode
    public String analyzeResume(String resumeText, String role, String mode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an intelligent AI career advisor and resume evaluator.\n");
        prompt.append("Your job is to analyze the candidate's resume in context of the applied role: '").append(role).append("'.\n");

        prompt.append("\n### Instructions:\n");
        prompt.append("- Extract the full name of the candidate (format: FirstName LastName). If missing, use \"Unnamed Candidate\".\n");
        prompt.append("- Assess if the resume is suited for the specified role.\n");
        prompt.append("- Identify strengths (aligned skills, experience, certifications).\n");
        prompt.append("- Identify weaknesses or gaps (missing or unrelated skills).\n");

        if (mode.equalsIgnoreCase("candidate")) {
            prompt.append("- Suggest personalized improvements such as:\n");
            prompt.append("  - Online courses to take\n");
            prompt.append("  - YouTube channels to follow\n");
            prompt.append("  - Career guides or roadmaps to refer\n");
            prompt.append("  - Alternate roles matching existing skills\n");
            prompt.append("  - Specific skills/tools/languages to learn\n");
        } else {
            prompt.append("- Include a comparative score if applicable to company evaluation.\n");
        }

        prompt.append("- Return only a valid JSON object. No explanations or extra text.\n\n");

        prompt.append("### Expected JSON Format:\n");
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
            prompt.append("    \"alternative_roles\": [\"role1\", \"role2\"],\n");
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

    // ✅ Resume Optimizer for Candidates
    public String generateImprovedResume(String resumeText, String role) {
        String prompt = "You are an AI resume optimizer. Your job is to enhance resumes to make them more effective for ATS and recruiter screening.\n\n" +
                "### Instructions:\n" +
                "- Optimize the resume for the role: '" + role + "'.\n" +
                "- Use concise bullet points, action verbs, quantifiable results.\n" +
                "- Improve formatting for clarity and scanning.\n" +
                "- Output only a valid JSON object, no explanation.\n\n" +
                "### Output Format:\n" +
                "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"improved_resume\": \"Full optimized resume content\"\n" +
                "}\n\n" +
                "### Original Resume:\n" + resumeText;

        return callOpenAI(prompt);
    }

    // ✅ Batch Resume Comparison for Company
    public String compareResumesInBatch(List<String> resumeTexts, List<String> fileNames, String role) {
        StringBuilder combinedResumes = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combinedResumes.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI recruiter comparing multiple resumes for the role of '" + role + "'.\n\n" +
                "### Instructions:\n" +
                "- Extract candidate name or fallback to file name.\n" +
                "- Rank resumes based on experience, skills, and fit for the role.\n" +
                "- Output a JSON object sorted by descending score.\n\n" +
                "### JSON Format:\n" +
                "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"ranking\": [\n" +
                "    {\n" +
                "      \"index\": number,\n" +
                "      \"file_name\": \"abc.pdf\",\n" +
                "      \"candidate_name\": \"Extracted or fallback\",\n" +
                "      \"score\": 87,\n" +
                "      \"summary\": \"Short performance summary\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "### Resumes:\n" + combinedResumes;

        return callOpenAI(prompt);
    }

    // ✅ Batch with Job Description for Recruiters
    public String compareResumesInBatchWithJD(List<String> resumeTexts, List<String> fileNames, String jobDescription, String userEmail) {
        StringBuilder combinedResumes = new StringBuilder();
        for (int i = 0; i < resumeTexts.size(); i++) {
            combinedResumes.append("Resume ").append(i + 1)
                    .append(" (File: ").append(fileNames.get(i)).append("):\n")
                    .append(resumeTexts.get(i)).append("\n\n");
        }

        String prompt = "You are an AI hiring assistant analyzing resumes against the following job description:\n\n" +
                "### Job Description:\n" + jobDescription + "\n\n" +
                "### Instructions:\n" +
                "- Score resumes (0–100) based on JD relevance.\n" +
                "- Extract candidate name or fallback to file name.\n" +
                "- Justify scoring in 1 sentence.\n" +
                "- Output only JSON.\n\n" +
                "### JSON Format:\n" +
                "[\n" +
                "  { \"file_name\": \"file1.pdf\", \"candidate_name\": \"John Doe\", \"score\": 85, \"summary\": \"Summary sentence\" }\n" +
                "]\n\n" +
                "### Resumes:\n" + combinedResumes;

        return callOpenAI(prompt);
    }

    // ✅ Common OpenAI API call
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

    // ✅ JSON Extractor
    private String extractJson(String aiResponse) {
        Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(aiResponse);
        return matcher.find() ? matcher.group() : "{\"error\":\"Invalid AI Response\"}";
    }
}
