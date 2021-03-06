package io.liftbridge;

import io.grpc.stub.StreamObserver;
import io.liftbridge.exceptions.DeadlineExceededException;
import io.liftbridge.exceptions.NoSuchPartitionException;
import io.liftbridge.exceptions.NoSuchStreamException;
import io.liftbridge.exceptions.StreamExistsException;
import io.liftbridge.proto.Api;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class ClientSubscribeTest extends BaseClientTest {

    private String populatedStreamName;

    @Before
    public void setupStreams() throws StreamExistsException, DeadlineExceededException {
        populatedStreamName = randomAlphabetic(10);
        StreamOptions opts = new StreamOptions();
        client.createStream(streamName, opts);
        client.createStream(populatedStreamName, opts);

        MessageOptions msgOpts = new MessageOptions().setAckDeadline(1, SECONDS);

        for (int i = 0; i < 10; ++i) {
            byte[] payload = ByteBuffer.allocate(4).putInt(i).array();
            client.publish(this.populatedStreamName, payload, msgOpts);
        }
    }

    @After
    public void teardownStreams() throws NoSuchStreamException {
        client.deleteStream(streamName);
        client.deleteStream(populatedStreamName);
    }

    @Test
    public void testSubscribeDefaultOptions() throws NoSuchPartitionException {
        Subscription sub = client.subscribe(streamName, new SubscriptionOptions(), new MessageHandler() {
            @Override
            public void onMessage(Message msg) {

            }

            @Override
            public void onError(Throwable t) {
                fail(t.getMessage());
            }
        });

        sub.unsubscribe();
    }

    @Test(expected = NoSuchPartitionException.class)
    public void testSubscribeNonExistentStream() throws NoSuchPartitionException {
        client.subscribe(randomAlphabetic(15), new SubscriptionOptions(), new MessageHandler() {
            @Override
            public void onMessage(Message msg) {
                fail("Received unexpected message");
            }

            @Override
            public void onError(Throwable t) {
                fail(t.getMessage());
            }
        });
    }

    @Test
    public void testSubscribeFromBeginning() throws NoSuchPartitionException {
        SubscriptionOptions opts = new SubscriptionOptions().startAtEarliestReceived();
        final List<Integer> streamValues = new ArrayList<>();

        Subscription sub = client.subscribe(populatedStreamName, opts, new MessageHandler() {
            @Override
            public void onMessage(Message msg) {
                streamValues.add(ByteBuffer.wrap(msg.getValue()).getInt());
            }

            @Override
            public void onError(Throwable t) {
                fail(t.getMessage());
            }
        });

        await().atMost(5, SECONDS).until(() -> streamValues.size() >= 10);
        Collections.sort(streamValues);
        Integer[] vals = new Integer[10];
        assertArrayEquals("All messages were received",
                new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
                streamValues.toArray(vals));

        sub.unsubscribe();
    }

    @Test
    public void testSubscribeFromLatestReceived() throws NoSuchPartitionException {
        SubscriptionOptions opts = new SubscriptionOptions().startAtLatestReceived();
        final AtomicLong lastOffset = new AtomicLong(-1);

        Subscription sub = client.subscribe(populatedStreamName, opts, new MessageHandler() {
            @Override
            public void onMessage(Message msg) {
                lastOffset.set(msg.getOffset());
            }

            @Override
            public void onError(Throwable t) {
                fail(t.getMessage());
            }
        });

        await().atMost(5, SECONDS).until(() -> lastOffset.get() >= 0);
        assertEquals("Last offset message was received", 9, lastOffset.get());

        sub.unsubscribe();
    }

    @Test
    public void testSubscribeFromNewOnly() throws NoSuchPartitionException, DeadlineExceededException {
        SubscriptionOptions opts = new SubscriptionOptions();
        final List<Integer> streamValues = new ArrayList<>();

        Subscription sub = client.subscribe(populatedStreamName, opts, new MessageHandler() {
            @Override
            public void onMessage(Message msg) {
                streamValues.add(ByteBuffer.wrap(msg.getValue()).getInt());
            }

            @Override
            public void onError(Throwable t) {
                fail(t.getMessage());
            }
        });

        MessageOptions msgOpts = new MessageOptions().setAckDeadline(10, MILLISECONDS);
        for (int i = 0; i < 10; ++i) {
            byte[] payload = ByteBuffer.allocate(20).putInt(i + 10).array();
            client.publish(this.populatedStreamName, payload, msgOpts);
        }
        await().atMost(5, SECONDS).until(() -> streamValues.size() >= 10);
        Collections.sort(streamValues);
        Integer[] vals = new Integer[10];
        assertArrayEquals("All messages were received",
                new Integer[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19},
                streamValues.toArray(vals));

        sub.unsubscribe();
    }

    @Test
    public void testSubscribeFromOffset() throws NoSuchPartitionException {
        SubscriptionOptions opts = new SubscriptionOptions().startAtOffset(5);
        final List<Long> offsets = new ArrayList<>();

        Subscription sub = client.subscribe(populatedStreamName, opts, new MessageHandler() {
            @Override
            public void onMessage(Message msg) {
                offsets.add(msg.getOffset());
            }

            @Override
            public void onError(Throwable t) {
                fail(t.getMessage());
            }
        });

        await().atMost(5, SECONDS).until(() -> offsets.size() >= 5);
        Collections.sort(offsets);
        Long[] vals = new Long[5];
        assertArrayEquals("All offset, starting with 5, were received",
                new Long[]{5L, 6L, 7L, 8L, 9L},
                offsets.toArray(vals));

        sub.unsubscribe();
    }

}
