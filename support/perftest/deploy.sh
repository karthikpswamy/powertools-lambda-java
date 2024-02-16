#!/bin/bash

set -e

export TEST_EXAMPLE=parameters
export TEST_EXAMPLE_FULLNAME=powertools-examples-$TEST_EXAMPLE
export TEST_EXAMPLE_PATH=$(dirname "$0")/../../examples/$TEST_EXAMPLE_FULLNAME
if [[ "$TEST_EXAMPLE" == "core-utilities" ]]; then
  TEST_EXAMPLE_PATH=$(dirname "$0")/../../examples/$TEST_EXAMPLE_FULLNAME/sam
fi

echo Building $TEST_EXAMPLE_FULLNAME
pushd "$TEST_EXAMPLE_PATH" || exit
mvn clean install package
sam build

echo Deploying $TEST_EXAMPLE_FULLNAME
sam deploy --stack-name PtPerfTest --resolve-s3 --capabilities CAPABILITY_IAM
API_URL=$(aws cloudformation describe-stacks --stack-name PtPerfTest --query "Stacks[0].Outputs[?OutputKey=='HelloWorldApi'].OutputValue" --output text)
echo API URL: $API_URL

echo Finding jar ...
DEPLOYMENT_JAR=$(find . -type f -name "$TEST_EXAMPLE_FULLNAME-*.jar" | head -n 1)
if [[ -n "$DEPLOYMENT_JAR" ]]; then
  # Use stat to get the file size in bytes and convert it to megabytes
  FILESIZE_BYTES=$(stat -f%z "$DEPLOYMENT_JAR")
  FILESIZE_MB=$(echo "scale=2; $FILESIZE_BYTES/1024" | bc)
  echo "$DEPLOYMENT_JAR size is $FILESIZE_MB KB"
else
  echo "File not found."
  exit 1
fi

echo Finding SAM function package ...
SAM_FUNCTION_PACKAGE=$(find .aws-sam -type d -name "*Function" | head -n 1)
if [[ -n "$SAM_FUNCTION_PACKAGE" ]]; then
  echo "Found SAM package at $SAM_FUNCTION_PACKAGE"
  SAM_FUNCTION_PACKAGE_SIZE=$(du -sk "$SAM_FUNCTION_PACKAGE" | awk '{print $1/1024 " MB"}')
  echo "SAM package size is $SAM_FUNCTION_PACKAGE_SIZE"
else
  echo "SAM package not found."
  exit 1
fi
