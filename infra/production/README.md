# SharedHouse production deployment

This stack runs the SharedHouse API and background workers on a Linux VPS with PostgreSQL and
publishes only the API through an outbound Cloudflare Tunnel. PostgreSQL, the API and workers have
no host ports. The public Android build is pinned to `https://houseapi.dohotstudio.com`.

The workers service checks active household-cost schedules every minute by default. It evaluates
the due date in each household's IANA timezone, creates an approved ledger occurrence with an exact
equal allocation, and advances the schedule in the same database transaction. A unique
template/date key plus row locking prevents duplicate expenses after retries, restarts or multiple
worker instances. `WORKER_POLL_INTERVAL_MS` and `WORKER_BATCH_SIZE` are bounded settings in
`production.env`; keep the documented defaults unless load testing justifies a change.

This repository prepares a repeatable production deployment, but it cannot create your Cloudflare
or Resend accounts, hold their secrets, provision a VPS, or complete Google Play ownership on your
behalf. Do not call the service live until every gate in **Final launch checks** passes.

## Fast interactive installation

After Docker Engine and its Compose plugin are installed on the VPS, keep the complete installation
below the SSH user's home directory. The recommended path is one interactive command:

```sh
cd /home/DEPLOY_USER/sharedhouse
chmod +x infra/production/scripts/*.sh
./infra/production/scripts/install-interactive.sh
```

The wizard checks Linux architecture, Docker access and free disk space; preserves existing
secrets; generates the PostgreSQL and AES outbox keys; requests the Resend and Cloudflare tokens
without echoing them; writes the non-secret environment file; validates Compose; confirms external
DNS/provider readiness; deploys; optionally creates the first backup; and runs the public health
gate. It never installs Docker from an unreviewed remote script and never opens API/database ports.
It refuses to run outside `/home`.

### Safe upload from the Windows workstation

Configure SSH key authentication first. Do not paste a private key or VPS password into this
repository. From PowerShell, run the read-only inventory before uploading anything:

```powershell
.\infra\production\scripts\upload-and-install-vps.ps1 `
  -SshTarget sharedhouse-vps `
  -SshPort 22 `
  -IdentityFile $env:USERPROFILE\.ssh\dohot_vps `
  -RemoteRoot /home/DEPLOY_USER/sharedhouse `
  -PreflightOnly
```

Then remove `-PreflightOnly` and type `DEPLOY`. The script packages only Git-visible/non-ignored
source files, uploads them below `/home`, and starts the secret-safe wizard in the SSH terminal. It
will not overwrite a non-empty directory unless that directory has its SharedHouse ownership marker.
The deploy gate snapshots every pre-existing container before and after `docker compose`; it stops
if an existing container ID, state, start time or restart count changed.

Use `-UploadOnly` to stage the source without starting the installer or creating containers. The
non-interactive `-ApproveUpload` switch is intended only for an already approved automated run.

At any later time, run the read-only live check:

```sh
./infra/production/scripts/install-interactive.sh --preflight
```

The manual sections below explain every wizard decision and remain the recovery procedure.

## 1. VPS prerequisites

Use a supported 64-bit Ubuntu or Debian VPS with at least 2 vCPU, 4 GB RAM, 40 GB SSD and working
outbound access. Apply security updates, configure SSH keys, disable password SSH login and install
Docker Engine plus the Compose plugin from Docker's official repository. With Cloudflare Tunnel,
only SSH needs an inbound firewall rule; do not expose ports 3000 or 5432.

Clone or securely copy the repository to `/home/DEPLOY_USER/sharedhouse`, then run all remaining commands from
that directory as a dedicated deployment user with Docker access.

## 2. Cloudflare Tunnel and API hostname

1. In Cloudflare open **Networking > Tunnels** and create a new remotely managed tunnel named
   `sharedhouse-production`. Do not edit, restart, reuse or delete the connector that serves your
   existing containers. A dedicated tunnel prevents Cloudflare from selecting an old connector that
   cannot resolve the isolated SharedHouse `api` service.
2. Add a **Published application** route:
   - Subdomain: `houseapi`
   - Domain: `dohotstudio.com`
   - Service type: `HTTP`
   - Service URL: `http://api:3000`
