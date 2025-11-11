package at.technikum_wien.DocumentDAL.repo;

import at.technikum_wien.DocumentDAL.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("docker-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class DocumentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DocumentRepository documentRepository;

    private Document testDocument1;
    private Document testDocument2;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        entityManager.flush();

        testDocument1 = new Document();
        testDocument1.setTitle("Spring Boot Guide");
        testDocument1.setSummary("A comprehensive guide to Spring Boot");
        testDocument1.setContent("This document covers all aspects of Spring Boot development");
        testDocument1.setFileName("spring-boot-guide.pdf");
        testDocument1.setMimeType("application/pdf");
        testDocument1.setSize(1024L);
        testDocument1.setUploadDate(LocalDateTime.now());

        testDocument2 = new Document();
        testDocument2.setTitle("Java Basics");
        testDocument2.setSummary("Introduction to Java programming");
        testDocument2.setContent("Basic concepts of Java programming language");
        testDocument2.setFileName("java-basics.txt");
        testDocument2.setMimeType("text/plain");
        testDocument2.setSize(512L);
        testDocument2.setUploadDate(LocalDateTime.now());

        entityManager.persistAndFlush(testDocument1);
        entityManager.persistAndFlush(testDocument2);
    }

    @Test
    void findById_ShouldReturnDocument() {
        Optional<Document> found = documentRepository.findById(testDocument1.getId());

        assertTrue(found.isPresent());
        Document document = found.get();
        assertEquals("Spring Boot Guide", document.getTitle());
        assertEquals("spring-boot-guide.pdf", document.getFileName());
    }

    @Test
    void findAll_ShouldReturnAllDocuments() {
        List<Document> documents = documentRepository.findAll();

        assertEquals(2, documents.size());
        assertTrue(documents.stream().anyMatch(d -> d.getTitle().equals("Spring Boot Guide")));
        assertTrue(documents.stream().anyMatch(d -> d.getTitle().equals("Java Basics")));
    }

    @Test
    void findAllWithoutFileData_ShouldReturnDocuments() {
        List<Document> documents = documentRepository.findAllWithoutFileData();

        assertEquals(2, documents.size());
        Document document = documents.stream()
                .filter(d -> d.getTitle().equals("Spring Boot Guide"))
                .findFirst()
                .orElse(null);

        assertNotNull(document);
        assertEquals("Spring Boot Guide", document.getTitle());
        assertEquals("spring-boot-guide.pdf", document.getFileName());
    }

    @Test
    void searchWithoutFileData_WithTitleMatch_ShouldReturnMatchingDocuments() {
        List<Document> results = documentRepository.searchWithoutFileData("Spring");

        assertEquals(1, results.size());
        Document document = results.get(0);
        assertEquals("Spring Boot Guide", document.getTitle());
    }

    @Test
    void searchWithoutFileData_WithSummaryMatch_ShouldReturnMatchingDocuments() {
        List<Document> results = documentRepository.searchWithoutFileData("Java programming");

        assertEquals(1, results.size());
        Document document = results.get(0);
        assertEquals("Java Basics", document.getTitle());
    }

    @Test
    void searchWithoutFileData_WithContentMatch_ShouldReturnMatchingDocuments() {
        List<Document> results = documentRepository.searchWithoutFileData("development");

        assertEquals(1, results.size());
        assertEquals("Spring Boot Guide", results.get(0).getTitle());
    }

    @Test
    void searchWithoutFileData_WithNoMatch_ShouldReturnEmptyList() {
        List<Document> results = documentRepository.searchWithoutFileData("NonExistentTerm");

        assertTrue(results.isEmpty());
    }

    @Test
    void searchWithoutFileData_WithPartialMatch_ShouldReturnMatchingDocuments() {
        List<Document> results = documentRepository.searchWithoutFileData("Boot");

        assertEquals(1, results.size());
        assertEquals("Spring Boot Guide", results.get(0).getTitle());
    }

    @Test
    void searchWithoutFileData_CaseInsensitive_ShouldReturnMatchingDocuments() {
        List<Document> results = documentRepository.searchWithoutFileData("spring");

        assertEquals(1, results.size());
        assertEquals("Spring Boot Guide", results.get(0).getTitle());
    }

    @Test
    void save_ShouldPersistNewDocument() {
        Document newDocument = new Document();
        newDocument.setTitle("New Document");
        newDocument.setSummary("New Summary");
        newDocument.setFileName("new.pdf");
        newDocument.setMimeType("application/pdf");
        newDocument.setSize(256L);
        newDocument.setUploadDate(LocalDateTime.now());

        Document saved = documentRepository.save(newDocument);

        assertNotNull(saved.getId());
        assertEquals("New Document", saved.getTitle());

        Optional<Document> found = documentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("New Document", found.get().getTitle());
    }

    @Test
    void deleteById_ShouldRemoveDocument() {
        int documentId = testDocument1.getId();
        assertTrue(documentRepository.existsById(documentId));

        documentRepository.deleteById(documentId);

        assertFalse(documentRepository.existsById(documentId));
        Optional<Document> found = documentRepository.findById(documentId);
        assertFalse(found.isPresent());
    }

    @Test
    void existsById_ShouldReturnTrueForExistingDocument() {
        assertTrue(documentRepository.existsById(testDocument1.getId()));
    }

    @Test
    void existsById_ShouldReturnFalseForNonExistingDocument() {
        assertFalse(documentRepository.existsById(99999));
    }
}