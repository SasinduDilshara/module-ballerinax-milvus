// Copyright (c) 2025 WSO2 LLC (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;

Client milvusClient = check new(serviceUrl = "http://localhost:19530");

string collectionName = "test_collection";
int id  = 10001;
string primaryKey = "primary_key";

@test:Config {}
function testCreateCollection() returns error? {
    check milvusClient->createCollection({
        collectionName,
        dimension: 3,
        primaryFieldName: "primary_key"
    });
}

@test:Config {}
function testListCollections() returns error? {
    string[] collection = check milvusClient->listCollections();
    test:assertNotEquals(collection.indexOf(collectionName), ());
}

@test:Config {}
function testUpsertEntry() returns error? {
    check milvusClient->upsert({
        collectionName,
        data: {
            primaryKey: {
                value: id,
                fieldName: "primary_key"
            },
            vectors: [0.3, 0.4, 0.5],
            properties: {
                "content": "test",
                "type": "text",
                "createdTime": "1723600000"
            }
        }
    });
}

@test:Config {
    groups: ["delete"],
    dependsOn: [testSearchNearVectors]
}
function testDeleteEntry() returns error? {
    _ = check milvusClient->delete({
        collectionName,
        filter: string `${primaryKey} == ${id}`
    });
}

@test:Config {
    groups: ["delete"],
    dependsOn: [testSearchNearVectors]
}
function testDeleteEntryWithIds() returns error? {
    int[] ids = [1, 2, 3];
    foreach int id in ids {
        check milvusClient->upsert({
            collectionName,
            data: {
                primaryKey: {
                    value: id,
                    fieldName: "primary_key"
                },
                vectors: [0.3, 0.4, 0.5]
            }
        });
    }
    _ = check milvusClient->delete({
        collectionName,
        ids
    });
}

@test:Config {
    groups: ["search"],
    dependsOn: [testUpsertEntry, testCreateCollection]
}
function testSearchNearVectors() returns error? {
    check milvusClient->loadCollection(collectionName);
    SearchResult[][] result = check milvusClient->search({
        collectionName,
        vectors: [0.3, 0.4, 0.5],
        topK: 10,
        outputFields: ["content", "type", "vector"]
    });
    if result.length() > 0 && result[0].length() > 0 {
        result = check milvusClient->search({
            collectionName,
            vectors: [[0.3, 0.4, 0.5]],
            topK: 1,
            filter: string `${primaryKey} == ${id} and content == "test"`,
            outputFields: ["content", "type", "vector"]
        });
        test:assertEquals(result[0][0].id, id);
    }
}

@test:Config {
    groups: ["query"],
    dependsOn: [testUpsertEntry, testCreateCollection]
}
function testQueryVectors() returns error? {
    check milvusClient->loadCollection(collectionName);
    QueryResult[][] result = check milvusClient->query({
        collectionName,
        filter: string `content == "test"`,
        outputFields: ["content", "type", "vector"]
    });
    if result.length() > 0 && result[0].length() > 0 {
        test:assertEquals(result[0][0]["content"], "test");
    }
}

// Regression coverage for the dynamic-field JSON pipeline.
//
// Write side: a Ballerina `decimal` (BDecimal) used to slip past `convertToJsonFields`
// and reach Gson, which reflectively dumped its internal `DecimalValue` fields as
// `{"valueKind":"OTHER","value":...}` instead of a JSON number. Once the row was
// persisted that way the value could never be read back as a number.
//
// Read side: any non-list dynamic-field value used to be `.toString()`'d into a
// `BString`, even when Milvus returned a Gson `JsonElement`. That stringification
// destroyed nested structure and collapsed every JSON number to a single textual
// representation, forcing every caller to re-parse the string and lose the
// decimal-vs-float distinction.
//
// This test exercises both halves end-to-end: it upserts a row whose dynamic
// `metadata` property is a nested record carrying a decimal, an int, a float, a
// boolean, a string, a json array, and a nested json object. It then queries the
// row back and asserts each value comes back with the correct Ballerina type.
string jsonMetadataCollection = "json_metadata_collection";
int jsonMetadataId = 70001;

@test:Config {
    groups: ["json-metadata"]
}
function testCreateJsonMetadataCollection() returns error? {
    check milvusClient->createCollection({
        collectionName: jsonMetadataCollection,
        dimension: 3,
        primaryFieldName: "primary_key"
    });
}

