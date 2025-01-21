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

import com.ververica.demo.backend.entities.Strategy;
import com.ververica.demo.backend.repositories.StrategyRepository;
import com.ververica.demo.backend.services.FlinkStrategiesService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StrategiesBootstrapper implements ApplicationRunner {

  private StrategyRepository strategyRepository;
  private FlinkStrategiesService flinkStrategiesService;

  @Autowired
  public StrategiesBootstrapper(StrategyRepository strategyRepository, FlinkStrategiesService flinkStrategiesService) {
    this.strategyRepository = strategyRepository;
    this.flinkStrategiesService = flinkStrategiesService;
  }

  public void run(ApplicationArguments args) {
    String payload1 =
        "{\"strategyId\":\"1\","
            + "\"aggregateFieldName\":\"metadata.totalFare\","
            + "\"aggregatorFunctionType\":\"SUM\","
            + "\"groupingKeyNames\":[\"accountUuid\", \"vtUuid\"],"
            + "\"limit\":\"200\","
            + "\"limitOperatorType\":\"GREATER\","
            + "\"strategyState\":\"ACTIVE\","
            + "\"events\":[\"pay\",\"refund\"],"
            + "\"windowMinutes\":\"43200\"}";

    Strategy strategy1 = new Strategy(payload1);

    String payload2 =
        "{\"strategyId\":\"2\","
            + "\"aggregateFieldName\":\"COUNT_FLINK\","
            + "\"aggregatorFunctionType\":\"SUM\","
            + "\"groupingKeyNames\":[\"accountUuid\"],"
            + "\"limit\":\"300\","
            + "\"limitOperatorType\":\"LESS\","
            + "\"strategyState\":\"PAUSE\","
            + "\"events\":[\"pay\",\"refund\"],"
            + "\"windowMinutes\":\"1440\"}";

    Strategy strategy2 = new Strategy(payload2);

    String payload3 =
        "{\"strategyId\":\"3\","
            + "\"aggregateFieldName\":\"metadata.amount\","
            + "\"aggregatorFunctionType\":\"SUM\","
            + "\"groupingKeyNames\":[\"vtUuid\"],"
            + "\"limit\":\"100\","
            + "\"limitOperatorType\":\"GREATER_EQUAL\","
            + "\"strategyState\":\"ACTIVE\","
            + "\"events\":[\"pay\"],"
            + "\"windowMinutes\":\"1440\"}";

    Strategy strategy3 = new Strategy(payload3);

    String payload4 =
        "{\"strategyId\":\"4\","
            + "\"aggregateFieldName\":\"COUNT_WITH_RESET_FLINK\","
            + "\"aggregatorFunctionType\":\"SUM\","
            + "\"groupingKeyNames\":[\"accountUuid\"],"
            + "\"limit\":\"100\","
            + "\"limitOperatorType\":\"GREATER_EQUAL\","
            + "\"strategyState\":\"ACTIVE\","
            + "\"events\":[\"pay\",\"refund\",\"open\",\"close\"],"
            + "\"windowMinutes\":\"1440\"}";

    Strategy strategy4 = new Strategy(payload4);

    strategyRepository.save(strategy1);
    strategyRepository.save(strategy2);
    strategyRepository.save(strategy3);
    strategyRepository.save(strategy4);

    List<Strategy> strategies = strategyRepository.findAll();
    strategies.forEach(strategy -> flinkStrategiesService.addStrategy(strategy));
  }
}
