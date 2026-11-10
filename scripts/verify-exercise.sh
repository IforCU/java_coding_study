#!/bin/sh
set -eu

project_root=$(CDPATH= cd -P "$(dirname "$0")/.." && pwd)
cd "$project_root"
sql_file=exercises/starter/phase-00/read-metadata.sql
case "$#" in
    0) ;;
    2) if [ "$1" = --sql-file ]; then sql_file=$2; else printf '%s\n' 'Usage: sh scripts/verify-exercise.sh [--sql-file path]' >&2; exit 2; fi ;;
    *) printf '%s\n' 'Usage: sh scripts/verify-exercise.sh [--sql-file path]' >&2; exit 2 ;;
esac
fail() {
    printf '[FAIL] %s\n' "$1" >&2
    exit 1
}
case "$sql_file" in /*) ;; *) sql_file="$project_root/$sql_file" ;; esac
[ -f "$sql_file" ] || fail 'The SQL file was not found.'
[ ! -L "$sql_file" ] || fail 'The SQL path must not be a symbolic link.'
sql_directory=$(CDPATH= cd -P "$(dirname "$sql_file")" && pwd) || fail 'The SQL directory was not found.'
sql_file="$sql_directory/$(basename "$sql_file")"
case "$sql_file" in "$project_root"/*) ;; *) fail 'The SQL file must be inside this project.' ;; esac
[ -s "$sql_file" ] || fail 'The SQL file is empty.'
command -v docker >/dev/null 2>&1 || fail 'Docker CLI is missing.'
# PGOPTIONS applies to the psql session; normal learner queries are read only.
if ! result=$(docker compose --profile phase0 exec -T -e 'PGOPTIONS=-c default_transaction_read_only=on' db sh -c 'exec psql -X -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -A -F "|"' < "$sql_file"); then
    fail 'SQL validation failed. Check your query and the running Phase 0 database.'
fi
[ "$result" = 'local-learning|1' ] || fail 'The query result does not match the exercise. Review exercise.md and hints.md.'
printf '%s\n' '[PASS] Phase 0 SQL exercise returned the expected metadata.'
