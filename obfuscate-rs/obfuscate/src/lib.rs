uniffi::include_scaffolding!("obfuscate");

use obfuscate_core::decrypt_bytes as decrypt_bytes0;
mod security;

static KEY: &[u8] = "a-very-secret-key-for-this-!@#$".as_bytes();

pub fn decrypt(name: Vec<u8>) -> String {
    let decrypted = decrypt_bytes(name);
    String::from_utf8_lossy(decrypted.as_slice()).into_owned()
}

pub fn decrypt_bytes(data: Vec<u8>) -> Vec<u8> {
    decrypt_bytes0(data, KEY)
}

pub fn collect_security_checks_json() -> String {
    security::collect_security_checks_json()
}

use jni::objects::{JClass, JString};
use jni::strings::JNIString;
use jni::EnvUnowned;

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
