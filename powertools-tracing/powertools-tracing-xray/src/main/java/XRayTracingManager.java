import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import software.amazon.lambda.powertools.tracing.TracingManager;


public class XRayTracingManager implements TracingManager {
    @Override
    public Subsegment startSegment(String segmentName) {
        return new XRaySubsegment(segmentName);
    }

    @Override
    public void stopSegment(Subsegment subsegment) {
        if (subsegment instanceof XRaySubsegment) {
            ((XRaySubsegment)subsegment).end();
        } else {
            throw new RuntimeException("TODO - thing about this");
        }
    }

    private class XRaySubsegment implements Subsegment {

        private final com.amazonaws.xray.entities.Subsegment subsegment;

        public XRaySubsegment(String subsegmentName) {
            this.subsegment = AWSXRay.beginSubsegment(subsegmentName);
        }

        @Override
        public void addQueryableTag(String tagName, String tagValue) {
            this.subsegment.putAnnotation(tagName, tagValue);
        }

        @Override
        public void addInformationalTag(String tagName, Object tagValue) {
            this.subsegment.putMetadata(tagName, tagValue);
        }

        public void addInformationalTag(String namespace, String tagName, Object tagValue) {
            this.subsegment.putMetadata(namespace, tagName, tagValue);
        }

        public void end() {
            AWSXRay.endSubsegment(subsegment);
        }
    }
}
