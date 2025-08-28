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
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.SearchResp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.ballerina.lib.milvus.Utils.applyDynamicFields;
import static io.ballerina.lib.milvus.Utils.createError;

public class Client {

    public static final BString AUTH_CONFIG = StringUtils.fromString("authConfig");
    public static final BString USERNAME = StringUtils.fromString("username");
    public static final BString PASSWORD = StringUtils.fromString("password");
    public static final BString DATABASE_NAME = StringUtils.fromString("databaseName");
    public static final BString BEARER_TOKEN = StringUtils.fromString("token");
    public static final String NATIVE_CLIENT = "client";
    public static final BString COLLECTION_NAME = StringUtils.fromString("collectionName");
    public static final BString DIMENSION = StringUtils.fromString("dimension");
    public static final BString FIELD_NAMES = StringUtils.fromString("fieldNames");
    public static final BString PRIMARY_KEY = StringUtils.fromString("primaryKey");
    public static final BString DATA = StringUtils.fromString("data");
    public static final BString VECTORS = StringUtils.fromString("vectors");
    public static final String ID_FIELD = "id";
    public static final BString ID = StringUtils.fromString(ID_FIELD);
    public static final BString IDS = StringUtils.fromString("ids");
    public static final String VECTOR = "vector";
    public static final BString PARTITION_NAME = StringUtils.fromString("partitionName");
    public static final BString FILTER = StringUtils.fromString("filter");
    public static final BString PARTITION_NAMES = StringUtils.fromString("partitionNames");
    public static final BString TOP_K = StringUtils.fromString("topK");
    public static final String SEARCH_RESULT = "SearchResult";
    public static final BString CREDENTIALS_CONFIG = StringUtils.fromString("credentialsConfig");
    public static final BString CONNECT_TIMEOUT = StringUtils.fromString("connectTimeout");
    public static final BString IDLE_TIMEOUT = StringUtils.fromString("idleTimeout");
    public static final BString RPC_DEADLINE = StringUtils.fromString("rpcDeadline");
    public static final BString KEEP_ALIVE_TIMEOUT = StringUtils.fromString("keepAliveTimeout");
    public static final BString SERVER_NAME = StringUtils.fromString("serverName");
    public static final BString PROXY_ADDRESS = StringUtils.fromString("proxyAddress");
    public static final BString KEEP_ALIVE_WITHOUT_CALLS = StringUtils.fromString("keepAliveWithoutCalls");
    public static final BString SEARCH_ID = StringUtils.fromString("id");
    public static final BString SIMILARITY_SCORE = StringUtils.fromString("similarityScore");
    public static final BString ENTITY = StringUtils.fromString("entity");
    public static final BString PRIMARY_KEY_VALUE = StringUtils.fromString("value");
    public static final BString PRIMARY_KEY_FIELD_NAME = StringUtils.fromString("fieldName");
    public static final BString PROPERTIES = StringUtils.fromString("properties");
    public static final BString OUTPUT_FIELDS = StringUtils.fromString("outputFields");
    public static final String PROPERTIES_RECORD = "Properties";

    public static BError initiateClient(BObject clientObj, BString serviceUrl, BMap<String, Object> config) {
        try {
            BMap<?, ?> authConfig = config.getMapValue(AUTH_CONFIG);
            BMap<?, ?> credentialConfig = config.getMapValue(CREDENTIALS_CONFIG);
            BString dbName = config.getStringValue(DATABASE_NAME);
            long connectTimeout = config.getIntValue(CONNECT_TIMEOUT) * 1000;
            Object idleTimeout = config.get(IDLE_TIMEOUT);
            long rpcDeadline = config.getIntValue(RPC_DEADLINE) * 1000;
            long keepAliveTimeout = config.getIntValue(KEEP_ALIVE_TIMEOUT) * 1000;
            BString serverName = config.getStringValue(SERVER_NAME);
            BString proxyAddress = config.getStringValue(PROXY_ADDRESS);
            boolean keepAliveWithoutCalls = config.getBooleanValue(KEEP_ALIVE_WITHOUT_CALLS);
            String token = null;
            if (authConfig != null) {
                BString tokenValue = authConfig.getStringValue(BEARER_TOKEN);
                if (tokenValue != null) {
                    token = tokenValue.getValue();
                }
            }
            ConnectConfig.ConnectConfigBuilder connectionConfig = ConnectConfig.builder();
            connectionConfig
                    .uri(serviceUrl.getValue())
                    .rpcDeadlineMs(rpcDeadline)
                    .keepAliveTimeoutMs(keepAliveTimeout)
                    .connectTimeoutMs(connectTimeout)
                    .keepAliveWithoutCalls(keepAliveWithoutCalls);

            connectionConfig = (idleTimeout != null) ?
                    connectionConfig.idleTimeoutMs(((Long) idleTimeout) * 1000) : connectionConfig;
            connectionConfig = (serverName != null) ?
                    connectionConfig.serverName(serverName.getValue()) : connectionConfig;
            connectionConfig = (proxyAddress != null) ?
                    connectionConfig.proxyAddress(proxyAddress.getValue()) : connectionConfig;
            connectionConfig = (token != null) ? connectionConfig.token(token) : connectionConfig;
            connectionConfig = (dbName != null) ? connectionConfig.dbName(dbName.getValue()) : connectionConfig;
            connectionConfig = (dbName != null) ? connectionConfig.dbName(dbName.getValue()) : connectionConfig;
            if (credentialConfig != null) {
                connectionConfig = connectionConfig.username(config.getStringValue(USERNAME).getValue());
                connectionConfig = connectionConfig.password(config.getStringValue(PASSWORD).getValue());
            }
            MilvusClientV2 client = new MilvusClientV2(connectionConfig.build());
            clientObj.addNativeData(NATIVE_CLIENT, client);
            return null;
        } catch (Exception error) {
            return createError("Failed to initiate Milvus client", error);
        }
    }

