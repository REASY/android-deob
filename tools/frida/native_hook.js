'use strict';

function getAsHex(arr, shouldTruncate = false) {
    const TRUNCATING_LIMIT = 256;
    if (shouldTruncate) {
        let sliced = arr;
        if (arr.length > TRUNCATING_LIMIT) {
            sliced = arr.slice(0, TRUNCATING_LIMIT);
        }
        const hexBytes = Array.from(sliced)
            .map(b => b.toString(16).padStart(2, '0'))
            .join(' ');
        if (arr.length > TRUNCATING_LIMIT) {
            return hexBytes + ' ... truncated';
        } else {
            return hexBytes;
        }
    } else {
        return Array.from(arr)
            .map(b => b.toString(16).padStart(2, '0'))
            .join(' ');
    }
}

var libjnidispatch_handler = null;
var libuniffi_obfuscate_handler = null;

const ONLY_SELECTED_HANDLERS = true;

// System.loadNative on Android will use `android_dlopen_ext`
// More https://android.googlesource.com/platform/art/+/refs/heads/android12-mainline-art-release/libnativebridge/native_bridge.cc#63
Interceptor.attach(Module.findGlobalExportByName('android_dlopen_ext'), {
    onEnter: function (args) {
        this.path = args[0].readUtf8String();
        this.mode = args[1];
    },
    onLeave: function (addr) {
        if (this.path.includes("libjnidispatch.so")) {
            console.log(`android_dlopen_ext(path=${this.path}, mode=${this.mode}) ret: ${addr}`);
            libjnidispatch_handler = new UInt64(addr.toString()).toNumber();
        }
        return addr;
    }
});

Interceptor.attach(Module.findGlobalExportByName('dlopen'), {
    onEnter: function (args) {
        this.path = args[0].readUtf8String();
        this.mode = args[1];
    },
    onLeave: function (addr) {
        if (this.path === "libuniffi_obfuscate.so") {
            console.log(`dlopen(path=${this.path}, mode=${this.mode}) ret: ${addr}`);
            libuniffi_obfuscate_handler = new UInt64(addr.toString()).toNumber();
        }
        return addr;
    }
});

Interceptor.attach(Module.findGlobalExportByName('dlsym'), {
    onEnter: function (args) {
        this.handle = args[0];
        this.name = args[1].readUtf8String();
    },
    onLeave: function (addr) {
        const this_handler = new UInt64(this.handle.toString()).toNumber();
        const shouldProcess = !ONLY_SELECTED_HANDLERS || (this_handler === libuniffi_obfuscate_handler || this_handler === libjnidispatch_handler);
        if (!shouldProcess) {
            return;
        }
        console.log(`dlsym(handle=${this.handle}, name=${this.name}) ret: ${addr}`);

        if (this.name === "uniffi_obfuscate_fn_func_decrypt") {
            console.log(`\n*** Found uniffi_obfuscate_fn_func_decrypt at: ${addr} *** `);

            Interceptor.attach(addr, {
                onEnter: function (args) {
                    console.log("\n=== uniffi_obfuscate_fn_func_decrypt called ===");
                    // x86_64: args[0] = hidden return buffer, args[1] = out_status pointer
                    // RustBuffer data is passed on stack, not in args array
                    console.log("Hidden return buffer: " + args[0]);
                    console.log("RustCallStatus* out_status: " + args[1]);

                    this.call_status = args[1];

                    // For x86_64, we need to read the RustBuffer from the stack
                    // The RustBuffer data starts at RSP+8 (after return address)
                    try {
                        const stack = this.context.rsp;
                        const rustBufferPtr = stack.add(Process.pointerSize);

                        const capacity = rustBufferPtr.readU64();
                        const len = rustBufferPtr.add(8).readU64();
                        const dataPtr = rustBufferPtr.add(16).readPointer();

                        console.log("RustBuffer data (from stack) - capacity: " + capacity +
                            ", len: " + len +
                            ", data pointer: " + dataPtr);

                        if (!dataPtr.isNull() && len > 0) {
                            const arrayBuf = dataPtr.readByteArray(parseInt(len));
                            const arr = new Uint8Array(arrayBuf);

                            const dataLen = new DataView(arr.buffer, arr.byteOffset, arr.byteLength).getInt32(0, false);
                            console.log("Data len            : " + dataLen);
                            console.log("Input data (hex)    : " + getAsHex(arr.subarray(4)));
                            console.log("Raw input data (hex): " + getAsHex(arr));

                        }
                    } catch (e) {
                        console.log("Could not read RustBuffer from stack: " + e);
                    }
                },
                onLeave: function (retval) {
                    console.log("\n=== uniffi_obfuscate_fn_func_decrypt returning ===");

                    // x86_64: return value written to hidden buffer pointed by args[0]
                    const retBuffer = ptr(retval);
                    const capacity = retBuffer.readU64();
                    const len = retBuffer.add(8).readU64();
                    const dataPtr = retBuffer.add(16).readPointer();

                    console.log("Return RustBuffer - capacity: " + capacity +
                        ", len: " + len +
                        ", data pointer: " + dataPtr);

                    if (!dataPtr.isNull() && len > 0) {
                        const retData = dataPtr.readByteArray(parseInt(len));
                        console.log("Returned data (hex)  : " + getAsHex(new Uint8Array(retData)));
                        console.log("Returned data (ascii): " + dataPtr.readCString(parseInt(len)));
                    }


                    // Also check the out_status for errors
                    try {
                        const outStatusPtr = this.call_status;
                        if (!outStatusPtr.isNull()) {
                            const errorCode = outStatusPtr.readS8();
                            console.log("RustCallStatus error code: " + errorCode);

                            if (errorCode !== 0) {
                                // RustCallStatus is padded to 8 bytes, so we need to read the next 8 bytes
                                const errorBufAddr = outStatusPtr.add(8);
                                const errorBufCapacity = errorBufAddr.readU64();
                                const errorBufLen = errorBufAddr.add(8).readU64();
                                const errorDataPtr = errorBufAddr.add(16).readPointer();
                                console.log("Error buffer - capacity: " + errorBufCapacity +
                                    ", len: " + errorBufLen +
                                    ", data: " + errorDataPtr);
                                console.log("Error: " + errorDataPtr.readCString(parseInt(errorBufLen)));
                            }
                        }
                    } catch (e) {
                        console.log("Could not read RustCallStatus: " + e);
                    }

                    console.log("=============================================\n");
                }
            });
        }
    }
});