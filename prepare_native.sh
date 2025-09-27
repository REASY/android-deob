#!/usr/bin/env bash

cd obfuscate-rs || exit

RELEASE="debug"

cross build --target x86_64-linux-android && cross build --target aarch64-linux-android

mkdir -p ../app/src/main/jniLibs/arm64-v8a/
mkdir -p ../app/src/main/jniLibs/x86_64/

cp target/aarch64-linux-android/${RELEASE}/libobfuscate.so ../app/src/main/jniLibs/arm64-v8a/libuniffi_obfuscate.so
cp target/x86_64-linux-android/${RELEASE}/libobfuscate.so ../app/src/main/jniLibs/x86_64/libuniffi_obfuscate.so

cd ../