package com.example.wmsmock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "wms.rabbitmq")
public class WmsRabbitMQProperties {

    private String exchange;
    private Queue queue = new Queue();
    private RoutingKey routingKey = new RoutingKey();

    @Getter
    @Setter
    public static class Queue {
        private String inboundComplete;
        private String outboundCmdAck;
    }

    @Getter
    @Setter
    public static class RoutingKey {
        private String inboundCmd;
        private String inboundComplete;
        private String inboundCancel;
        private String outboundCmd;
        private String outboundCmdAck;
    }
}
