package at.technikum_wien.ocrworker.client;

import at.technikum_wien.ocrworker.exceptions.GenAiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class GenAIClient {
    private static final Logger log = LoggerFactory.getLogger(GenAIClient.class);
    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String baseEndpoint;
    private final String model;
    private final int maxRetries;
    private final long timeoutMillis;

    public GenAIClient(
            @Value("${GOOGLE_API_KEY:}") String apiKey,
            @Value("${GENAI_ENDPOINT:https://generativelanguage.googleapis.com}") String baseEndpoint,
            @Value("${GENAI_MODEL:gemini-2.0-flash}") String model,
            @Value("${GENAI_MAX_RETRIES:3}") int maxRetries,
            @Value("${GENAI_TIMEOUT:15000}") long timeoutMillis
    ) {
        this.apiKey = apiKey;
        this.baseEndpoint = baseEndpoint.endsWith("/") ? baseEndpoint.substring(0, baseEndpoint.length()-1) : baseEndpoint;
        this.model = model;
        this.maxRetries = maxRetries;
        this.timeoutMillis = timeoutMillis;
        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofMillis(timeoutMillis))
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofMillis(timeoutMillis))
                .build();
    }

    public String summarize(String text) throws GenAiException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GenAiException("Missing GOOGLE_API_KEY");
        }
        if (text == null) text = "";
        String prompt = "Erstelle eine prägnante Zusammenfassung des folgenden Dokuments in deutscher Sprache. "
                + "Maximal 5 Sätze, keine Einleitung, keine Wiederholung:\n\n" + text;

        // Gemini generateContent Body
        Map<String,Object> bodyMap = Map.of(
                "contents", new Object[] {
                        Map.of("role","user",
                                "parts", new Object[] {
                                        Map.of("text", prompt)
                                })
                }
        );

        String url = baseEndpoint + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                RequestBody rb = RequestBody.create(
                        mapper.writeValueAsBytes(bodyMap),
                        MediaType.get("application/json; charset=utf-8")
                );
                Request req = new Request.Builder()
                        .url(url)
                        .post(rb)
                        // Alternative wäre Header x-goog-api-key; Query reicht
                        .build();

                try (Response res = http.newCall(req).execute()) {
                    int code = res.code();
                    String resp = res.body() != null ? res.body().string() : "";
                    if (res.isSuccessful()) {
                        return extractGeminiText(resp);
                    }

                    if (code == 429 && attempt < maxRetries) {
                        long wait = 500L * attempt;
                        log.warn("Gemini 429 retry in {}ms attempt={}/{}", wait, attempt, maxRetries);
                        Thread.sleep(wait);
                        continue;
                    }
                    if (code >= 500 && attempt < maxRetries) {
                        long wait = 400L * attempt;
                        log.warn("Gemini {} retry in {}ms attempt={}/{}", code, wait, attempt, maxRetries);
                        Thread.sleep(wait);
                        continue;
                    }
                    throw new GenAiException("GenAI API error " + code + " body=" + resp);
                }
            } catch (GenAiException e) {
                throw e;
            } catch (Exception ex) {
                if (attempt >= maxRetries) throw new GenAiException("GenAI client failed: " + ex.getMessage(), ex);
                try { TimeUnit.MILLISECONDS.sleep(300L * attempt); } catch (InterruptedException ignored) {}
            }
        }
    }

    private String extractGeminiText(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            // response.candidates[0].content.parts[*].text
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content").path("parts");
                if (content.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : content) {
                        String t = part.path("text").asText("");
                        if (!t.isBlank()) sb.append(t);
                    }
                    if (sb.length() > 0) return sb.toString().trim();
                }
            }
            return json;
        } catch (Exception e) {
            return json;
        }
    }
}