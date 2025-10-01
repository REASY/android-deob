'use strict';

function tryHookLoadLibrary() {
    const System = Java.use('java.lang.System');
    const Runtime = Java.use('java.lang.Runtime');
    const SystemLoad_2 = System.loadLibrary.overload('java.lang.String');
    const VMStack = Java.use('dalvik.system.VMStack');

    SystemLoad_2.implementation = function (library) {
        console.log("Loading dynamic library => " + library);
        try {
            Runtime.getRuntime().loadLibrary0(VMStack.getCallingClassLoader(), library);
            console.log(`loadLibrary0 succeeded for ${library}`);
        } catch (ex) {
            console.log(ex);
        }
    };
}

let timer = setInterval(() => {
    // Wait for Java to be initialized
    if (!Java.available) return;

    Java.perform(tryHookLoadLibrary);

    clearInterval(timer);
}, 250);
