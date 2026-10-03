#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
toolchain_dir=${CHISEL_TOOLCHAIN_DIR:-"$project_dir/../../.tools/chisel"}
cache_dir=${CHISEL_CACHE_DIR:-"$project_dir/.cache/chisel"}

if [[ ! -x "$toolchain_dir/jdk/bin/java" || ! -r "$toolchain_dir/sbt-launch-1.10.7.jar" ]]; then
  echo "Local Chisel toolchain not found. Set CHISEL_TOOLCHAIN_DIR or use an installed sbt." >&2
  exit 1
fi

# Seed a writable project-local cache without changing the shared toolchain.
mkdir -p "$cache_dir"
for entry in boot coursier global ivy; do
  if [[ ! -d "$cache_dir/$entry" && -d "$toolchain_dir/cache/$entry" ]]; then
    cp -a "$toolchain_dir/cache/$entry" "$cache_dir/$entry"
  fi
done

export JAVA_HOME="$toolchain_dir/jdk"
export PATH="$JAVA_HOME/bin:$PATH"
export COURSIER_CACHE="$cache_dir/coursier"
cd "$project_dir"
# sbt 1.10.7 uses XDG_RUNTIME_DIR for its boot socket even when the build
# server is disabled. Let it use the writable system temporary directory.
# forcestart allows batch execution to continue without a boot socket if
# the environment disallows sockets; the build server remains disabled.
exec env -u XDG_RUNTIME_DIR "$JAVA_HOME/bin/java" \
  -Djava.io.tmpdir="${TMPDIR:-/tmp}" \
  -Dsbt.boot.directory="$cache_dir/boot" \
  -Dsbt.global.base="$cache_dir/global" \
  -Dsbt.ivy.home="$cache_dir/ivy" \
  -Dsbt.repository.config="$toolchain_dir/repositories" \
  -Dsbt.override.build.repos=true \
  -Dsbt.server.autostart=false \
  -Dsbt.server.forcestart=true \
  -Dsbt.supershell=false \
  -Dsbt.log.noformat=true \
  -jar "$toolchain_dir/sbt-launch-1.10.7.jar" "$@"
