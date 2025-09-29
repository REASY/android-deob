fn main() {
    // Generate Rust scaffolding from UDL at build time
    uniffi_build::generate_scaffolding("src/obfuscate.udl").expect("Can't generate scaffolding");
}
