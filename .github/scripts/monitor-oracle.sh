#!/usr/bin/env bash

# Sample while the test JVM is alive; Testcontainers removes Oracle on JVM exit.
set -u

output_dir=$1
mkdir -p "$output_dir"

while true; do
    container_ids=$(docker ps --filter ancestor=gvenzl/oracle-free:23-full-faststart --format '{{.ID}}')
    for container_id in $container_ids; do
        {
            date -u '+%Y-%m-%dT%H:%M:%SZ'
            timeout 8s docker exec -i --user oracle "$container_id" bash -lc 'sqlplus -L -s / as sysdba' <<'SQL'
WHENEVER SQLERROR EXIT FAILURE
SET LINESIZE 320 PAGESIZE 1000 TRIMSPOOL ON TAB OFF
ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS';
COLUMN resource_name FORMAT A12
COLUMN username FORMAT A24
COLUMN machine FORMAT A32
COLUMN program FORMAT A48
COLUMN module FORMAT A48
COLUMN event FORMAT A48
SELECT instance_name, status FROM v$instance;
SELECT resource_name, current_utilization, max_utilization, limit_value
FROM v$resource_limit
WHERE resource_name IN ('processes', 'sessions');
SELECT con_id, username, status, machine, program, COUNT(*) AS session_count
FROM v$session
WHERE type = 'USER'
GROUP BY con_id, username, status, machine, program
ORDER BY session_count DESC;
SELECT con_id, sid, serial#, username, status, logon_time, last_call_et,
       program, module, event
FROM v$session
WHERE type = 'USER'
ORDER BY logon_time;
EXIT
SQL
            echo "Query exit status: $?"
        } >> "$output_dir/sessions-$container_id.log" 2>&1
        timeout 8s docker logs --timestamps "$container_id" > "$output_dir/container-$container_id.log" 2>&1
        timeout 8s docker exec --user oracle "$container_id" bash -lc \
            'find /opt/oracle/diag -type f \( -name "alert_*.log" -o -name "listener.log" \) -exec tail -n 200 {} +' \
            > "$output_dir/alert-listener-$container_id.log" 2>&1
        timeout 8s docker stats --no-stream "$container_id" >> "$output_dir/resources-$container_id.log" 2>&1
    done
    sleep 10
done
