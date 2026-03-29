# EC2 + RDS Deployment Runbook

## Architecture

- `web` container (Nginx) serves Vue static files on port `80`
- `web` proxies `/api/*` to `api:8080` in Docker network
- `api` container connects to MySQL on Amazon RDS
- uploaded media remains in S3

This keeps browser traffic in a single origin (cookie/session friendly).

## 1. One-time AWS setup

1. Create an RDS MySQL instance in private subnets.
2. Create an EC2 instance in the same VPC.
3. Security group rules:
   - EC2 inbound: `80` (and `443` after TLS) from internet
   - RDS inbound: `3306` only from EC2 security group
4. Create an IAM role for EC2 and attach S3 least-privilege policy:
   - use [iam-policy-s3-media.json](/Users/hezibin/bindev/simple-social-media/deploy/aws/iam-policy-s3-media.json)
   - replace `REPLACE_BUCKET_NAME` with your real bucket
5. Attach the IAM role to EC2 instance profile.

Recommended: use IAM role credentials on EC2 and do not store AWS access keys in `.env.api`.

## 2. EC2 bootstrap

Run once on EC2 (Ubuntu):

```bash
cd /tmp
curl -fsSL -o bootstrap-ubuntu.sh https://raw.githubusercontent.com/<your-org>/<your-repo>/main/deploy/ec2/bootstrap-ubuntu.sh
chmod +x bootstrap-ubuntu.sh
./bootstrap-ubuntu.sh
```

If you prefer local script copy, run [bootstrap-ubuntu.sh](/Users/hezibin/bindev/simple-social-media/deploy/ec2/bootstrap-ubuntu.sh) directly.

Prepare runtime env:

```bash
cd /opt/simple-social-media/deploy
cp .env.api.example .env.api
# fill .env.api with RDS/Twilio/Auth/S3 settings
```

Critical checks:

- `AWS_REGION` must match the real bucket region
- `MYSQL_HOST` points to RDS endpoint
- `AUTH_COOKIE_SECURE=true` only when HTTPS is enabled

## 3. GitHub Secrets

Repository secrets for `.github/workflows/cd-ec2.yml`:

- `EC2_HOST`: public host or IP
- `EC2_USER`: SSH user (example: `ubuntu`)
- `EC2_SSH_PRIVATE_KEY`: private key for EC2
- `EC2_SSH_KNOWN_HOSTS` (recommended): exact known_hosts line for host key pinning
- `GHCR_READ_TOKEN`: PAT with `read:packages` to pull GHCR images

If `EC2_SSH_KNOWN_HOSTS` is absent, workflow falls back to `ssh-keyscan` (less strict).

## 4. CI pipeline

`.github/workflows/ci.yml` runs on PR and main:

- lint (`web` + `api`)
- tests (`web` + `api`)
- builds (`web` + `api`)

CI uses MySQL service container so API tests run against real MySQL behavior.

## 5. CD pipeline

`.github/workflows/cd-ec2.yml`:

1. build API and web Docker images
2. push images to GHCR by immutable `${GITHUB_SHA}` tags
3. upload `deploy/` manifests to EC2
4. run [deploy.sh](/Users/hezibin/bindev/simple-social-media/deploy/ec2/deploy.sh) remotely
5. wait until `/api/actuator/health` returns `UP`

Deployment metadata is written to:

- `/opt/simple-social-media/deploy/.env.images.current`
- `/opt/simple-social-media/deploy/.env.images.previous`

## 6. Rollback

On EC2:

```bash
cd /opt/simple-social-media/deploy
./ec2/rollback.sh
```

`rollback.sh` deploys image tags from `.env.images.previous`.

## 7. Operations checks

On EC2:

```bash
cd /opt/simple-social-media/deploy
docker compose -f docker-compose.ec2.yml ps
docker compose -f docker-compose.ec2.yml logs api --tail=200
docker compose -f docker-compose.ec2.yml logs web --tail=200
curl -fsS http://127.0.0.1/api/actuator/health
curl -i http://127.0.0.1/api/posts
```

## 8. Production hardening checklist

- enable HTTPS (ALB or Nginx+Certbot), then keep `AUTH_COOKIE_SECURE=true`
- keep RDS private and backup enabled
- limit S3 permission scope to `post-images/*`
- rotate Twilio/Auth secrets periodically
- set CloudWatch alarms for EC2 CPU/memory and RDS free storage
