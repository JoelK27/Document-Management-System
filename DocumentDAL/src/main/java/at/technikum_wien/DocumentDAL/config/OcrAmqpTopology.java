package at.technikum_wien.DocumentDAL.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrAmqpTopology {

    @Value("${DOC_EVENTS_EXCHANGE:documents.exchange}")
    private String exchangeName;

    @Value("${DOC_EVENTS_QUEUE:documents.uploaded}")
    private String queueName;

    @Value("${DOC_EVENTS_ROUTING_KEY:document.uploaded}")
    private String routingKey;

    @Bean
    public DirectExchange documentsExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue documentsQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding documentsBinding(Queue documentsQueue, DirectExchange documentsExchange) {
        return BindingBuilder.bind(documentsQueue).to(documentsExchange).with(routingKey);
    }
}