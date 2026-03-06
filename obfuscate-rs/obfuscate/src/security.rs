use serde::Serialize;
use std::fs;
use std::io::ErrorKind;
use std::time::{SystemTime, UNIX_EPOCH};

#[cfg(target_os = "android")]
use std::ffi::{CStr, CString};
#[cfg(target_os = "android")]
use std::os::raw::{c_char, c_int};

#[cfg(target_os = "android")]
const PROP_VALUE_MAX: usize = 92;

#[cfg(target_os = "android")]
unsafe extern "C" {
    fn __system_property_get(name: *const c_char, value: *mut c_char) -> c_int;
}

const EMULATOR_PROPERTY_KEYS: &[&str] = &[
    "init.svc.android-hardware-media-c2-goldfish-hal-1-0",
    "init.svc.goldfish-logcat",
    "init.svc.goldfish-setup",
    "init.svc.qemu-adb-keys",
    "init.svc.qemu-adb-setup",
    "init.svc.qemu-props",
    "init.svc.qemu-props-bootcomplete",
    "init.svc.qemud",
    "init.svc.qemud-props",
    "qemu.adb.secure",
    "qemu.cmdline",
    "qemu.gles",
    "qemu.hw.mainkeys",
    "qemu.logcat",
    "qemu.networknamespace",
    "qemu.sf.fake.camera",
    "qemu.sf.fake_camera",
    "qemu.sf.lcd.density",
    "qemu.sf.lcd_density",
    "qemu.timezone",
    "ro.adb.qemud",
    "ro.boot.qemu",
    "ro.boot.qemu.avd_name",
    "ro.boot.qemu.camera_hq_edge_processing",
    "ro.boot.qemu.camera_protocol_ver",
    "ro.boot.qemu.cpuvulkan.version",
    "ro.boot.qemu.gltransport.drawFlushInterval",
    "ro.boot.qemu.gltransport.name",
    "ro.boot.qemu.hwcodec.avcdec",
    "ro.boot.qemu.hwcodec.hevcdec",
    "ro.boot.qemu.settings.system.screen_off_timeout",
    "ro.boot.qemu.virtiowifi",
    "ro.boot.qemu.vsync",
    "ro.kernel.android.qemud",
    "ro.kernel.qemu",
    "ro.kernel.qemu.avd_name",
    "ro.kernel.qemu.camera_hq_edge_processing",
    "ro.kernel.qemu.camera_protocol_ver",
    "ro.kernel.qemu.dalvik.vm.heapsize",
    "ro.kernel.qemu.encrypt",
    "ro.kernel.qemu.gles",
    "ro.kernel.qemu.gltransport",
    "ro.kernel.qemu.gltransport.drawFlushInterval",
    "ro.kernel.qemu.opengles.version",
    "ro.kernel.qemu.settings.system.screen_off_timeout",
    "ro.kernel.qemu.uirenderer",
    "ro.kernel.qemu.vsync",
    "ro.kernel.qemu.wifi",
    "ro.qemu.initrc",
    "vendor.qemu.dev.bootcomplete",
    "vendor.qemu.sf.fake_camera",
    "vendor.qemu.timezone",
    "vendor.qemu.vport.bluetooth",
    "vendor.qemu.vport.modem",
];

const ROOT_TAMPER_PROPERTY_KEYS: &[&str] = &[
    "ro.secure",
    "ro.debuggable",
    "service.adb.root",
    "ro.boot.verifiedbootstate",
    "ro.boot.flash.locked",
    "ro.boot.vbmeta.device_state",
];

const CONTEXT_PROPERTY_KEYS: &[&str] = &[
    "ro.build.fingerprint",
    "ro.product.model",
    "ro.product.manufacturer",
    "ro.product.brand",
    "ro.product.device",
    "ro.hardware",
    "ro.kernel.version",
    "ro.serialno",
];

const EMULATOR_PATHS: &[&str] = &[
    "/dev/qemu_pipe",
    "/dev/socket/qemud",
    "/dev/socket/genyd",
    "/dev/socket/baseband_genyd",
    "/sys/qemu_trace",
    "/system/bin/qemu-props",
    "/system/bin/qemud",
    "/system/lib/libc_malloc_debug_qemu.so",
    "/system/bin/nox-prop",
    "fstab.nox",
    "init.nox.rc",
    "ueventd.nox.rc",
    "system/lib/libnoxspeedup.so",
    "fstab.vbox86",
    "init.vbox86.rc",
    "ueventd.vbox86.rc",
    "/dev/vboxguest",
    "/dev/vboxuser",
    "/dev/__properties__/u:object_r:qemu_hw_prop:s0",
    "/dev/__properties__/u:object_r:qemu_sf_lcd_density_prop:s0",
];

const ROOT_TAMPER_PATHS: &[&str] = &[
    "/cache/su",
    "/data/local/bin/su",
    "/data/local/su",
    "/data/local/xbin/su",
    "/data/su",
    "/dev/su",
    "/sbin/.magisk/modules/riru-core",
    "/sbin/.magisk/modules/riru_lsposed",
    "/sbin/.magisk/modules/zygisk_lsposed",
    "/sbin/.magisk/modules/zygisk_shamiko",
    "/sbin/su",
    "/su/bin/su",
    "/system/app/Superuser.apk",
    "/system/bin/.ext/su",
    "/system/bin/failsafe/su",
    "/system/bin/su",
    "/system/sbin/su",
    "/system/sd/xbin/su",
    "/system/usr/we-need-root/su",
    "/system/xbin/su",
    "/vendor/bin/su",
    "/data/adb/kpatch",
    "/data/adb/apd",
    "/apex/com.android.art/bin/busybox",
    "/apex/com.android.art/bin/su",
    "/apex/com.android.runtime/bin/busybox",
    "/apex/com.android.runtime/bin/su",
    "/odm/bin/busybox",
    "/odm/bin/su",
    "/product/bin/busybox",
    "/product/bin/su",
    "/system/bin/busybox",
    "/system/xbin/busybox",
    "/system_ext/bin/busybox",
    "/system_ext/bin/su",
    "/vendor/bin/busybox",
    "/vendor/xbin/busybox",
    "/vendor/xbin/su",
];

