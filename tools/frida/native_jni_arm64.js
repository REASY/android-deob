'use strict';

// Find the RegisterNatives method in libart.so, the name is mangled, so we need to find the symbol by name
// https://android.googlesource.com/platform/art/+/refs/heads/android12-dev/runtime/jni/jni_internal.cc#2463
const libArt = Process.getModuleByName("libart.so");
/*
```
redroid_arm64_only:/ # readelf -s /system/apex/com.android.art/lib64/libart.so  | grep RegisterNatives
  8264: 0000000000438cfc  1836 FUNC    LOCAL  HIDDEN    14 _ZN3art12_GLOBAL__N_18CheckJNI15RegisterNativesEP7_JNIEnvP7_jclassPK15JNINativeMethodi.llvm.11435978510829101840
  9210: 00000000004b8574  3948 FUNC    LOCAL  DEFAULT   14 _ZN3art3JNIILb0EE15RegisterNativesEP7_JNIEnvP7_jclassPK15JNINativeMethodi
  9449: 000000000051b39c  3948 FUNC    LOCAL  DEFAULT   14 _ZN3art3JNIILb1EE15RegisterNativesEP7_JNIEnvP7_jclassPK15JNINativeMethodi
```
*/
const registerNativesMethod = libArt.getSymbolByName("_ZN3art3JNIILb1EE15RegisterNativesEP7_JNIEnvP7_jclassPK15JNINativeMethodi");
console.log(`RegisterNatives address: ${registerNativesMethod}`);

Interceptor.attach(registerNativesMethod, {
    onEnter: function (args) {
        let java_class = args[1];
        let class_name = Java.vm.tryGetEnv().getClassName(java_class);
        let methods_ptr = ptr(args[2]);
        let method_count = args[3].toInt32();
        console.log("RegisterNatives method_count:", method_count);

        for (let i = 0; i < method_count; i++) {
            const name_ptr = methods_ptr.readPointer();
            const sig_ptr = methods_ptr.add(Process.pointerSize).readPointer();
            const fnPtr_ptr = methods_ptr.add(Process.pointerSize * 2).readPointer();
            methods_ptr = methods_ptr.add(Process.pointerSize * 3);

            const name = name_ptr.readCString();
            const sig = sig_ptr.readCString();
            const symbol = DebugSymbol.fromAddress(fnPtr_ptr)
            const callee = DebugSymbol.fromAddress(this.returnAddress);
            console.log(`RegisterNatives java_class: ${class_name}, name: ${name}, sig: ${sig}, fnPtr: ${fnPtr_ptr}, fnOffset: ${symbol}, callee: ${callee}`);
        }
    },
    onLeave: function (ret) {
        console.log(`RegisterNatives ret: ${ret}`);
        return ret;
    }
});

// System.loadNative on Android will use `android_dlopen_ext`
// More https://android.googlesource.com/platform/art/+/refs/heads/android12-mainline-art-release/libnativebridge/native_bridge.cc#63
Interceptor.attach(Module.findGlobalExportByName('android_dlopen_ext'), {
    onEnter: function (args) {
        this.path = args[0].readUtf8String();
        this.mode = args[1];
    },
    onLeave: function (addr) {
        console.log(`android_dlopen_ext(path=${this.path}, mode=${this.mode}) ret: ${addr}`);
        return addr;
    }
});

Interceptor.attach(Module.findGlobalExportByName('dlopen'), {
    onEnter: function (args) {
        this.path = args[0].readUtf8String();
        this.mode = args[1];
    },
    onLeave: function (addr) {
        console.log(`dlopen(path=${this.path}, mode=${this.mode}) ret: ${addr}`);
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
        console.log(`dlsym(handle=${this.handle}, name=${this.name}) ret: ${addr}`);

        // Uncomment to put a hook on a specific native method

        // const this_name = this.name;
        // Interceptor.attach(addr, {
        //     onEnter: function (args) {
        //         // Log that we entered the function
        //         console.log(`\n=== ${this_name} called ===`);
        //     },
        //     onLeave: function (retval) {
        //         console.log(`\n=== ${this_name} returning ===`);
        //         console.log("=============================================\n");
        //     }
        // });
    }
});
