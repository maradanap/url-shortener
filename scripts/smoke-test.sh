#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ALIAS="smoke$(date +%s)"
DESTINATION="https://example.com/smoke-test"

assert_status() {
    local description="$1"
    local expected="$2"
    local actual="$3"

    if [[ "$actual" != "$expected" ]]; then
        echo "FAIL: $description expected $expected but received $actual"
        exit 1
    fi

    echo "PASS: $description returned $actual"
}

echo "Testing URL Shortener at $BASE_URL"
echo "Using alias $ALIAS"

health_status="$(
    curl --silent \
         --output /dev/null \
         --write-out "%{http_code}" \
         "$BASE_URL/actuator/health"
)"

assert_status "Health check" "200" "$health_status"

create_status="$(
    curl --silent \
         --output /tmp/url-shortener-create-response.json \
         --write-out "%{http_code}" \
         --request POST \
         --header "Content-Type: application/json" \
         --data "{\"url\":\"$DESTINATION\",\"customAlias\":\"$ALIAS\"}" \
         "$BASE_URL/api/v1/urls"
)"

assert_status "URL creation" "201" "$create_status"

metadata_status="$(
    curl --silent \
         --output /dev/null \
         --write-out "%{http_code}" \
         "$BASE_URL/api/v1/urls/$ALIAS"
)"

assert_status "Metadata retrieval" "200" "$metadata_status"

redirect_headers="$(
    curl --silent \
         --dump-header - \
         --output /dev/null \
         "$BASE_URL/$ALIAS" |
    tr -d '\r'
)"

redirect_status="$(
    printf '%s\n' "$redirect_headers" |
    awk 'NR == 1 { print $2 }'
)"

redirect_location="$(
    printf '%s\n' "$redirect_headers" |
    awk 'tolower($1) == "location:" { print $2 }'
)"

assert_status "Redirect" "302" "$redirect_status"

if [[ "$redirect_location" != "$DESTINATION" ]]; then
    echo "FAIL: Redirect Location expected $DESTINATION but received $redirect_location"
    exit 1
fi

echo "PASS: Redirect Location is correct"

analytics_response="$(
    curl --silent \
         "$BASE_URL/api/v1/urls/$ALIAS/analytics"
)"

if ! printf '%s' "$analytics_response" |
     grep --extended-regexp --quiet \
     '"totalClicks"[[:space:]]*:[[:space:]]*1'; then

    echo "FAIL: Analytics did not contain totalClicks=1"
    echo "$analytics_response"
    exit 1
fi

echo "PASS: Analytics click count increased"

duplicate_status="$(
    curl --silent \
         --output /dev/null \
         --write-out "%{http_code}" \
         --request POST \
         --header "Content-Type: application/json" \
         --data "{\"url\":\"https://example.com/duplicate\",\"customAlias\":\"$ALIAS\"}" \
         "$BASE_URL/api/v1/urls"
)"

assert_status "Duplicate alias" "409" "$duplicate_status"

invalid_status="$(
    curl --silent \
         --output /dev/null \
         --write-out "%{http_code}" \
         --request POST \
         --header "Content-Type: application/json" \
         --data '{"url":"javascript:alert(1)","customAlias":"invalid-smoke"}' \
         "$BASE_URL/api/v1/urls"
)"

assert_status "Invalid URL" "400" "$invalid_status"

disable_status="$(
    curl --silent \
         --output /dev/null \
         --write-out "%{http_code}" \
         --request DELETE \
         "$BASE_URL/api/v1/urls/$ALIAS"
)"

assert_status "Disable URL" "204" "$disable_status"

gone_status="$(
    curl --silent \
         --output /dev/null \
         --write-out "%{http_code}" \
         "$BASE_URL/$ALIAS"
)"

assert_status "Disabled redirect" "410" "$gone_status"

echo
echo "All smoke tests passed."