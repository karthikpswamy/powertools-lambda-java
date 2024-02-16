#!/bin/bash

set -e

export MAX_REQUESTS=1000
export MAX_PARALLEL_REQUESTS=100
export TEMPFILE_COLD=$(mktemp)
export TEMPFILE_HOT=$(mktemp)

echo "Cold: $TEMPFILE_COLD"
echo "Hot: $TEMPFILE_HOT"

# Get the API URL
export API_URL=$(aws cloudformation describe-stacks --stack-name PtPerfTest --query "Stacks[0].Outputs[?OutputKey=='ParametersApi'].OutputValue" --output text)
echo API URL: $API_URL

# Function to make a request and record its latency and status code
make_request() {
    # Time and execute
    START=$(gdate +%s.%N)
    STATUS_AND_BODY=$(curl -o - "$API_URL" -s -w "\n%{http_code}" )
    END=$(gdate +%s.%N)
    LATENCY=$(echo "$END - $START" | bc)

    # Pull out status code, and response body. Response body will
    # tell us if it was a hot or cold start
    STATUS_CODE=$(echo "$STATUS_AND_BODY" | tail -n 1)
    RESPONSE_BODY=$(echo "$STATUS_AND_BODY" | head -n 1)

    # Write it out
    if [[ "$RESPONSE_BODY" == *"cold"* ]]; then
        echo "$LATENCY $STATUS_CODE" >> "$TEMPFILE_COLD"
    elif [[ "$RESPONSE_BODY" == *"hot"* ]]; then
        echo "$LATENCY $STATUS_CODE" >> "$TEMPFILE_HOT"
    fi
}
export -f make_request

# Generate MAX_REQUESTS lines, use xargs to run make_request in parallel
seq $MAX_REQUESTS | xargs -I {} -P $MAX_PARALLEL_REQUESTS bash -c 'make_request'


# Calculate and display results
calculate_results() {
    TEMPFILE=$1
    TYPE=$2

    if [ ! -s "$TEMPFILE" ]; then
        # File is empty or does not exist
        echo "No data collected for $TYPE!"
        return
    fi

    TOTAL_REQUESTS=$(wc -l $TEMPFILE)
    TOTAL_FAILURES=$(awk '{if ($2 >= 400) print $0}' $TEMPFILE | wc -l)
    AVERAGE_LATENCY=$(awk '{total += $1; count++} END {print total/count}' $TEMPFILE)
    MIN_LATENCY=$(sort -n $TEMPFILE | head -n 1 | awk '{print $1}')
    MAX_LATENCY=$(sort -n $TEMPFILE | tail -n 1 | awk '{print $1}')

    echo "$TYPE Total Requests: $TOTAL_REQUESTS"
    echo "$TYPE Total Failures: $TOTAL_FAILURES"
    echo "$TYPE Average Latency: $AVERAGE_LATENCY seconds"
    echo "$TYPE Min Latency: $MIN_LATENCY seconds"
    echo "$TYPE Max Latency: $MAX_LATENCY seconds"
}

calculate_results "$TEMPFILE_COLD" "Cold"
calculate_results "$TEMPFILE_HOT" "Hot"

rm "$TEMPFILE_HOT"
rm "$TEMPFILE_COLD"