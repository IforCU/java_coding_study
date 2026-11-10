#!/bin/sh
set -eu
project_root=$(CDPATH= cd -P "$(dirname "$0")/.." && pwd)
cd "$project_root"
mkdir -p .cache/java-home .cache/tmp
export MAVEN_OPTS="${MAVEN_OPTS:-} -Duser.home=\"$project_root/.cache/java-home\" -Djava.io.tmpdir=\"$project_root/.cache/tmp\""
# All extra arguments are passed as individual Maven arguments, e.g. -o or -pl modules/01-java-core -am.
if [ -f .tools/apache-maven-3.10.0/bin/mvn ]; then
    sh .tools/apache-maven-3.10.0/bin/mvn -B -ntp "-Dmaven.repo.local=$project_root/.cache/maven-repository" "$@" verify
elif command -v mvn >/dev/null 2>&1; then
    mvn -B -ntp "-Dmaven.repo.local=$project_root/.cache/maven-repository" "$@" verify
else
    printf '%s\n' 'Maven is unavailable. Use the Docker test profile or a project-local Maven distribution.' >&2
    exit 1
fi
