#!/bin/sh
set -eu
project_root=$(CDPATH= cd -P "$(dirname "$0")/.." && pwd)
cd "$project_root"
integration=false
restart=false
for option in "$@"; do
    case "$option" in
        --integration) integration=true ;;
        --restart) restart=true ;;
        *) printf '%s\n' 'Usage: sh scripts/verify-all.sh [--integration] [--restart]' >&2; exit 2 ;;
    esac
done
compose() {
    docker compose --profile phase0 --profile phase1 --profile phase2 --profile phase3 --profile phase4 --profile phase5 --profile phase6 "$@"
}
sh scripts/doctor.sh
command -v curl >/dev/null 2>&1 || { printf '%s\n' 'curl is required.' >&2; exit 1; }
compose up --build -d --wait --wait-timeout 240 legacy servlet-jsp spring-mvc banking-transaction security-operations spring-boot
compose run --build --rm java-core
if [ "$restart" = true ]; then sh scripts/verify.sh --restart; fi
for service in legacy servlet-jsp spring-mvc banking-transaction security-operations spring-boot; do
    binding=$(compose port "$service" 8080)
    case "$binding" in 127.0.0.1:*) port=${binding#127.0.0.1:} ;; *) printf '%s\n' 'Unexpected public binding.' >&2; exit 1 ;; esac
    case "$port" in ''|*[!0-9]*) exit 1 ;; esac
    health_path=/health
    if [ "$service" = legacy ]; then health_path=/health/ready; fi
    response=$(curl -fsS --max-time 10 "http://127.0.0.1:$port$health_path")
    printf '%s\n' "$response" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'
    printf '[PASS] HTTP/DB health: %s\n' "$service"
done
if [ "$integration" = true ]; then
    docker compose --profile integration run --build --rm integration-test
else
    printf '%s\n' '[NOT RUN] PostgreSQL integration tests. Use --integration to run them.'
fi
printf '%s\n' 'Requested checks passed. Containers and volumes are retained.'
