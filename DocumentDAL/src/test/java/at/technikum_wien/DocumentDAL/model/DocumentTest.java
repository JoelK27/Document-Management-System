package at.technikum_wien.DocumentDAL.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    private Document document;
    private LocalDateTime testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDateTime.now();
        document = new Document();
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        document.setId(42);
        document.setTitle("Test Title");
        document.setContent("Test Content");
        document.setSummary("Test Summary");
        document.setUploadDate(testDate);
        document.setFileName("test.pdf");
        document.setMimeType("application/pdf");
        document.setSize(2048L);
        document.setStorageBucket("documents");
        document.setStorageKey("uuid-test.pdf");

        assertEquals(42, document.getId());
        assertEquals("Test Title", document.getTitle());
        assertEquals("Test Content", document.getContent());
        assertEquals("Test Summary", document.getSummary());
        assertEquals(testDate, document.getUploadDate());
        assertEquals("test.pdf", document.getFileName());
        assertEquals("application/pdf", document.getMimeType());
        assertEquals(2048L, document.getSize());
        assertEquals("documents", document.getStorageBucket());
        assertEquals("uuid-test.pdf", document.getStorageKey());
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyDocument() {
        assertNotNull(document);
        assertEquals(null, document.getId());
        assertNull(document.getTitle());
        assertNull(document.getContent());
        assertNull(document.getSummary());
        assertNull(document.getUploadDate());
        assertNull(document.getFileName());
        assertNull(document.getMimeType());
        assertEquals(0L, document.getSize());
        assertNull(document.getStorageBucket());
        assertNull(document.getStorageKey());
    }

    @Test
    void uploadDate_CanBeNull() {
        document.setUploadDate(null);
        assertNull(document.getUploadDate());
    }

    @Test
    void size_CanBeZero() {
        document.setSize(0L);
        assertEquals(0L, document.getSize());
    }

    @Test
    void fileFields_CanBeEmptyStrings() {
        document.setFileName("");
        document.setMimeType("");
        document.setTitle("");
        document.setContent("");
        document.setSummary("");

        assertEquals("", document.getFileName());
        assertEquals("", document.getMimeType());
        assertEquals("", document.getTitle());
        assertEquals("", document.getContent());
        assertEquals("", document.getSummary());
    }
}