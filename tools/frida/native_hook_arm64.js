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

// Intercept dlsym calls to find the uniffi_obfuscate_fn_func_decrypt function
// Once the method is found, we can hook it and inspect the input data and the return value
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
                    // https://developer.arm.com/documentation/102374/0103/Procedure-Call-Standard
                    // The result will be written into Indirect result register, the address is prepared right before the call
                    this.xr = this.context.x8;

                    // Log that we entered the function
                    console.log("\n=== uniffi_obfuscate_fn_func_decrypt called ===");

                    // --- Access args[0], which is the first RustBuffer ---
                    // Assuming the ABI passes the struct by value, it might be in registers/stack.
                    // For aarch64, check if it's passed by a pointer in args[0].
                    let rustBufferPtr = args[0]; // Often, large structs are passed by pointer

                    // Read the RustBuffer struct from the pointer
                    let capacity = rustBufferPtr.readU64();
                    let len = rustBufferPtr.add(8).readU64();
                    let dataPtr = rustBufferPtr.add(16).readPointer();
                    console.log("RustBuffer data: capacity: " + capacity + ", len: " + len + ", data pointer: " + dataPtr);

                    if (!dataPtr.isNull() && len > 0) {
                        const arrayBuf = dataPtr.readByteArray(parseInt(len));
                        const arr = new Uint8Array(arrayBuf);

                        const dataLen = new DataView(arr.buffer, arr.byteOffset, arr.byteLength).getInt32(0, false);
                        console.log("Data len            : " + dataLen);
                        console.log("Input data (hex)    : " + getAsHex(arr.subarray(4)));
                        console.log("Raw input data (hex): " + getAsHex(arr));
                    }

                    // --- Access args[1], which is a pointer to RustCallStatus ---
                    let callStatusPtr = args[1];
                    console.log(`RustCallStatus *out_status: ${callStatusPtr}`);
                },
                onLeave: function (retval) {
                    console.log("\n=== uniffi_obfuscate_fn_func_decrypt returning ===");
                    // The result was written into Indirect result register, use it
                    const retPtr = this.xr;
                    let capacity = retPtr.readU64();
                    let len = retPtr.add(8).readU64();
                    let dataPtr = retPtr.add(16).readPointer();
                    console.log("Return RustBuffer - capacity: " + capacity + ", len: " + len + ", data pointer: " + dataPtr);

                    if (!dataPtr.isNull() && len > 0) {
                        const retData = dataPtr.readByteArray(parseInt(len));
                        console.log("Returned data (hex)  : " + getAsHex(new Uint8Array(retData)));
                        console.log("Returned data (ascii): " + dataPtr.readCString(parseInt(len)));
                    }

                    console.log("=============================================\n");
                }
            });
        }
    }
});