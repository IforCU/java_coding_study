#!/bin/sh
set -eu

project_root=$(CDPATH= cd -P "$(dirname "$0")/.." && pwd)
cd "$project_root"
printf '%s\n' 'Phase 0 environment check (read only)'
if command -v java >/dev/null 2>&1; then
    printf '%s\n' '[INFO] Host Java is available. The build uses the Java 17 container.'
else
    printf '%s\n' '[INFO] Host Java is optional; the build uses the Java 17 container.'
fi
if command -v mvn >/dev/null 2>&1; then
    printf '%s\n' '[INFO] Host Maven is available. The container build is the reference build.'
else
    printf '%s\n' '[INFO] Host Maven is optional; Maven is supplied by the build container.'
fi
if ! command -v docker >/dev/null 2>&1; then
    printf '%s\n' '[FAIL] Docker CLI is missing. Install Docker Desktop with Linux containers before running Phase 0.' >&2
    exit 1
fi
if ! docker compose version; then
    printf '%s\n' '[FAIL] Docker Compose v2 is unavailable.' >&2
    exit 1
fi
if ! server_type=$(docker info --format '{{.OSType}}'); then
    printf '%s\n' '[FAIL] Docker engine is unavailable. Start Docker Desktop and select Linux containers, then retry.' >&2
    exit 1
fi
if [ "$server_type" != linux ]; then
    printf '%s\n' '[FAIL] Phase 0 requires a Linux Docker engine. Select Linux containers and retry.' >&2
    exit 1
fi
printf '%s\n' '[PASS] Linux Docker engine is reachable.'
if ! docker compose --profile phase0 config --quiet; then
    printf '%s\n' '[FAIL] Compose configuration is invalid. Review compose.yaml and the project .env file.' >&2
    exit 1
fi
printf '%s\n' '[PASS] Compose configuration is valid.'
printf '%s\n' 'Environment check passed. Runtime build and HTTP/DB checks have not been run.'
