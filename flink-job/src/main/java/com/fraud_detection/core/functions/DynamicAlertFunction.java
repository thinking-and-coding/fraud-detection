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
import com.fraud_detection.core.entity.Alert;
import com.fraud_detection.core.entity.Event;
import com.fraud_detection.core.entity.Strategy;
import com.fraud_detection.core.entity.enums.ControlType;
import com.fraud_detection.core.entity.enums.StrategyState;
import com.fraud_detection.core.utils.AccumulatorHelper;
import com.fraud_detection.core.utils.FieldsExtractor;
import com.fraud_detection.core.utils.ProcessingUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.accumulators.SimpleAccumulator;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import static com.fraud_detection.core.utils.ProcessingUtils.handleStrategyBroadcast;

/**
 * Implements main strategy evaluation and alerting logic.
 */
@Slf4j
public class DynamicAlertFunction extends KeyedBroadcastProcessFunction<String, Tuple3<Event, String, Integer>, Strategy, Alert> {

    private static final String COUNT = "COUNT_FLINK";

    private static final String COUNT_WITH_RESET = "COUNT_WITH_RESET_FLINK";

    private static final int WIDEST_STRATEGY_KEY = Integer.MIN_VALUE;

    private static final int CLEAR_STATE_COMMAND_KEY = Integer.MIN_VALUE + 1;

    private transient MapState<Long, Set<Event>> windowState;

    private Meter alertMeter;

    @Override
    public void open(Configuration parameters) {

        windowState = getRuntimeContext().getMapState(Descriptors.windowStateDescriptor);

        alertMeter = new MeterView(60);
        getRuntimeContext().getMetricGroup().meter("alertsPerMin", alertMeter);
    }

    @Override
    public void processElement(Tuple3<Event, String, Integer> tuple, ReadOnlyContext ctx, Collector<Alert> out) throws Exception {
        Event event = tuple.f0;
        long currentEventTime = event.getTimestamp();

        ProcessingUtils.addToStateValuesSet(windowState, currentEventTime, event);

        long ingestionTime = event.getIngestionTimestamp();
        ctx.output(Descriptors.latencySinkTag, System.currentTimeMillis() - ingestionTime);

        Strategy strategy = ctx.getBroadcastState(Descriptors.strategiesDescriptor).get(tuple.f2);

        if (noStrategyAvailable(strategy)) {
            log.error("Strategy with ID {} does not exist", tuple.f2);
            return;
        }

        if (strategy.getStrategyState() == StrategyState.ACTIVE) {
            Long windowStartForEvent = strategy.getWindowStartFor(currentEventTime);

            long cleanupTime = (currentEventTime / 1000) * 1000;
            ctx.timerService().registerEventTimeTimer(cleanupTime);

            SimpleAccumulator<BigDecimal> aggregator = AccumulatorHelper.getAggregator(strategy.getAggregatorFunctionType());
            for (Long stateEventTime : windowState.keys()) {
                if (isStateValueInWindow(stateEventTime, windowStartForEvent, currentEventTime)) {
                    aggregateValuesInState(stateEventTime, aggregator, strategy);
                }
            }
            BigDecimal aggregateResult = aggregator.getLocalValue();
            boolean strategyResult = strategy.apply(aggregateResult);

            ctx.output(
                Descriptors.demoSinkTag,
                "Engine Strategy "
                    + strategy.getStrategyId()
                    + " | "
                    + tuple.f1
                    + " : "
                    + aggregateResult.toString()
                    + " -> "
                    + strategyResult);

            if (strategyResult) {
                if (COUNT_WITH_RESET.equals(strategy.getAggregateFieldName())) {
                    evictAllStateElements();
                }
                alertMeter.markEvent();
                out.collect(new Alert<>(strategy.getStrategyId(), strategy, tuple.f1, tuple.f0, aggregateResult));
            }
        }
    }

    @Override
    public void processBroadcastElement(Strategy strategy, Context ctx, Collector<Alert> out) throws Exception {
        log.info("{}", strategy);
        BroadcastState<Integer, Strategy> broadcastState = ctx.getBroadcastState(Descriptors.strategiesDescriptor);
        handleStrategyBroadcast(strategy, broadcastState);
        updateWidestWindowStrategy(strategy, broadcastState);
        if (strategy.getStrategyState() == StrategyState.CONTROL) {
            handleControlCommand(strategy, broadcastState, ctx);
        }
    }

