package ru.ashalugin.config.master;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ashalugin.config.QueueUtil;
import ru.ashalugin.config.slave.SlaveMessage;

import java.util.Date;

@Service
@Transactional
@Conditional(MasterInstance.class)
@Slf4j
public class MasterJms {

    @Autowired
    private JmsMessagingTemplate jmsQueueTemplate;
    @Value("${slave.queue.prefix.list}")
    private String queuePrefixList;
    @Value("${jms.slave.list}")
    private String jmsSlaveList;

    @Scheduled(fixedDelay = 10000)
    public void produceMessage() {
        String[] queuePrefixArray = queuePrefixList.split(",");
        String[] slaveArray = jmsSlaveList.split("\\|");
        for (String slaveSettings : slaveArray) {
            String[] settings = slaveSettings.split(",");
            for (String queuePrefix : queuePrefixArray) {
                String queueName = QueueUtil.generateQueueName(queuePrefix, settings[0]);
                sendTQueue(queueName, "SL1");
            }
        }
        jmsQueueTemplate.convertAndSend("masterQueue", new SlaveMessage(System.nanoTime(), "master", "MASTER"));

        long t = System.currentTimeMillis();
        log.info("End send, {} ms", System.currentTimeMillis() - t);
    }

    private void sendTQueue(String queue, String instanceCode) {
        MasterMessage masterMessage = new MasterMessage(System.currentTimeMillis(), new Date().toString() + queue + instanceCode);
        jmsQueueTemplate.convertAndSend(queue, masterMessage);
        log.info("Master produced message: {}", masterMessage);
    }

    @JmsListener(destination = "masterQueue")
    public void rcv(SlaveMessage message) {
        log.info("Master received message: {}", message);
    }
}