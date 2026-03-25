#!/bin/bash
set -e

echo "========================================="
echo " Starting Dockerized Test Environment    "
echo "========================================="

# Pull down any old containers
docker compose --profile test down -v

# Run the test-runner container, aborting and failing if it fails
set +e
docker compose --profile test up --build --abort-on-container-exit --exit-code-from test-runner test-runner
TEST_STATUS=$?
set -e

# Clean up
docker compose --profile test down -v

echo "========================================="
if [ $TEST_STATUS -eq 0 ]; then
    echo "[PASS] All tests passed in Docker!"
    exit 0
else
    echo "[FAIL] Tests failed in Docker environment."
    exit 1
fi
