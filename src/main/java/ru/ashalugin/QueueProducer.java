package ru.ashalugin;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.BridgeConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jms.hornetq.HornetQConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import ru.ashalugin.config.QueueUtil;
import ru.ashalugin.config.master.MasterInstance;
import ru.ashalugin.config.slave.SlaveInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shalugin
 */
@Configuration
public class QueueProducer implements BeanFactoryAware {
    private static final String MASTER_TO_SLAVE_CONNECTOR_PREFIX = "nettySlaveToMaster";
    private static final String SLAVE_TO_MASTER_CONNECTOR_NAME = "nettyMasterToSlave";

    // Bridge settings
    @Value("${jms.bridge.bridge-postfix}")
    private String bridgePostfix;
    @Value("${jms.bridge.queue-prefix}")
    private String bridgeQueuePrefix;
    @Value("${jms.bridge.retry-interval}")
    private String bridgeRetryInterval;
    @Value("${jms.bridge.retry-interval-multiplier}")
    private String bridgeRetryIntervalMultiplier;
    @Value("${jms.bridge.reconnect-attempts}")
    private String bridgeReconnectAttempts;
    @Value("${jms.bridge.confirmation-window-size}")
    private String bridgeConfirmationWindowSize;
    // Instance settings
    @Value("${instance.code}")
    private String instanceCode;
    @Value("${application.mode}")
    private String applicationMode;
    // Broker settings
    @Value("${jms.broker.host}")
    private String jmsBrokerHost;
    @Value("${jms.broker.port}")
    private String jmsBrokerPort;
    // Queue settings
    @Value("${slave.queue.prefix.list}")
    private String queuePrefixList;
    // Master settings
    @Value("${jms.slave.list:}")
    private String jmsSlaveList;
    // Slave settings
    @Value("${jms.bridge.master.host:}")
    private String bridgeMasterHost;
    @Value("${jms.bridge.master.port:}")
    private String bridgeMasterPort;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
        if (MasterInstance.INSTANCE_CODE.equals(applicationMode)) {
            produceMasterQueues(configurableBeanFactory);
        } else if (SlaveInstance.INSTANCE_CODE.equals(applicationMode)) {
            produceSlaveQueues(configurableBeanFactory);
        }
    }

    @Bean(name = "hornetCustomizer")
    @Conditional(MasterInstance.class)
    public HornetQConfigurationCustomizer masterCustomizer() {
        return configuration -> {
            TransportConfiguration brokerAcceptor = QueueUtil.getBrokerAcceptor(jmsBrokerHost, jmsBrokerPort);
            configuration.getAcceptorConfigurations().add(brokerAcceptor);
            String[] queuePrefixArray = getQueuePrefixArray();

            String[] slaveArray = jmsSlaveList.split("\\|");
            for (String slaveSettings : slaveArray) {
                String[] settings = slaveSettings.split(",");
                String connectorName = MASTER_TO_SLAVE_CONNECTOR_PREFIX + settings[0];
                TransportConfiguration transportConfiguration = QueueUtil.getConnector(settings[1], settings[2]);
                configuration.getConnectorConfigurations().put(connectorName, transportConfiguration);

                for (String queuePrefix : queuePrefixArray) {
                    String queueName = QueueUtil.generateQueueName(queuePrefix, settings[0]);
                    BridgeConfiguration bridgeConfiguration = getBridgeConfiguration(queueName, connectorName);
                    configuration.getBridgeConfigurations().add(bridgeConfiguration);
                }
            }
        };
    }

    private void produceMasterQueues(ConfigurableBeanFactory configurableBeanFactory) {
        String[] queuePrefixArray = getQueuePrefixArray();
        String[] slaveArray = jmsSlaveList.split("\\|");
        for (String slave : slaveArray) {
            String[] settings = slave.split(",");
            QueueUtil.createQueues(configurableBeanFactory, queuePrefixArray, settings[0]);
        }
    }

    @Bean(name = "hornetCustomizer")
    @Conditional(SlaveInstance.class)
    public HornetQConfigurationCustomizer slaveCustomizer(@Value("${spring.hornetq.embedded.queues}") String queues) {
        return configuration -> {
            TransportConfiguration brokerAcceptor = QueueUtil.getBrokerAcceptor(jmsBrokerHost, jmsBrokerPort);
            configuration.getAcceptorConfigurations().add(brokerAcceptor);

            TransportConfiguration transportConfiguration = QueueUtil.getConnector(bridgeMasterHost, bridgeMasterPort);
            configuration.getConnectorConfigurations().put(SLAVE_TO_MASTER_CONNECTOR_NAME, transportConfiguration);

            String[] queueArray = queues.split(",");
            for (String queue : queueArray) {
                BridgeConfiguration bridgeConfiguration = getBridgeConfiguration(queue, SLAVE_TO_MASTER_CONNECTOR_NAME);
                configuration.getBridgeConfigurations().add(bridgeConfiguration);
            }
        };
    }

    private void produceSlaveQueues(ConfigurableBeanFactory configurableBeanFactory) {
        QueueUtil.createQueues(configurableBeanFactory, getQueuePrefixArray(), this.instanceCode);
    }

    private BridgeConfiguration getBridgeConfiguration(String queue, String connectorName) {
        String queueName = bridgeQueuePrefix + queue;
        BridgeConfiguration bridgeConfiguration = new BridgeConfiguration();
        bridgeConfiguration.setForwardingAddress(queueName);
        bridgeConfiguration.setName(queue + bridgePostfix);
        bridgeConfiguration.setQueueName(queueName);
        bridgeConfiguration.setRetryInterval(Long.parseLong(bridgeRetryInterval));
        bridgeConfiguration.setRetryIntervalMultiplier(Double.parseDouble(bridgeRetryIntervalMultiplier));
        bridgeConfiguration.setReconnectAttempts(Integer.parseInt(bridgeReconnectAttempts));
        bridgeConfiguration.setConfirmationWindowSize(Integer.parseInt(bridgeConfirmationWindowSize));
        bridgeConfiguration.setUseDuplicateDetection(false);
        List<String> staticConnectors = new ArrayList<>();
        staticConnectors.add(connectorName);
        bridgeConfiguration.setStaticConnectors(staticConnectors);
        return bridgeConfiguration;
    }

    private String[] getQueuePrefixArray() {
        return queuePrefixList.split(",");
    }
}