#!/usr/bin/env bash

set -e

cd obfuscate-rs || exit

RELEASE="release"

cross build --release --quiet --target x86_64-linux-android && cross build --release --quiet --target aarch64-linux-android

mkdir -p ../app/src/main/jniLibs/arm64-v8a/
mkdir -p ../app/src/main/jniLibs/x86_64/

cp target/aarch64-linux-android/${RELEASE}/libobfuscate.so ../app/src/main/jniLibs/arm64-v8a/libuniffi_obfuscate.so
cp target/x86_64-linux-android/${RELEASE}/libobfuscate.so ../app/src/main/jniLibs/x86_64/libuniffi_obfuscate.so

echo "Copied native libraries"

cargo run --release --quiet --features=uniffi/cli \
    --package obfuscate \
    --bin uniffi-bindgen \
    generate obfuscate/src/obfuscate.udl \
    --language kotlin \
    --out-dir obfuscate/src/kotlin

cargo run --release --quiet --features=uniffi/cli \
    --package obfuscate \
    --bin uniffi-bindgen \
    generate obfuscate/src/obfuscate.udl \
    --language swift \
    --out-dir obfuscate/src/swift/uniffi/obfuscate

mkdir -p ../app/src/main/java/uniffi/
cp -r obfuscate/src/kotlin/uniffi/ ../app/src/main/java/

echo "Copied generated Kotlin code"

cd ../