3. Save the route. Cloudflare creates the proxied DNS route for
   `houseapi.dohotstudio.com`; do not create a second A/CNAME record for the same name.
4. Copy only the tunnel token into the VPS secret file described below. Never commit or paste it in
   issue trackers, logs or application configuration.
5. In Cloudflare enable **Always Use HTTPS**. After deployment, verify that the tunnel is Healthy and
   that `/v1/health/ready` returns JSON with `status: ok`.

The connector uses outbound-only encrypted connections, so the origin API cannot be bypassed through
a public VPS port. Review Cloudflare WAF/rate-limit rules before a wider launch; do not cache
`/v1/auth/*`, `/v1/account*` or household API responses.

## 3. Transactional verification email without a mail server

SharedHouse uses the Resend HTTPS API. You do not need to install or maintain SMTP software on the
VPS.

1. Create a Resend account and add the sending subdomain `mail.dohotstudio.com`.
2. Use Resend's **Sign in to Cloudflare** flow, or copy the exact SPF, DKIM and return-path records
   shown by Resend into Cloudflare. Email authentication records must be **DNS only**, not proxied.
3. Wait until Resend reports the domain as **Verified**. Do not guess DNS values from this document;
   use the values issued for your account.
4. Create a Resend API key restricted to sending and place it only in the VPS secret file.
5. The configured sender is `SharedHouse <verify@mail.dohotstudio.com>`. Resend permits sending from
   an address on a verified domain without operating a mailbox, although a monitored reply address
   should be added before customer support goes live.

Production registration writes an AES-256-GCM encrypted verification message to PostgreSQL in the
same transaction as the account. The API dispatcher sends it with a Resend idempotency key, retries
temporary failures with exponential backoff, stops after eight attempts and never logs the code or
recipient address. The encrypted code payload is erased after successful delivery or a terminal
failure. Codes are single-use, expire after 15 minutes and allow five entry attempts. A user can
request a replacement after a 60-second cooldown; responses do not reveal whether an account
exists.

## 4. Create local secret files on the VPS

```sh
cd /home/DEPLOY_USER/sharedhouse
cp infra/production/production.env.example infra/production/production.env
mkdir -p infra/production/secrets
chmod 700 infra/production/secrets
umask 077

openssl rand -base64 36 > infra/production/secrets/postgres_password
openssl rand -base64 32 > infra/production/secrets/email_outbox_key

printf 'Resend API key: ' >&2
stty -echo
IFS= read -r resend_key
stty echo
printf '\n' >&2
printf '%s' "$resend_key" > infra/production/secrets/resend_api_key
unset resend_key

printf 'Cloudflare tunnel token: ' >&2
stty -echo
IFS= read -r tunnel_token
stty echo
printf '\n' >&2
printf '%s' "$tunnel_token" > infra/production/secrets/cloudflare_tunnel_token
unset tunnel_token

chgrp 0 infra/production/secrets/*
chmod 640 infra/production/secrets/*
```

Mode `0640` keeps the files writable only by root while allowing the capability-dropped, non-root
API and connector to read them through supplemental group 0. Keep offline encrypted backups of
`postgres_password` and `email_outbox_key`. Losing the PostgreSQL
password blocks database access; losing the email outbox key makes queued verification messages
unreadable. Rotate the Resend and Tunnel tokens from their provider dashboards after suspected
exposure.

Review `production.env`. It must keep `NODE_ENV=production`, development verification disabled, and
the sender on the verified domain. It contains no secret values.

## 5. Deploy

```sh
chmod +x infra/production/scripts/deploy.sh infra/production/scripts/backup.sh
./infra/production/scripts/deploy.sh
```

The deployment script validates required files, validates Compose, pulls/builds the API image,
starts PostgreSQL/API/cloudflared, waits for container readiness and finally checks the public HTTPS
endpoint. If the final public request fails, the deployment is incomplete.

Useful checks:

```sh
docker compose --env-file infra/production/production.env \
  -p sharedhouse-production -f infra/production/compose.yaml ps
docker compose --env-file infra/production/production.env \
  -p sharedhouse-production -f infra/production/compose.yaml logs --tail=200 api workers cloudflared
curl --fail --show-error https://houseapi.dohotstudio.com/v1/health
curl --fail --show-error https://houseapi.dohotstudio.com/v1/health/ready
```

