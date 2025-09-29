pub fn decrypt_bytes(data: Vec<u8>, keys: &[u8]) -> Vec<u8> {
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
            "com.example.device.DeviceInfoCollector",
            "collectDeviceInfo",
        ];
        let key = "a-very-secret-key-for-this-!@#$".as_bytes();
        for plain in plains {
            let cipher = encrypt_bytes(plain.as_bytes(), key);
            let dec = decrypt_bytes(cipher.clone(), key);
            let bytes = cipher
                .iter()
                .map(|&b| format!("0x{:02X} ", b))
                .collect::<Vec<String>>()
                .join(",");
            let java_type = format!("getStr(new byte[] {{ {} }})", bytes);
            println!("{} -> {}", plain, java_type);
            assert_eq!(
                String::from_utf8(dec).expect("failed to create string"),
                plain
            );
        }
    }
}
