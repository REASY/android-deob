pub fn decrypt_bytes0(data: Vec<u8>, keys: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(data.len());
    for (i, &b) in data.iter().enumerate() {
        let k = keys[i % keys.len()];
        out.push(b ^ k);
    }
    out
}

pub fn encrypt_bytes(bytes: &[u8], keys: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(bytes.len());
    for (i, &b) in bytes.iter().enumerate() {
        out.push(b ^ keys[i % keys.len()]);
    }
    out
}

uniffi::include_scaffolding!("obfuscate");

static KEY: &[u8] = "a-very-secret-key-for-this-!@#$".as_bytes();

pub fn decrypt(name: Vec<u8>) -> String {
    let decrypted = decrypt_bytes(name);
    String::from_utf8_lossy(decrypted.as_slice()).into_owned()
}

pub fn decrypt_bytes(data: Vec<u8>) -> Vec<u8> {
    decrypt_bytes0(data, KEY)
}

use jni::EnvUnowned;

// These objects are what you should use as arguments to your native function.
// They carry extra lifetime information to prevent them escaping from the
// current local frame (which is the scope within which local (temporary)
// references to Java objects remain valid)
use jni::objects::{JClass, JString};

use jni::strings::JNIString;

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_Obfuscate_hello<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _class: JClass<'local>,
    input: JString<'local>,
) -> JString<'local> {
    let outcome = unowned_env.with_env(|env| -> jni::errors::Result<_> {
        let input: String = input.to_string();
        env.new_string(JNIString::from(format!("Hello, {}!", input)))
    });
    outcome.resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn roundtrip() {
        let plains = [
            "getAssets",
            "java.nio.ByteBuffer",
            "wrap",
            "getClassLoader",
            "dalvik.system.InMemoryDexClassLoader",
            "java.nio.ByteBuffer",
            "java.lang.ClassLoader",
            "loadClass",
            "open",
            "java.io.ByteArrayOutputStream",
            "java.io.InputStream",
            "read",
            "write",
            "close",
            "toByteArray",
            "uniffi.obfuscate.ObfuscateKt",
            "decryptBytes",
            "data.bin",
        ];
        for plain in plains {
            let cipher = encrypt_bytes(plain.as_bytes(), KEY);
            let dec = decrypt(cipher.clone());
            let bytes = cipher
                .iter()
                .map(|&b| format!("0x{:02X} ", b))
                .collect::<Vec<String>>()
                .join(",");
            let java_type = format!("getStr(new byte[] {{ {} }})", bytes);
            println!("{} -> {}", plain, java_type);
            assert_eq!(dec, plain);
        }
    }
}
