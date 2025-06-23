package com.resumehelp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final ObjectMapper mapper = new ObjectMapper();

    public String analyzeResume(String resumeText, String role, String mode) {
        // Only candidate mode scoring is modified; company mode stays as is
        if ("candidate".equalsIgnoreCase(mode)) {
            return handleCandidate(resumeText, role);
        }
        // Company mode delegates to existing logic
        return handleCompany(resumeText, role, mode);
    }

    private String handleCandidate(String resumeText, String role) {
        // 1. Ask OpenAI for evaluation + score
        ObjectNode payload = mapper.createObjectNode();
        payload.put("resume", resumeText);
        payload.put("role", role);
        payload.put("mode", "candidate");

        String raw = callOpenAI(buildPrompt(payload.toString(), role, "candidate"));
        ObjectNode result = parseAndEnrich(raw, true);

        // 2. Force valid fields if missing
        enforceDefaults(result);

        return result.toString();
    }

    private String handleCompany(String resumeText, String role, String mode) {
        // Fallback to original company mode (responds to batch in controller)
        // not shown here
        return callOpenAI(buildPrompt(resumeText, role, mode));
    }

    private ObjectNode parseAndEnrich(String raw, boolean isCandidate) {
        try {
            ObjectNode root = (ObjectNode) mapper.readTree(raw);

            // If score provided, keep; else try to calculate heuristically
            if (isCandidate) {
                // Example: length-of-strong_points ratio
                double sp = root.has("strong_points") ? root.get("strong_points").size() : 0;
                double wp = root.has("weak_points") ? root.get("weak_points").size() : 1;
                int score = (int) (100 * sp / (sp + wp));
                root.put("score", score);
            }

            return root;
        } catch (Exception e) {
            ObjectNode err = mapper.createObjectNode();
            err.put("status", "error");
            err.put("error", "Invalid JSON");
            return err;
        }
    }

    private void enforceDefaults(ObjectNode root) {
        if (!root.hasNonNull("strong_points") || root.get("strong_points").size() == 0) {
            ArrayNode arr = mapper.createArrayNode();
            arr.add("Has valuable experience, but not directly in this role");
            root.set("strong_points", arr);
        }
        if (!root.hasNonNull("weak_points") || root.get("weak_points").size() == 0) {
            ArrayNode arr = mapper.createArrayNode();
            arr.add("Identify role-specific gaps to improve");
            root.set("weak_points", arr);
        }

        if (root.has("recommendations")) {
            ObjectNode rec = (ObjectNode) root.get("recommendations");
            ensureArray(rec, "online_courses");
            ensureArray(rec, "youtube_channels");
            ensureArray(rec, "career_guides");
            ensureArray(rec, "alternative_roles");
            ensureArray(rec, "skills_to_learn");
        }
    }

    private void ensureArray(ObjectNode parent, String key) {
        if (!parent.hasNonNull(key) || parent.get(key).size() == 0) {
            ArrayNode arr = mapper.createArrayNode();
            arr.add("Explore " + key.replace("_", " "));
            parent.set(key, arr);
        }
    }

    private String buildPrompt(String resumeOrJson, String role, String mode) {
        // Build the OpenAI prompt based on your previously provided logic
        // (omitted here for brevity)
        return "...";
    }

    private String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "model", "gpt-4",
            "messages", List.of(Map.of("role","user","content",prompt))
        );

        HttpEntity<Map<String,Object>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(OPENAI_API_URL, req, Map.class);
            String content = ((Map)((List) resp.getBody().get("choices")).get(0)).get("message").toString();
            int start = content.indexOf("{"), end = content.lastIndexOf("}") + 1;
            return content.substring(start, end);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage()+"\"}";
        }
    }
}
