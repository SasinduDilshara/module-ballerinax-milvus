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
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.ballerina.lib.milvus.Client.OUTPUT_FIELDS;
import static io.ballerina.lib.milvus.Client.PRIMARY_KEY;
import static io.ballerina.lib.milvus.Client.QUERY_RESULT;
import static io.ballerina.lib.milvus.Client.SEARCH_ID;
import static io.ballerina.lib.milvus.Client.SEARCH_RESULT;
import static io.ballerina.lib.milvus.Client.SIMILARITY_SCORE;
import static io.ballerina.lib.milvus.ModuleUtils.getModule;

public class Utils {
    private static final String ERROR_TYPE = "Error";

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
            for (int index = 0; index < arr.size(); index++) {
                list.add(convertToJsonFields(arr.get(index)));
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

    protected static BMap<BString, Object>[] generateSearchResult(RecordType outputFieldsType,
                                                                  List<SearchResp.SearchResult> results) {
        BMap<BString, Object>[] responses = new BMap[results.size()];
        for (SearchResp.SearchResult result : results) {
            BMap<BString, Object> response =
                    ValueCreator.createRecordValue(ModuleUtils.getModule(), SEARCH_RESULT);
            BMap<BString, Object> entity = getResult(result, outputFieldsType);
            response.put(PRIMARY_KEY, StringUtils.fromString(result.getPrimaryKey()));
            response.put(SEARCH_ID, result.getId());
            response.put(SIMILARITY_SCORE, result.getScore().doubleValue());
            response.put(OUTPUT_FIELDS, entity);
            responses[results.indexOf(result)] = response;
        }
        return responses;
    }

    protected static BMap<BString, Object>[] generateQueryResult(List<QueryResp.QueryResult> results) {
        BMap<BString, Object>[] responses = new BMap[results.size()];
        for (QueryResp.QueryResult result : results) {
            BMap<BString, Object> response = getResult(result);
            responses[results.indexOf(result)] = response;
        }
        return responses;
    }

    private static BMap<BString, Object> getResult(SearchResp.SearchResult result, RecordType outputFieldsType) {
        BMap<BString, Object> entity = ValueCreator.createRecordValue(outputFieldsType);
        processEntityFields(result.getEntity().entrySet(), entity);
        return entity;
    }

    private static BMap<BString, Object> getResult(QueryResp.QueryResult result) {
        BMap<BString, Object> response = ValueCreator.createRecordValue(ModuleUtils.getModule(), QUERY_RESULT);
        processEntityFields(result.getEntity().entrySet(), response);
        return response;
    }

    private static void processEntityFields(Set<Map.Entry<String, Object>> entityEntries,
                                            BMap<BString, Object> targetMap) {
        for (Map.Entry<String, Object> entry : entityEntries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!(value instanceof List<?> list)) {
                targetMap.put(StringUtils.fromString(key),
                        StringUtils.fromString(value.toString()));
                continue;
            }
            if (list.isEmpty()) {
                continue;
            }
            if (list.get(0) instanceof Number) {
                double[] values = list.stream()
                        .mapToDouble(element -> ((Number) element).floatValue())
                        .toArray();
                BArray floatArray = ValueCreator.createArrayValue(values);
                targetMap.put(StringUtils.fromString(key), floatArray);
            } else {
                BString[] values = list.stream()
                        .map(val -> StringUtils.fromString(val.toString()))
                        .toArray(BString[]::new);
                BArray stringArray = ValueCreator.createArrayValue(values);
                targetMap.put(StringUtils.fromString(key), stringArray);
            }
        }
    }

    protected static List<BaseVector> getVectors(BArray vectors) {
        List<BaseVector> vectorArray = new ArrayList<>();
        if (vectors.getElementType().getTag() == TypeTags.FLOAT_TAG) {
            vectorArray.add(new FloatVec(Arrays.stream(vectors.getFloatArray())
                    .mapToObj(value -> (float) value)
                    .collect(Collectors.toList())));
            return vectorArray;
        }
        for (int index = 0; index < vectors.size(); index++) {
            BArray currentVector = (BArray) vectors.get(index);
            vectorArray.add(new FloatVec(Arrays.stream(currentVector.getFloatArray())
                    .mapToObj(value -> (float) value)
                    .collect(Collectors.toList())));
        }
        return vectorArray;
    }

    static SearchRequest parseSearchRequest(BMap<String, Object> request) {
        BString collectionName = request.getStringValue(Client.COLLECTION_NAME);
        BArray partitionNames = request.getArrayValue(Client.PARTITION_NAMES);
        BArray vectors = request.getArrayValue(Client.VECTORS);
        BString filter = request.getStringValue(Client.FILTER);
        Long topK = request.getIntValue(Client.TOP_K);
        BArray outputFields = request.containsKey(OUTPUT_FIELDS) ?
            request.getArrayValue(OUTPUT_FIELDS) : null;

        return new SearchRequest(
            collectionName != null ? collectionName.getValue() : null,
            partitionNames != null ? Arrays.asList(partitionNames.getStringArray()) : null,
            vectors,
            filter != null ? filter.getValue() : null,
            topK != null ? topK.intValue() : null,
            outputFields != null ? Arrays.asList(outputFields.getStringArray()) : null
        );
    }

