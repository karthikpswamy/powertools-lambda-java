# Performance Testing

A simple set of scripts useful for testing the performance of Powertools, focussing on measuring 
cold start duration. This lets us make changes to the library - e.g., Switching SDK versions - and
measuring what impact that has on our performance.

## Process

1. Make sure that `aws.sdk.version` is set to the latest. It's important the same version is used for each test suite! 
2. `mvn clean install -DskipTests` the root of the powertools project