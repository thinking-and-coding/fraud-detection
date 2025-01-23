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

package com.ververica.demo.backend.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ververica.demo.backend.entities.Strategy;
import com.ververica.demo.backend.model.StrategyPayload;
import com.ververica.demo.backend.model.StrategyPayload.ControlType;
import com.ververica.demo.backend.model.StrategyPayload.StrategyState;
import com.ververica.demo.backend.services.FlinkStrategiesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FlinkController {

  private final FlinkStrategiesService flinkStrategiesService;

  // Currently strategies channel is also (mis)used for control messages. This has to do with how control
  // channels are set up in Flink Job.
  FlinkController(FlinkStrategiesService flinkStrategiesService) {
    this.flinkStrategiesService = flinkStrategiesService;
  }

  private final ObjectMapper mapper = new ObjectMapper();

  @GetMapping("/syncStrategies")
  void syncStrategies() throws JsonProcessingException {
    Strategy command = createControlCommand(ControlType.EXPORT_STRATEGIES_CURRENT);
    flinkStrategiesService.addStrategy(command);
  }

  @GetMapping("/clearState")
  void clearState() throws JsonProcessingException {
    Strategy command = createControlCommand(ControlType.CLEAR_STATE_ALL);
    flinkStrategiesService.addStrategy(command);
  }

  private Strategy createControlCommand(ControlType clearStateAll) throws JsonProcessingException {
    StrategyPayload payload = new StrategyPayload();
    payload.setStrategyState(StrategyState.CONTROL);
    payload.setControlType(clearStateAll);
    Strategy strategy = new Strategy();
    strategy.setStrategyPayload(mapper.writeValueAsString(payload));
    return strategy;
  }
}
