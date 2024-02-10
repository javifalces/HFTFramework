package com.lambda.investing.connector.ordinary;

import com.google.common.base.Stopwatch;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorListener;
import com.lambda.investing.model.messaging.TypeMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class OrdinaryConnectorPublisherProviderTest implements ConnectorListener {
    OrdinaryConnectorPublisherProvider ordinaryConnectorPublisherProvider = null;
    OrdinaryConnectorConfiguration ordinaryConnectorConfiguration = new OrdinaryConnectorConfiguration();
    List<ReceivedItem> lastItemsUpdate = new ArrayList();
    CountDownLatch waiter;

    @AllArgsConstructor
    @Getter
    private class ReceivedItem {
        ConnectorConfiguration configuration;
        long timestampReceived;
        TypeMessage typeMessage;
        String content;
    }

    @Override
    public void onUpdate(ConnectorConfiguration configuration, long timestampReceived, TypeMessage typeMessage, String content) {
        lastItemsUpdate.add(new ReceivedItem(configuration, timestampReceived, typeMessage, content));
        if (waiter != null) {
            waiter.countDown();
        }

    }

    @Test
    @RepeatedTest(25)
    public void testSendReceiveSimple() throws InterruptedException {
        Stopwatch timer = Stopwatch.createStarted();
        ordinaryConnectorPublisherProvider = new OrdinaryConnectorPublisherProvider("junit_test", 1, Thread.NORM_PRIORITY);
        ordinaryConnectorPublisherProvider.register(ordinaryConnectorConfiguration, this);

        String topic = "topic1";
        TypeMessage typeMessage = TypeMessage.info;
        String message = "message";
        waiter = new CountDownLatch(1);
        lastItemsUpdate.clear();
        ordinaryConnectorPublisherProvider.publish(ordinaryConnectorConfiguration, typeMessage, topic, message);
        waiter.await();
        assertEquals(1, lastItemsUpdate.size());
        ReceivedItem itemReceived = lastItemsUpdate.get(0);
        assertEquals(typeMessage, itemReceived.getTypeMessage());
        assertEquals(message, itemReceived.getContent());
        System.out.println("Method took: " + timer.stop());

    }
}
