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

package com.fraud_detection.config;

import org.apache.flink.api.java.utils.ParameterTool;

import java.util.Arrays;
import java.util.List;

public class Parameters {

    private final ParameterTool tool;

    public Parameters(ParameterTool tool) {
        this.tool = tool;
    }

    <T> T getOrDefault(Param<T> param) {
        if (!tool.has(param.getName())) {
            return param.getDefaultValue();
        }
        Object value;
        if (param.getType() == Integer.class) {
            value = tool.getInt(param.getName());
        } else if (param.getType() == Long.class) {
            value = tool.getLong(param.getName());
        } else if (param.getType() == Double.class) {
            value = tool.getDouble(param.getName());
        } else if (param.getType() == Boolean.class) {
            value = tool.getBoolean(param.getName());
        } else {
            value = tool.get(param.getName());
        }
        return param.getType().cast(value);
    }

    public static Parameters fromArgs(String[] args) {
        ParameterTool tool = ParameterTool.fromArgs(args);
        return new Parameters(tool);
    }

    // Kafka:
    public static final Param<String> KAFKA_SERVERS = Param.string("kafka-servers", "localhost:9092");

    public static final Param<String> DATA_TOPIC = Param.string("data-topic", "live-events");

    public static final Param<String> KAFKA_GROUP_ID = Param.string("group-id", "fraud-detection");

    public static final Param<String> ALERTS_TOPIC = Param.string("alerts-topic", "alerts");

    public static final Param<String> STRATEGIES_TOPIC = Param.string("strategies-topic", "strategies");

    public static final Param<String> LATENCY_TOPIC = Param.string("latency-topic", "latency");

    public static final Param<String> STRATEGIES_EXPORT_TOPIC = Param.string("current-strategies-topic", "current-strategies");

    public static final Param<String> OFFSET = Param.string("offset", "latest");

    // General:
    public static final Param<String> EVENTS_SOURCE = Param.string("data-source", "GENERATOR");
    public static final Param<String> ALERTS_SINK = Param.string("alerts-sink", "STDOUT");
    public static final Param<String> LATENCY_SINK = Param.string("latency-sink", "STDOUT");
    public static final Param<String> STRATEGIES_EXPORT_SINK = Param.string("strategies-export-sink", "STDOUT");
    public static final Param<String> ERROR_STRATEGIES_SINK = Param.string("error_strategies-sink", "STDOUT");

    public static final Param<Integer> RECORDS_PER_SECOND = Param.integer("records-per-second", 2);

    public static final Param<Boolean> LOCAL_EXECUTION = Param.bool("local", false);

    public static final Param<Integer> SOURCE_PARALLELISM = Param.integer("source-parallelism", 2);

    public static final Param<Boolean> ENABLE_CHECKPOINTS = Param.bool("checkpoints", false);

    public static final Param<Integer> CHECKPOINT_INTERVAL = Param.integer("checkpoint-interval", 60_000_0);
    public static final Param<Integer> MIN_PAUSE_BETWEEN_CHECKPOINTS = Param.integer("min-pause-btwn-checkpoints", 10000);
    public static final Param<Integer> OUT_OF_ORDERLESS = Param.integer("out-of-orderless", 500);
    public static final Param<Integer> MAX_POLL_RECORDS = Param.integer("max-poll-records", 100);


    public static final List<Param<String>> STRING_PARAMS = Arrays.asList(
            KAFKA_SERVERS,
            KAFKA_GROUP_ID,
            DATA_TOPIC,
            ALERTS_TOPIC,
            STRATEGIES_TOPIC,
            LATENCY_TOPIC,
            STRATEGIES_EXPORT_TOPIC,
            OFFSET,
            EVENTS_SOURCE,
            ALERTS_SINK,
            LATENCY_SINK,
            STRATEGIES_EXPORT_SINK,
            ERROR_STRATEGIES_SINK
    );

    public static final List<Param<Integer>> INT_PARAMS = Arrays.asList(
            RECORDS_PER_SECOND,
            SOURCE_PARALLELISM,
            CHECKPOINT_INTERVAL,
            MIN_PAUSE_BETWEEN_CHECKPOINTS,
            OUT_OF_ORDERLESS,
            MAX_POLL_RECORDS
    );

    public static final List<Param<Boolean>> BOOL_PARAMS = Arrays.asList(
            LOCAL_EXECUTION,
            ENABLE_CHECKPOINTS
    );
}
