#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/simple-social-media}"
TARGET_USER="${TARGET_USER:-${SUDO_USER:-$USER}}"

sudo apt-get update
sudo apt-get install -y ca-certificates curl docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "${TARGET_USER}"

sudo mkdir -p "${DEPLOY_ROOT}/deploy"
sudo chown -R "${TARGET_USER}:${TARGET_USER}" "${DEPLOY_ROOT}"

cat <<EOF
Bootstrap complete.
Next:
1. Re-login (or run 'newgrp docker') so docker group is applied.
2. Copy deploy files to ${DEPLOY_ROOT}/deploy.
3. Create ${DEPLOY_ROOT}/deploy/.env.api with production values.
EOF
