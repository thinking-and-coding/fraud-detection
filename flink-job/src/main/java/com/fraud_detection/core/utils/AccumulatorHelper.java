package com.fraud_detection.core.utils;

import com.fraud_detection.core.entity.enums.AggregatorFunctionType;
import com.fraud_detection.core.operators.accumulators.AverageAccumulator;
import com.fraud_detection.core.operators.accumulators.BigDecimalCounter;
import com.fraud_detection.core.operators.accumulators.BigDecimalMaximum;
import com.fraud_detection.core.operators.accumulators.BigDecimalMinimum;
import org.apache.flink.api.common.accumulators.SimpleAccumulator;

import java.math.BigDecimal;

public class AccumulatorHelper {

    /* Picks and returns a new accumulator, based on the Rule's aggregator function type. */
    public static SimpleAccumulator<BigDecimal> getAggregator(AggregatorFunctionType aggregatorFunctionType) {
        switch (aggregatorFunctionType) {
            case SUM:
                return new BigDecimalCounter();
            case AVG:
                return new AverageAccumulator();
            case MAX:
                return new BigDecimalMaximum();
            case MIN:
                return new BigDecimalMinimum();
            default:
                throw new RuntimeException("Unsupported aggregation function type: " + aggregatorFunctionType);
        }
    }
}
