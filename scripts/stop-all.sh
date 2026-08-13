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

stopped=0
skipped=0

while read -r pid; do
    [[ -z "$pid" ]] && continue

    if ! kill -0 "$pid" 2>/dev/null; then
        # Already gone - it crashed, or was killed by hand. Worth saying so rather
        # than silently counting it as stopped.
        skipped=$((skipped + 1))
    fi

    if kill -0 "$pid" 2>/dev/null; then
        stopped=$((stopped + 1))
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

# start-all.sh writes one of these per service so it can tell "still starting" from
# "already dead" while it waits. They mean nothing once the run is over.
rm -f "$ROOT"/logs/*.pid

if [[ $skipped -gt 0 ]]; then
    echo "Done - stopped $stopped, $skipped already gone."
else
    echo "Done - stopped $stopped."
fi
