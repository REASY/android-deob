#!/usr/bin/env bash

set -euo pipefail

ENCRYPTION_KEY="a-very-secret-key-for-this-!@#$"

resolve_d8() {
    if command -v d8 >/dev/null 2>&1; then
        command -v d8
        return 0
    fi

    # Honor configured SDK roots first, then common macOS/Linux defaults.
    local sdk_roots=("${ANDROID_SDK_ROOT:-}" "${ANDROID_HOME:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk")
    local sdk_root=""
    local candidate=""

    for sdk_root in "${sdk_roots[@]}"; do
        [[ -z "${sdk_root}" ]] && continue

        candidate="${sdk_root}/cmdline-tools/latest/bin/d8"
        if [[ -x "${candidate}" ]]; then
            echo "${candidate}"
            return 0
        fi

        for candidate in "${sdk_root}"/build-tools/*/d8; do
            if [[ -x "${candidate}" ]]; then
                echo "${candidate}"
                return 0
            fi
        done
    done

    return 1
}

D8_BIN="$(resolve_d8 || true)"
if [[ -z "${D8_BIN}" ]]; then
    echo "Could not find d8. Add it to PATH or set ANDROID_SDK_ROOT/ANDROID_HOME."
    exit 1
fi

# Build
./gradlew build

OUTPUT=$(mktemp -d)

"${D8_BIN}" --release ./app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes/com/example/device/*.class --output "${OUTPUT}"

echo -ne "${ENCRYPTION_KEY}" > "${OUTPUT}/key.bin"

cd obfuscate-rs
cargo run --release --quiet \
    --package obfuscate-tools \
    --bin encryptor \
    -- --input-file "${OUTPUT}/classes.dex" \
    --output-file ../app/src/main/assets/data.bin \
    --key="$ENCRYPTION_KEY"
