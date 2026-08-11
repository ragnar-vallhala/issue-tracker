#!/usr/bin/env bash
#
# Stops everything start-all.sh started.

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PIDS="$ROOT/logs/pids"

if [[ ! -f "$PIDS" ]]; then
    echo "No pid file at $PIDS - nothing recorded as running."
    exit 0
fi

while read -r pid; do
    [[ -z "$pid" ]] && continue

    if kill -0 "$pid" 2>/dev/null; then
        echo "Stopping pid $pid"
        kill "$pid" 2>/dev/null

        # Give it a few seconds to shut down cleanly - a service that is killed outright
        # never deregisters, and lingers on the Eureka dashboard as UP.
        for _ in {1..10}; do
            kill -0 "$pid" 2>/dev/null || break
            sleep 1
        done

        if kill -0 "$pid" 2>/dev/null; then
            echo "  did not stop, forcing"
            kill -9 "$pid" 2>/dev/null
        fi
    fi
done < "$PIDS"

: > "$PIDS"
echo "Done."
