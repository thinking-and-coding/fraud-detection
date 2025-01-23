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
import com.ververica.demo.backend.exceptions.StrategyNotFoundException;
import com.ververica.demo.backend.model.StrategyPayload;
import com.ververica.demo.backend.repositories.StrategyRepository;
import com.ververica.demo.backend.services.FlinkStrategiesService;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class StrategyRestController {

  private final StrategyRepository repository;
  private final FlinkStrategiesService flinkStrategiesService;

  StrategyRestController(StrategyRepository repository, FlinkStrategiesService flinkStrategiesService) {
    this.repository = repository;
    this.flinkStrategiesService = flinkStrategiesService;
  }

  private final ObjectMapper mapper = new ObjectMapper();

  @GetMapping("/strategies")
  List<Strategy> all() {
    return repository.findAll();
  }

  @PostMapping("/strategies")
  Strategy newStrategy(@RequestBody Strategy newStrategy) throws IOException {
    Strategy savedStrategy = repository.save(newStrategy);
    Integer id = savedStrategy.getId();
    StrategyPayload payload = mapper.readValue(savedStrategy.getStrategyPayload(), StrategyPayload.class);
    payload.setStrategyId(id);
    String payloadJson = mapper.writeValueAsString(payload);
    savedStrategy.setStrategyPayload(payloadJson);
    Strategy result = repository.save(savedStrategy);
    flinkStrategiesService.addStrategy(result);
    return result;
  }

  @GetMapping("/strategies/pushToFlink")
  void pushToFlink() {
    List<Strategy> strategies = repository.findAll();
    for (Strategy strategy : strategies) {
      flinkStrategiesService.addStrategy(strategy);
    }
  }

  @GetMapping("/strategies/{id}")
  Strategy one(@PathVariable Integer id) {
    return repository.findById(id).orElseThrow(() -> new StrategyNotFoundException(id));
  }

  @DeleteMapping("/strategies/{id}")
  void deleteStrategy(@PathVariable Integer id) throws JsonProcessingException {
    repository.deleteById(id);
    flinkStrategiesService.deleteStrategy(id);
  }

  @DeleteMapping("/strategies")
  void deleteAll() throws JsonProcessingException {
    List<Strategy> strategies = repository.findAll();
    for (Strategy strategy : strategies) {
      repository.deleteById(strategy.getId());
      flinkStrategiesService.deleteStrategy(strategy.getId());
    }
  }
}
