#!/bin/bash

set -e

export MAX_REQUESTS=1000
export MAX_PARALLEL_REQUESTS=20
export TEMPFILE=$(mktemp)

# Get the API URL
export API_URL=$(aws cloudformation describe-stacks --stack-name PtPerfTest --query "Stacks[0].Outputs[?OutputKey=='HelloWorldApi'].OutputValue" --output text)
echo API URL: $API_URL

# Function to make a request and record its latency and status code
make_request() {
    start=$(gdate +%s.%N)
    status_code=$(curl -o /dev/null $API_URL -s -w "%{http_code}" )
    end=$(gdate +%s.%N)
    latency=$(echo "$end - $start" | bc)
    echo "$latency $status_code" >> $TEMPFILE
}
export -f make_request

# Generate MAX_REQUESTS lines, use xargs to run make_request in parallel
seq $MAX_REQUESTS | xargs -I {} -P $MAX_PARALLEL_REQUESTS bash -c 'make_request'

# Calculate and report results
TOTAL_FAILURES=$(awk '{if ($2 >= 400) print $0}' "$TEMPFILE" | wc -l)
AVERAGE_LATENCY=$(awk '{total += $1; count++} END {print total/count}' "$TEMPFILE")

echo "Total Failures: $TOTAL_FAILURES"
echo "Average Latency: $AVERAGE_LATENCY seconds"

# Cleanup
rm "$TEMPFILE"
