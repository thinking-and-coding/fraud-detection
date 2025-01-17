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

package com.fraud_detection.core;

import static org.junit.Assert.assertEquals;

import com.fraud_detection.core.entity.Strategy;
import com.fraud_detection.core.entity.enums.AggregatorFunctionType;
import com.fraud_detection.core.entity.enums.LimitOperatorType;
import com.fraud_detection.core.entity.enums.StrategyState;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.fraud_detection.core.utils.StrategyParser;
import org.junit.Test;

public class StrategyParserTest {

  @SafeVarargs
  public static <T> List<T> lst(T... ts) {
    return Arrays.asList(ts);
  }

  @Test
  public void testStrategyParsedPlain() throws Exception {
    String strategyString1 = "1,(active),(pay&refund),(taxiId&driverId),(totalFare),(sum),(>),(5),(20)";

    StrategyParser strategyParser = new StrategyParser();
    Strategy strategy1 = strategyParser.fromString(strategyString1);

    assertEquals("ID incorrect", 1, (int) strategy1.getStrategyId());
    assertEquals("Strategy state incorrect", StrategyState.ACTIVE, strategy1.getStrategyState());
    assertEquals("Event names incorrect", lst("pay", "refund"), strategy1.getEvents());
    assertEquals("Key names incorrect", lst("taxiId", "driverId"), strategy1.getGroupingKeyNames());
    assertEquals("Cumulative key incorrect", "totalFare", strategy1.getAggregateFieldName());
    assertEquals(
        "Aggregator function incorrect",
        AggregatorFunctionType.SUM,
        strategy1.getAggregatorFunctionType());
    assertEquals(
        "Limit operator incorrect", LimitOperatorType.GREATER, strategy1.getLimitOperatorType());
    assertEquals("Limit incorrect", BigDecimal.valueOf(5), strategy1.getLimit());
    assertEquals("Window incorrect", 20, (int) strategy1.getWindowMinutes());
  }

  @Test
  public void testStrategyParsedJson() throws Exception {
    String strategyString1 =
        "{\n"
            + "  \"strategyId\": 1,\n"
            + "  \"strategyState\": \"ACTIVE\",\n"
            + "  \"events\": [\"pay\", \"refund\"],\n"
            + "  \"groupingKeyNames\": [\"taxiId\", \"driverId\"],\n"
            + "  \"aggregateFieldName\": \"totalFare\",\n"
            + "  \"aggregatorFunctionType\": \"SUM\",\n"
            + "  \"limitOperatorType\": \"GREATER\",\n"
            + "  \"limit\": 50,\n"
            + "  \"windowMinutes\": 20\n"
            + "}";

    StrategyParser strategyParser = new StrategyParser();
    Strategy strategy1 = strategyParser.fromString(strategyString1);

    assertEquals("ID incorrect", 1, (int) strategy1.getStrategyId());
    assertEquals("Strategy state incorrect", StrategyState.ACTIVE, strategy1.getStrategyState());
    assertEquals("Event names incorrect", lst("pay", "refund"), strategy1.getEvents());
    assertEquals("Key names incorrect", lst("taxiId", "driverId"), strategy1.getGroupingKeyNames());
    assertEquals("Cumulative key incorrect", "totalFare", strategy1.getAggregateFieldName());
    assertEquals(
        "Aggregator function incorrect",
        AggregatorFunctionType.SUM,
        strategy1.getAggregatorFunctionType());
    assertEquals(
        "Limit operator incorrect", LimitOperatorType.GREATER, strategy1.getLimitOperatorType());
    assertEquals("Limit incorrect", BigDecimal.valueOf(50), strategy1.getLimit());
    assertEquals("Window incorrect", 20, (int) strategy1.getWindowMinutes());
  }
}
