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

package software.amazon.lambda.powertools.tracing.internal;

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.coldStartDone;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isColdStart;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isHandlerMethod;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.isSamLocal;
import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.serviceName;
import static software.amazon.lambda.powertools.tracing.TracingUtils.objectMapper;

import com.amazonaws.xray.AWSXRay;
import java.util.function.Supplier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingManager;

@Aspect
public final class LambdaTracingAspect {

    private TracingManager tracingManager;

    @SuppressWarnings({"EmptyMethod"})
    @Pointcut("@annotation(tracing)")
    public void callAt(Tracing tracing) {
    }

    public thing MyRequestHandler() {

        @Tracing
        public void HandleRequest() {

        }
    }

    @Around(value = "callAt(tracing) && execution(@Tracing * *.*(..))", argNames = "pjp,tracing")
    public Object around(ProceedingJoinPoint pjp,
                         Tracing tracing) throws Throwable {
        Object[] proceedArgs = pjp.getArgs();

        /**
         * **General strategy so far**
         *  I've tried pushing the X-Ray interaction down behind an interface. Whether or not this
         *  is going to be a sensible approach will depend on how much datadog varies. I figure
         *  we should try stand DD up behind the same interface, and see what falls over.
         *
         *  If it's too fiddly we can basically defer from this join point straight down to the TracingManager
         *  impl for the given provider, and let it set itself up how it wants.
         *
         *  Extra complication is - we'll need some common interface for TracingUtils anyway. So to some
         *  extent we'll need to tease out a common contract, even if we can't use it here ...
         */

        // Start subsegment
        TracingManager.Subsegment segment = tracingManager.startSegment(customSegmentNameOrDefault(tracing,
                        () -> "## " + pjp.getSignature().getName()));

        // Set namespace
        // TODO - does Datadog have this?
        // segment.setNamespace(namespace(tracing));

        if (isHandlerMethod(pjp)) {
            segment.addQueryableTag("ColdStart", isColdStart() ? "true" : "false"); // TODO - need to type this?
            segment.addQueryableTag("Service", namespace(tracing));
        }

        boolean captureResponse = captureResponse(tracing);
        boolean captureError = captureError(tracing);

        try {
            Object methodReturn = pjp.proceed(proceedArgs);
            if (captureResponse) {
                segment.addInformationalTag(namespace(tracing), pjp.getSignature().getName() + " response",
                        null != objectMapper() ? objectMapper().writeValueAsString(methodReturn) : methodReturn);
            }

            if (isHandlerMethod(pjp)) {
                coldStartDone();
            }

            return methodReturn;
        } catch (Exception e) {
            if (captureError) {
                segment.addInformationalTag(namespace(tracing), pjp.getSignature().getName() + " error",
                        null != objectMapper() ? objectMapper().writeValueAsString(e) : e);
            }
            throw e;
        } finally {
            if (!isSamLocal()) {
                AWSXRay.endSubsegment();
            }
        }
    }

    private boolean captureResponse(Tracing powerToolsTracing) {
        switch (powerToolsTracing.captureMode()) {
            case ENVIRONMENT_VAR:
                boolean captureResponse = environmentVariable("POWERTOOLS_TRACER_CAPTURE_RESPONSE");
                return isEnvironmentVariableSet("POWERTOOLS_TRACER_CAPTURE_RESPONSE") ? captureResponse :
                        powerToolsTracing.captureResponse();
            case RESPONSE:
            case RESPONSE_AND_ERROR:
                return true;
            case DISABLED:
            default:
                return false;
        }
    }

    private boolean captureError(Tracing powerToolsTracing) {
        switch (powerToolsTracing.captureMode()) {
            case ENVIRONMENT_VAR:
                boolean captureError = environmentVariable("POWERTOOLS_TRACER_CAPTURE_ERROR");
                return isEnvironmentVariableSet("POWERTOOLS_TRACER_CAPTURE_ERROR") ? captureError :
                        powerToolsTracing.captureError();
            case ERROR:
            case RESPONSE_AND_ERROR:
                return true;
            case DISABLED:
            default:
                return false;
        }
    }

    private String customSegmentNameOrDefault(Tracing powerToolsTracing, Supplier<String> defaultSegmentName) {
        String segmentName = powerToolsTracing.segmentName();
        return segmentName.isEmpty() ? defaultSegmentName.get() : segmentName;
    }

    private String namespace(Tracing powerToolsTracing) {
        return powerToolsTracing.namespace().isEmpty() ? serviceName() : powerToolsTracing.namespace();
    }

    private boolean environmentVariable(String key) {
        return Boolean.parseBoolean(SystemWrapper.getenv(key));
    }

    private boolean isEnvironmentVariableSet(String key) {
        return SystemWrapper.containsKey(key);
    }
}
