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

import com.fraud_detection.core.entity.Alert;
import com.fraud_detection.core.entity.Event;
import com.fraud_detection.core.entity.Strategy;
import com.fraud_detection.core.functions.DynamicAlertFunction;
import com.fraud_detection.core.functions.DynamicKeyFunction;
import com.fraud_detection.core.util.AssertUtils;
import com.fraud_detection.core.util.BroadcastStreamKeyedOperatorTestHarness;
import com.fraud_detection.core.util.BroadcastStreamNonKeyedOperatorTestHarness;
import com.fraud_detection.core.utils.StrategyParser;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.TestHarnessUtil;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Tests for the {@link Engine}. */
public class EngineTest {

  @Test
  public void shouldProduceKeyedOutput() throws Exception {
    StrategyParser strategyParser = new StrategyParser();
    Strategy strategy1 =
        strategyParser.fromString("1,(active),(pay&refund),(accountUuid&vtUuid),(metadata.totalFare),(SUM),(>),(50),(20)");
    Event event1 = Event.fromString("1,1,pay,1735660800,A,B,{\"totalFare\":21.5}");

    try (BroadcastStreamNonKeyedOperatorTestHarness<
            Event, Strategy, Tuple3<Event, String, Integer>>
        testHarness =
            BroadcastStreamNonKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicKeyFunction(), Descriptors.strategiesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(strategy1, 12L));
      testHarness.processElement1(new StreamRecord<>(event1, 15L));

      Queue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      expectedOutput.add(
          new StreamRecord<>(new Tuple3<>(event1, "{accountUuid=A;vtUuid=B}", 1), 15L));

      TestHarnessUtil.assertOutputEquals(
          "Wrong dynamically keyed output", expectedOutput, testHarness.getOutput());
    }
  }

  @Test
  public void shouldProduceKeyedOutput2() throws Exception {
    StrategyParser strategyParser = new StrategyParser();
    Strategy strategy1 =
            strategyParser.fromString("1,(active),(pay&refund),(accountUuid&metadata.vtUuid),(metadata.totalFare),(SUM),(>),(50),(20)");
    Event event1 = Event.fromString("1,1,pay,1735660800,A,B,{\"totalFare\":21.5,\"vtUuid\":\"B\"}");

    try (BroadcastStreamNonKeyedOperatorTestHarness<
            Event, Strategy, Tuple3<Event, String, Integer>>
                 testHarness =
                 BroadcastStreamNonKeyedOperatorTestHarness.getInitializedTestHarness(
                         new DynamicKeyFunction(), Descriptors.strategiesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(strategy1, 12L));
      testHarness.processElement1(new StreamRecord<>(event1, 15L));

      Queue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      expectedOutput.add(
              new StreamRecord<>(new Tuple3<>(event1, "{accountUuid=A;metadata.vtUuid=B}", 1), 15L));

      TestHarnessUtil.assertOutputEquals(
              "Wrong dynamically keyed output", expectedOutput, testHarness.getOutput());
    }
  }

  @Test
  public void shouldStoreStrategiesInBroadcastStateDuringDynamicKeying() throws Exception {
    StrategyParser strategyParser = new StrategyParser();
    Strategy strategy1 = strategyParser.fromString("1,(active),(pay&refund),(accountUuid),(metadata.totalFare),(SUM),(>),(50),(20)");

    try (BroadcastStreamNonKeyedOperatorTestHarness<
            Event, Strategy, Tuple3<Event, String, Integer>>
        testHarness =
            BroadcastStreamNonKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicKeyFunction(), Descriptors.strategiesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(strategy1, 12L));

      BroadcastState<Integer, Strategy> broadcastState =
          testHarness.getBroadcastState(Descriptors.strategiesDescriptor);

      Map<Integer, Strategy> expectedState = new HashMap<>();
      expectedState.put(strategy1.getStrategyId(), strategy1);

      AssertUtils.assertEquals(broadcastState, expectedState, "Output was not correct.");
    }
  }

  @Test
  public void shouldOutputSimplestAlert() throws Exception {
    StrategyParser strategyParser = new StrategyParser();
    // pay & refund events
    Strategy strategy1 =
        strategyParser.fromString("1,(active),(pay&refund),(accountUuid&event),(metadata.totalFare),(SUM),(>),(20),(20)");

    Event event1 = Event.fromString("1,1,pay,1735660800,A,B,{\"totalFare\":22}");
    Event event2 = Event.fromString("1,2,refund,1735661800,A,B,{\"totalFare\":19}");
    Event event3 = Event.fromString("1,3,refund,1735662800,A,B,{\"totalFare\":2}");

    Tuple3<Event, String, Integer> keyed1 = new Tuple3<>(event1, "{accountUuid=A;event=pay}", 1);
    Tuple3<Event, String, Integer> keyed2 = new Tuple3<>(event2, "{accountUuid=A;event=refund}", 1);
    Tuple3<Event, String, Integer> keyed3 = new Tuple3<>(event3, "{accountUuid=A;event=refund}", 1);

    try (BroadcastStreamKeyedOperatorTestHarness<
            String, Tuple3<Event, String, Integer>, Strategy, Alert>
        testHarness =
            BroadcastStreamKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicAlertFunction(),
                    tuple-> tuple.f1,
                null,
                BasicTypeInfo.STRING_TYPE_INFO,
                Descriptors.strategiesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(strategy1, 12L));

      testHarness.processElement1(new StreamRecord<>(keyed1, 15L));
      testHarness.processElement1(new StreamRecord<>(keyed2, 16L));
      testHarness.processElement1(new StreamRecord<>(keyed3, 17L));

      ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      Alert<Event, BigDecimal> alert1 =
          new Alert<>(strategy1.getStrategyId(), strategy1, "{accountUuid=A;event=pay}", event1, BigDecimal.valueOf(22));
      Alert<Event, BigDecimal> alert2 =
          new Alert<>(strategy1.getStrategyId(), strategy1, "{accountUuid=A;event=refund}", event3, BigDecimal.valueOf(21));

      expectedOutput.add(new StreamRecord<>(alert1, 15L));
      expectedOutput.add(new StreamRecord<>(alert2, 17L));

      TestHarnessUtil.assertOutputEquals(
          "Output was not correct.", expectedOutput, testHarness.getOutput());
    }
  }

  @Test
  public void shouldOutputSimplestPayEventAlert() throws Exception {
    StrategyParser strategyParser = new StrategyParser();
    // pay events
    Strategy strategy1 = strategyParser.fromString("1,(active),(pay),(event),(metadata.totalFare),(SUM),(=),(24),(20)");
    Event event1 = Event.fromString("1,1,pay,1735660800,A,B,{\"totalFare\":22}");
    Event event2 = Event.fromString("1,2,refund,1735661800,A,B,{\"totalFare\":19}");
    Event event3 = Event.fromString("1,3,pay,1735662800,A,B,{\"totalFare\":2}");

    Tuple3<Event, String, Integer> keyed1 = new Tuple3<>(event1, "{event=pay}", 1);
    Tuple3<Event, String, Integer> keyed2 = new Tuple3<>(event2, "{event=refund}", 1);
    Tuple3<Event, String, Integer> keyed3 = new Tuple3<>(event3, "{event=pay}", 1);
    try (BroadcastStreamKeyedOperatorTestHarness<
            String, Tuple3<Event, String, Integer>, Strategy, Alert>
                 testHarness =
                 BroadcastStreamKeyedOperatorTestHarness.getInitializedTestHarness(
                         new DynamicAlertFunction(),
                         tuple-> tuple.f1,
                         null,
                         BasicTypeInfo.STRING_TYPE_INFO,
                         Descriptors.strategiesDescriptor)) {
      testHarness.processElement2(new StreamRecord<>(strategy1, 12L));

      testHarness.processElement1(new StreamRecord<>(keyed1, 15L));
      testHarness.processElement1(new StreamRecord<>(keyed2, 16L));
      testHarness.processElement1(new StreamRecord<>(keyed3, 17L));

      ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      Alert<Event, BigDecimal> alert1 =
              new Alert<>(strategy1.getStrategyId(), strategy1, "{event=pay}", event3, BigDecimal.valueOf(24));

      expectedOutput.add(new StreamRecord<>(alert1, 17L));

      TestHarnessUtil.assertOutputEquals(
              "Output was not correct.", expectedOutput, testHarness.getOutput());
    }
  }

  @Test
  public void shouldHandleSameTimestampEventsCorrectly() throws Exception {
    StrategyParser strategyParser = new StrategyParser();
    Strategy strategy1 =
        strategyParser.fromString("1,(active),(pay&refund),(event),(metadata.totalFare),(SUM),(>),(20),(20)");

    Event event1 = Event.fromString("1,1,pay,1735660800,A,B,{\"totalFare\":19}");

    Event event2 = Event.fromString("1,2,pay,1735660800,B,C,{\"totalFare\":2}");

    Tuple3<Event, String, Integer> keyed1 = new Tuple3<>(event1, "{event=pay}", 1);
    Tuple3<Event, String, Integer> keyed2 = new Tuple3<>(event2, "{event=pay}", 1);

    try (BroadcastStreamKeyedOperatorTestHarness<
            String, Tuple3<Event, String, Integer>, Strategy, Alert>
        testHarness =
            BroadcastStreamKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicAlertFunction(),
                    tuple-> tuple.f1,
                null,
                BasicTypeInfo.STRING_TYPE_INFO,
                Descriptors.strategiesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(strategy1, 12L));

      testHarness.processElement1(new StreamRecord<>(keyed1, 15L));
      testHarness.processElement1(new StreamRecord<>(keyed2, 16L));

      ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      Alert<Event, BigDecimal> alert1 =
          new Alert<>(strategy1.getStrategyId(), strategy1, "{event=pay}", event2, new BigDecimal(21));

      expectedOutput.add(new StreamRecord<>(alert1, 16L));

      TestHarnessUtil.assertOutputEquals(
          "Output was not correct.", expectedOutput, testHarness.getOutput());
    }
  }

  @Test
  public void shouldCleanupStateBasedOnWatermarks() throws Exception {
    StrategyParser strategyParser = new StrategyParser();
    Strategy strategy1 =
        strategyParser.fromString("1,(active),(pay&refund),(paymentType),(metadata.totalFare),(SUM),(>),(10),(4)");

    Event event1 = Event.fromString("1,1,pay,1735660800,A,B,{\"totalFare\":3}");

    Event event2 = Event.fromString("1,2,pay,1735660860,C,D,{\"totalFare\":3}");

    Event event3 = Event.fromString("1,3,pay,1735660920,E,F,{\"totalFare\":5}");

    Event event4 = Event.fromString("1,4,pay,1735661100,G,H,{\"totalFare\":3}");

    Tuple3<Event, String, Integer> keyed1 = new Tuple3<>(event1, "{event=pay}", 1);
    Tuple3<Event, String, Integer> keyed2 = new Tuple3<>(event2, "{event=pay}", 1);
    Tuple3<Event, String, Integer> keyed3 = new Tuple3<>(event3, "{event=pay}", 1);
    Tuple3<Event, String, Integer> keyed4 = new Tuple3<>(event4, "{event=pay}", 1);

    try (BroadcastStreamKeyedOperatorTestHarness<
            String, Tuple3<Event, String, Integer>, Strategy, Alert>
        testHarness =
            BroadcastStreamKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicAlertFunction(),
                    tuple-> tuple.f1,
                null,
                BasicTypeInfo.STRING_TYPE_INFO,
                Descriptors.strategiesDescriptor)) {

      //      long halfAMinuteMillis = 30 * 1000l;
      long watermarkDelay = 2 * 60 * 1000L;

      testHarness.processElement2(new StreamRecord<>(strategy1, 1L));

      testHarness.processElement1(toStreamRecord(keyed1));
      testHarness.watermark(event1.getTimestamp() - watermarkDelay);

      testHarness.processElement1(toStreamRecord(keyed2));
      testHarness.watermark(event2.getTimestamp() - watermarkDelay);

      testHarness.processElement1(toStreamRecord(keyed4));
      testHarness.watermark(event4.getTimestamp() - watermarkDelay);

      // Cleaning up on per-event fixed basis had caused event4 to delete event1 from the state,
      // hence event3 would not have fired. We expect event3 to fire.
      testHarness.processElement1(toStreamRecord(keyed3));

      ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      Alert<Event, BigDecimal> alert1 =
          new Alert<>(strategy1.getStrategyId(), strategy1, "{event=pay}", event3, new BigDecimal(11));

      expectedOutput.add(new StreamRecord<>(alert1, event3.getTimestamp()));

      TestHarnessUtil.assertOutputEquals(
          "Output was not correct.", expectedOutput, filterOutWatermarks(testHarness.getOutput()));
    }
  }

  private StreamRecord<Tuple3<Event, String, Integer>> toStreamRecord(
      Tuple3<Event, String, Integer> tuple) {
    return new StreamRecord<>(tuple, tuple.f0.getTimestamp());
  }

  private Queue<Object> filterOutWatermarks(Queue<Object> in) {
    Queue<Object> out = new LinkedList<>();
    for (Object o : in) {
      if (!(o instanceof Watermark)) {
        out.add(o);
      }
    }
    return out;
  }
}
