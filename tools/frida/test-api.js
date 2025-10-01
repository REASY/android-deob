'use strict';

function safeDo(name, fn) {
    try {
        var r = fn();
        console.log('[OK] ' + name + ' => ' + String(r));
    } catch (e) {
        console.log('[ERR] ' + name + ' => ' + String(e));
    }
}

safeDo('Process.enumerateModules (type)', function() { return typeof Process.enumerateModules; });
safeDo('Module.findGlobalExportByName (type)', function() { return typeof Module.findGlobalExportByName; });
safeDo('Interceptor.attach (type)', function() { return typeof Interceptor.attach; });
safeDo('Memory.readUtf8String (type)', function() { return typeof Memory.readUtf8String; });
safeDo('Process.enumerateModules() length', function() {
    var m = Process.enumerateModules();
    return (m ? m.length : '<null>');
});

safeDo('Module.findGlobalExportByName("android_dlopen_ext")', function() {
    try { return String(Module.findGlobalExportByName('android_dlopen_ext')); } catch (e) { return e.toString(); }
});

safeDo('Module.findGlobalExportByName("dlopen")', function() {
    try { return String(Module.findGlobalExportByName('dlopen')); } catch (e) { return e.toString(); }
});

safeDo('Module.findGlobalExportByName("dlsym")', function() {
    try { return String(Module.findGlobalExportByName('dlsym')); } catch (e) { return e.toString(); }
});


Process.enumerateModules().forEach(function(m) {
    if (m.name === 'libc.so' || m.name === "libdl.so") {
        console.log(m.name + ", " + m.path);
    }
})


