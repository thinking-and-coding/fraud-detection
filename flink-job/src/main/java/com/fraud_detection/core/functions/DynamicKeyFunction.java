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

package com.fraud_detection.core.functions;


import com.fraud_detection.core.Descriptors;
import com.fraud_detection.core.entity.Event;
import com.fraud_detection.core.entity.Strategy;
import com.fraud_detection.core.entity.enums.ControlType;
import com.fraud_detection.core.entity.enums.StrategyState;
import com.fraud_detection.core.utils.KeysExtractor;
import com.fraud_detection.core.utils.ProcessingUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.*;
import java.util.Map.Entry;

/**
 * Implements dynamic data partitioning based on a set of broadcasted strategies.
 */
@Slf4j
public class DynamicKeyFunction extends BroadcastProcessFunction<Event, Strategy, Tuple3<Event, String, Integer>> {

    private StrategyCounterGauge strategyCounterGauge;

    @Override
    public void open(Configuration parameters) {
        strategyCounterGauge = new StrategyCounterGauge();
        getRuntimeContext().getMetricGroup().gauge("numberOfActiveStrategiesByEvent", strategyCounterGauge);
    }

    @Override
    public void processElement(Event event, ReadOnlyContext ctx, Collector<Tuple3<Event, String, Integer>> out) throws Exception {
        ReadOnlyBroadcastState<Integer, Strategy> strategiesState = ctx.getBroadcastState(Descriptors.strategiesDescriptor);
        forkEventForEachGroupingKey(event, strategiesState, out);
    }

    private void forkEventForEachGroupingKey(Event event, ReadOnlyBroadcastState<Integer, Strategy> strategiesState, Collector<Tuple3<Event, String, Integer>> out) throws Exception {
        List<Integer> strategyIds = new ArrayList<>();
        for (Map.Entry<Integer, Strategy> entry : strategiesState.immutableEntries()) {
            final Strategy strategy = entry.getValue();
            if (strategy.getEvents().contains(event.getEvent())) {
                String key = KeysExtractor.getKey(strategy.getGroupingKeyNames(), event);
                if (StringUtils.isNotBlank(key)) {
                    out.collect(new Tuple3<>(event, key, strategy.getStrategyId()));
                }
                strategyIds.add(strategy.getStrategyId());
            }
        }
        Map<String, List<Integer>> map = new HashMap<>();
        map.put(event.getEvent(), strategyIds);
        strategyCounterGauge.setValue(map);
    }

    @Override
    public void processBroadcastElement(Strategy strategy, Context ctx, Collector<Tuple3<Event, String, Integer>> out) throws Exception {
        log.info("{}", strategy);
        BroadcastState<Integer, Strategy> broadcastState = ctx.getBroadcastState(Descriptors.strategiesDescriptor);
        ProcessingUtils.handleStrategyBroadcast(strategy, broadcastState);
        if (strategy.getStrategyState() == StrategyState.CONTROL) {
            handleControlCommand(strategy.getControlType(), broadcastState);
        }
    }

    private void handleControlCommand(ControlType controlType, BroadcastState<Integer, Strategy> strategiesState) throws Exception {
        switch (controlType) {
            case DELETE_STRATEGIES_ALL:
                Iterator<Entry<Integer, Strategy>> entriesIterator = strategiesState.iterator();
                while (entriesIterator.hasNext()) {
                    Entry<Integer, Strategy> strategyEntry = entriesIterator.next();
                    strategiesState.remove(strategyEntry.getKey());
                    log.info("Removed Strategy {}", strategyEntry.getValue());
                }
                break;
        }
    }

    @Data
    private static class StrategyCounterGauge implements Gauge<Map<String, List<Integer>>> {

        private Map<String, List<Integer>> value = new HashMap<>();

    }
}
