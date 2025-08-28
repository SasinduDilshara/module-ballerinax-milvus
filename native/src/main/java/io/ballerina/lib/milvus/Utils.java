/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.milvus;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.ballerina.lib.milvus.ModuleUtils.getModule;

public class Utils {
    private static final String ERROR_TYPE = "Error";
    private static final String VECTORS = "vectors";
    private Utils() {
    }

    public static BError createError(String message, Throwable throwable) {
        BError cause = Objects.isNull(throwable) ? null : ErrorCreator.createError(throwable);
        return ErrorCreator.createError(getModule(), ERROR_TYPE, StringUtils.fromString(message), cause, null);
    }

    static void applyDynamicFields(BMap<?, ?> data, Gson gson, JsonObject row) {
        Object[] keys = data.getKeys();
        for (Object keyObj : keys) {
            String key = (keyObj instanceof BString) ? ((BString) keyObj).getValue() : String.valueOf(keyObj);
            Object val = data.get(StringUtils.fromString(key));
            row.add(key, gson.toJsonTree(convertToJsonFields(val)));
        }
    }

    private static Object convertToJsonFields(Object value) {
        if (value instanceof BString stringValue) {
            return stringValue.getValue();
        }
        if (value instanceof BArray arr) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                list.add(convertToJsonFields(arr.get(i)));
            }
            return list;
        }
        if (value instanceof BMap<?, ?> map) {
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            Object[] mapKeys = map.getKeys();
            for (Object keyObj : mapKeys) {
                jsonMap.put(keyObj.toString(), convertToJsonFields(map.get(keyObj)));
            }
            return jsonMap;
        }
        return value;
    }
    protected static void generateQueryResult(List<QueryResp.QueryResult> results,
                                              BMap<BString, Object>[] responses) {
        for (QueryResp.QueryResult result : results) {
            BMap<BString, Object> response =
                    ValueCreator.createRecordValue(ModuleUtils.getModule(), QUERY_RESULT);
            getResult(result, response);
            responses[results.indexOf(result)] = response;
        }
    }


    private static void getResult(QueryResp.QueryResult result, BMap<BString, Object> entity) {
        for (String key: result.getEntity().keySet()) {
            if (result.getEntity().get(key) instanceof List<?> list) {
                if (!list.isEmpty() && list.get(0) instanceof Number) {
                    double[] values = list.stream()
                            .mapToDouble(value -> ((Number) value).floatValue())
                            .toArray();
                    BArray floatArray = ValueCreator.createArrayValue(values);
                    entity.put(StringUtils.fromString(key), floatArray);
                } else {
                    BString[] values = list.stream()
                            .map(value -> StringUtils.fromString(value.toString()))
                            .toArray(BString[]::new);
                    BArray stringArray = ValueCreator.createArrayValue(values);
                    entity.put(StringUtils.fromString(key), stringArray);
                }
            } else {
                entity.put(StringUtils.fromString(key),
                        StringUtils.fromString(result.getEntity().get(key).toString()));
            }
        }
    }
}
