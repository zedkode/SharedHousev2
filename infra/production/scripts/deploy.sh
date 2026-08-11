#!/usr/bin/env sh
set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
cd "$repository_root"

environment_file=infra/production/production.env
secret_directory=infra/production/secrets
compose_file=infra/production/compose.yaml
compose_project=sharedhouse-production
compose_working_directory=$(CDPATH= cd -- "$(dirname -- "$compose_file")" && pwd)

case "$repository_root" in
  /home/*) ;;
  *) echo "Safety policy: deploy SharedHouse only below /home (current path: $repository_root)." >&2; exit 73 ;;
esac

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

grep -q "^SHAREDHOUSE_COMPOSE_PROJECT=$compose_project$" "$environment_file" || {
  echo "SHAREDHOUSE_COMPOSE_PROJECT must be $compose_project." >&2
  exit 66
}

existing_project_ids=$(docker ps -aq --filter "label=com.docker.compose.project=$compose_project")
for container_id in $existing_project_ids; do
  working_directory=$(docker inspect --format '{{index .Config.Labels "com.docker.compose.project.working_dir"}}' "$container_id")
  if [ "$working_directory" != "$compose_working_directory" ]; then
    echo "Refusing to touch Compose project $compose_project owned by $working_directory." >&2
    exit 73
  fi
done

inventory_before=$(mktemp "$repository_root/.sharedhouse-containers-before.XXXXXX")
inventory_after=$(mktemp "$repository_root/.sharedhouse-containers-after.XXXXXX")
cleanup() {
  rm -f -- "$inventory_before" "$inventory_after"
}
trap cleanup EXIT HUP INT TERM

snapshot_unrelated_containers() {
  destination=$1
  docker ps -aq | while IFS= read -r container_id; do
    [ -n "$container_id" ] || continue
    project=$(docker inspect --format '{{index .Config.Labels "com.docker.compose.project"}}' "$container_id")
    [ "$project" = "$compose_project" ] && continue
    docker inspect --format '{{.Id}}|{{.State.Status}}|{{.State.StartedAt}}|{{.RestartCount}}' "$container_id"
  done | sort > "$destination"
}

snapshot_unrelated_containers "$inventory_before"

docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" config --quiet
docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" build --pull api workers
docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" up -d --remove-orphans

snapshot_unrelated_containers "$inventory_after"
if ! cmp -s "$inventory_before" "$inventory_after"; then
  echo "SAFETY ALERT: a pre-existing container changed during deployment." >&2
  diff -u "$inventory_before" "$inventory_after" || true
  echo "SharedHouse will not continue to the public health gate. Investigate before any further action." >&2
  exit 74
fi
echo "Container isolation gate passed: every pre-existing container is unchanged."

attempt=0
while [ "$attempt" -lt 30 ]; do
  api_container_id=$(docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" ps -q api)
  status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$api_container_id" 2>/dev/null || true)
  if [ "$status" = healthy ]; then
    break
  fi
  attempt=$((attempt + 1))
  sleep 2
done

if [ "${status:-missing}" != healthy ]; then
  docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" ps
  docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" logs --tail=100 api
  echo "API did not become healthy." >&2
  exit 1
fi

worker_container_id=$(docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" ps -q workers)
worker_status=$(docker inspect --format '{{.State.Status}}' "$worker_container_id" 2>/dev/null || true)
if [ "$worker_status" != running ]; then
  docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" logs --tail=100 workers
  echo "Workers service is not running." >&2
  exit 1
fi

docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" ps
curl --fail --silent --show-error --max-time 15 \
  https://houseapi.dohotstudio.com/v1/health/ready
printf '\nDeployment health gate passed.\n'