    static QueryRequest parseQueryRequest(BMap<String, Object> request) {
        BString collectionName = request.getStringValue(Client.COLLECTION_NAME);
        BArray partitionNames = request.getArrayValue(Client.PARTITION_NAMES);
        BString filter = request.getStringValue(Client.FILTER);
        BArray ids = request.getArrayValue(Client.IDS);
        BArray outputFields = request.containsKey(OUTPUT_FIELDS) ?
            request.getArrayValue(OUTPUT_FIELDS) : null;

        return new QueryRequest(
            collectionName != null ? collectionName.getValue() : null,
            partitionNames != null ? Arrays.asList(partitionNames.getStringArray()) : null,
            filter != null ? filter.getValue() : null,
            ids != null ? Arrays.stream(ids.getIntArray()).boxed().collect(Collectors.toList()) : null,
            outputFields != null ? Arrays.asList(outputFields.getStringArray()) : null
        );
    }

    static SearchReq buildSearchRequest(SearchRequest params) {
        SearchReq.SearchReqBuilder<?, ?> searchReq = SearchReq.builder();
        searchReq = searchReq.data(getVectors(params.vectors()));
        searchReq = (params.collectionName() != null) ? searchReq.collectionName(params.collectionName()) : searchReq;
        searchReq = (params.partitionNames() != null) ? searchReq.partitionNames(params.partitionNames()) : searchReq;
        searchReq = (params.filter() != null) ? searchReq.filter(params.filter()) : searchReq;
        searchReq = (params.topK() != null) ? searchReq.topK(params.topK()) : searchReq;
        searchReq = (params.outputFields() != null) ? searchReq.outputFields(params.outputFields()) : searchReq;
        return searchReq.build();
    }

    static QueryReq buildQueryRequest(QueryRequest params) {
        QueryReq.QueryReqBuilder<?, ?> queryReq = QueryReq.builder();
        queryReq = (params.collectionName() != null) ? queryReq.collectionName(params.collectionName()) : queryReq;
        queryReq = (params.partitionNames() != null) ? queryReq.partitionNames(params.partitionNames()) : queryReq;
        queryReq = (params.filter() != null) ? queryReq.filter(params.filter()) : queryReq;
        queryReq = (params.ids() != null) ? queryReq.ids(Collections.singletonList(params.ids())) : queryReq;
        queryReq = (params.outputFields() != null) ? queryReq.outputFields(params.outputFields()) : queryReq;
        return queryReq.build();
    }

    static Object transformSearchResults(SearchResp searchResp) {
        List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();

        RecordType recordType = TypeCreator.createRecordType(SEARCH_RESULT,
                getModule(), 0, false, 1);
        ArrayType arrayType = TypeCreator.createArrayType(recordType);
        RecordType outputFieldsType = TypeCreator.createRecordType(Client.OUTPUT_FIELDS_TYPE,
                getModule(), 0, false, 1);

        BArray[] resultArrays = new BArray[searchResults.size()];
        for (int i = 0; i < searchResults.size(); i++) {
            List<SearchResp.SearchResult> results = searchResults.get(i);
            BMap<BString, Object>[] responses = generateSearchResult(outputFieldsType, results);
            BArray responseArray = ValueCreator.createArrayValue(responses,
                    TypeCreator.createArrayType(recordType));
            resultArrays[i] = responseArray;
        }
        return ValueCreator.createArrayValue(resultArrays, TypeCreator.createArrayType(arrayType));
    }

    static Object transformQueryResults(QueryResp queryResp) {
        List<QueryResp.QueryResult> queryResults = queryResp.getQueryResults();

        RecordType recordType = TypeCreator.createRecordType(QUERY_RESULT,
                getModule(), 0, false, 1);
        ArrayType arrayType = TypeCreator.createArrayType(recordType);

        BArray[] resultArrays = new BArray[queryResults.size()];
        for (int i = 0; i < queryResults.size(); i++) {
            QueryResp.QueryResult result = queryResults.get(i);
            BMap<BString, Object>[] responses = generateQueryResult(List.of(result));
            BArray responseArray = ValueCreator.createArrayValue(responses,
                    TypeCreator.createArrayType(recordType));
            resultArrays[i] = responseArray;
        }
        return ValueCreator.createArrayValue(resultArrays, TypeCreator.createArrayType(arrayType));
    }
}