    private void handleControlCommand(Strategy command, BroadcastState<Integer, Strategy> strategiesState, Context ctx) throws Exception {
        ControlType controlType = command.getControlType();
        switch (controlType) {
            case EXPORT_STRATEGIES_CURRENT:
                for (Map.Entry<Integer, Strategy> entry : strategiesState.entries()) {
                    ctx.output(Descriptors.currentStrategiesSinkTag, entry.getValue());
                }
                break;
            case CLEAR_STATE_ALL:
                ctx.applyToKeyedState(Descriptors.windowStateDescriptor, (key, state) -> state.clear());
                break;
            case CLEAR_STATE_ALL_STOP:
                strategiesState.remove(CLEAR_STATE_COMMAND_KEY);
                break;
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

    private boolean isStateValueInWindow(Long stateEventTime, Long windowStartForEvent, long currentEventTime) {
        return stateEventTime >= windowStartForEvent && stateEventTime <= currentEventTime;
    }

    private void aggregateValuesInState(Long stateEventTime, SimpleAccumulator<BigDecimal> aggregator, Strategy strategy) throws Exception {
        Set<Event> inWindow = windowState.get(stateEventTime);
        for (Event event : inWindow) {
            if (strategy.getEvents().contains(event.getEvent())) {
                if (COUNT.equals(strategy.getAggregateFieldName()) || COUNT_WITH_RESET.equals(strategy.getAggregateFieldName())) {
                    aggregator.add(BigDecimal.ONE);
                } else {
                    BigDecimal aggregatedValue;
                    if (strategy.getAggregateFieldName().startsWith("metadata.")) {
                        aggregatedValue = FieldsExtractor.getBigDecimalByMapName(event, strategy.getAggregateFieldName());
                    }
                    else{
                        aggregatedValue = FieldsExtractor.getBigDecimalByName(event, strategy.getAggregateFieldName());
                    }
                    aggregator.add(aggregatedValue);
                }
            }
        }
    }

    private boolean noStrategyAvailable(Strategy strategy) {
        // This could happen if the BroadcastState in this CoProcessFunction was updated after it was
        // updated and used in `DynamicKeyFunction`
        if (strategy == null) {
            return true;
        }
        return false;
    }

    private void updateWidestWindowStrategy(Strategy strategy, BroadcastState<Integer, Strategy> broadcastState) throws Exception {
        Strategy widestWindowStrategy = broadcastState.get(WIDEST_STRATEGY_KEY);

        if (strategy.getStrategyState() != StrategyState.ACTIVE) {
            return;
        }

        if (widestWindowStrategy == null) {
            broadcastState.put(WIDEST_STRATEGY_KEY, strategy);
            return;
        }

        if (widestWindowStrategy.getWindowMillis() < strategy.getWindowMillis()) {
            broadcastState.put(WIDEST_STRATEGY_KEY, strategy);
        }
    }

    @Override
    public void onTimer(final long timestamp, final OnTimerContext ctx, final Collector<Alert> out) throws Exception {

        Strategy widestWindowStrategy = ctx.getBroadcastState(Descriptors.strategiesDescriptor).get(WIDEST_STRATEGY_KEY);

        Optional<Long> cleanupEventTimeWindow = Optional.ofNullable(widestWindowStrategy).map(Strategy::getWindowMillis);
        Optional<Long> cleanupEventTimeThreshold = cleanupEventTimeWindow.map(window -> timestamp - window);

        cleanupEventTimeThreshold.ifPresent(this::evictAgedElementsFromWindow);
    }

    private void evictAgedElementsFromWindow(Long threshold) {
        try {
            Iterator<Long> keys = windowState.keys().iterator();
            while (keys.hasNext()) {
                Long stateEventTime = keys.next();
                if (stateEventTime < threshold) {
                    keys.remove();
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void evictAllStateElements() {
        try {
            Iterator<Long> keys = windowState.keys().iterator();
            while (keys.hasNext()) {
                keys.next();
                keys.remove();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
