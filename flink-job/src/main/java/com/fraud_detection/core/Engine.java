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
import com.fraud_detection.core.entity.Rule;
import com.fraud_detection.core.entity.Transaction;
import com.fraud_detection.core.operators.AverageAggregate;
import com.fraud_detection.core.functions.DynamicAlertFunction;
import com.fraud_detection.core.functions.DynamicKeyFunction;
import com.fraud_detection.sinks.AlertsSink;
import com.fraud_detection.sinks.CurrentRulesSink;
import com.fraud_detection.sinks.LatencySink;
import com.fraud_detection.sources.RulesSource;
import com.fraud_detection.sources.TransactionsSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.OutputTag;

import java.util.concurrent.TimeUnit;

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

    boolean isLocal = config.get(LOCAL_EXECUTION);
    boolean enableCheckpoints = config.get(ENABLE_CHECKPOINTS);
    int checkpointsInterval = config.get(CHECKPOINT_INTERVAL);
    int minPauseBtwnCheckpoints = config.get(CHECKPOINT_INTERVAL);

    // Environment setup
    StreamExecutionEnvironment env = configureStreamExecutionEnvironment(isLocal);

    if (enableCheckpoints) {
      env.enableCheckpointing(checkpointsInterval);
      env.getCheckpointConfig().setMinPauseBetweenCheckpoints(minPauseBtwnCheckpoints);
    }

    // Streams setup
    DataStream<Rule> rulesUpdateStream = getRulesUpdateStream(env);
    DataStream<Transaction> transactions = getTransactionsStream(env);

    BroadcastStream<Rule> rulesStream = rulesUpdateStream.broadcast(Descriptors.rulesDescriptor);

    // Processing pipeline setup
    DataStream<Alert> alerts =
        transactions
            .connect(rulesStream)
            .process(new DynamicKeyFunction())
            .uid("DynamicKeyFunction")
            .name("Dynamic Partitioning Function")
            .keyBy(keyed -> keyed.getKey())
            .connect(rulesStream)
            .process(new DynamicAlertFunction())
            .uid("DynamicAlertFunction")
            .name("Dynamic Rule Evaluation Function");

    DataStream<String> allRuleEvaluations =
        ((SingleOutputStreamOperator<Alert>) alerts).getSideOutput(Descriptors.demoSinkTag);

    DataStream<Long> latency =
        ((SingleOutputStreamOperator<Alert>) alerts).getSideOutput(Descriptors.latencySinkTag);

    DataStream<Rule> currentRules =
        ((SingleOutputStreamOperator<Alert>) alerts).getSideOutput(Descriptors.currentRulesSinkTag);

    alerts.print().name("Alert STDOUT Sink");
    allRuleEvaluations.print().setParallelism(1).name("Rule Evaluation Sink");

    DataStream<String> alertsJson = AlertsSink.alertsStreamToJson(alerts);
    DataStream<String> currentRulesJson = CurrentRulesSink.rulesStreamToJson(currentRules);

    currentRulesJson.print();

    DataStreamSink<String> alertsSink = AlertsSink.addAlertsSink(config, alertsJson);
    alertsSink.setParallelism(1).name("Alerts JSON Sink");

    DataStreamSink<String> currentRulesSink =
        CurrentRulesSink.addRulesSink(config, currentRulesJson);
    currentRulesSink.setParallelism(1);

    DataStream<String> latencies =
        latency
            .timeWindowAll(Time.seconds(10))
            .aggregate(new AverageAggregate())
            .map(String::valueOf);

    DataStreamSink<String> latencySink = LatencySink.addLatencySink(config, latencies);
    latencySink.name("Latency Sink");

    env.execute("Fraud Detection Engine");
  }

  private DataStream<Transaction> getTransactionsStream(StreamExecutionEnvironment env) {
    // Data stream setup
    int sourceParallelism = config.get(SOURCE_PARALLELISM);
    DataStream<String> transactionsStringsStream =
        TransactionsSource.initTransactionsSource(config, env)
            .name("Transactions Source")
            .setParallelism(sourceParallelism);
    DataStream<Transaction> transactionsStream =
        TransactionsSource.stringsStreamToTransactions(transactionsStringsStream);
    return transactionsStream.assignTimestampsAndWatermarks(
        new SimpleBoundedOutOfOrdernessTimestampExtractor<>(config.get(OUT_OF_ORDERNESS)));
  }

  private DataStream<Rule> getRulesUpdateStream(StreamExecutionEnvironment env) {
    DataStream<String> rulesStrings =
        RulesSource.initRulesSource(config, env)
            .name("Rules Update Kafka Source")
            .setParallelism(1);
    return RulesSource.stringsStreamToRules(rulesStrings);
  }

  private StreamExecutionEnvironment configureStreamExecutionEnvironment(boolean isLocal) {
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

    configureRestartStrategy(env);
    return env;
  }

  private static class SimpleBoundedOutOfOrdernessTimestampExtractor<T extends Transaction>
      extends BoundedOutOfOrdernessTimestampExtractor<T> {

    public SimpleBoundedOutOfOrdernessTimestampExtractor(int outOfOrderdnessMillis) {
      super(Time.of(outOfOrderdnessMillis, TimeUnit.MILLISECONDS));
    }

    @Override
    public long extractTimestamp(T element) {
      return element.getEventTime();
    }
  }

  private void configureRestartStrategy(StreamExecutionEnvironment env) {
    env.setRestartStrategy(RestartStrategies.noRestart());
  }

  public static class Descriptors {
    public static final MapStateDescriptor<Integer, Rule> rulesDescriptor =
        new MapStateDescriptor<>(
            "rules", BasicTypeInfo.INT_TYPE_INFO, TypeInformation.of(Rule.class));

    public static final OutputTag<String> demoSinkTag = new OutputTag<String>("demo-sink") {};
    public static final OutputTag<Long> latencySinkTag = new OutputTag<Long>("latency-sink") {};
    public static final OutputTag<Rule> currentRulesSinkTag =
        new OutputTag<Rule>("current-rules-sink") {};
  }
}
