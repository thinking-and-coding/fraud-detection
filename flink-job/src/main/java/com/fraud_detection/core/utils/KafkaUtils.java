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

import com.fraud_detection.config.Config;
import com.fraud_detection.config.Parameters;

import java.util.Properties;
import java.util.UUID;

public class KafkaUtils {

    public static Properties initConsumerProperties(Config config) {
        Properties kafkaProps = initProperties(config);
        String offset = config.get(Parameters.OFFSET);
        kafkaProps.setProperty("auto.offset.reset", offset);
        String groupId = config.get(Parameters.KAFKA_GROUP_ID);
        kafkaProps.setProperty("group.id", groupId);
        Integer maxPollRecords = config.get(Parameters.MAX_POLL_RECORDS);
        kafkaProps.setProperty("max.poll.records", maxPollRecords.toString());
        kafkaProps.setProperty("client.id", "flink-kafka-client-" + UUID.randomUUID());
        return kafkaProps;
    }

    public static Properties initProducerProperties(Config params) {
        return initProperties(params);
    }

    private static Properties initProperties(Config config) {
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", config.get(Parameters.KAFKA_SERVERS));
        return kafkaProps;
    }
}
