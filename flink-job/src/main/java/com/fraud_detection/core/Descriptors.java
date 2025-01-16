package com.fraud_detection.core;

import com.fraud_detection.core.entity.Rule;
import com.fraud_detection.core.entity.Transaction;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.OutputTag;

import java.util.Set;

public class Descriptors {

    public static final MapStateDescriptor<Integer, Rule> rulesDescriptor =
            new MapStateDescriptor<>("rules", BasicTypeInfo.INT_TYPE_INFO, TypeInformation.of(Rule.class));

    public static final MapStateDescriptor<Long, Set<Transaction>> windowStateDescriptor =
            new MapStateDescriptor<>(
                    "windowState",
                    BasicTypeInfo.LONG_TYPE_INFO,
                    TypeInformation.of(new TypeHint<Set<Transaction>>() {}));

    public static final OutputTag<String> demoSinkTag = new OutputTag<String>("demo-sink"){

    };

    public static final OutputTag<Long> latencySinkTag = new OutputTag<Long>("latency-sink") {

    };

    public static final OutputTag<Rule> currentRulesSinkTag = new OutputTag<Rule>("current-rules-sink") {

    };
}
