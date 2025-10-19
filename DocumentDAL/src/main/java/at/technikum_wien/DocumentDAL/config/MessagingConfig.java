package at.technikum_wien.DocumentDAL.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    @Value("${DOC_EVENTS_EXCHANGE:documents.exchange}")
    private String exchangeName;

    @Value("${DOC_EVENTS_QUEUE:documents.uploaded}")
    private String queueName;

    @Value("${DOC_EVENTS_ROUTING_KEY:document.uploaded}")
    private String routingKey;

    @Bean
    public TopicExchange documentsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue documentsUploadedQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding documentsUploadedBinding(Queue documentsUploadedQueue, TopicExchange documentsExchange) {
        return BindingBuilder.bind(documentsUploadedQueue).to(documentsExchange).with(routingKey);
    }
}
