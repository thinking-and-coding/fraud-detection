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

package com.fraud_detection.sources.generators;

import com.fraud_detection.core.entity.Event;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class EventsGenerator extends BaseGenerator<Event> {

    private static final List<String> EVENT_LIST = new ArrayList<>(Arrays.asList("pay", "refund", "open", "close"));

    private static final List<String> CHARACTOR_LIST = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"));

    public EventsGenerator(int maxRecordsPerSecond) {
        super(maxRecordsPerSecond);
    }

    @Override
    public Event randomEvent(SplittableRandom rnd, long id) {
        return Event
                .builder()
                .id(UUID.randomUUID().toString())
                .event(EVENT_LIST.get(rnd.nextInt(EVENT_LIST.size())))
                .accountUuid(CHARACTOR_LIST.get(rnd.nextInt(CHARACTOR_LIST.size())))
                .vtUuid(CHARACTOR_LIST.get(rnd.nextInt(CHARACTOR_LIST.size())))
                .timestamp(System.currentTimeMillis())
                .ingestionTimestamp(System.currentTimeMillis())
                .build();
    }

}
