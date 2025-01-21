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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

public class FieldsExtractor {

    public static String getFieldAsString(Object object, String fieldName) throws IllegalAccessException, NoSuchFieldException {
        Class<?> cls = object.getClass();
        Field field = cls.getField(fieldName);
        return field.get(object).toString();
    }

    public static BigDecimal getBigDecimalByName(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        return new BigDecimal(getFieldAsString(object, fieldName));
    }

    public static String getMapFieldAsString(Object object, String mapFieldName) throws IllegalAccessException, NoSuchFieldException {
        Class<?> cls = object.getClass();
        // 使用反射获取 metadata 字段
        String[] fieldNames = mapFieldName.split("\\.");
        Field metadataField = cls.getDeclaredField(fieldNames[0]);
        metadataField.setAccessible(true);

        // 获取 metadata 的值并强制转换为 Map
        Map<String, Object> metadata = (Map<String, Object>) metadataField.get(object);

        // 从 metadata 中获取指定属性值
        Object mapValue = metadata.get(fieldNames[1]);
        return mapValue.toString();
    }

  public static BigDecimal getBigDecimalByMapName(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
    return new BigDecimal(getMapFieldAsString(object, fieldName));
  }

    @SuppressWarnings("unchecked")
    public static <T> T getByKeyAs(String keyName, Object object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getField(keyName);
        return (T) field.get(object);
    }
}
