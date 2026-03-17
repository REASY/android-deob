# peer-info-server

`peer-info-server` is a small Rust server that exposes the Android app contract over three transports:

- TCP
- UDP
- HTTP (cleartext only)

Each transport accepts the request payload `Hello` and returns JSON describing the remote peer plus the received request details.

## Run

```bash
cargo run -- --bind 0.0.0.0 --tcp-port 9000 --udp-port 9001 --http-port 9002
```

Defaults:

- `--bind 0.0.0.0`
- `--tcp-port 9000`
- `--udp-port 9001`
- `--http-port 9002`

For the Android emulator, the app can typically reach the host machine as `10.0.2.2`, so the matching endpoints would be:

- `tcp:10.0.2.2:9000`
- `udp:10.0.2.2:9001`
- `http:10.0.2.2:9002`

## Response shape

TCP and UDP responses include:

```json
{
  "server": "peer-info-server",
  "protocol": "tcp",
  "peer": {
    "ip": "127.0.0.1",
    "port": 40123,
    "socket": "127.0.0.1:40123",
    "family": "ipv4"
  },
  "local": {
    "ip": "127.0.0.1",
    "port": 9000,
    "socket": "127.0.0.1:9000",
    "family": "ipv4"
  },
  "request": {
    "byte_length": 5,
    "utf8": "Hello",
    "hex": "48656c6c6f",
    "is_hello": true
  },
  "timestamp_unix_ms": 1730000000000
}
```

HTTP responses add an `http` object with method, path, version, and headers:

```json
{
  "server": "peer-info-server",
  "protocol": "http",
  "peer": { "...": "..." },
  "local": { "...": "..." },
  "request": {
    "byte_length": 5,
    "utf8": "Hello",
    "hex": "48656c6c6f",
    "is_hello": true
  },
  "http": {
    "method": "POST",
    "path": "/",
    "version": "HTTP/1.1",
    "headers": [
      { "name": "Host", "value": "10.0.2.2:9002" }
    ]
  },
  "timestamp_unix_ms": 1730000000000
}
```
