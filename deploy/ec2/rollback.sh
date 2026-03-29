#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PREVIOUS_IMAGE_FILE="${DEPLOY_DIR}/.env.images.previous"

if [[ ! -f "${PREVIOUS_IMAGE_FILE}" ]]; then
  echo "No previous deployment metadata found at ${PREVIOUS_IMAGE_FILE}"
  exit 1
fi

set -a
source "${PREVIOUS_IMAGE_FILE}"
set +a

if [[ -z "${API_IMAGE:-}" || -z "${WEB_IMAGE:-}" ]]; then
  echo "Previous deployment metadata is missing API_IMAGE or WEB_IMAGE"
  exit 1
fi

echo "Rolling back to:"
echo "  API_IMAGE=${API_IMAGE}"
echo "  WEB_IMAGE=${WEB_IMAGE}"

GIT_SHA="${GIT_SHA:-rollback}" API_IMAGE="${API_IMAGE}" WEB_IMAGE="${WEB_IMAGE}" "${SCRIPT_DIR}/deploy.sh"
