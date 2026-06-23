#!/bin/bash
# infra/nacos/init-config.sh
# Publishes caa-common.yaml to Nacos with default values.
# Run once after Nacos is healthy. Safe to re-run (PUT is idempotent).

set -euo pipefail

NACOS_ADDR="${NACOS_SERVER_ADDR:-localhost:8848}"
NACOS_USER="${NACOS_USERNAME:-nacos}"
NACOS_PASS="${NACOS_PASSWORD:-nacos}"
NAMESPACE="${NACOS_NAMESPACE:-}"
GROUP="DEFAULT_GROUP"
DATA_ID="caa-common.yaml"

echo "Waiting for Nacos at ${NACOS_ADDR}..."
until curl -sf "http://${NACOS_ADDR}/nacos/actuator/health" > /dev/null 2>&1; do
  sleep 3
done
echo "Nacos is ready."

CONFIG_CONTENT=$(cat <<'YAML'
# CAA common configuration — managed by Nacos, hot-reloaded via @RefreshScope

jwt:
  algorithm: HS256
  secret: ${JWT_SECRET:please-change-this-secret-in-production-min-32-chars}
  expiration_seconds: 7200

login:
  max_fail_count: 5
  lock_duration_seconds: 900

captcha:
  expiration_seconds: 300

single_device:
  enabled: false

gateway:
  whitelist:
    - /auth/login
    - /auth/captcha
    - /auth/wechat/authorize
    - /auth/wechat/callback
    - /auth/sso/*/authorize
    - /auth/sso/*/callback
    - /auth/tenant-config
    - /actuator/health
    - /actuator/info
    - /v3/api-docs/**
    - /swagger-ui/**
YAML
)

NS_PARAM=""
if [ -n "$NAMESPACE" ]; then
  NS_PARAM="&tenant=${NAMESPACE}"
fi

echo "Publishing ${DATA_ID} to Nacos (group=${GROUP})..."
HTTP_CODE=$(curl -sf -o /dev/null -w "%{http_code}" -X POST \
  "http://${NACOS_ADDR}/nacos/v1/cs/configs${NS_PARAM:+?tenant=${NAMESPACE}}" \
  -u "${NACOS_USER}:${NACOS_PASS}" \
  --data-urlencode "dataId=${DATA_ID}" \
  --data-urlencode "group=${GROUP}" \
  --data-urlencode "content=${CONFIG_CONTENT}" \
  --data-urlencode "type=yaml")

if [ "$HTTP_CODE" = "200" ]; then
  echo "Published ${DATA_ID} successfully."
else
  echo "WARNING: Nacos returned HTTP ${HTTP_CODE} for ${DATA_ID}"
fi

echo "Nacos config initialization complete."
