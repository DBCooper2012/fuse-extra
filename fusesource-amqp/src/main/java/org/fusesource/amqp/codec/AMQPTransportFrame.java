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
import org.fusesource.amqp.codec.marshaller.BitUtils;
import org.fusesource.amqp.codec.marshaller.TypeReader;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.DataByteArrayInputStream;

import java.io.DataInput;
import java.io.DataOutput;

/**
 * Represents an AMQP frame
 */
public class AMQPTransportFrame extends AMQPFrame {

    public static final Buffer EMPTY = new Buffer(0);

    public static final int AMQP_FRAME_TYPE = 0x00;
    public static final int AMQP_SASL_FRAME_TYPE = 0x01;

    protected Buffer header = new Buffer(8);
    protected Buffer extHeader = new Buffer(0);
    protected org.fusesource.amqp.types.AMQPFrame performative = null;
    protected Buffer payload = null;

    protected static final int SIZE_OFFSET = 0;
    protected static final int DOFF_OFFSET = 4;
    protected static final int TYPE_OFFSET = 5;
    protected static final int CHANNEL_OFFSET = 6;

    public AMQPTransportFrame() {
        setPerformative(NoPerformative.INSTANCE);
        setPayload(EMPTY);
    }

    public AMQPTransportFrame(org.fusesource.amqp.types.AMQPFrame performative) {
        setPerformative(performative);
        setPayload(EMPTY);
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
    }

    public AMQPTransportFrame(org.fusesource.amqp.types.AMQPFrame performative, Buffer payload) {
        setPerformative(performative);
        setPayload(payload);
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
    }

    public AMQPTransportFrame(int channel, org.fusesource.amqp.types.AMQPFrame performative) {
        setChannel(channel);
        setPerformative(performative);
        setPayload(EMPTY);
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
    }

    public AMQPTransportFrame(int channel, org.fusesource.amqp.types.AMQPFrame performative, Buffer payload) {
        setChannel(channel);
        setPerformative(performative);
        setPayload(payload);
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
    }

    public AMQPTransportFrame(Buffer header, Buffer body) {
        this.header = header;
        initExtHeader(body);
        fromBuffer(body);
    }

    private void fromBuffer(Buffer body) {
        try {
            DataByteArrayInputStream in = new DataByteArrayInputStream(body);
            if (in.available() > 0) {
                setPerformative((org.fusesource.amqp.types.AMQPFrame)TypeReader.read(in));
            } else {
                setPerformative(NoPerformative.INSTANCE);
            }
            if (in.available() > 0) {
                Buffer payload = new Buffer(in.available());
                payload.readFrom((DataInput)in);
                setPayload(payload);
            } else {
                setPayload(EMPTY);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating frame from buffer : " + e.getMessage(), e);
        }
    }

    public AMQPTransportFrame(DataInput in) throws Exception {
        read(in);
    }

    public boolean equals(AMQPTransportFrame other) {
        if ( other == null ) {
            return false;
        }
        if ( !header.equals(other.header) ) {
            return false;
        }
        if ( !extHeader.equals(other.extHeader) ) {
            return false;
        }
        if ( this.payload == null && other.payload != null ) {
            return false;
        }
        return payload.equals(other.payload);
    }

    public int calculateDataOffset() {
        return (header.length + extHeader.length) / 4;
    }

    public void read(DataInput in) throws Exception {
        header.readFrom(in);
        initExtHeader();
        extHeader.readFrom(in);
        if ( getDataSize() > 0 ) {
            Buffer body = new Buffer((int) getDataSize());
            body.readFrom(in);
            fromBuffer(body);
        }
    }

    private void initExtHeader() {
        initExtHeader(EMPTY);
    }

    private void initExtHeader(Buffer body) {
        int dataOffset = getDoff() * 4;
        if ( dataOffset > header.length ) {
            if (body == null || body == EMPTY) {
                extHeader = new Buffer(dataOffset - header.length);
            } else {
                extHeader = new Buffer(body.data, 0, dataOffset - header.length);
                body.offset(dataOffset - header.length);
            }
        }
    }


    public long getDataSize() {
        return getSize() - (header.length() + extHeader.length());
    }

    public void write(DataOutput out) throws Exception {
        setDoff(calculateDataOffset());
        setSize(getFrameSize());
        header.writeTo(out);
        extHeader.writeTo(out);
        performative.write(out);
        payload.writeTo(out);
    }

    public int getFrameSize() {
        int ret = header.length + extHeader.length;
        if (performative != null) {
            ret += performative.size();
        }
        if ( payload != null ) {
            ret += payload.length();
        }
        return ret;
    }

    public org.fusesource.amqp.types.AMQPFrame getPerformative() {
        return performative;
    }

    public void setPerformative(org.fusesource.amqp.types.AMQPFrame performative) {
        this.performative = performative;
        if (this.performative == null) {
            performative = NoPerformative.INSTANCE;
        }
    }

    public Buffer getPayload() {
        return payload;
    }

    public void setPayload(Buffer buffer) {
        this.payload = buffer;
        if (this.payload == null) {
            payload = EMPTY;
        }
    }

    public final void setSize(long size) {
        BitUtils.setUInt(header.data, header.offset+SIZE_OFFSET, size);
    }

    public long getSize() {
        return BitUtils.getUInt(header.data, header.offset+SIZE_OFFSET);
    }

    public final void setType(int type) {
        BitUtils.setUByte(header.data, header.offset+TYPE_OFFSET, (short) type);
    }

    public final int getType() {
        return BitUtils.getUByte(header.data, header.offset+TYPE_OFFSET);
    }

    public final void setDoff(int doff) {
        BitUtils.setUByte(header.data, header.offset+DOFF_OFFSET, (short) doff);
    }

    public final int getDoff() {
        return BitUtils.getUByte(header.data, header.offset+DOFF_OFFSET);
    }

    public final void setChannel(int channel) {
        BitUtils.setUShort(header.data, header.offset+CHANNEL_OFFSET, channel);
    }

    public final int getChannel() {
        return BitUtils.getUShort(header.data, header.offset+CHANNEL_OFFSET);
    }

    public String toString() {
        return toString("");
    }
    public String toString(String indent) {
        if( performative!=null && payload.length()==0 && getType()==0) {
            return performative.toString(indent);
        }
        return "[AMQPTransportFrame, {\n"+
                indent+"   frameSize:"+getFrameSize()+",\n" +
                indent+"   dataOffset:"+getDoff()+",\n" +
                indent+"   channel:"+getChannel()+",\n" +
                indent+"   type:"+getType()+",\n" +
                indent+"   performative:"+performative.toString(indent+"  ")+",\n" +
                indent+"   payload:"+payload+"}]";
    }
}
