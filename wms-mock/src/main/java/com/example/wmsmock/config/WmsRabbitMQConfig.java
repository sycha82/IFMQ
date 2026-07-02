package com.example.wmsmock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WmsRabbitMQConfig {

    private final WmsRabbitMQProperties props;

    @Bean
    public TopicExchange wmsExchange() {
        return new TopicExchange(props.getExchange(), true, false);
    }

    @Bean
    public Queue inboundCompleteQueue() {
        return new Queue(props.getQueue().getInboundComplete(), true);
    }

    @Bean
    public Binding inboundCompleteBinding(TopicExchange wmsExchange) {
        return BindingBuilder.bind(inboundCompleteQueue())
                .to(wmsExchange)
                .with(props.getRoutingKey().getInboundComplete());
    }

    @Bean
    public Queue outboundCmdAckQueue() {
        return new Queue(props.getQueue().getOutboundCmdAck(), true);
    }

    @Bean
    public Binding outboundCmdAckBinding(TopicExchange wmsExchange) {
        return BindingBuilder.bind(outboundCmdAckQueue())
                .to(wmsExchange)
                .with(props.getRoutingKey().getOutboundCmdAck());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
