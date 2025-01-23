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

import com.fraud_detection.core.entity.enums.AggregatorFunctionType;
import com.fraud_detection.core.entity.enums.ControlType;
import com.fraud_detection.core.entity.enums.LimitOperatorType;
import com.fraud_detection.core.entity.enums.StrategyState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.flink.api.common.time.Time;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strategies representation.
 */
@EqualsAndHashCode
@ToString
@Data
public class Strategy {

    private Integer strategyId;

    private StrategyState strategyState;

    private List<String> events;

    private List<String> groupingKeyNames; // aggregation

    private String aggregateFieldName;

    private AggregatorFunctionType aggregatorFunctionType;

    private LimitOperatorType limitOperatorType;

    private BigDecimal limit;

    private Integer windowMinutes;

    private ControlType controlType;

    public Long getWindowMillis() {
        return Time.minutes(this.windowMinutes).toMilliseconds();
    }

    /**
     * Evaluates this strategy by comparing provided value with strategies' limit based on limit operator type.
     *
     * @param comparisonValue value to be compared with the limit
     */
    public boolean apply(BigDecimal comparisonValue) {
        switch (this.limitOperatorType) {
            case EQUAL:
                return comparisonValue.compareTo(limit) == 0;
            case NOT_EQUAL:
                return comparisonValue.compareTo(limit) != 0;
            case GREATER:
                return comparisonValue.compareTo(limit) > 0;
            case LESS:
                return comparisonValue.compareTo(limit) < 0;
            case LESS_EQUAL:
                return comparisonValue.compareTo(limit) <= 0;
            case GREATER_EQUAL:
                return comparisonValue.compareTo(limit) >= 0;
            default:
                throw new RuntimeException("Unknown limit operator type: " + this.limitOperatorType);
        }
    }

    public long getWindowStartFor(Long timestamp) {
        Long strategyWindowMillis = this.getWindowMillis();
        return (timestamp - strategyWindowMillis);
    }

}
