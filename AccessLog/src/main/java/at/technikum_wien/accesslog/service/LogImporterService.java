package at.technikum_wien.accesslog.service;

import at.technikum_wien.accesslog.model.AccessStat;
import at.technikum_wien.accesslog.model.AccessLogsXml;
import at.technikum_wien.accesslog.repo.AccessStatRepository;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogImporterService {

    private final AccessStatRepository repository;
    private final XmlMapper xmlMapper = new XmlMapper();

    @Value("${import.folder.input:/data/input}")
    private String inputFolder;

    @Value("${import.folder.archive:/data/archive}")
    private String archiveFolder;

    @Value("${import.file.pattern:*.xml}")
    private String filePattern;

    // Läuft täglich um 01:00 Uhr
    @Scheduled(cron = "${import.schedule.cron:0 0 1 * * *}")
    public void importAccessLogs() {
        log.info("Starting batch import from {}", inputFolder);

        try (Stream<Path> paths = Files.list(Paths.get(inputFolder))) {
            paths.filter(p -> p.toString().endsWith(".xml")) // Einfacher Pattern-Check
                    .forEach(this::processFile);
        } catch (IOException e) {
            log.error("Error accessing input folder", e);
        }
    }

    private void processFile(Path path) {
        log.info("Found file: {}", path); // Logge den vollen Pfad
        try {
            AccessLogsXml logs = xmlMapper.readValue(path.toFile(), AccessLogsXml.class);
            log.info("XML parsed successfully. Date: {}, Entries: {}", logs.getDate(), logs.getEntries() != null ? logs.getEntries().size() : 0);

            LocalDate date = (logs.getDate() != null)
                    ? LocalDate.parse(logs.getDate())
                    : LocalDate.now();

            for (AccessLogsXml.LogEntry entry : logs.getEntries()) {
                log.info("Saving stat: DocID={}, Count={}", entry.getDocumentId(), entry.getCount()); // Debug Log

                AccessStat stat = repository.findByDocumentIdAndAccessDate(entry.getDocumentId(), date)
                        .orElse(new AccessStat(entry.getDocumentId(), date, 0));

                stat.setAccessCount(stat.getAccessCount() + entry.getCount());
                repository.save(stat);
            }
            log.info("Database update finished.");

            // Archivieren NUR bei Erfolg verschieben
            Path archivePath = Paths.get(archiveFolder);
            if (!Files.exists(archivePath)) Files.createDirectories(archivePath);

            Path target = archivePath.resolve(path.getFileName());
            Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("File moved to archive: {}", target);

        } catch (Exception e) {
            log.error("CRITICAL ERROR processing file " + path, e);
        }
    }
}