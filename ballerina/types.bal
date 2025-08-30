// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
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

# Represents the configuration for the Milvus connection.
public type ConnectionConfig record {
    # The auth configurations for the Milvus connection
    AuthConfig authConfig?;
    # The credentials configurations for the Milvus connection
    CredentialsConfig credentialsConfig?;
    # The idle timeout for a connection
    int idleTimeout?;
    # The time between keep-alive probes sent by the client to the server. (Default: 55 seconds)
    int keepAliveTime = 55;
    # The timeout duration for the server to respond to a keep-alive probe sent by the client. (Default: 20 seconds)
    int keepAliveTimeout = 20;
    # Whether to send keep-alive probes without making requests. (Default: false)
    boolean keepAliveWithoutCalls = false;
    # The deadline for the rpc operation to be completed. The value defaults to 0, which indicates the deadline is disabled
    int rpcDeadline = 0;
    # The timeout duration for this operation. (Default: 10 seconds)
    int connectTimeout = 10;
    # The name of the database to which the target Milvus instance belongs
    string databaseName?;
    # The expected name of the server
    string serverName?;
    # The proxy server’s address through which the connection is to be established
    string proxyAddress?;
    # The secure configurations for the Milvus connection
    SecureConfig secureConfig?;
};

# Represents the secure configurations for the Milvus connection.
public type SecureConfig record {
    # The path to the client key file for mutual authentication
    string clientKeyPath;
    # The path to the client PEM file for mutual authentication
    string clientPemPath;
    # The path to the server PEM file for mutual authentication
    string serverPemPath;
    # The path to the CA PEM file for mutual authentication
    string caPemPath;
};

# Represents the auth configurations for the Milvus connection.
public type AuthConfig record {
    # A valid access token to access the specified Milvus instance
    string token;
};

# Represents the configuration for the Milvus connection with credentials.
public type CredentialsConfig record {
    # The username used to connect to the specified Milvus instance
    string username;
    # The password used to connect to the specified Milvus instance
    string password;
};

# Represents the request for the upsert operation.
public type UpsertRequest record {
    # The name of the collection to upsert data into
    string collectionName;
    # The name of the partition to upsert data into
    string partitionName?;
    # The name of the database to upsert data into
    string databaseName?;
    # The data to upsert into the Milvus collection
    Entry data;
};

# Represents a single entry to be upserted into a Milvus collection.
public type Entry record {
    # The primary key value for the entry
    record {
        string fieldName = "id";
        int value;
    } primaryKey?;
    # The vectors to upsert into the Milvus collection
    float[] vectors;
    # The properties to upsert into the Milvus collection
    record{} properties?;
};

# Represents the request for the delete operation.
public type DeleteRequest record {
    # The name of the collection to delete data from
    string collectionName;
    # The name of the partition to delete data from
    string partitionName?;
    # The ids of the entries to delete
    int[] ids?;
    # The filter to delete data from the Milvus collection
    string filter?;
};

# Represents the request for the search operation.
public type SearchRequest record {
    # The name of the collection to search data from
    string collectionName;
    # The name of the partition to search data from
    string partitionName?;
    # The vectors to search for
    float[][]|float[] vectors;
    # The number of results to return
    int topK;
    # The filter expression to apply during the search operation
    string filter?;
    # The fields to return in the search result
    string[] outputFields?;
};

# Represents the request for the query operation.
public type QueryRequest record {
    # The name of the collection to search data from
    string collectionName;
    # The name of the partition to search data from
    string partitionName?;
    # The filter to search
    string filter?;
    # The fields to return in the query result
    string[] outputFields?;
};

# Represents the result of the search operation.
public type SearchResult record {|
    # The name of the primary key of the result
    string primaryKey;
    # The id of the result
    int id;
    # The similarity score of the result
    float similarityScore;
    # The output fields of the result
    OutputFields outputFields?;
|};

# Represents the result of the query operation.
# 
public type QueryResult record {};

# Represents the properties of the search result.
#
# + vector - The vector embeddings of the search result
public type OutputFields record {
    float[] vector?;
};

# Represents the request for the create collection operation.
public type CreateCollectionRequest record {
    # The name of the collection to create
    string collectionName;
    # The name of the primary field of the collection
    string primaryFieldName = "id";
    # The dimension of the collection
    int dimension;
};

# Represents the request for the create index operation.
public type CreateIndexRequest record {
    # The name of the collection to create an index for
    string collectionName;
    # The name of the primary key of the collection
    string primaryKey;
    # The names of the fields to create an index for
    string[] fieldNames;
};
