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

package org.fusesource.amqp.types;

import java.io.DataInput;
import java.io.DataOutput;

/**
 *
 */
public class NoPerformative implements AMQPFrame {

    public static final NoPerformative INSTANCE = new NoPerformative();

    public Object getArrayConstructor() {
        return null;
    }

    public void write(DataOutput out) throws Exception {
    }

    public byte writeConstructor(DataOutput out) throws Exception {
        return 0;
    }

    public void writeBody(byte formatCode, DataOutput out) throws Exception {

    }

    public void read(byte formatCode, DataInput in) throws Exception {

    }

    public long size() {
        return 0;
    }

    public long sizeOfConstructor() {
        return 0;
    }

    public long sizeOfBody() {
        return 0;
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    public String toString(String indent) {
        return toString();
    }
}