Logs are size-bounded. They must not contain access tokens, refresh tokens, passwords, verification
codes, database URLs or provider secrets.

## 6. Validate the first public account

Only after both health endpoints pass, install a public build and complete this exact flow:

1. Open SharedHouse, complete or skip the tutorial, then choose **Create account**.
2. Enter your name, an inbox you can access and a unique password of at least 15 characters. Accept
   the required terms and submit.
3. Open the message sent by `verify@mail.dohotstudio.com` and enter its eight-digit code within 15
   minutes. If it does not arrive, check spam and use **Send a new code** after 60 seconds.
4. Create the household: choose its name, country, IANA timezone, currency, first day of week and
   billing cycle. The dashboard, interactive calendar, Money and Tasks then become available.
5. In **Tasks**, add a temporary real validation task assigned to yourself, start it, complete it
   with a note, then verify it remains in **Completed**. If another member is available, validate one
   help/swap/postpone request and owner/admin decision. Do not create fake records in a real tenant.
6. Sign out and sign in again on the same phone. Confirm the household and task history are restored
   from PostgreSQL.

If no email arrives, inspect the Resend delivery log and the API log by outbox ID. Never copy a
verification code or recipient address into a support ticket or server log.

## 7. Backup before every update

Choose a directory outside the repository, ideally on an encrypted attached volume:

```sh
./infra/production/scripts/backup.sh /var/backups/sharedhouse
```

The script writes a PostgreSQL custom-format dump atomically and prints its final path. Copy backups
off the VPS and periodically test restoration into a separate disposable database. Do not test
restore against the live `sharedhouse` database.

For an update, create a new immutable `SHAREDHOUSE_IMAGE_TAG` in `production.env`, back up, then run
the deploy script. Keep the previous image tag and database backup. A code rollback may select the
previous image tag, but a schema rollback requires an explicitly reviewed forward repair; never
delete migration rows or edit an applied migration.

## 8. Build the owner-signed public Android release

Create and back up an upload keystore outside the repository. The build reads signing data only from
the current process environment and refuses a public release without all values.

```powershell
$env:SHAREDHOUSE_RELEASE_STORE_FILE = "C:\secure\sharedhouse-upload.jks"
$env:SHAREDHOUSE_RELEASE_STORE_PASSWORD = "<from-secret-manager>"
$env:SHAREDHOUSE_RELEASE_KEY_ALIAS = "sharedhouse-upload"
$env:SHAREDHOUSE_RELEASE_KEY_PASSWORD = "<from-secret-manager>"

.\gradlew.bat :apps:android:app:packagePublicReleaseApk
.\gradlew.bat :apps:android:app:copyPublicReleaseBundle
```

Expected named outputs:

- `apps/android/app/build/outputs/apk/release/SharedHouse-v0.1.0-public-release-signed.apk`
- `apps/android/app/build/outputs/bundle/release/SharedHouse-v0.1.0-public-release-signed.aab`

Verify the APK certificate with `apksigner verify --verbose --print-certs`. Store the upload key and
passwords in separate encrypted backups. Prefer Play App Signing for store distribution. Never
reuse the Android debug certificate for public distribution.

## 9. Final launch checks

- Cloudflare tunnel is Healthy and public liveness/readiness checks pass over HTTPS.
- PostgreSQL is healthy, a backup exists off-server and a restore test passed in an isolated DB.
- Resend domain, SPF and DKIM are verified; a real registration email reached at least Gmail and
  Outlook test inboxes without exposing a code in the API response.
- API tests, dependency audit, OpenAPI validation, Android release lint/tests and R8 release build
  pass on the exact release commit.
- Final APK/AAB certificate digest matches the owner-approved upload key.
- Privacy policy, terms, support contact, deletion/export flows and store declarations are live and
  reviewed. The current repository does not yet satisfy all of these product/legal launch gates.

Official references:

- Cloudflare Tunnel: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/
- Cloudflare tunnel creation: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/get-started/create-remote-tunnel/
- Resend with Cloudflare: https://resend.com/docs/knowledge-base/cloudflare
- Resend send-email API: https://resend.com/docs/api-reference/emails/send-email