    public static Object createCollection(BObject clientObject, BMap<String, Object> request) {
        try {
            MilvusClientV2 client = (MilvusClientV2) clientObject.getNativeData(NATIVE_CLIENT);
            String collectionName = request.getStringValue(COLLECTION_NAME).getValue();
            Long dimension = request.getIntValue(DIMENSION);
            CreateCollectionReq createCollectionRequest = CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .dimension(dimension.intValue())
                    .enableDynamicField(true)
                    .primaryFieldName(request.getStringValue(StringUtils.fromString("primaryFieldName")).getValue())
                    .build();
            client.createCollection(createCollectionRequest);
            return null;
        } catch (Exception error) {
            return createError("Failed to create the collection", error);
        }
    }

    public static Object loadCollection(BObject clientObject, BString collectionName) {
        try {
            MilvusClientV2 client = (MilvusClientV2) clientObject.getNativeData(NATIVE_CLIENT);
            client.loadCollection(LoadCollectionReq.builder()
                    .collectionName(collectionName.getValue())
                    .build());
            return null;
        } catch (Exception error) {
            return createError("Failed to load the collection", error);
        }
    }

    public static Object listCollections(BObject clientObject) {
        try {
            MilvusClientV2 client = (MilvusClientV2) clientObject.getNativeData(NATIVE_CLIENT);
            List<String> collectionsList = client.listCollections().getCollectionNames();
            BString[] collectionNames = collectionsList.stream()
                    .map(StringUtils::fromString)
                    .toArray(BString[]::new);
            return ValueCreator.createArrayValue(collectionNames);
        } catch (Exception error) {
            return createError("Failed to list collections", error);
        }
    }

    public static Object createIndex(BObject clientObject, BMap<String, Object> request) {
        try {
            String collectionName = request.getStringValue(COLLECTION_NAME).getValue();
            BArray fieldNames = request.getArrayValue(FIELD_NAMES);
            BString primaryKey = request.getStringValue(PRIMARY_KEY);
            MilvusClientV2 client = (MilvusClientV2) clientObject.getNativeData(NATIVE_CLIENT);
            List<IndexParam> indexParams = new ArrayList<>();
            for (String fieldName : fieldNames.getStringArray()) {
                indexParams.add(IndexParam.builder().fieldName(fieldName).build());
            }
            indexParams.add(IndexParam.builder()
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .fieldName(primaryKey.getValue())
                    .build());
            CreateIndexReq createIndexReq = CreateIndexReq.builder()
                    .collectionName(collectionName)
                    .indexParams(indexParams)
                    .build();
            client.createIndex(createIndexReq);
            return null;
        } catch (Exception error) {
            return createError("Failed to create index", error);
        }
    }

    public static Object upsert(BObject clientObject, BMap<String, Object> request) {
        try {
            MilvusClientV2 client = (MilvusClientV2) clientObject.getNativeData(NATIVE_CLIENT);
            String collectionName = request.getStringValue(COLLECTION_NAME).getValue();
            BMap<?, ?> data = request.getMapValue(DATA);
            double[] vectors = ((BArray) data.get(VECTORS)).getFloatArray();
            BMap<?, ?> primaryKey = data.getMapValue(PRIMARY_KEY);
            long id = primaryKey.getIntValue(PRIMARY_KEY_VALUE);
            String primaryKeyFieldName = primaryKey.getStringValue(PRIMARY_KEY_FIELD_NAME).getValue();
            Gson gson = new Gson();
            JsonObject row = new JsonObject();
            List<Double> vectorList = new ArrayList<>();
            for (double vector : vectors) {
                vectorList.add(vector);
            }
            row.add(VECTOR, gson.toJsonTree(vectorList));
            row.add(primaryKeyFieldName, gson.toJsonTree(id));
            if (data.containsKey(PROPERTIES)) {
                BMap<?, ?> properties = data.getMapValue(PROPERTIES);
                applyDynamicFields(properties, gson, row);
            }
            List<JsonObject> dataList = new ArrayList<>();
            dataList.add(row);
            UpsertReq upsertRequest = UpsertReq.builder()
                    .collectionName(collectionName)
                    .data(dataList)
                    .build();
            client.upsert(upsertRequest);
            return null;
        } catch (Exception error) {
            return createError("Failed to upsert data", error);
        }
    }

