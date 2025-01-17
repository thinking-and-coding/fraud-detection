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

import com.fraud_detection.config.Config;
import com.fraud_detection.core.entity.Alert;
import com.fraud_detection.core.entity.Event;
import com.fraud_detection.core.entity.Strategy;
import com.fraud_detection.core.functions.DynamicAlertFunction;
import com.fraud_detection.core.functions.DynamicKeyFunction;
import com.fraud_detection.core.operators.AverageAggregate;
import com.fraud_detection.sinks.AlertsSink;
import com.fraud_detection.sinks.CurrentStrategiesSink;
import com.fraud_detection.sinks.LatencySink;
import com.fraud_detection.sources.StrategiesSource;
import com.fraud_detection.sources.EventsSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;

import static com.fraud_detection.config.Parameters.*;

@Slf4j
public class Engine {

  private final Config config;

  Engine(Config config) {
    this.config = config;
  }

  public static Engine fromConfig(Config config) {
    return new Engine(config);
  }

  public void startDetect() throws Exception {
    // Environment setup
    StreamExecutionEnvironment env = configureStreamExecutionEnvironment();
    // Streams setup
    DataStream<Strategy> strategiesUpdateStream = getStrategiesUpdateStream(env);
    DataStream<Event> events = getEventsStream(env);

    BroadcastStream<Strategy> strategiesStream = strategiesUpdateStream.broadcast(Descriptors.strategiesDescriptor);

    // Processing pipeline setup
    SingleOutputStreamOperator<Alert> alerts =
        events
            .connect(strategiesStream)
            .process(new DynamicKeyFunction())
            .uid("DynamicKeyFunction")
            .name("Dynamic Partitioning Function")
            .keyBy(keyed -> keyed.getKey())
            .connect(strategiesStream)
            .process(new DynamicAlertFunction())
            .uid("DynamicAlertFunction")
            .name("Dynamic Strategy Evaluation Function");

    DataStream<String> allStrategyEvaluations =
        alerts.getSideOutput(Descriptors.demoSinkTag);

    DataStream<Long> latency =
        alerts.getSideOutput(Descriptors.latencySinkTag);

    DataStream<Strategy> currentStrategies =
        alerts.getSideOutput(Descriptors.currentStrategiesSinkTag);

    alerts.print().name("Alert STDOUT Sink");
    allStrategyEvaluations.print().setParallelism(1).name("Strategy Evaluation Sink");

    DataStream<String> alertsJson = AlertsSink.alertsStreamToJson(alerts);
    DataStream<String> currentStrategiesJson = CurrentStrategiesSink.strategiesStreamToJson(currentStrategies);

    currentStrategiesJson.print();

    DataStreamSink<String> alertsSink = AlertsSink.addAlertsSink(config, alertsJson);
    alertsSink.setParallelism(1).name("Alerts JSON Sink");

    DataStreamSink<String> currentStrategiesSink =
        CurrentStrategiesSink.addStrategiesSink(config, currentStrategiesJson);
    currentStrategiesSink.setParallelism(1);

    DataStream<String> latencies =
        latency
            .timeWindowAll(Time.seconds(10))
            .aggregate(new AverageAggregate())
            .map(String::valueOf);

    DataStreamSink<String> latencySink = LatencySink.addLatencySink(config, latencies);
    latencySink.name("Latency Sink");

    env.execute("Fraud Detection Engine");
  }

  private DataStream<Event> getEventsStream(StreamExecutionEnvironment env) {
    // Data stream setup
    int sourceParallelism = config.get(SOURCE_PARALLELISM);
    DataStream<String> eventsStringsStream =
        EventsSource.initEventsSource(config, env)
            .name("Events Source")
            .setParallelism(sourceParallelism);
    DataStream<Event> eventsStream =
        EventsSource.stringsStreamToEvents(eventsStringsStream);
    return eventsStream.assignTimestampsAndWatermarks(WatermarkStrategy
            .<Event>forBoundedOutOfOrderness(Duration.ofMillis(config.get(OUT_OF_ORDERLESS)))
            .withTimestampAssigner((event, timestamp) -> event.getEventTime()));
  }

  private DataStream<Strategy> getStrategiesUpdateStream(StreamExecutionEnvironment env) {
    DataStream<String> strategiesStrings =
        StrategiesSource.initStrategiesSource(config, env)
            .name("Strategies Update Kafka Source")
            .setParallelism(1);
    return StrategiesSource.stringsStreamToStrategies(strategiesStrings);
  }

  private StreamExecutionEnvironment configureStreamExecutionEnvironment() {
    boolean isLocal = config.get(LOCAL_EXECUTION);
    boolean enableCheckpoints = config.get(ENABLE_CHECKPOINTS);
    int checkpointsInterval = config.get(CHECKPOINT_INTERVAL);
    int minPauseBtwnCheckpoints = config.get(CHECKPOINT_INTERVAL);

    Configuration flinkConfig = new Configuration();
    flinkConfig.setBoolean(ConfigConstants.LOCAL_START_WEBSERVER, true);

    StreamExecutionEnvironment env =
        isLocal
            ? StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(flinkConfig)
            : StreamExecutionEnvironment.getExecutionEnvironment();

    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
    env.getCheckpointConfig().setCheckpointInterval(config.get(CHECKPOINT_INTERVAL));
    env.getCheckpointConfig()
        .setMinPauseBetweenCheckpoints(config.get(MIN_PAUSE_BETWEEN_CHECKPOINTS));
    // restartStrategy
    env.setRestartStrategy(RestartStrategies.noRestart());

    if (enableCheckpoints) {
      env.enableCheckpointing(checkpointsInterval);
      env.getCheckpointConfig().setMinPauseBetweenCheckpoints(minPauseBtwnCheckpoints);
    }
    return env;
  }

}
