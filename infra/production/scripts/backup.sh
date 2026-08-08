#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 /absolute/backup/directory" >&2
  exit 64
fi

backup_directory=$1
case "$backup_directory" in
  /home/*) ;;
  *) echo "Safety policy: backup directory must be below /home." >&2; exit 64 ;;
esac

mkdir -p "$backup_directory"
chmod 700 "$backup_directory"
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
temporary_path="$backup_directory/.sharedhouse-$timestamp.dump.tmp"
final_path="$backup_directory/sharedhouse-$timestamp.dump"

cleanup() {
  rm -f -- "$temporary_path"
}
trap cleanup EXIT HUP INT TERM

docker compose -p sharedhouse-production --env-file infra/production/production.env \
  -f infra/production/compose.yaml exec -T postgres \
  pg_dump --format=custom --no-owner --no-privileges --username=sharedhouse sharedhouse \
  > "$temporary_path"

test -s "$temporary_path"
mv -- "$temporary_path" "$final_path"
chmod 600 "$final_path"
trap - EXIT HUP INT TERM
echo "$final_path"
