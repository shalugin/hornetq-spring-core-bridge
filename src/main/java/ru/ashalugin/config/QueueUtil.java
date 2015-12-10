package ru.ashalugin.config;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Shalugin
 */
public final class QueueUtil {

    private QueueUtil() {
    }

    public static String generateQueueName(String queuePrefix, String instanceCode) {
        return queuePrefix + instanceCode + "Queue";
    }

    public static void createQueues(ConfigurableBeanFactory configurableBeanFactory, String[] queuePrefixArray, String instanceCode) {
        for (String queuePrefix : queuePrefixArray) {
            String queueName = QueueUtil.generateQueueName(queuePrefix, instanceCode);
            JMSQueueConfigurationImpl jmsQueueConfiguration = new JMSQueueConfigurationImpl(queueName, null, true, queueName);
            configurableBeanFactory.registerSingleton(queueName + "Bean", jmsQueueConfiguration);
        }
    }

    public static TransportConfiguration getConnector(String host, String port) {
        return getTransportConfiguration(host, port, NettyConnectorFactory.class.getName());
    }

    public static TransportConfiguration getBrokerAcceptor(String host, String port) {
        return getTransportConfiguration(host, port, NettyAcceptorFactory.class.getName());
    }

    private static TransportConfiguration getTransportConfiguration(String host, String port, String name) {
        Map<String, Object> params = new HashMap<>();
        params.put("host", host);
        params.put("port", port);
        return new TransportConfiguration(name, params);
    }
}