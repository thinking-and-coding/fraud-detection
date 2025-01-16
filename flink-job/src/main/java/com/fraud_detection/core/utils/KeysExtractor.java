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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.List;

/** Utilities for dynamic keys extraction by field name. */
@Slf4j
public class KeysExtractor {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Extracts and concatenates field values by names.
   *
   * @param keyNames list of field names
   * @param object target for values extraction
   */
  public static String getKey(List<String> keyNames, Object object) throws JsonProcessingException {
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      if (CollectionUtils.isNotEmpty(keyNames)) {
        Iterator<String> it = keyNames.iterator();
        appendKeyValue(sb, object, it.next());

        while (it.hasNext()) {
          sb.append(";");
          appendKeyValue(sb, object, it.next());
        }
      }
      sb.append("}");
      return sb.toString();
    } catch (IllegalAccessException | NoSuchFieldException e) {
      log.error("Can't extract keyNames {} from {}. exception:", objectMapper.writeValueAsString(keyNames), objectMapper.writeValueAsString(object), e);
    }
    return StringUtils.EMPTY;
  }

  private static void appendKeyValue(StringBuilder sb, Object object, String fieldName)
      throws IllegalAccessException, NoSuchFieldException {
    sb.append(fieldName);
    sb.append("=");
    sb.append(FieldsExtractor.getFieldAsString(object, fieldName));
  }
}