#[derive(Debug, Serialize)]
struct SecurityReport {
    generated_at_ms: u64,
    checks: Vec<CheckRecord>,
    summary: CheckSummary,
}

#[derive(Debug, Serialize)]
struct CheckSummary {
    total_checks: usize,
    total_hits: usize,
    emulator_hits: usize,
    root_tamper_hits: usize,
    property_checks: usize,
    fs_checks: usize,
}

#[derive(Debug, Serialize)]
struct CheckRecord {
    source: &'static str,
    signal_type: &'static str,
    operation: &'static str,
    target: &'static str,
    status: String,
    hit: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    value: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<String>,
}

pub fn collect_security_checks_json() -> String {
    let mut checks = Vec::new();

    for key in EMULATOR_PROPERTY_KEYS {
        checks.push(property_check("emulator", key));
    }

    for key in ROOT_TAMPER_PROPERTY_KEYS {
        checks.push(property_check("root_tamper", key));
    }

    for key in CONTEXT_PROPERTY_KEYS {
        checks.push(property_check("context", key));
    }

    for path in EMULATOR_PATHS {
        checks.push(fs_check("emulator", path));
    }

    for path in ROOT_TAMPER_PATHS {
        checks.push(fs_check("root_tamper", path));
    }

    let summary = build_summary(&checks);
    let report = SecurityReport {
        generated_at_ms: now_ms(),
        checks,
        summary,
    };

    serde_json::to_string_pretty(&report)
        .unwrap_or_else(|err| format!("{{\"error\":\"json_serialize_failed: {}\"}}", err))
}

fn build_summary(checks: &[CheckRecord]) -> CheckSummary {
    let total_hits = checks.iter().filter(|c| c.hit).count();
    let emulator_hits = checks
        .iter()
        .filter(|c| c.signal_type == "emulator" && c.hit)
        .count();
    let root_tamper_hits = checks
        .iter()
        .filter(|c| c.signal_type == "root_tamper" && c.hit)
        .count();
    let property_checks = checks.iter().filter(|c| c.source == "property").count();
    let fs_checks = checks.iter().filter(|c| c.source == "fs").count();

    CheckSummary {
        total_checks: checks.len(),
        total_hits,
        emulator_hits,
        root_tamper_hits,
        property_checks,
        fs_checks,
    }
}

fn property_check(signal_type: &'static str, key: &'static str) -> CheckRecord {
    match read_property(key) {
        Ok(value) => {
            let value = value.trim().to_string();
            let hit = !value.is_empty();
            let status = if hit { "present" } else { "empty" };
            CheckRecord {
                source: "property",
                signal_type,
                operation: "__system_property_get",
                target: key,
                status: status.to_string(),
                hit,
                value: Some(value),
                error: None,
            }
        }
        Err(error) => CheckRecord {
            source: "property",
            signal_type,
            operation: "__system_property_get",
            target: key,
            status: "error".to_string(),
            hit: false,
            value: None,
            error: Some(error),
        },
    }
}

fn fs_check(signal_type: &'static str, path: &'static str) -> CheckRecord {
    match fs::metadata(path) {
        Ok(meta) => {
            let kind = if meta.is_dir() {
                "dir"
            } else if meta.is_file() {
                "file"
            } else {
                "other"
            };
            CheckRecord {
                source: "fs",
                signal_type,
                operation: "stat",
                target: path,
                status: "exists".to_string(),
                hit: true,
                value: Some(kind.to_string()),
                error: None,
            }
        }
        Err(err) => {
            let status = match err.kind() {
                ErrorKind::NotFound => "missing",
                ErrorKind::PermissionDenied => "permission_denied",
                _ => "error",
            };
            CheckRecord {
                source: "fs",
                signal_type,
                operation: "stat",
                target: path,
                status: status.to_string(),
                hit: false,
                value: None,
                error: Some(err.to_string()),
            }
        }
    }
}

fn read_property(key: &str) -> Result<String, String> {
    #[cfg(target_os = "android")]
    {
        let property_name =
            CString::new(key).map_err(|_| "invalid_property_name: contains_nul".to_string())?;
        let mut value = [0 as c_char; PROP_VALUE_MAX];

        let _length = unsafe { __system_property_get(property_name.as_ptr(), value.as_mut_ptr()) };
        let value = unsafe { CStr::from_ptr(value.as_ptr()) };
        return Ok(value.to_string_lossy().into_owned());
    }

    #[cfg(not(target_os = "android"))]
    {
        let _ = key;
        Err("system_property_get_unavailable_on_non_android".to_string())
    }
}

fn now_ms() -> u64 {
    let Ok(duration) = SystemTime::now().duration_since(UNIX_EPOCH) else {
        return 0;
    };
    duration.as_millis() as u64
}
