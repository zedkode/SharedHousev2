# Local infrastructure

Only synthetic development data is permitted in this environment.

1. Copy `.env.example` to `.env` and keep the file local.
2. Start services with `docker compose --env-file infra/.env -f infra/compose.yaml up -d`.
3. Stop services with `docker compose --env-file infra/.env -f infra/compose.yaml down`.

The compose stack provides PostgreSQL, Redis, and S3-compatible object storage. It contains no
production credentials and is not a production deployment definition.

The API can also run without Docker. When `DATABASE_URL` is unset in development, it uses the
embedded PGlite database under `tmp/sharedhouse-pglite` and applies the checked-in migrations. To
exercise the PostgreSQL service instead, start the stack and export the synthetic local connection
before starting the API:

```powershell
$env:DATABASE_URL = 'postgresql://sharedhouse_local:synthetic-local-only@localhost:5432/sharedhouse_local'
npm run dev:api
```

Development and test environments may return the email verification code in the registration
response so the end-to-end flow works without an email provider. Production startup rejects that
behaviour and requires `DATABASE_URL`; real email delivery remains a separate deployment
integration.

Run `npm run smoke:api` from the repository root to build the contracts/API and verify a complete
register, verify, create-household, process-restart, sign-in and persisted-household sequence using
an isolated ignored PGlite directory.