@test:Config {
    groups: ["json-metadata"],
    dependsOn: [testCreateJsonMetadataCollection]
}
function testUpsertJsonMetadataWithDecimal() returns error? {
    record {} metadata = {
        "fileName": "decimals.txt",
        "fileSize": 16541d,
        "ratio": 0.75d,
        "index": 7,
        "approved": true,
        "tags": <json>["alpha", "beta"],
        "nested": <json>{"source": "unit-test", "revision": 3}
    };
    check milvusClient->upsert({
        collectionName: jsonMetadataCollection,
        data: {
            primaryKey: {
                value: jsonMetadataId,
                fieldName: "primary_key"
            },
            vectors: [0.7, 0.8, 0.9],
            properties: {
                "content": "decimals",
                "type": "text",
                "metadata": metadata
            }
        }
    });
    check milvusClient->createIndex({
        collectionName: jsonMetadataCollection,
        primaryKey: "primary_key",
        fieldNames: ["vector"]
    });
}

@test:Config {
    groups: ["json-metadata"],
    dependsOn: [testUpsertJsonMetadataWithDecimal]
}
function testQueryJsonMetadataPreservesTypes() returns error? {
    check milvusClient->loadCollection(jsonMetadataCollection);
    SearchResult[][] result = check milvusClient->search({
        collectionName: jsonMetadataCollection,
        vectors: [[0.7, 0.8, 0.9]],
        topK: 1,
        outputFields: ["content", "metadata"]
    });
    test:assertTrue(result.length() > 0 && result[0].length() > 0,
        "expected the upserted row to be retrievable");

    OutputFields? outputFields = result[0][0].outputFields;
    if outputFields is () {
        test:assertFail("expected outputFields to be returned");
    }

    anydata rawMetadata = outputFields["metadata"];
    if rawMetadata !is map<anydata> {
        test:assertFail("expected JSON dynamic column to be returned as a structured map, not a string");
    }

    test:assertEquals(rawMetadata["fileName"], "decimals.txt");
    // JSON has one number type, so a whole-number `decimal` on the way in is
    // indistinguishable from an `int` on the way out. The connector returns
    // the most natural Ballerina type — int for whole numbers (when they fit
    // in long), decimal for fractional. Callers that need the value pinned
    // back to `decimal` should rely on a typed schema (e.g. `cloneWithType`).
    test:assertEquals(rawMetadata["fileSize"], 16541);
    // The crux of the fix: a fractional Ballerina decimal must round-trip as
    // `decimal`, not collapse to `float` and not get reflected into a
    // `{"valueKind":"OTHER",...}` blob on the write side.
    test:assertEquals(rawMetadata["ratio"], 0.75d);
    // Whole numbers should come back as int, not float, since they fit in long.
    test:assertEquals(rawMetadata["index"], 7);
    test:assertEquals(rawMetadata["approved"], true);
    // Nested json structures must preserve their shape, not be flattened to a string.
    test:assertEquals(rawMetadata["tags"], <json>["alpha", "beta"]);
    anydata nested = rawMetadata["nested"];
    test:assertTrue(nested is map<anydata>,
        "expected nested object to be returned as a structured map");
    if nested is map<anydata> {
        test:assertEquals(nested["source"], "unit-test");
        test:assertEquals(nested["revision"], 3);
    }
}

@test:Config {
    groups: ["json-metadata"],
    dependsOn: [testQueryJsonMetadataPreservesTypes]
}
function testFilterOnJsonMetadataField() returns error? {
    // The `metadata["fileName"]` filter syntax only works when the dynamic
    // column is stored as a real JSON object — i.e. when the write path didn't
    // collapse the record into a string. This guards against any future
    // regression that puts metadata back through `toJsonString()` (or similar)
    // on the Ballerina side.
    SearchResult[][] result = check milvusClient->search({
        collectionName: jsonMetadataCollection,
        vectors: [[0.7, 0.8, 0.9]],
        topK: 1,
        filter: string `metadata["fileName"] == "decimals.txt"`,
        outputFields: ["content"]
    });
    test:assertTrue(result.length() > 0 && result[0].length() > 0,
        "expected metadata[\"fileName\"] filter to match the upserted row");
    test:assertEquals(result[0][0].id, jsonMetadataId);
}
