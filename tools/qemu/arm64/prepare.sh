#!/usr/bin/env bash

set -e

# Download the image
wget "https://cloud-images.ubuntu.com/releases/plucky/release/ubuntu-25.04-server-cloudimg-arm64.img"

#cp -f /home/user/Downloads/ubuntu-25.04-server-cloudimg-arm64.img ubuntu-25.04-server-cloudimg-arm64.img

# Resize it to 50G
qemu-img resize ubuntu-25.04-server-cloudimg-arm64.img 50G

truncate -s 64m varstore.img

truncate -s 64m efi.img
dd if=/usr/share/qemu-efi-aarch64/QEMU_EFI.fd of=efi.img conv=notrunc

# sudo apt-get install cloud-image-utils
cloud-localds user-data.img cloud-init.yaml