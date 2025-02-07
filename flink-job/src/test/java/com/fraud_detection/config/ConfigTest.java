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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConfigTest {

  @Test
  public void testParameters() {
    String[] args = new String[] {"--kafka-servers", "host-from-args"};
    Parameters parameters = Parameters.fromArgs(args);
    Config config = Config.fromParameters(parameters);

    final String kafkaServers = config.get(Parameters.KAFKA_SERVERS);
    assertEquals("Wrong config parameter retrived", "host-from-args", kafkaServers);
  }

  @Test
  public void testParameterWithDefaults() {
    String[] args = new String[] {};
    Parameters parameters = Parameters.fromArgs(args);
    Config config = Config.fromParameters(parameters);

    final String kafkaGroupId = config.get(Parameters.KAFKA_GROUP_ID);
    assertEquals("Wrong config parameter retrived", "fraud-detection", kafkaGroupId);
  }
}
