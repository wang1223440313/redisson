/**
 * Copyright (c) 2013-2024 Nikita Koksharov
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
package org.redisson.client.protocol.decoder;

import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.misc.RedisURI;

import java.util.List;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public class RedisURIDecoder implements MultiDecoder<RedisURI> {

    private final String scheme;

    public RedisURIDecoder(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public Decoder<Object> getDecoder(Codec codec, int paramNum, State state, long size) {
        return StringCodec.INSTANCE.getValueDecoder();
    }
    
    @Override
    public RedisURI decode(List<Object> parts, State state) {
        if (parts.isEmpty()) {
            return null;
        }
        return new RedisURI(scheme, (String) parts.get(0), Integer.valueOf((String) parts.get(1)));
    }

}
