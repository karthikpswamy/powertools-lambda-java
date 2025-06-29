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
 *
 */

package software.amazon.lambda.powertools.logging;

/**
 * Supported Event types from which Correlation ID can be extracted
 */
public class CorrelationIdPaths {
    /**
     * To use when function is expecting API Gateway Rest API Request event
     */
    public static final String API_GATEWAY_REST = "requestContext.requestId";
    /**
     * To use when function is expecting API Gateway HTTP API Request event
     */
    public static final String API_GATEWAY_HTTP = "requestContext.requestId";
    /**
     * To use when function is expecting Application Load balancer Request event
     */
    public static final String APPLICATION_LOAD_BALANCER = "headers.\"x-amzn-trace-id\"";
    /**
     * To use when function is expecting Event Bridge Request event
     */
    public static final String EVENT_BRIDGE = "id";
    /**
     * To use when function is expecting an AppSync request
     */
    public static final String APPSYNC_RESOLVER = "request.headers.\"x-amzn-trace-id\"";
}
