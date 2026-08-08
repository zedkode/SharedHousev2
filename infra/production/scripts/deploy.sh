#!/usr/bin/env sh
set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
cd "$repository_root"

environment_file=infra/production/production.env
secret_directory=infra/production/secrets
compose_file=infra/production/compose.yaml

if [ ! -f "$environment_file" ]; then
  echo "Missing $environment_file. Copy production.env.example and review it." >&2
  exit 66
fi

for secret_name in postgres_password resend_api_key email_outbox_key cloudflare_tunnel_token; do
  secret_path="$secret_directory/$secret_name"
  if [ ! -s "$secret_path" ]; then
    echo "Missing or empty secret: $secret_path" >&2
    exit 66
  fi
done

docker compose --env-file "$environment_file" -f "$compose_file" config --quiet
docker compose --env-file "$environment_file" -f "$compose_file" build --pull api
docker compose --env-file "$environment_file" -f "$compose_file" up -d --remove-orphans

attempt=0
while [ "$attempt" -lt 30 ]; do
  api_container_id=$(docker compose --env-file "$environment_file" -f "$compose_file" ps -q api)
  status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$api_container_id" 2>/dev/null || true)
  if [ "$status" = healthy ]; then
    break
  fi
  attempt=$((attempt + 1))
  sleep 2
done

if [ "${status:-missing}" != healthy ]; then
  docker compose --env-file "$environment_file" -f "$compose_file" ps
  docker compose --env-file "$environment_file" -f "$compose_file" logs --tail=100 api
  echo "API did not become healthy." >&2
  exit 1
fi

docker compose --env-file "$environment_file" -f "$compose_file" ps
curl --fail --silent --show-error --max-time 15 \
  https://houseapi.dohotstudio.com/v1/health/ready
printf '\nDeployment health gate passed.\n'
