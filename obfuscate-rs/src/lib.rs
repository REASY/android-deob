uniffi::include_scaffolding!("obfuscate");

pub fn greet(name: String) -> String {
    let msg = format!("Hello, {name} from Rust via UniFFI!");
    println!("{}", msg);
    msg
}

#[cfg(test)]
mod tests {
    use super::*;
}
