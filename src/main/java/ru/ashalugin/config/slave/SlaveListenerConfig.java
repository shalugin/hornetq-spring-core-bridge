package ru.ashalugin.config.slave;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import ru.ashalugin.config.QueueUtil;
import ru.ashalugin.config.master.MasterMessage;

import javax.jms.JMSException;
import java.util.UUID;

@Conditional(SlaveInstance.class)
@Configuration
@Slf4j
public class SlaveListenerConfig implements JmsListenerConfigurer {

    @Value("${instance.code}")
    private String instanceCode;
    @Value("${slave.queue.prefix.list}")
    private String queuePrefixList;

    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
        String[] queuePrefixArray = queuePrefixList.split(",");
        for (String queuePrefix : queuePrefixArray) {
            registerOne(registrar, queuePrefix);
        }
    }

    private void registerOne(JmsListenerEndpointRegistrar registrar, String queuePrefix) {
        SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
        endpoint.setId(UUID.randomUUID().toString());
        endpoint.setDestination(QueueUtil.generateQueueName(queuePrefix, instanceCode));
        endpoint.setMessageListener(message -> {
            try {
                MasterMessage masterMessage = message.getBody(MasterMessage.class);
                log.info("Slave received message: {}", masterMessage);
            } catch (JMSException e) {
                log.error(e.getMessage(), e);
            }
        });
        registrar.registerEndpoint(endpoint);
    }
}