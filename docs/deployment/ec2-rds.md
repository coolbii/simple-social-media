# EC2 + RDS Deployment Runbook

## Architecture

- `web` container (Nginx) serves Vue static files on port `80`
- `web` proxies `/api/*` to `api:8080` in Docker network
- `api` container connects to MySQL on Amazon RDS
- uploaded media remains in S3

This avoids cross-origin cookie issues by keeping browser traffic on one origin.

## 1. One-time AWS setup

1. Create an RDS MySQL instance (private subnet preferred).
2. Create an EC2 instance in the same VPC.
3. Security group rules:
   - EC2 inbound: `80` (and `443` if TLS) from Internet
   - RDS inbound: `3306` from EC2 security group only
4. Ensure S3/IAM/Twilio production credentials are ready.

## 2. EC2 bootstrap

Run once on EC2:

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker

mkdir -p /opt/simple-social-media/deploy
```

Create production API env file:

```bash
cd /opt/simple-social-media/deploy
cp .env.api.example .env.api
# edit .env.api with real RDS/S3/Twilio/Auth values
```

## 3. GitHub Secrets

Repository secrets required by `.github/workflows/cd-ec2.yml`:

- `EC2_HOST`: public host or IP
- `EC2_USER`: SSH user (for example `ubuntu`)
- `EC2_SSH_PRIVATE_KEY`: private key for EC2
- `GHCR_READ_TOKEN`: PAT with `read:packages` to pull GHCR images

## 4. CI pipeline

`.github/workflows/ci.yml` runs on PR and main:

- lint (web + api)
- tests (web + api)
- builds (web + api)

CI uses a MySQL 8 service container so backend tests run with realistic DB behavior.

## 5. CD pipeline

`.github/workflows/cd-ec2.yml`:

1. Build API and web Docker images
2. Push images to GHCR with immutable tag `${GITHUB_SHA}`
3. SSH to EC2 and run deploy script:
   - `docker compose pull`
   - `docker compose up -d --remove-orphans`

## 6. Operations checks

On EC2:

```bash
cd /opt/simple-social-media/deploy
docker compose -f docker-compose.ec2.yml ps
docker compose -f docker-compose.ec2.yml logs api --tail=100
docker compose -f docker-compose.ec2.yml logs web --tail=100
curl -i http://127.0.0.1/api/auth/me
curl -i http://127.0.0.1/api/posts
```

## 7. Best-practice notes

- deploy by immutable image tag (`sha`) instead of rebuilding on EC2
- keep secrets in EC2 `.env.api` and GitHub Secrets, not in repo
- run API behind reverse proxy (single-origin cookie/session flow)
- use HTTPS in production and set `AUTH_COOKIE_SECURE=true`
- keep RDS private and reachable only from EC2 security group
