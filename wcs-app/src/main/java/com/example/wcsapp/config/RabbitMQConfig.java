package com.example.wcsapp.config;

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
public class RabbitMQConfig {

    private final RabbitMQProperties props;

    @Bean
    public TopicExchange wcsExchange() {
        return new TopicExchange(props.getExchange(), true, false);
    }

    @Bean
    public Queue inboundCmdQueue() {
        return new Queue(props.getQueue().getInboundCmd(), true);
    }

    @Bean
    public Binding inboundCmdBinding(Queue inboundCmdQueue, TopicExchange wcsExchange) {
        return BindingBuilder.bind(inboundCmdQueue)
                .to(wcsExchange)
                .with(props.getRoutingKey().getInboundCmd());
    }

    @Bean
    public Queue inboundCancelQueue() {
        return new Queue(props.getQueue().getInboundCancel(), true);
    }

    @Bean
    public Binding inboundCancelBinding(Queue inboundCancelQueue, TopicExchange wcsExchange) {
        return BindingBuilder.bind(inboundCancelQueue)
                .to(wcsExchange)
                .with(props.getRoutingKey().getInboundCancel());
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
