#!/usr/bin/env sh
set -eu

repository_root=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
cd "$repository_root"

environment_file=infra/production/production.env
environment_template=infra/production/production.env.example
secret_directory=infra/production/secrets
compose_file=infra/production/compose.yaml
production_hostname=houseapi.dohotstudio.com
compose_project=sharedhouse-production
compose_working_directory=$(CDPATH= cd -- "$(dirname -- "$compose_file")" && pwd)
mode=install
temporary_secret=""
temporary_environment=""

cleanup() {
  stty echo 2>/dev/null || true
  [ -z "$temporary_secret" ] || rm -f -- "$temporary_secret"
  [ -z "$temporary_environment" ] || rm -f -- "$temporary_environment"
}
trap cleanup EXIT HUP INT TERM

case "${1:-}" in
  "") ;;
  --preflight) mode=preflight ;;
  --help|-h)
    echo "Usage: $0 [--preflight]"
    echo "  no option    configure secrets interactively and optionally deploy"
    echo "  --preflight  read-only validation of the prepared production stack"
    exit 0
    ;;
  *) echo "Unknown option: $1" >&2; exit 64 ;;
esac

say() {
  printf '%s\n' "$*"
}

fail() {
  say "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command is missing: $1"
}

confirm() {
  question=$1
  default_answer=$2
  if [ ! -t 0 ]; then
    fail "Interactive input is required. Run this script from a terminal."
  fi
  if [ "$default_answer" = yes ]; then
    suffix='[Y/n]'
  else
    suffix='[y/N]'
  fi
  printf '%s %s ' "$question" "$suffix" >&2
  IFS= read -r answer
  case "$answer" in
    y|Y|yes|YES) return 0 ;;
    n|N|no|NO) return 1 ;;
    "") [ "$default_answer" = yes ] ;;
    *) say "Please answer y or n." >&2; confirm "$question" "$default_answer" ;;
  esac
}

read_hidden() {
  label=$1
  printf '%s: ' "$label" >&2
  stty -echo
  IFS= read -r REPLY
  stty echo
  printf '\n' >&2
}

write_secret() {
  secret_name=$1
  secret_value=$2
  secret_path="$secret_directory/$secret_name"
  temporary_secret="$secret_path.tmp.$$"
  umask 077
  printf '%s' "$secret_value" > "$temporary_secret"
  chmod 600 "$temporary_secret"
  mv -- "$temporary_secret" "$secret_path"
  temporary_secret=""
}

