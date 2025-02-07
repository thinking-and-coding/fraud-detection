package com.fraud_detection.core;

import com.fraud_detection.core.entity.Event;
import com.fraud_detection.core.entity.Strategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.OutputTag;

import java.util.Set;

public class Descriptors {

    public static final MapStateDescriptor<Integer, Strategy> strategiesDescriptor =
            new MapStateDescriptor<>("strategies", BasicTypeInfo.INT_TYPE_INFO, TypeInformation.of(Strategy.class));

    public static final MapStateDescriptor<Long, Set<Event>> windowStateDescriptor =
            new MapStateDescriptor<>(
                    "windowState",
                    BasicTypeInfo.LONG_TYPE_INFO,
                    TypeInformation.of(new TypeHint<Set<Event>>() {}));

    public static final OutputTag<String> evaluateSinkTag = new OutputTag<String>("evaluate-sink") {
    };

    public static final OutputTag<Long> latencySinkTag = new OutputTag<Long>("latency-sink") {
    };

    public static final OutputTag<Strategy> currentStrategiesSinkTag = new OutputTag<Strategy>("current-strategies-sink") {
    };

    public static final OutputTag<String> errorStrategiesSinkTag = new OutputTag<String>("error-strategies-sink") {
    };
}