    public static Object delete(BObject clientObject, BMap<String, Object> request) {
        try {
            MilvusClientV2 client = (MilvusClientV2) clientObject.getNativeData(NATIVE_CLIENT);
            BString collectionName = request.getStringValue(COLLECTION_NAME);
            BString partitionName = request.getStringValue(PARTITION_NAME);
            BArray ids = request.getArrayValue(IDS);
            BString filter = request.getStringValue(FILTER);
            DeleteReq.DeleteReqBuilder<?, ?> deleteReq = DeleteReq.builder();
            deleteReq = (collectionName != null) ? deleteReq.collectionName(collectionName.getValue()) : deleteReq;
            deleteReq = (partitionName != null) ? deleteReq.partitionName(partitionName.getValue()) : deleteReq;
            deleteReq = (ids != null)
                    ? deleteReq.ids(Arrays.stream(ids.getIntArray()).boxed().collect(Collectors.toList())) : deleteReq;
            deleteReq = (filter != null) ? deleteReq.filter(filter.getValue()) : deleteReq;
            DeleteResp deleteResp = client.delete(deleteReq.build());
            return deleteResp.getDeleteCnt();
        } catch (Exception error) {
            return createError("Failed to delete data", error);
        }
    }

    public static Object search(BObject clientObject, BMap<String, Object> request) {
        try {
            MilvusClientV2 client = (MilvusClientV2) clientObject.getNativeData(NATIVE_CLIENT);
            BString collectionName = request.getStringValue(COLLECTION_NAME);
            BArray partitionName = request.getArrayValue(PARTITION_NAMES);
            BArray vectors = request.getArrayValue(VECTORS);
            BString filter = request.getStringValue(FILTER);
            Long topK = request.getIntValue(TOP_K);
            if (vectors == null || vectors.size() == 0) {
                return createError("Vectors cannot be null or empty", null);
            }
            List<BaseVector> vectorArray = new ArrayList<>();

            if (vectors.getElementType().getTag() == TypeTags.FLOAT_TAG) {
                vectorArray.add(new FloatVec(Arrays.stream(vectors.getFloatArray())
                        .mapToObj(d -> (float) d)
                        .collect(Collectors.toList())));
            } else {
                for (int i = 0; i < vectors.size(); i++) {
                    BArray currentVector = (BArray) vectors.get(i); // Use get(i) instead of array access
                    vectorArray.add(new FloatVec(Arrays.stream(currentVector.getFloatArray())
                            .mapToObj(d -> (float) d)
                            .collect(Collectors.toList())));
                }
            }

            SearchReq.SearchReqBuilder<?, ?> searchReq = SearchReq.builder();
            searchReq = (collectionName != null) ? searchReq.collectionName(collectionName.getValue()) : searchReq;
            searchReq = (partitionName != null)
                    ? searchReq.partitionNames(Arrays.asList(partitionName.getStringArray())) : searchReq;
            searchReq = searchReq.data(vectorArray);
            searchReq = (filter != null) ? searchReq.filter(filter.getValue()) : searchReq;
            searchReq = (topK != null) ? searchReq.topK(topK.intValue()) : searchReq;

            if (request.containsKey(OUTPUT_FIELDS)) {
                BArray outputFields = request.getArrayValue(OUTPUT_FIELDS);
                List<String> fields = Arrays.asList(outputFields.getStringArray());
                searchReq.outputFields(fields);
            }
            SearchResp searchR = client.search(searchReq.build());
            List<List<SearchResp.SearchResult>> searchResults = searchR.getSearchResults();

            RecordType recordType = TypeCreator.createRecordType(SEARCH_RESULT,
                    ModuleUtils.getModule(), 0, false, 1);
            ArrayType arrayType = TypeCreator.createArrayType(recordType);
            BArray[] resultArrays = new BArray[searchResults.size()];
            RecordType outputFieldsType = TypeCreator.createRecordType(PROPERTIES_RECORD,
                    ModuleUtils.getModule(), 0, false, 1);
            for (List<SearchResp.SearchResult> results : searchResults) {
                BMap<BString, Object>[] responses = new BMap[results.size()];
                for (SearchResp.SearchResult result : results) {
                    BMap<BString, Object> response =
                            ValueCreator.createRecordValue(ModuleUtils.getModule(), SEARCH_RESULT);
                    BMap<BString, Object> entity = ValueCreator.createRecordValue(outputFieldsType);
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
                    response.put(PRIMARY_KEY, StringUtils.fromString(result.getPrimaryKey()));
                    response.put(SEARCH_ID, result.getId());
                    response.put(SIMILARITY_SCORE, result.getScore().doubleValue());
                    response.put(OUTPUT_FIELDS, entity);
                    responses[results.indexOf(result)] = response;
                }
                BArray responseArray = ValueCreator.createArrayValue(responses,
                        TypeCreator.createArrayType(recordType));
                resultArrays[searchResults.indexOf(results)] = responseArray;
            }
            return ValueCreator.createArrayValue(resultArrays, TypeCreator.createArrayType(arrayType));
        } catch (Exception error) {
            return createError("Failed to search data", error);
        }
    }
}
