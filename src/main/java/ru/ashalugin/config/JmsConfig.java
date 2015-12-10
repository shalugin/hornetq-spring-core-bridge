package ru.ashalugin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.jms.ConnectionFactory;

@Configuration
@EnableJms
@EnableScheduling
public class JmsConfig {

    @Bean
    public JmsMessagingTemplate jmsQueueTemplate(ConnectionFactory connectionFactory) {
        JmsMessagingTemplate template = new JmsMessagingTemplate(connectionFactory);
        template.getJmsTemplate().setPubSubDomain(false);
        return template;
    }
}