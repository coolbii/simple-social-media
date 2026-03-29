#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${API_IMAGE:-}" || -z "${WEB_IMAGE:-}" ]]; then
  echo "API_IMAGE and WEB_IMAGE must be set"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1/api/actuator/health}"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-60}"

cd "${DEPLOY_DIR}"

if [[ ! -f ".env.api" ]]; then
  echo ".env.api is missing in ${DEPLOY_DIR}"
  exit 1
fi

if [[ -f ".env.images.current" ]]; then
  cp ".env.images.current" ".env.images.previous"
fi

docker compose -f docker-compose.ec2.yml pull
docker compose -f docker-compose.ec2.yml up -d --remove-orphans

echo "Waiting for API health: ${HEALTH_URL}"
for ((i=1; i<=HEALTH_TIMEOUT_SECONDS; i++)); do
  if curl -fsS "${HEALTH_URL}" | grep -q '"status":"UP"'; then
    cat > ".env.images.current" <<EOF
API_IMAGE=${API_IMAGE}
WEB_IMAGE=${WEB_IMAGE}
DEPLOYED_AT_UTC=$(date -u +%Y-%m-%dT%H:%M:%SZ)
GIT_SHA=${GIT_SHA:-unknown}
EOF
    docker image prune -f
    echo "Deployment successful"
    exit 0
  fi
  sleep 1
done

echo "Deployment failed: API health check timed out (${HEALTH_TIMEOUT_SECONDS}s)"
docker compose -f docker-compose.ec2.yml ps
docker compose -f docker-compose.ec2.yml logs api --tail=200
exit 1
