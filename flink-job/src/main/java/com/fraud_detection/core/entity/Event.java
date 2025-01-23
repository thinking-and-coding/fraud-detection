/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fraud_detection.core.entity;

import com.fraud_detection.core.utils.JsonMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event implements TimestampAssignable<Long> {

    private static JsonMapper<Map> parser = new JsonMapper<>(Map.class);

    public String id;

    public String event;

    public long timestamp;

    public String accountUuid;

    public String vtUuid;

    public Map<String, Object> metadata;

    private long ingestionTimestamp;

    public static Event fromString(String line) {
        List<String> tokens = Arrays.asList(line.split(",",7));
        int numArgs = 7;
        if (tokens.size() != numArgs) {
            throw new RuntimeException("Invalid event: " + line + ". Required number of arguments: " + numArgs + " found " + tokens.size());
        }

        Event event = new Event();

        try {
            Iterator<String> iter = tokens.iterator();
            event.ingestionTimestamp = Long.parseLong(iter.next());
            event.id = iter.next();
            event.event = iter.next();
            event.timestamp = Long.parseLong(iter.next());
            event.accountUuid = iter.next();
            event.vtUuid = iter.next();
            event.metadata = parser.fromString(iter.next());
        } catch (NumberFormatException | IOException nfe) {
            throw new RuntimeException("Invalid record: " + line, nfe);
        }

        return event;
    }

    @Override
    public void assignIngestionTimestamp(Long timestamp) {
        this.ingestionTimestamp = timestamp;
    }
}
