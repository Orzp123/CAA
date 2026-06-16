#!/bin/sh
# MinIO bucket initialization script
# Run after MinIO container is healthy

set -e

MC_ALIAS="local"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:-minioadmin}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-minioadmin_secret}"

# Configure mc alias
mc alias set "$MC_ALIAS" "$MINIO_ENDPOINT" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

# Create buckets if they don't exist
mc mb --ignore-existing "$MC_ALIAS/caa-files"
mc mb --ignore-existing "$MC_ALIAS/caa-exports"
mc mb --ignore-existing "$MC_ALIAS/caa-models"

# Set public read policy on caa-files for serving static assets
# mc anonymous set download "$MC_ALIAS/caa-files"

echo "MinIO buckets initialized."
