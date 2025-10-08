package at.technikum_wien.ocrworker.config;

import at.technikum_wien.ocrworker.model.DocumentUploadedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    private static final String BACKEND_EVENT_FQCN =
            "at.technikum_wien.DocumentDAL.messaging.events.DocumentUploadedEvent";

    @Bean
    public ClassMapper eventClassMapper() {
        DefaultClassMapper mapper = new DefaultClassMapper();
        mapper.setIdClassMapping(Map.of(
                BACKEND_EVENT_FQCN, DocumentUploadedEvent.class
        ));
        mapper.setTrustedPackages(
                "at.technikum_wien.DocumentDAL.messaging.events",
                "at.technikum_wien.ocrworker.model"
        );
        return mapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(
            ObjectMapper objectMapper,
            ClassMapper eventClassMapper
    ) {
        Jackson2JsonMessageConverter conv = new Jackson2JsonMessageConverter(objectMapper);
        conv.setClassMapper(eventClassMapper);
        return conv;
    }
}