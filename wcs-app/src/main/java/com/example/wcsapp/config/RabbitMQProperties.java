package com.example.wcsapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "wcs.rabbitmq")
public class RabbitMQProperties {

    private String exchange;
    private Queue queue = new Queue();
    private RoutingKey routingKey = new RoutingKey();

    @Getter
    @Setter
    public static class Queue {
        private String inboundCmd;
    }

    @Getter
    @Setter
    public static class RoutingKey {
        private String inboundCmd;
        private String inboundComplete;
    }
}
