## Overview

[Milvus](https://milvus.io/) is an open-source vector database built for scalable similarity search and AI applications. It provides high-performance vector storage and retrieval capabilities, making it ideal for applications involving machine learning, deep learning, and similarity search scenarios.

The Milvus connector provides seamless integration with the Milvus vector database, enabling developers to perform operations such as creating collections, inserting vector data, building indexes, and conducting similarity searches.

### Key Features

- Scalable similarity search and AI application support
- High-performance vector storage and retrieval
- Efficient collection and index management
- Support for Milvus SDK Java v2.6.1

## Setup guide

To utilize the Milvus connector, you must have access to a running Milvus instance. You can use one of the following methods for that.

### Option 1: Using Docker

1. Make sure Docker is installed on your system.

2. Use the following command to start a Milvus standalone instance in docker

```bash
# Download the installation script
$ curl -sfL https://raw.githubusercontent.com/milvus-io/milvus/master/scripts/standalone_embed.sh -o standalone_embed.sh

#Start the Docker container
$ bash standalone_embed.sh start
```

For detailed installation instructions, refer to the official Milvus documentation.

- **Linux/macOS**: [Run Milvus in Docker](https://milvus.io/docs/install_standalone-docker.md)
- **Windows**: [Run Milvus in Docker on Windows](https://milvus.io/docs/install_standalone-windows.md)

### Option 2: Using Milvus Cloud by Zilliz

[Zilliz Cloud](https://cloud.zilliz.com/) provides a fully managed Milvus service. Follow these steps to set up your cloud instance:

1. **Sign up to Zilliz Cloud**: Visit [Zilliz Cloud](https://cloud.zilliz.com/) and create an account.

   <img src="https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-milvus/main/ballerina/resources/sign_up.png" alt="Zilliz Cloud Sign Up" width="60%">

2. **Set up your account**: Complete the account setup process with your details.

   <img src="https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-milvus/main/ballerina/resources/setup_account.png" alt="Account Setup" width="60%">

3. **Create a new cluster**: From the welcome page, select "Create Cluster" to start setting up your Milvus instance.

   <img src="https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-milvus/main/ballerina/resources/welcome_page.png" alt="Welcome Page" width="60%">

4. **Configure cluster details**: Provide the necessary configuration details for your cluster, including cluster name, cloud provider, and region.

   <img src="https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-milvus/main/ballerina/resources/create_cluster.png" alt="Create Cluster" width="60%">

5. **Download credentials**: Once your cluster is created, download the authentication credentials and connection details.

   <img src="https://raw.githubusercontent.com/ballerina-platform/module-ballerinax-milvus/main/ballerina/resources/cluster_creation.png" alt="Cluster Creation Complete" width="60%">

6. **Generate API Key**: Navigate to the API Keys section in your cluster dashboard and generate an API key for authentication.

## Quickstart

To use the Milvus connector in your Ballerina project, modify the `.bal` file as follows.

### Step 1: Import the module

Import the `ballerinax/milvus` module into your Ballerina project.

```ballerina
import ballerinax/milvus;
```

### Step 2: Instantiate a new connector

Create a `milvus:Client` with the Milvus server URL and optional configuration.

```ballerina
configurable string milvusUrl = ?;

milvus:Client milvusClient = check new(milvusUrl);
```

For authenticated connections, you can provide additional configuration.

```ballerina
milvus:Client milvusClient = check new(milvusUrl);
```

### Step 3: Invoke the connector operations

You can now utilize the operations available within the connector.

```ballerina
public function main() returns error? {
    check milvusClient->createCollection({
        collectionName: "example_collection",
        dimension: 2
    });
}
```

### Step 4: Run the Ballerina application

Use the following command to compile and run the Ballerina program.

```bash
bal run
```
