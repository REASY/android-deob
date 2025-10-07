#!/bin/bash

qemu-system-aarch64 \
 -smp 12 \
 -m 8192 \
 -cpu max \
 -accel tcg,thread=multi \
 -M virt \
 -nographic \
 -drive if=pflash,format=raw,file=efi.img,readonly=on \
 -drive if=pflash,format=raw,file=varstore.img \
 -drive if=none,file=ubuntu-25.04-server-cloudimg-arm64.img,id=hd0 \
 -drive file=user-data.img,format=raw,id=cloud \
 -device virtio-blk-device,drive=hd0 \
 -net user,hostfwd=tcp::30022-:22,hostfwd=tcp::5551-:5555 \
 -net nic \
 -vnc :3
