package redis;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import graph.Graph;
import graph.Graph.InfosJSON;


public class InfosWorker {
    private static final String JEDIS_SERVER = "0.0.0.0";
    private static final String QUEUE_NAME = "infograph/infos";

    private ArrayList<String> messageContainer = new ArrayList<String>();

    private CountDownLatch messageReceivedLatch = new CountDownLatch(1);
    private CountDownLatch publishLatch = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        new InfosWorker().run();
    }

    public void run() throws InterruptedException {
//        setupPublisher();
        JedisPubSub jedisPubSub = setupSubscriber();

        // publish away!
//        publishLatch.countDown();

        messageReceivedLatch.await();
//        Iterator msgIter = messageContainer.iterator();
//        while (msgIter.hasNext()){
//            log("Got message: %s", msgIter.next());
//        }

//        jedisPubSub.unsubscribe();
    }

    private void setupPublisher() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    log("Connecting");
                    Jedis jedis = new Jedis(JEDIS_SERVER);
                    log("Waiting to publish");
                    publishLatch.await();
                    log("Ready to publish, waiting one sec");
                    Thread.sleep(1000);
                    log("publishing");
                    jedis.publish("test", "This is a message");
                    log("published, closing publishing connection");
                    Thread.sleep(10000);
                    jedis.quit();
                    log("publishing connection closed");
                } catch (Exception e) {
                    log(">>> OH NOES Pub, " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }, "publisherThread").start();
    }

    private JedisPubSub setupSubscriber() {
        final JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                log("onUnsubscribe");
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                log("onSubscribe");
            }

            @Override
            public void onPUnsubscribe(String pattern, int subscribedChannels) {
            }

            @Override
            public void onPSubscribe(String pattern, int subscribedChannels) {
            }

            @Override
            public void onPMessage(String pattern, String channel, String message) {
            }

            @Override
            public void onMessage(String channel, String message) {
                messageContainer.add(message);
                log("Message received");
                addInfos(message);
//            log("Got message: %s",message);
                messageReceivedLatch.countDown();
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    log("Connecting");
                    Jedis jedis = new Jedis(JEDIS_SERVER);
                    log("subscribing");
                    jedis.subscribe(jedisPubSub, QUEUE_NAME);
                    log("subscribe returned, closing down");
                    jedis.quit();
                } catch (Exception e) {
                    log(">>> OH NOES Sub - " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }, "subscriberThread").start();
        return jedisPubSub;
    }

    public void addInfos(String msg) {
        log("Got message: %s", msg);

        Gson gson = new Gson();
        InfosJSON infosJSON = gson.fromJson(msg, InfosJSON.class);

        Graph graph = new Graph();
        graph.addInfos(infosJSON.getContent());
    }

    static final long startMillis = System.currentTimeMillis();

    private static void log(String string, Object... args) {
        long millisSinceStart = System.currentTimeMillis() - startMillis;
        System.out.printf("%20s %6d %s\n", Thread.currentThread().getName(), millisSinceStart,
                String.format(string, args));
    }
}
