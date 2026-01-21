package at.technikum_wien.accesslog.service;

import at.technikum_wien.accesslog.model.AccessStat;
import at.technikum_wien.accesslog.repo.AccessStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogImporterServiceTest {

    @Mock
    private AccessStatRepository repository;

    @InjectMocks
    private LogImporterService importerService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Wir injizieren die Pfade manuell, da @Value ohne Spring Context nicht funktioniert
        ReflectionTestUtils.setField(importerService, "inputFolder", tempDir.toString());
        ReflectionTestUtils.setField(importerService, "archiveFolder", tempDir.resolve("archive").toString());
        ReflectionTestUtils.setField(importerService, "filePattern", "*.xml");
    }

    @Test
    void importAccessLogs_ShouldPickUpXmlFile_AndSaveToRepo() throws Exception {
        // 1. Setup: XML-Datei im simulierten Input-Ordner erstellen
        Path xmlFile = tempDir.resolve("test-log.xml");
        String xmlContent = """
            <accessLogs date="2026-01-21">
                <entry documentId="99" count="10" />
            </accessLogs>
            """;
        Files.writeString(xmlFile, xmlContent);

        // 2. Mock: Repository findet noch keinen Eintrag für ID 99 am 2026-01-21
        when(repository.findByDocumentIdAndAccessDate(eq(99), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // 3. Ausführen
        importerService.importAccessLogs();

        // 4. Verifizieren: Wurde save() aufgerufen?
        ArgumentCaptor<AccessStat> statCaptor = ArgumentCaptor.forClass(AccessStat.class);
        verify(repository, times(1)).save(statCaptor.capture());

        AccessStat savedStat = statCaptor.getValue();
        assertThat(savedStat.getDocumentId()).isEqualTo(99);
        assertThat(savedStat.getAccessCount()).isEqualTo(10);
        assertThat(savedStat.getAccessDate()).isEqualTo(LocalDate.of(2026, 1, 21));
    }

    @Test
    void importAccessLogs_ShouldUpdateExistingStat() throws Exception {
        // 1. XML erstellen
        Path xmlFile = tempDir.resolve("update-log.xml");
        Files.writeString(xmlFile, "<accessLogs><entry documentId='1' count='5'/></accessLogs>");

        // 2. Mock: Es gibt schon einen Eintrag mit Count = 20
        AccessStat existingStat = new AccessStat(1, LocalDate.now(), 20);
        when(repository.findByDocumentIdAndAccessDate(eq(1), any(LocalDate.class)))
                .thenReturn(Optional.of(existingStat));

        // 3. Ausführen
        importerService.importAccessLogs();

        // 4. Prüfen: Wurde addiert? (20 + 5 = 25)
        assertThat(existingStat.getAccessCount()).isEqualTo(25);
        verify(repository).save(existingStat);
    }
}