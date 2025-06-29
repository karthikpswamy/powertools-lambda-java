/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.lambda.powertools.kafka.serializers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.UnexpectedException;

/**
 * Deserializer for Kafka records using JSON format.
 */
public class KafkaJsonDeserializer extends AbstractKafkaDeserializer {

    @Override
    protected <T> T deserializeObject(byte[] data, Class<T> type, SchemaRegistryType schemaRegistryType) throws IOException {
        String decodedStr = new String(data, StandardCharsets.UTF_8);

        return objectMapper.readValue(decodedStr, type);
    }
}
