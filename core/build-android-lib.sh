#!/bin/sh

set -e

cargo build --lib --release --target=aarch64-linux-android
cargo build --lib --release --target=x86_64-linux-android
cp target/aarch64-linux-android/release/libmanga.so ../android/app/src/main/jniLibs/arm64-v8a/libuniffi_manga.so
cp target/x86_64-linux-android/release/libmanga.so ../android/app/src/main/jniLibs/x86_64/libuniffi_manga.so

cargo run --features=uniffi/cli --bin uniffi-bindgen -- generate -l kotlin -o ../android/app/src/main/java src/manga.udl
