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
#
# + authConfig - The auth configurations for the Milvus connection
# + credentialsConfig - The credentials for the Milvus connection
# + idleTimeout - The idle timeout for a connection
# + keepAliveTime - The time between keep-alive probes sent by the client to the server. (Default: 55 seconds)
# + keepAliveTimeout - The timeout duration for the server to respond to a keep-alive probe sent by the client. (Default: 20 seconds)
# + keepAliveWithoutCalls - Whether to send keep-alive probes without making requests. (Default: false)
# + rpcDeadline - The deadline for the rpc operation to be completed. The value defaults to 0, which indicates the deadline is disabled
# + connectTimeout - The timeout duration for this operation. (Default: 10 seconds)
# + databaseName - The name of the database to which the target Milvus instance belongs
# + serverName - The expected name of the server
# + proxyAddress - The proxy server’s address through which the connection is to be established
# + secureConfig - The secure configurations for the Milvus connection
public type ConnectionConfig record {
    AuthConfig authConfig?;
    CredentialsConfig credentialsConfig?;
    int idleTimeout?;
    int keepAliveTime = 55;
    int keepAliveTimeout = 20;
    boolean keepAliveWithoutCalls = false;
    int rpcDeadline = 0;
    int connectTimeout = 10;
    string databaseName?;
    string serverName?;
    string proxyAddress?;
    SecureConfig secureConfig?;
};

# Represents the secure configurations for the Milvus connection.
#
# + clientKeyPath - The path to the client key file for mutual authentication
# + clientPemPath - The path to the client pem file for mutual authentication
# + serverPemPath - The path to the server PEM file for mutual authentication
# + caPemPath - The path to the CA PEM file for mutual authentication
public type SecureConfig record {
    string clientKeyPath;
    string clientPemPath;
    string serverPemPath;
    string caPemPath;
};

# Represents the auth configurations for the Milvus connection.
#
# + token - A valid access token to access the specified Milvus instance.
public type AuthConfig record {
    string token;
};

# Represents the configuration for the Milvus connection with credentials.
#
# + username - The username used to connect to the specified Milvus instance.
# + password - The password used to connect to the specified Milvus instance.
public type CredentialsConfig record {
    string username;
    string password;
};

# Represents the request for the upsert operation.
#
# + collectionName - The name of the collection to upsert data into
# + partitionName - The name of the partition to upsert data into
# + databaseName - The name of the database to upsert data into
# + data - The data to upsert into the Milvus collection
public type UpsertRequest record {
    string collectionName;
    string partitionName?;
    string databaseName?;
    Entry data;
};

# Represents a single entry to be upserted into a Milvus collection.
#
# + primaryKey -  Primary key value for the entry. Omit when the collection is configured to auto-generate primary keys
# + vectors - Dense vector embedding for this entry. The length must match the collection's configured dimension.
# + properties - Additional scalar field values for this record, excluding the vector and primary key fields.
public type Entry record {
    record {
        string fieldName = "id";
        int value;
    } primaryKey?;
    float[] vectors;
    record{} properties?;
};

# Represents the request for the delete operation.
#
# + collectionName - The name of the collection to delete data from
# + partitionName - The name of the partition to delete data from
# + ids - An array of ids of the entries to delete
# + filter - The filter to delete data from the Milvus collection
public type DeleteRequest record {
    string collectionName;
    string partitionName?;
    int[] ids?;
    string filter?;
};

# Represents the request for the search operation.
#
# + collectionName - The name of the collection to search data from
# + partitionName - The name of the partition to search data from
# + vectors - The vectors to search for
# + topK - The number of results to return
# + filter - The filter to search for
# + outputFields - The fields to return
public type SearchRequest record {
    string collectionName;
    string partitionName?;
    float[][]|float[] vectors;
    int topK;
    string filter?;
    string[] outputFields?;
};

# Represents the result of the search operation.
#
# + primaryKey - The name of the primary key of the result
# + id - The id of the result
# + similarityScore - The similarity score of the result
# + outputFields - The output fields of the result
public type SearchResult record {|
    string primaryKey;
    int id;
    float similarityScore;
    Properties outputFields?;
|};

# Represents the properties of the search result.
#
# + vector - The vector embeddings of the search result
public type Properties record {
    float[] vector?;
};

# Represents the request for the create collection operation.
#
# + collectionName - The name of the collection to create
# + primaryFieldName - The name of the primary field of the collection
# + dimension - The dimension of the collection
public type CreateCollectionRequest record {
    string collectionName;
    string primaryFieldName = "id";
    int dimension;
};

# Represents the request for the create index operation.
#
# + collectionName - The name of the collection to create an index for
# + primaryKey - The name of the primary key of the collection
# + fieldNames - The names of the fields to create an index for
public type CreateIndexRequest record {
    string collectionName;
    string primaryKey;
    string[] fieldNames;
};
