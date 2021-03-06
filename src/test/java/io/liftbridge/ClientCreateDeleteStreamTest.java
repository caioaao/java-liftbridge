package io.liftbridge;

import io.liftbridge.exceptions.NoSuchStreamException;
import io.liftbridge.exceptions.StreamExistsException;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ClientCreateDeleteStreamTest extends BaseClientTest {
    // TODO write assertions when a "fetch metadata" method is available
    @Test
    public void testCreateStreamDefaultOptions() throws StreamExistsException, NoSuchStreamException {
        client.createStream(streamName);
        client.deleteStream(streamName);
    }

    @Test
    public void testCreateStreamWithMorePartitions() throws StreamExistsException, NoSuchStreamException {
        client.createStream(streamName, new StreamOptions().setPartitions(3));
        client.deleteStream(streamName);
    }

    @Test
    public void testCreateStreamWithGroup() throws StreamExistsException, NoSuchStreamException {
        client.createStream(streamName, new StreamOptions().setGroup("grp1"));
        client.deleteStream(streamName);
    }

    @Test
    public void testCreateStreamWithSubject() throws StreamExistsException, NoSuchStreamException {
        client.createStream(streamName, new StreamOptions().setSubject("subj-rand"));
        client.deleteStream(streamName);
    }

    @Test
    public void testCreateStreamWithReplicationFactor() throws StreamExistsException, NoSuchStreamException {
        client.createStream(streamName, new StreamOptions().setReplicationFactor(1));
        client.deleteStream(streamName);
    }

    @Test
    public void testCreateStreamWithAllOptions() throws StreamExistsException, NoSuchStreamException {
        StreamOptions options = new StreamOptions()
                .setPartitions(3)
                .setGroup("grp1")
                .setSubject("subj-rand")
                .setReplicationFactor(1);
        client.createStream(streamName, options);
        client.deleteStream(streamName);
    }

    @Test
    public void testCreateStreamDuplicatedFails() throws StreamExistsException, NoSuchStreamException {
        client.createStream(streamName, new StreamOptions());
        try {
            client.createStream(streamName, new StreamOptions());
            fail("Expected StreamExistsException");
        } catch (StreamExistsException ignored) {
        } finally {
            client.deleteStream(streamName);
        }
    }

    @Test(expected = NoSuchStreamException.class)
    public void testDeleteStreamNoSuchStream() throws NoSuchStreamException {
        client.deleteStream("foo");
    }
}
