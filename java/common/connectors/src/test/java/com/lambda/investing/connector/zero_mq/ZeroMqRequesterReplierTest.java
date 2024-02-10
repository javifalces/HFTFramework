package com.lambda.investing.connector.zero_mq;

import com.lambda.investing.connector.ConnectorReplier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ZeroMqRequesterReplierTest {

    ZeroMqConfiguration zeroMqConfiguration = new ZeroMqConfiguration("localhost", 5555, "");
    ZeroMqRequester zeroMqRequester = new ZeroMqRequester();
    ZeroMqReplier zeroMqReplier = new ZeroMqReplier(zeroMqConfiguration, this::reply);

    String lastRequestMessage = null;
    List<String> lastRequestMessages = new ArrayList<>();

    public ZeroMqRequesterReplierTest() throws IOException {
        zeroMqReplier = new ZeroMqReplier(zeroMqConfiguration, this::reply);
        zeroMqReplier.start();
    }

    public String reply(long timestampReceived, String content) {
        lastRequestMessage = content;
        lastRequestMessages.add(content);
        return content + "-reply";
    }

    @Before
    public void setUp() throws Exception {
        lastRequestMessage = null;
        lastRequestMessages.clear();
    }


//    @Test
//    public void sendSimpleMessageTest() {
//        String message = "message";
//        String reply = zeroMqRequester.request(zeroMqConfiguration, message);
//        Assert.assertEquals(message+"-reply",reply);
//        Assert.assertEquals(message,lastRequestMessage);
//    }

    @Test
    public void sendMultiThreadMessageTest() throws InterruptedException {
        String message = "message_";
        int numThreads = 10;
        List<Thread> threads = new ArrayList<>();
        List<MyRunnable> runnables = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            MyRunnable runnable = new MyRunnable(zeroMqRequester, zeroMqConfiguration, message + i);
            runnables.add(runnable);
            Thread thread = new Thread(runnable);
            threads.add(thread);
            thread.start();
        }
        //wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                assert false;
            }
        }
        Assert.assertEquals(numThreads, lastRequestMessages.size());

        for (int i = 0; i < numThreads; i++) {
            MyRunnable runnable = runnables.get(i);
            String request = runnable.requestMessage;
            String reply = runnable.replyMessage;
            Assert.assertEquals(request + "-reply", reply);

        }

    }

    private static class MyRunnable implements Runnable {
        private String requestMessage;
        private String replyMessage;

        private ZeroMqConfiguration zeroMqConfiguration;
        private ZeroMqRequester zeroMqRequester;


        public MyRunnable(ZeroMqRequester zeroMqRequester, ZeroMqConfiguration zeroMqConfiguration, String requestMessage) {
            this.zeroMqRequester = zeroMqRequester;
            this.zeroMqConfiguration = zeroMqConfiguration;
            this.requestMessage = requestMessage;
        }

        @Override
        public void run() {
            replyMessage = zeroMqRequester.request(zeroMqConfiguration, requestMessage);

        }

    }


}
