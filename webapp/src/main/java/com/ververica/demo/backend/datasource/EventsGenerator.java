/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.demo.backend.datasource;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class EventsGenerator implements Runnable {

    private static final List<String> EVENT_LIST = new ArrayList<>(Arrays.asList("pay", "refund", "open", "close"));

    private static final List<String> CHARACTOR_LIST = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"));

    private static final List<Map<String, Object>> MAP_LIST = new ArrayList<>(Arrays.asList(new HashMap<String, Object>() {{
        put("totalFare", 5);
        put("amount", 1);
        put("fee", 2.5f);
    }}, new HashMap<String, Object>() {{
        put("totalFare", 10);
        put("amount", 2);
        put("fee", 1.8f);
    }}, new HashMap<String, Object>() {{
        put("totalFare", 20);
        put("amount", 5);
        put("fee", 3.3f);
    }}));

    private final Throttler throttler;

    private volatile boolean running = true;

    private Consumer<Event> consumer;

    public EventsGenerator(Consumer<Event> consumer, int maxRecordsPerSecond) {
        this.consumer = consumer;
        this.throttler = new Throttler(maxRecordsPerSecond);
    }

    public void adjustMaxRecordsPerSecond(long maxRecordsPerSecond) {
        throttler.adjustMaxRecordsPerSecond(maxRecordsPerSecond);
    }

    protected Event randomEvent(SplittableRandom rnd) {
        return Event
                .builder()
                .id(UUID.randomUUID().toString())
                .event(EVENT_LIST.get(rnd.nextInt(EVENT_LIST.size())))
                .accountUuid(CHARACTOR_LIST.get(rnd.nextInt(CHARACTOR_LIST.size())))
                .vtUuid(CHARACTOR_LIST.get(rnd.nextInt(CHARACTOR_LIST.size())))
                .timestamp(System.currentTimeMillis())
                .metadata(MAP_LIST.get(rnd.nextInt(MAP_LIST.size())))
                .build();
    }

    public Event generateOne() {
        return randomEvent(new SplittableRandom());
    }

    @Override
    public final void run() {
        running = true;

        final SplittableRandom rnd = new SplittableRandom();

        while (running) {
            Event event = randomEvent(rnd);
            log.debug("{}", event);
            consumer.accept(event);
            try {
                throttler.throttle();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("Finished run()");
    }

    public final void cancel() {
        running = false;
        log.info("Cancelled");
    }
}
