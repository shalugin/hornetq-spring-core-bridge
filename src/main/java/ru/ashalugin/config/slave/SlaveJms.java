package ru.ashalugin.config.slave;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@Slf4j
@Conditional(SlaveInstance.class)
public class SlaveJms {

    @Value("${instance.code}")
    private String instanceCode;
    @Autowired
    private JmsMessagingTemplate jmsQueueTemplate;

    @Scheduled(fixedDelay = 10000)
    public void produceMessage() {
        long t = System.currentTimeMillis();
        log.info("Begin send {}", new Date());
        SlaveMessage slaveMessage = new SlaveMessage(System.nanoTime(), new Date().toString() + "-slave", instanceCode);
        jmsQueueTemplate.convertAndSend("masterQueue", slaveMessage);
        log.info("Slave produced message: {}", slaveMessage);
        log.info("End send, {} ms", System.currentTimeMillis() - t);
    }
}