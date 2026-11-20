#!/usr/bin/env bash
# 编译 nuist_vpn 原生 cdylib，供 Dart 侧通过 dart:ffi 直接加载。
# 与原项目一致输出 libnuist_vpn.so（名字不能改，Dart 侧按这个 soname 查找）。
set -euo pipefail
WS="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
NDK=${ANDROID_NDK_HOME:-$WS/toolchain/android-sdk/ndk/27.3.13750724}
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
API=33
RUST=${RUSTUP_TOOLCHAIN:-nightly}
MANIFEST="$WS/nuist-sta-app/native/vpn/Cargo.toml"

build() {
  local target="$1" abi="$2" cc="$3"
  local out="$WS/nuist-sta-app/compose/app/src/main/jniLibs/$abi"
  local rustflags="-C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-z,common-page-size=16384"
  mkdir -p "$out"
  env \
    CC_aarch64_linux_android="$TOOLCHAIN/bin/${cc}" \
    CC_x86_64_linux_android="$TOOLCHAIN/bin/${cc}" \
    CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/${cc}" \
    CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/${cc}" \
    AR_aarch64_linux_android="$TOOLCHAIN/bin/llvm-ar" \
    AR_x86_64_linux_android="$TOOLCHAIN/bin/llvm-ar" \
    RUSTUP_TOOLCHAIN="$RUST" \
    RUSTFLAGS="$rustflags" \
    cargo build --release --target "$target" --manifest-path "$MANIFEST"
  cp "$WS/nuist-sta-app/native/vpn/target/$target/release/libnuist_vpn.so" \
     "$out/libnuist_vpn.so"
  echo "built $abi"
}

build aarch64-linux-android arm64-v8a "aarch64-linux-android${API}-clang"
build x86_64-linux-android x86_64 "x86_64-linux-android${API}-clang"