#!/usr/bin/env bash

set -e

truncate -s 64m varstore.img

truncate -s 64m efi.img
dd if=/usr/share/qemu-efi-aarch64/QEMU_EFI.fd of=efi.img conv=notrunc

cat >user-data <<EOF
#cloud-config
password: 1
chpasswd: { expire: False }
ssh_pwauth: True
EOF

# sudo apt-get install cloud-image-utils
cloud-localds user-data.img user-data