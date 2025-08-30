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

import ballerina/jballerina.java;

# Milvus is a high-performance, cloud-native vector database built for scalable vector ANN search.
#
public isolated client class Client {

    # Gets invoked to initialize the `connector`.
    #
    # + config - The configurations to be used when initializing the `connector` 
    # + serviceUrl - URL of the target service 
    # + return - An error if connector initialization failed 
    public isolated function init(string serviceUrl, *ConnectionConfig config) returns Error? {
        return self.initiateClient(serviceUrl, config);
    }

    private isolated function initiateClient(string serviceUrl, *ConnectionConfig config) returns Error? = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;

    # Lists all the collections in the Milvus vector database.
    #
    # + return - A list of collection names
    remote isolated function listCollections() returns string[]|Error = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;

    // TODO: Add support for both dynamic fields and collection schema when creating a collection
    # Creates a new collection in the Milvus vector database.
    #
    # + request - The request to create a collection
    # + return - `()` if the collection is created successfully, otherwise an error
    remote isolated function createCollection(CreateCollectionRequest request) returns Error? = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;

    # Loads a collection in the Milvus vector database.
    # 
    # + collectionName - The name of the collection to load
    # + return - `()` if the collection is loaded successfully, otherwise an error
    remote isolated function loadCollection(string collectionName) returns Error? = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;

    # Creates an index for a collection in the Milvus vector database.
    #
    # + request - The request to create an index
    # + return - `()` if the index is created successfully, otherwise an error
    remote isolated function createIndex(CreateIndexRequest request) returns Error? = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;

    # Upserts data into a collection in the Milvus vector database.
    #
    # + request - The request to upsert data
    # + return - `()` if the data is upserted successfully, otherwise an error
    remote isolated function upsert(UpsertRequest request) returns Error? = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;

    # Deletes data from a collection in the Milvus vector database.
    # 
    # + request - The request to delete data
    # + return - The number of deleted entities if the data is deleted successfully, otherwise an error
    remote isolated function delete(DeleteRequest request) returns int|Error = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;

    # Performs a vector similarity search for data in a collection in the Milvus vector database.
    #
    # + request - The request to search for data
    # + return - On success, returns a two-dimensional array of `SearchResult` records. Otherwise, returns an `Error`.
    remote isolated function search(SearchRequest request) returns SearchResult[][]|Error = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;

    # Performs a scalar filtering for data in a collection in the Milvus vector database.
    #
    # + request - The request to search for data
    # + return - On success, returns a two-dimensional array of `QueryResult` records. Otherwise, returns an `Error`.
    remote isolated function query(QueryRequest request) returns QueryResult[][]|Error = @java:Method {
        'class: "io.ballerina.lib.milvus.Client"
    } external;
}
