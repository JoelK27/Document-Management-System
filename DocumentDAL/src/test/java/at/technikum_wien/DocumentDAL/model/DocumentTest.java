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
    void constructor_WithAllParameters_ShouldSetAllFields() {
        Document doc = new Document(1, "Title", "Content", "Summary",
                testDate, "file.pdf", "application/pdf", 1024L);

        assertEquals(1, doc.getId());
        assertEquals("Title", doc.getTitle());
        assertEquals("Content", doc.getContent());
        assertEquals("Summary", doc.getSummary());
        assertEquals(testDate, doc.getUploadDate());
        assertEquals("file.pdf", doc.getFileName());
        assertEquals("application/pdf", doc.getMimeType());
        assertEquals(1024L, doc.getSize());
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyDocument() {
        assertNotNull(document);
        assertEquals(0, document.getId());
        assertNull(document.getTitle());
        assertNull(document.getContent());
        assertNull(document.getSummary());
        assertNull(document.getUploadDate());
        assertNull(document.getFileName());
        assertNull(document.getMimeType());
        assertEquals(0L, document.getSize());
        assertNull(document.getFileData());
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
        document.setFileData("test data".getBytes());

        assertEquals(42, document.getId());
        assertEquals("Test Title", document.getTitle());
        assertEquals("Test Content", document.getContent());
        assertEquals("Test Summary", document.getSummary());
        assertEquals(testDate, document.getUploadDate());
        assertEquals("test.pdf", document.getFileName());
        assertEquals("application/pdf", document.getMimeType());
        assertEquals(2048L, document.getSize());
        assertArrayEquals("test data".getBytes(), document.getFileData());
    }

    @Test
    void fileData_ShouldBeNullByDefault() {
        assertNull(document.getFileData());
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