check_platform() {
  [ "$(uname -s)" = Linux ] || fail "The production installer supports Linux VPS hosts only."
  case "$repository_root" in
    /home/*) ;;
    *) fail "Safety policy: install SharedHouse only below /home (current path: $repository_root)." ;;
  esac
  architecture=$(uname -m)
  case "$architecture" in
    x86_64|aarch64|arm64) ;;
    *) fail "Unsupported VPS architecture: $architecture" ;;
  esac
  [ -f "$compose_file" ] || fail "Run the installer from a complete SharedHouse repository."
  [ -f "$environment_template" ] || fail "Missing $environment_template"
}

check_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    say "Docker Engine is not installed." >&2
    say "Install it from https://docs.docker.com/engine/install/ for this VPS distribution," >&2
    say "then rerun the wizard. SharedHouse does not execute an unreviewed remote install script." >&2
    exit 69
  fi
  docker compose version >/dev/null 2>&1 || fail "Docker Compose plugin is unavailable."
  docker info >/dev/null 2>&1 || fail "Docker is not running or this user cannot access it."
}

check_capacity() {
  available_kb=$(df -Pk "$repository_root" | awk 'NR == 2 { print $4 }')
  case "$available_kb" in
    ''|*[!0-9]*) say "WARNING: Could not determine available disk space." ;;
    *)
      if [ "$available_kb" -lt 10485760 ]; then
        fail "At least 10 GB of free disk space is required before installation."
      fi
      ;;
  esac
}

validate_files() {
  [ -s "$environment_file" ] || fail "Missing $environment_file"
  for secret_name in postgres_password resend_api_key email_outbox_key cloudflare_tunnel_token; do
    secret_path="$secret_directory/$secret_name"
    [ -s "$secret_path" ] || fail "Missing or empty secret file: $secret_path"
    permissions=$(stat -c '%a' "$secret_path" 2>/dev/null || true)
    [ "$permissions" = 640 ] || fail "$secret_path must have permission 640, found ${permissions:-unknown}."
  done
  grep -q '^NODE_ENV=production$' "$environment_file" || fail "NODE_ENV must be production."
  grep -q "^SHAREDHOUSE_COMPOSE_PROJECT=$compose_project$" "$environment_file" ||
    fail "SHAREDHOUSE_COMPOSE_PROJECT must be $compose_project."
  grep -q '^AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE=false$' "$environment_file" ||
    fail "Development verification codes must remain disabled."
  grep -q '^EMAIL_PROVIDER=resend$' "$environment_file" || fail "EMAIL_PROVIDER must be resend."
  docker compose -p "$compose_project" --env-file "$environment_file" -f "$compose_file" config --quiet
}

check_compose_ownership() {
  existing_ids=$(docker ps -aq --filter "label=com.docker.compose.project=$compose_project")
  [ -n "$existing_ids" ] || return 0

  for container_id in $existing_ids; do
    working_directory=$(docker inspect --format '{{index .Config.Labels "com.docker.compose.project.working_dir"}}' "$container_id")
    [ "$working_directory" = "$compose_working_directory" ] ||
      fail "Compose project $compose_project is already owned by another directory: $working_directory"
  done
  say "Existing SharedHouse containers belong to this installation and may be upgraded."
}

run_preflight() {
  say "SharedHouse production preflight"
  check_platform
  require_command awk
  require_command curl
  require_command grep
  require_command stat
  check_docker
  check_compose_ownership
  check_capacity
  validate_files
  say "Configuration and secret permissions: OK"

  if command -v getent >/dev/null 2>&1 && getent hosts "$production_hostname" >/dev/null 2>&1; then
    say "DNS for $production_hostname: resolved"
  else
    fail "DNS for $production_hostname does not resolve yet. Create the Cloudflare Tunnel route."
  fi
  readiness=$(curl --fail --silent --show-error --max-time 15 \
    "https://$production_hostname/v1/health/ready") ||
    fail "The public readiness endpoint is not healthy."
  case "$readiness" in
    *'"status":"ok"'*) say "Public API readiness: OK" ;;
    *) fail "The readiness response did not contain status=ok." ;;
  esac
  say "Preflight passed. The public API is reachable and database-ready."
}

if [ "$mode" = preflight ]; then
  run_preflight
  exit 0
fi

say "SharedHouse interactive production installer"
say "This wizard does not print secrets and does not open public database/API ports."
say "It preserves existing secret files unless you explicitly replace them."
say ""
check_platform
require_command awk
require_command curl
require_command grep
require_command openssl
require_command stat
check_docker
check_compose_ownership
check_capacity

mkdir -p "$secret_directory"
chmod 700 "$secret_directory"
umask 077

postgres_secret="$secret_directory/postgres_password"
if [ ! -s "$postgres_secret" ]; then
  write_secret postgres_password "$(openssl rand -base64 36)"
  say "Generated PostgreSQL password."
else
  say "Preserved existing PostgreSQL password."
fi

outbox_secret="$secret_directory/email_outbox_key"
if [ ! -s "$outbox_secret" ]; then
  write_secret email_outbox_key "$(openssl rand -base64 32)"
  say "Generated email outbox encryption key."
else
  say "Preserved existing email outbox encryption key."
fi

resend_secret="$secret_directory/resend_api_key"
if [ -s "$resend_secret" ] && ! confirm "Replace the existing Resend API key?" no; then
  say "Preserved existing Resend API key."
else
  read_hidden "Resend sending-only API key"
  case "$REPLY" in
    re_*) ;;
    *) fail "The Resend key must start with re_." ;;
  esac
  [ "${#REPLY}" -ge 20 ] || fail "The Resend key is unexpectedly short."
  write_secret resend_api_key "$REPLY"
  REPLY=""
fi

tunnel_secret="$secret_directory/cloudflare_tunnel_token"
if [ -s "$tunnel_secret" ] && ! confirm "Replace the existing Cloudflare Tunnel token?" no; then
  say "Preserved existing Cloudflare Tunnel token."
else
  read_hidden "Cloudflare Tunnel token"
  [ "${#REPLY}" -ge 40 ] || fail "The Cloudflare Tunnel token is unexpectedly short."
  write_secret cloudflare_tunnel_token "$REPLY"
  REPLY=""
fi

if [ -s "$environment_file" ] && ! confirm "Replace the existing non-secret production.env?" no; then
  say "Preserved $environment_file."
else
  printf 'Verification sender [%s]: ' 'SharedHouse <verify@mail.dohotstudio.com>' >&2
  IFS= read -r email_from
  if [ -z "$email_from" ]; then
    email_from='SharedHouse <verify@mail.dohotstudio.com>'
  fi
  case "$email_from" in
    *'@mail.dohotstudio.com>'|*'@mail.dohotstudio.com') ;;
    *) fail "The sender must use the verified mail.dohotstudio.com domain." ;;
  esac
  printf 'Immutable image tag [%s]: ' '0.1.0' >&2
  IFS= read -r image_tag
  image_tag=${image_tag:-0.1.0}
  case "$image_tag" in
    *[!A-Za-z0-9._-]*|'') fail "The image tag contains unsupported characters." ;;
  esac
  temporary_environment="$environment_file.tmp.$$"
  {
    printf '%s\n' 'NODE_ENV=production'
    printf 'SHAREDHOUSE_COMPOSE_PROJECT=%s\n' "$compose_project"
    printf '%s\n' 'PORT=3000'
    printf '%s\n' 'DATABASE_URL=postgresql://sharedhouse@postgres:5432/sharedhouse'
    printf '%s\n' 'AUTH_EXPOSE_DEVELOPMENT_VERIFICATION_CODE=false'
    printf '%s\n' 'EMAIL_PROVIDER=resend'
    printf 'EMAIL_FROM=%s\n' "$email_from"
    printf 'SHAREDHOUSE_IMAGE_TAG=%s\n' "$image_tag"
  } > "$temporary_environment"
  chmod 600 "$temporary_environment"
  mv -- "$temporary_environment" "$environment_file"
  temporary_environment=""
  say "Wrote $environment_file without secret values."
fi

chgrp 0 "$secret_directory"/*
chmod 640 "$secret_directory"/*
chmod 600 "$environment_file"
validate_files
say "Compose configuration and secret permissions are valid."
say ""
say "Cloudflare must publish $production_hostname to http://api:3000."
say "Resend must show mail.dohotstudio.com, SPF and DKIM as verified."

cloudflare_ready=no
resend_ready=no
if confirm "Is the Cloudflare Tunnel route already saved?" no; then cloudflare_ready=yes; fi
if confirm "Is the Resend sending domain verified?" no; then resend_ready=yes; fi

if [ "$cloudflare_ready" != yes ] || [ "$resend_ready" != yes ]; then
  say "Configuration is saved, but deployment was not started because an external provider is not ready."
  say "Complete those dashboard steps, then run this installer again. Existing secrets will be preserved."
  exit 0
fi

if confirm "Build and deploy SharedHouse now?" yes; then
  chmod +x infra/production/scripts/deploy.sh infra/production/scripts/backup.sh
  ./infra/production/scripts/deploy.sh
  say "Deployment passed its public health gate."
  if confirm "Create the first PostgreSQL backup now?" yes; then
    default_backup_directory="$(dirname "$repository_root")/sharedhouse-backups"
    printf 'Absolute backup directory [%s]: ' "$default_backup_directory" >&2
    IFS= read -r backup_directory
    backup_directory=${backup_directory:-$default_backup_directory}
    case "$backup_directory" in
      /home/*) ;;
      *) fail "Safety policy: the backup directory must be below /home." ;;
    esac
    ./infra/production/scripts/backup.sh "$backup_directory"
  fi
  run_preflight
else
  say "Installation files are ready. Deploy later with ./infra/production/scripts/deploy.sh"
fi
