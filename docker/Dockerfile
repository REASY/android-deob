FROM ubuntu:25.10

ARG DEBIAN_FRONTEND=noninteractive
ARG RUST_TOOLCHAIN=1.90.0
ARG ANDROID_NDK_VERSION=27.2.12479018
ARG ANDROID_SDK_CMDLINE_TOOLS=13114758
ARG VERSION=dev
ARG BUILD_DATE=unknown
ARG COMMIT_SHA=unknown

LABEL \
    org.opencontainers.image.name="android-deob-native-builder" \
    org.opencontainers.image.description="Android Deob native Android library builder image" \
    org.opencontainers.image.url="https://github.com/REASY/android-deob" \
    org.opencontainers.image.source="https://github.com/REASY/android-deob.git" \
    org.opencontainers.image.version="${VERSION}" \
    org.opencontainers.image.licenses="Apache-2.0" \
    org.opencontainers.image.authors="Artavazd Balaian <reasyu@gmail.com>" \
    org.opencontainers.image.base.name="ubuntu:25.10" \
    org.opencontainers.image.created="${BUILD_DATE}" \
    org.opencontainers.image.revision="${COMMIT_SHA}"

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV CARGO_HOME=/opt/cargo
ENV RUSTUP_HOME=/opt/rustup
ENV ANDROID_NDK_HOME=/opt/android-sdk/ndk/${ANDROID_NDK_VERSION}
ENV NDK_HOME=/opt/android-sdk/ndk/${ANDROID_NDK_VERSION}
ENV PATH=/opt/cargo/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash \
        build-essential \
        ca-certificates \
        clang \
        curl \
        file \
        git \
        openjdk-21-jre-headless \
        pkg-config \
        unzip \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_CMDLINE_TOOLS}_latest.zip -o /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp \
    && mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && mv /tmp/cmdline-tools/* ${ANDROID_SDK_ROOT}/cmdline-tools/latest/ \
    && rm -rf /tmp/cmdline-tools /tmp/cmdline-tools.zip \
    && yes | sdkmanager --licenses >/dev/null \
    && sdkmanager "platform-tools" "ndk;${ANDROID_NDK_VERSION}"

RUN curl -fsSL https://sh.rustup.rs | sh -s -- -y --profile minimal --default-toolchain ${RUST_TOOLCHAIN} \
    && rustup target add aarch64-linux-android x86_64-linux-android \
    && cargo install --locked cargo-ndk

WORKDIR /workspace

COPY build_native.sh /usr/local/bin/build_native.sh
RUN chmod +x /usr/local/bin/build_native.sh

ENTRYPOINT ["/usr/local/bin/build_native.sh"]
