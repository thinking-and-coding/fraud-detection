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

import com.ververica.demo.backend.datasource.DemoEventsGenerator;
import com.ververica.demo.backend.datasource.EventsGenerator;
import com.ververica.demo.backend.services.KafkaEventsPusher;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class DataGenerationController {

  private EventsGenerator eventsGenerator;
  private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private boolean generatingEvents = false;
  private boolean listenerContainerRunning = true;

  @Value("${kafka.listeners.events.id}")
  private String eventsListenerId;

  @Value("${eventsRateDisplayLimit}")
  private int eventsRateDisplayLimit;

  @Autowired
  public DataGenerationController(
      KafkaEventsPusher eventsPusher,
      KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
    eventsGenerator = new DemoEventsGenerator(eventsPusher, 5);
    this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
  }

  @GetMapping("/api/startEventsGeneration")
  public void startEventsGeneration() throws Exception {
    log.info("{}", "startEventsGeneration called");
    generateEvents();
  }

  private void generateEvents() {
    if (!generatingEvents) {
      executor.submit(eventsGenerator);
      generatingEvents = true;
    }
  }

  @GetMapping("/api/stopEventsGeneration")
  public void stopEventsGeneration() {
    eventsGenerator.cancel();
    generatingEvents = false;
    log.info("{}", "stopEventsGeneration called");
  }

  @GetMapping("/api/generatorSpeed/{speed}")
  public void setGeneratorSpeed(@PathVariable Long speed) {
    log.info("Generator speed change request: " + speed);
    if (speed <= 0) {
      eventsGenerator.cancel();
      generatingEvents = false;
      return;
    } else {
      generateEvents();
    }

    MessageListenerContainer listenerContainer =
        kafkaListenerEndpointRegistry.getListenerContainer(eventsListenerId);
    if (speed > eventsRateDisplayLimit) {
      listenerContainer.stop();
      listenerContainerRunning = false;
    } else if (!listenerContainerRunning) {
      listenerContainer.start();
    }

    if (eventsGenerator != null) {
      eventsGenerator.adjustMaxRecordsPerSecond(speed);
    }
  }
}
