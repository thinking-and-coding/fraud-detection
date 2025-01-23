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

package com.fraud_detection.core.utils;

import com.fraud_detection.core.entity.Strategy;
import com.fraud_detection.core.entity.enums.AggregatorFunctionType;
import com.fraud_detection.core.entity.enums.LimitOperatorType;
import com.fraud_detection.core.entity.enums.StrategyState;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

public class StrategyParser {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public Strategy fromString(String line) throws IOException {
    if (StringUtils.isNotBlank(line) && '{' == line.charAt(0)) {
      return parseJson(line);
    } else {
      return parsePlain(line);
    }
  }

  private Strategy parseJson(String strategyString) throws IOException {
    return objectMapper.readValue(strategyString, Strategy.class);
  }

  private static Strategy parsePlain(String strategyString) throws IOException {
    List<String> tokens = Arrays.asList(strategyString.split(","));
    if (tokens.size() != 9) {
      throw new IOException("Invalid strategy (wrong number of tokens): " + strategyString);
    }

    Iterator<String> iter = tokens.iterator();
    Strategy strategy = new Strategy();

    strategy.setStrategyId(Integer.parseInt(stripBrackets(iter.next())));
    strategy.setStrategyState(StrategyState.valueOf(stripBrackets(iter.next()).toUpperCase()));
    strategy.setEvents(getNames(iter.next()));
    strategy.setGroupingKeyNames(getNames(iter.next()));
    strategy.setAggregateFieldName(stripBrackets(iter.next()));
    strategy.setAggregatorFunctionType(
        AggregatorFunctionType.valueOf(stripBrackets(iter.next()).toUpperCase()));
    strategy.setLimitOperatorType(LimitOperatorType.fromString(stripBrackets(iter.next())));
    strategy.setLimit(new BigDecimal(stripBrackets(iter.next())));
    strategy.setWindowMinutes(Integer.parseInt(stripBrackets(iter.next())));

    return strategy;
  }

  private static String stripBrackets(String expression) {
    return expression.replaceAll("[()]", "");
  }

  private static List<String> getNames(String expression) {
    String keyNamesString = expression.replaceAll("[()]", "");
    if (StringUtils.isNotBlank(keyNamesString)) {
      String[] tokens = keyNamesString.split("&", -1);
      return Arrays.asList(tokens);
    } else {
      return new ArrayList<>();
    }
  }
}
