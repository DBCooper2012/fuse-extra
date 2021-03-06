/**
 * Copyright (C) 2012 FuseSource Corp. All rights reserved.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.amqp.codec;

import org.fusesource.amqp.types.*;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.junit.Test;

import java.util.ArrayList;

import static org.fusesource.amqp.codec.TestSupport.writeRead;
import static org.fusesource.hawtbuf.Buffer.ascii;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DescribedTypeTest {

    @Test
    public void testApplicationProperties() throws Exception {
        ApplicationProperties in = new ApplicationProperties();
        in.setValue(new MapEntries());
        in.getValue().add(new AMQPSymbol(ascii("one").buffer()), new AMQPString("two"));
        in.getValue().add(new AMQPSymbol(ascii("three").buffer()), new AMQPString("four"));
        ApplicationProperties out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testAMQPValue() throws Exception {
        AMQPValue in = new AMQPValue();
        in.setValue(new AMQPString("Hello world!"));
        AMQPValue out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testAMQPSequence() throws Exception {
        AMQPSequence in = new AMQPSequence();
        in.setValue(new ArrayList());
        in.getValue().add(new AMQPString("Hello world!"));
        in.getValue().add(new AMQPString("and stuff"));
        in.getValue().add(new AMQPLong(123L));
        AMQPSequence out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testOpen() throws Exception {
        Open in = new Open();
        in.setChannelMax((int) Short.MAX_VALUE);
        in.setContainerID("foo");
        in.setHostname("localhost");
        in.setOfferedCapabilities(new AMQPSymbol[]{new AMQPSymbol(new Buffer("blah".getBytes()))});
        Open out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testBegin() throws Exception {
        Begin in = new Begin();
        in.setHandleMax((long) Integer.MAX_VALUE);
        in.setNextOutgoingID(0L);
        in.setIncomingWindow(10L);
        in.setOutgoingWindow(10L);
        Begin out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testEnd() throws Exception {
        End in = new End();
        End out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testTransfer() throws Exception {
        Transfer in = new Transfer();
        in.setHandle(0L);
        in.setDeliveryTag(new AsciiBuffer("0").buffer());
        in.setDeliveryID(0L);
        in.setSettled(false);
        in.setAborted(false);
        in.setMessageFormat(0L);
        in.setMore(false);
        in.setResume(false);
        in.setRcvSettleMode(ReceiverSettleMode.FIRST.getValue());
        in.setState(new Accepted());
        Transfer out = writeRead(in);
        System.out.printf("\n\n%s\n%s\n\n", in, out);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testAttach() throws Exception {
        Attach in = new Attach();
        in.setName("TEST");
        in.setHandle(0L);
        in.setRole(Role.SENDER.getValue());
        in.setInitialDeliveryCount(0L);
        Source source = new Source();
        Target target = new Target();
        target.setAddress(new AddressString("Foo"));
        in.setSource(source);
        in.setTarget(target);

        Attach out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testDetach() throws Exception {
        Detach in = new Detach();
        in.setHandle(0L);
        in.setClosed(true);
        Detach out = writeRead(in);
        assertEquals(in.toString(), out.toString());
    }
}
