#!/usr/bin/env bash

set -e

cd obfuscate-rs || exit

RELEASE="debug"

cross build --quiet --target x86_64-linux-android && cross build --quiet --target aarch64-linux-android

mkdir -p ../app/src/main/jniLibs/arm64-v8a/
mkdir -p ../app/src/main/jniLibs/x86_64/

cp target/aarch64-linux-android/${RELEASE}/libobfuscate.so ../app/src/main/jniLibs/arm64-v8a/libuniffi_obfuscate.so
cp target/x86_64-linux-android/${RELEASE}/libobfuscate.so ../app/src/main/jniLibs/x86_64/libuniffi_obfuscate.so

echo "Copied native libraries"

cargo run --quiet --features=uniffi/cli \
    --bin uniffi-bindgen \
    generate src/obfuscate.udl \
    --language kotlin

mkdir -p ../app/src/main/java/uniffi/
cp -r src/uniffi/ ../app/src/main/java/uniffi/

echo "Copied generated Kotlin code"

cd ../