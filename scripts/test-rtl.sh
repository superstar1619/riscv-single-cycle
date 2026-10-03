#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
verilator_bin=${VERILATOR:-verilator}
cd "$project_dir" # Generated $readmemh paths are relative to the project root.
# The installed Verilator invokes ccache; keep its writes inside this project.
export CCACHE_DIR="$project_dir/.cache/ccache"
mkdir -p "$CCACHE_DIR"
"$verilator_bin" --version

for scenario in book expanded; do
  if [[ "$scenario" == book ]]; then
    rtl_file="$project_dir/generated/riscv-single/RiscvSingle.v"
  else
    rtl_file="$project_dir/generated/riscv-single128/RiscvSingle.v"
  fi
  build_dir="$project_dir/target/rtl-test/$scenario"
  mkdir -p "$build_dir"
  # Each RTL file already includes the complete hierarchy. Do not add the
  # standalone module exports, which would introduce duplicate definitions.
  if ! "$verilator_bin" --cc --exe --build -Wall \
      -Wno-DECLFILENAME -Wno-UNUSEDSIGNAL \
      --x-initial unique --x-assign unique \
      --top-module RiscvSingle --prefix VRiscvSingle --Mdir "$build_dir" \
      -CFLAGS '-std=c++17' "$rtl_file" \
      "$project_dir/src/test/cpp/RiscvSingleRtlTest.cpp" \
      > "$build_dir/build.log" 2>&1; then
    cat "$build_dir/build.log" >&2
    exit 1
  fi
  for seed in 1 17 2026; do
    "$build_dir/VRiscvSingle" "$scenario" "$seed"
  done
done
