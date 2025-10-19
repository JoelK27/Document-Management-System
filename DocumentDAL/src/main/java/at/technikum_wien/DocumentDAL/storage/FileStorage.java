package at.technikum_wien.DocumentDAL.storage;

public interface FileStorage {
    void put(String bucket, String key, byte[] data, String contentType) throws Exception;

    byte[] get(String bucket, String key) throws Exception;

    void delete(String bucket, String key) throws Exception;
}