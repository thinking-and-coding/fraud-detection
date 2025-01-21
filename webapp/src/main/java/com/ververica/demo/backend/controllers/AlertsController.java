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
import com.ververica.demo.backend.datasource.Event;
import com.ververica.demo.backend.entities.Strategy;
import com.ververica.demo.backend.exceptions.StrategyNotFoundException;
import com.ververica.demo.backend.model.Alert;
import com.ververica.demo.backend.repositories.StrategyRepository;
import com.ververica.demo.backend.services.KafkaEventsPusher;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class AlertsController {

  private final StrategyRepository repository;
  private final KafkaEventsPusher eventsPusher;
  private SimpMessagingTemplate simpSender;

  @Value("${web-socket.topic.alerts}")
  private String alertsWebSocketTopic;

  @Autowired
  public AlertsController(
      StrategyRepository repository,
      KafkaEventsPusher eventsPusher,
      SimpMessagingTemplate simpSender) {
    this.repository = repository;
    this.eventsPusher = eventsPusher;
    this.simpSender = simpSender;
  }

  ObjectMapper mapper = new ObjectMapper();

  @GetMapping("/strategies/{id}/alert")
  Alert mockAlert(@PathVariable Integer id) throws JsonProcessingException {
    Strategy strategy = repository.findById(id).orElseThrow(() -> new StrategyNotFoundException(id));
    Event triggeringEvent = eventsPusher.getLastEvent();
    String violatedStrategy = strategy.getStrategyPayload();
    double randomDouble = ThreadLocalRandom.current().nextDouble(10, 2000);
    BigDecimal triggeringValue = BigDecimal.valueOf(randomDouble);

    Alert alert = new Alert(strategy.getId(), violatedStrategy, triggeringEvent, triggeringValue);

    String result = mapper.writeValueAsString(alert);

    simpSender.convertAndSend(alertsWebSocketTopic, result);

    log.info("alert string:{}", result);
    return alert;
  }
}
