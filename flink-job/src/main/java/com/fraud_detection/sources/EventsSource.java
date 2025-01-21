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

package com.fraud_detection.sources;

import static com.fraud_detection.config.Parameters.DATA_TOPIC;
import static com.fraud_detection.config.Parameters.RECORDS_PER_SECOND;
import static com.fraud_detection.config.Parameters.EVENTS_SOURCE;

import com.fraud_detection.config.Config;
import com.fraud_detection.core.entity.Event;
import com.fraud_detection.core.utils.KafkaUtils;
import com.fraud_detection.core.operators.JsonDeserializer;
import com.fraud_detection.sources.generators.JsonGeneratorWrapper;
import com.fraud_detection.core.operators.TimeStamper;
import com.fraud_detection.sources.generators.EventsGenerator;
import java.util.Properties;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class EventsSource {

  public static DataStreamSource<String> initEventsSource(
      Config config, StreamExecutionEnvironment env) {

    String sourceType = config.get(EVENTS_SOURCE);
    EventsSource.Type eventsSourceType =
        EventsSource.Type.valueOf(sourceType.toUpperCase());
    int eventsPerSecond = config.get(RECORDS_PER_SECOND);
    DataStreamSource<String> dataStreamSource;

    switch (eventsSourceType) {
      case KAFKA:
        Properties kafkaProps = KafkaUtils.initConsumerProperties(config);
        String eventsTopic = config.get(DATA_TOPIC);

        // NOTE: Idiomatically, watermarks should be assigned here, but this done later
        // because of the mix of the new Source (Kafka) and SourceFunction-based interfaces.
        // TODO: refactor when FLIP-238 is added

        KafkaSource<String> kafkaSource =
            KafkaSource.<String>builder()
                .setProperties(kafkaProps)
                .setTopics(eventsTopic)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        dataStreamSource =
            env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Events Kafka Source");
        break;
      default:
        JsonGeneratorWrapper<Event> generatorSource =
            new JsonGeneratorWrapper<>(new EventsGenerator(eventsPerSecond));
        dataStreamSource = env.addSource(generatorSource);
    }
    return dataStreamSource;
  }

  public static DataStream<Event> stringsStreamToEvents(
      DataStream<String> eventStrings) {
    return eventStrings
        .flatMap(new JsonDeserializer<>(Event.class))
        .returns(Event.class)
        .flatMap(new TimeStamper<>())
        .returns(Event.class)
        .name("Events Deserialization");
  }

  public enum Type {
    GENERATOR("Events Source (generated locally)"),
    KAFKA("Events Source (Kafka)");

    private String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
