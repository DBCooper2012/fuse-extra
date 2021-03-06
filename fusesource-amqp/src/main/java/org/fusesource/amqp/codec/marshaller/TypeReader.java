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

package org.fusesource.amqp.codec.marshaller;

import org.fusesource.amqp.types.AMQPType;
import org.fusesource.amqp.types.AMQPSymbol;
import org.fusesource.amqp.types.AMQPULong;

import java.io.DataInput;
import java.io.EOFException;

/**
 *
 */
public class TypeReader {

    public static byte readFormatCode(DataInput in) throws Exception {
        try {
            return in.readByte();
        } catch (EOFException e) {
            return -1;
        }
    }

    public static AMQPType readDescriptor(DataInput in) throws Exception {
        byte formatCode = readFormatCode(in);
        return readPrimitive(formatCode, in);
    }

    public static AMQPType readPrimitive(byte formatCode, DataInput in) throws Exception {
        if ( checkEOS(formatCode) ) {
            return null;
        }
        AMQPType primitive = (AMQPType) TypeRegistry.instance().getPrimitiveFormatCodeMap().get(formatCode).newInstance();
        if ( primitive != null ) {
            primitive.read(formatCode, in);
        }
        return primitive;
    }

    public static boolean checkEOS(byte formatCode) {
        return formatCode == -1;
    }

    public static Class getDescribedTypeClass(AMQPType descriptor) {
        Class rc = null;
        if ( descriptor instanceof AMQPULong ) {
            rc = TypeRegistry.instance().getFormatCodeMap().get(((AMQPULong) descriptor).getValue());
        } else if ( descriptor instanceof AMQPSymbol ) {
            rc = TypeRegistry.instance().getSymbolicCodeMap().get(((AMQPSymbol) descriptor).getValue());
        } else {
            throw new IllegalArgumentException("Unknown AMQP descriptor type");
        }
        if( rc == null ) {
            TypeRegistry instance = TypeRegistry.instance();
            System.out.println(instance);
            throw new IllegalArgumentException("Unknown AMQP descriptor: "+descriptor);
        }
        return rc;
    }

    public static AMQPType readDescribedType(AMQPType descriptor, DataInput in) throws Exception {
        AMQPType rc = (AMQPType)getDescribedTypeClass(descriptor).newInstance();
        if ( rc != null ) {
            rc.read((byte) 0x0, in);
        }
        return rc;
    }

    public static AMQPType read(DataInput in) throws Exception {
        byte formatCode = readFormatCode(in);
        if ( checkEOS(formatCode) ) {
            return null;
        }
        if ( formatCode == TypeRegistry.DESCRIBED_FORMAT_CODE ) {
            AMQPType descriptor = readDescriptor(in);
            return readDescribedType(descriptor, in);
        } else if ( formatCode == TypeRegistry.NULL_FORMAT_CODE ) {
            return null;
        } else {
            return readPrimitive(formatCode, in);
        }
    }

}
