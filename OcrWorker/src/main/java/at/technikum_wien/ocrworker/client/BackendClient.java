package at.technikum_wien.ocrworker.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BackendClient {

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper om;
    private final String baseUrl;

    public BackendClient(ObjectMapper om,
                         @Value("${BACKEND_BASE_URL:http://backend:8080/api}") String baseUrl) {
        this.om = om;
        this.baseUrl = baseUrl;
    }

    public DocumentDto getDocument(int id) throws Exception {
        Request req = new Request.Builder().url(baseUrl + "/documents/" + id).get().build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IllegalStateException("GET " + res.code());
            return om.readValue(res.body().bytes(), DocumentDto.class);
        }
    }

    public void updateDocument(DocumentDto doc) throws Exception {
        byte[] body = om.writeValueAsBytes(doc);
        Request req = new Request.Builder()
                .url(baseUrl + "/documents/" + doc.id)
                .put(RequestBody.create(body, MediaType.parse("application/json")))
                .build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IllegalStateException("PUT " + res.code() + " " + res.message());
        }
    }

    public void updateContent(int id, String content) throws Exception {
        DocumentDto current = getDocument(id);
        current.content = content;
        updateDocument(current);
    }

    public static class DocumentDto {
        public Integer id;
        public String title;
        public String content;
        public String summary;
        public String uploadDate;
        public String fileName;
        public String mimeType;
        public Long size;
        public String storageBucket;
        public String storageKey;
    }
}