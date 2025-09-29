use clap::Parser;
use obfuscate_core::encrypt_bytes;
use std::fs::File;
use std::io::{BufReader, BufWriter, Read, Write};

#[derive(Parser, Debug, Clone)]
#[clap(author, about, long_about = None)]
struct AppArgs {
    /// The input file to encrypt
    #[clap(long)]
    input_file: String,
    /// The output file to write the encrypted data to
    #[clap(long)]
    output_file: String,
    /// The key to use for encryption
    #[clap(long)]
    key: String,
}

fn main() {
    let args = AppArgs::parse();
    let key = args.key.as_bytes();

    let mut rdr = BufReader::new(File::open(args.input_file).expect("failed to open input file"));
    let mut wrt =
        BufWriter::new(File::create(args.output_file).expect("failed to open output file"));
    let mut buffer: [u8; 64 * 1024] = [0; 64 * 1024];

    loop {
        match rdr.read(&mut buffer) {
            Ok(read_bytes) => {
                if read_bytes == 0 {
                    break;
                }
                let data = &buffer[0..read_bytes];
                let encrypted = encrypt_bytes(data, key);
                wrt.write(encrypted.as_slice())
                    .expect("failed to write data");
            }
            Err(err) => {
                println!("{:?}", err);
                break;
            }
        }
        let read = rdr.read(&mut buffer);
        if read.is_err() {
            break;
        }
    }
}
