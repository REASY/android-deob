#!/bin/bash

qemu-system-x86_64 \
    -enable-kvm \
    -machine q35,vmport=off \
    -m 8196 \
    -smp 8 \
    -cpu host \
    -bios /usr/share/ovmf/OVMF.fd \
    -cdrom /media/android_dev_disk/arcadia-x86/out/target/product/x86_64/Bliss-v15.9.3-x86_64-UNOFFICIAL-foss-20250904.iso \
    -usb \
    -device virtio-tablet \
    -device virtio-keyboard \
    -device qemu-xhci,id=xhci \
    -device vhost-vsock-pci,id=vhost-vsock-pci0,guest-cid=3 \
    -device virtio-vga-gl \
    -device AC97,audiodev=snd0 \
    -display sdl,gl=on \
    -audiodev pa,id=snd0\
    -net nic,model=virtio-net-pci \
    -net user,hostfwd=tcp::4444-:5555
