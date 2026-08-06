#!/bin/sh
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "$BASE_DIR/LabSync/mvnw" -f "$BASE_DIR/pom.xml" "$@"
