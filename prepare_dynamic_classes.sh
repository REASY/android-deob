#!/usr/bin/env bash

set -e

PATH_D8="$HOME/Android/Sdk/build-tools/35.0.1/d8"
ENCRYPTION_KEY="a-very-secret-key-for-this-!@#$"

# Build
./gradlew build

OUTPUT=$(mktemp -d)

$PATH_D8 --release ./app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes/com/example/device/*.class --output $OUTPUT

echo -ne "a-very-secret-key-for-this-!@#$" > $OUTPUT/key.bin

cd obfuscate-rs
cargo run --release --quiet \
    --package obfuscate-tools \
    --bin encryptor \
    -- --input-file $OUTPUT/classes.dex \
    --output-file ../app/src/main/assets/data.bin \
    --key="$ENCRYPTION_KEY"
