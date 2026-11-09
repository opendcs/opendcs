#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/../.."
compose=(docker compose -p opendcs-cwms-updater-test -f docker-compose.cwms-test.yaml)
cleanup()
{
    result=$?
    if (( result != 0 )); then
        "${compose[@]}" logs --no-color
    fi
    "${compose[@]}" down --volumes --remove-orphans
    exit "$result"
}
trap cleanup EXIT

"${compose[@]}" build migration
"${compose[@]}" up -d --wait --wait-timeout 1000 db
"${compose[@]}" run --rm migration
"${compose[@]}" exec -T db sqlplus -s / as sysdba @/test/seed.sql
if "${compose[@]}" run --rm -e APP_PASSWORD=incorrect_test_password migration; then
    echo "Manager unexpectedly accepted invalid application credentials" >&2
    exit 1
fi
"${compose[@]}" run --rm migration
"${compose[@]}" exec -T db sqlplus -s / as sysdba @/test/verify.sql
"${compose[@]}" run --rm migration
"${compose[@]}" exec -T db sqlplus -s / as sysdba @/test/verify.sql
