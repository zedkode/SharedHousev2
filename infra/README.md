# Local infrastructure

Only synthetic development data is permitted in this environment.

1. Copy `.env.example` to `.env` and keep the file local.
2. Start services with `docker compose --env-file infra/.env -f infra/compose.yaml up -d`.
3. Stop services with `docker compose --env-file infra/.env -f infra/compose.yaml down`.

The compose stack provides PostgreSQL, Redis, and S3-compatible object storage. It contains no
production credentials and is not a production deployment definition.
