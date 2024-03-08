package software.amazon.lambda.powertools.tracing;

public interface TracingManager {
    
    Subsegment startSegment(String segmentName);
    
    interface Subsegment {

        // TODO
        // Need to clarify this against DD interface. In X-ray, (https://docs.aws.amazon.com/xray/latest/devguide/xray-sdk-java-segment.html)
        // 'Annotations' - are queryable key-value pairs - e.g. you can filter in UI on them
        // 'Metadata' - is also KV, but not queryable in the UI, only viewable.
        void addQueryableTag(String tagName, String tagValue);

        // ... I've made a coarse cut of this here. Not happy with naming. Let's see.
        // I think my  ....
        //         queryableTag      = Annotation (X-Ray) = Tag (Datadog)
        //         informationalTag  = Metadata   (X-Ray) = Attribute (Datadog)
        void addInformationalTag(String tagName, Object tagValue);

        // ... X-Ray has a variant of this that sets namespace too. Datadog?
        void addInformationalTag(String namespace, String tagName, Object tagValue);
    }
}
