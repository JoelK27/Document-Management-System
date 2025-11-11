package at.technikum_wien.DocumentDAL.storage;

import io.minio.*;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class MinioFileStorage implements FileStorage {

    private final MinioClient client;
    private final String defaultBucket;

    public MinioFileStorage(MinioClient client, @Value("${minio.bucket}") String defaultBucket) throws Exception {
        this.client = client;
        this.defaultBucket = defaultBucket;
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(defaultBucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(defaultBucket).build());
        }
    }

    @Override
    public void put(String bucket, String key, byte[] data, String contentType) throws Exception {
        String b = bucket != null ? bucket : defaultBucket;
        client.putObject(PutObjectArgs.builder()
                .bucket(b)
                .object(key)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build());
    }

    @Override
    public byte[] get(String bucket, String key) throws Exception {
        String b = bucket != null ? bucket : defaultBucket;
        try (GetObjectResponse res = client.getObject(GetObjectArgs.builder().bucket(b).object(key).build())) {
            return res.readAllBytes();
        }
    }

    @Override
    public void delete(String bucket, String key) throws Exception {
        String b = bucket != null ? bucket : defaultBucket;
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(b).object(key).build());
        } catch (MinioException ignore) {
            // idempotent
        }
    }

    public String getDefaultBucket() { return defaultBucket; }
}