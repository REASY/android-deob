use clap::Parser;
use serde::Serialize;
use std::io::{self, ErrorKind, Read, Write};
use std::net::{Shutdown, SocketAddr, TcpListener, TcpStream, UdpSocket};
use std::thread;
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use thiserror::Error;

const SERVER_NAME: &str = "peer-info-server";
const HELLO_BYTES: &[u8] = b"Hello";
const FIRST_BYTE_TIMEOUT: Duration = Duration::from_secs(5);
const IDLE_TIMEOUT: Duration = Duration::from_millis(750);
const MAX_HTTP_HEADER_BYTES: usize = 64 * 1024;
const MAX_HTTP_BODY_BYTES: usize = 1024 * 1024;

#[derive(Clone, Debug, Parser, PartialEq, Eq)]
#[command(
    name = "peer-info-server",
    version,
    about = "Return peer and request metadata as JSON over TCP, UDP, and HTTP.",
    long_about = None
)]
struct ServerConfig {
    #[arg(long = "bind", default_value = "0.0.0.0")]
    bind_addr: String,

    #[arg(long = "tcp-port", default_value_t = 9000_u16)]
    tcp_port: u16,

    #[arg(long = "udp-port", default_value_t = 9001_u16)]
    udp_port: u16,

    #[arg(long = "http-port", default_value_t = 9002_u16)]
    http_port: u16,
}

#[derive(Debug, Error)]
enum ServerError {
    #[error("Failed to bind TCP listener on {addr}: {source}")]
    BindTcp {
        addr: String,
        #[source]
        source: io::Error,
    },

    #[error("Failed to bind UDP socket on {addr}: {source}")]
    BindUdp {
        addr: String,
        #[source]
        source: io::Error,
    },

    #[error("Failed to bind HTTP listener on {addr}: {source}")]
    BindHttp {
        addr: String,
        #[source]
        source: io::Error,
    },

    #[error("Failed to read TCP local address: {source}")]
    ReadTcpLocalAddr {
        #[source]
        source: io::Error,
    },

    #[error("Failed to read UDP local address: {source}")]
    ReadUdpLocalAddr {
        #[source]
        source: io::Error,
    },

    #[error("Failed to read HTTP local address: {source}")]
    ReadHttpLocalAddr {
        #[source]
        source: io::Error,
    },

    #[error("The {protocol} server thread panicked.")]
    ThreadPanicked { protocol: &'static str },
}

#[derive(Debug, Error)]
enum ClientError {
    #[error("TCP client I/O failed: {0}")]
    TcpIo(#[source] io::Error),

    #[error("HTTP client I/O failed: {0}")]
    HttpIo(#[source] io::Error),

    #[error(transparent)]
    HttpRequest(#[from] HttpRequestError),

    #[error(transparent)]
    Response(#[from] ResponseError),
}

#[derive(Debug, Error)]
enum ResponseError {
    #[error("Failed to serialize JSON response: {0}")]
    Serialize(#[from] serde_json::Error),
}

#[derive(Debug, Error)]
enum HttpRequestError {
    #[error("Failed to read HTTP request bytes: {0}")]
    Io(#[source] io::Error),

    #[error("HTTP request ended before headers were complete.")]
    HeadersIncomplete,

    #[error("HTTP headers exceeded the size limit.")]
    HeadersTooLarge,

    #[error("HTTP request line was missing.")]
    MissingRequestLine,

    #[error("HTTP method was missing.")]
    MissingMethod,

    #[error("HTTP path was missing.")]
    MissingPath,

    #[error("HTTP version was missing.")]
    MissingVersion,

    #[error("HTTP request line had too many parts.")]
    TooManyRequestLineParts,

    #[error("HTTP header line was malformed.")]
    MalformedHeader,

    #[error("HTTP Content-Length was not numeric.")]
    InvalidContentLength,

    #[error("HTTP body exceeded the size limit.")]
    BodyTooLarge,

    #[error("HTTP body ended before Content-Length bytes were received.")]
    BodyTruncated,
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
struct HttpHeader {
    name: String,
    value: String,
}

#[derive(Debug, PartialEq, Eq)]
struct HttpRequest {
    method: String,
    path: String,
    version: String,
    headers: Vec<HttpHeader>,
    body: Vec<u8>,
}

#[derive(Debug, Serialize, PartialEq, Eq)]
struct SocketAddrInfo {
    ip: String,
    port: u16,
    socket: String,
    family: &'static str,
}

#[derive(Debug, Serialize, PartialEq, Eq)]
struct RequestPayload {
    byte_length: usize,
    utf8: String,
    hex: String,
    is_hello: bool,
}

#[derive(Debug, Serialize, PartialEq, Eq)]
struct HttpMetadata {
    method: String,
    path: String,
    version: String,
    headers: Vec<HttpHeader>,
}

#[derive(Debug, Serialize, PartialEq, Eq)]
struct TransportResponse {
    server: &'static str,
    protocol: &'static str,
    peer: SocketAddrInfo,
    local: SocketAddrInfo,
    request: RequestPayload,
    timestamp_unix_ms: u128,
}

#[derive(Debug, Serialize, PartialEq, Eq)]
struct HttpResponseBody {
    server: &'static str,
    protocol: &'static str,
    peer: SocketAddrInfo,
    local: SocketAddrInfo,
    request: RequestPayload,
    http: HttpMetadata,
    timestamp_unix_ms: u128,
}

#[derive(Debug, Serialize, PartialEq, Eq)]
struct ErrorResponse {
    server: &'static str,
    protocol: &'static str,
    peer: Option<SocketAddrInfo>,
    local: Option<SocketAddrInfo>,
    error: String,
    timestamp_unix_ms: u128,
}

impl ServerConfig {
    fn tcp_bind_addr(&self) -> String {
        join_host_port(&self.bind_addr, self.tcp_port)
    }

    fn udp_bind_addr(&self) -> String {
        join_host_port(&self.bind_addr, self.udp_port)
    }

    fn http_bind_addr(&self) -> String {
        join_host_port(&self.bind_addr, self.http_port)
    }
}

fn main() {
    let config = ServerConfig::parse();

    if let Err(error) = run(config) {
        eprintln!("{error}");
        std::process::exit(1);
    }
}

fn run(config: ServerConfig) -> Result<(), ServerError> {
    let tcp_bind_addr = config.tcp_bind_addr();
    let udp_bind_addr = config.udp_bind_addr();
    let http_bind_addr = config.http_bind_addr();

    let tcp_listener =
        TcpListener::bind(&tcp_bind_addr).map_err(|source| ServerError::BindTcp {
            addr: tcp_bind_addr,
            source,
        })?;
    let udp_socket = UdpSocket::bind(&udp_bind_addr).map_err(|source| ServerError::BindUdp {
        addr: udp_bind_addr,
        source,
    })?;
    let http_listener =
        TcpListener::bind(&http_bind_addr).map_err(|source| ServerError::BindHttp {
            addr: http_bind_addr,
            source,
        })?;

    println!(
        "TCP listening on {}",
        tcp_listener
            .local_addr()
            .map_err(|source| ServerError::ReadTcpLocalAddr { source })?
    );
    println!(
        "UDP listening on {}",
        udp_socket
            .local_addr()
            .map_err(|source| ServerError::ReadUdpLocalAddr { source })?
    );
    println!(
        "HTTP listening on {}",
        http_listener
            .local_addr()
            .map_err(|source| ServerError::ReadHttpLocalAddr { source })?
    );

    let tcp_thread = thread::spawn(move || serve_tcp(tcp_listener));
    let udp_thread = thread::spawn(move || serve_udp(udp_socket));
    let http_thread = thread::spawn(move || serve_http(http_listener));

    join_server_thread(tcp_thread, "tcp")?;
    join_server_thread(udp_thread, "udp")?;
    join_server_thread(http_thread, "http")?;
    Ok(())
}

fn join_server_thread(
    handle: thread::JoinHandle<()>,
    protocol: &'static str,
) -> Result<(), ServerError> {
    handle
        .join()
        .map_err(|_| ServerError::ThreadPanicked { protocol })
}

fn serve_tcp(listener: TcpListener) {
    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                thread::spawn(move || {
                    if let Err(error) = handle_tcp_client(stream) {
                        eprintln!("TCP client error: {error}");
                    }
                });
            }
            Err(error) => eprintln!("TCP accept error: {error}"),
        }
    }
}

fn serve_udp(socket: UdpSocket) {
    let mut buffer = [0_u8; 65_507];

    loop {
        match socket.recv_from(&mut buffer) {
            Ok((size, peer_addr)) => {
                let payload = &buffer[..size];
                let response = match socket.local_addr() {
                    Ok(local_addr) => {
                        build_transport_response("udp", peer_addr, local_addr, payload)
                    }
                    Err(error) => {
                        build_error_response("udp", Some(peer_addr), None, &error.to_string())
                    }
                };

                match response {
                    Ok(response) => {
                        if let Err(error) = socket.send_to(&response, peer_addr) {
                            eprintln!("UDP send error: {error}");
                        }
                    }
                    Err(error) => eprintln!("UDP response error: {error}"),
                }
            }
            Err(error) => eprintln!("UDP receive error: {error}"),
        }
    }
}

fn serve_http(listener: TcpListener) {
    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                thread::spawn(move || {
                    if let Err(error) = handle_http_client(stream) {
                        eprintln!("HTTP client error: {error}");
                    }
                });
            }
            Err(error) => eprintln!("HTTP accept error: {error}"),
        }
    }
}

fn handle_tcp_client(mut stream: TcpStream) -> Result<(), ClientError> {
    stream
        .set_read_timeout(Some(FIRST_BYTE_TIMEOUT))
        .map_err(ClientError::TcpIo)?;
    stream
        .set_write_timeout(Some(FIRST_BYTE_TIMEOUT))
        .map_err(ClientError::TcpIo)?;

    let peer_addr = stream.peer_addr().map_err(ClientError::TcpIo)?;
    let local_addr = stream.local_addr().map_err(ClientError::TcpIo)?;
    let payload = read_payload_bytes(&mut stream).map_err(ClientError::TcpIo)?;
    let response = build_transport_response("tcp", peer_addr, local_addr, &payload)?;

    stream.write_all(&response).map_err(ClientError::TcpIo)?;
    stream.flush().map_err(ClientError::TcpIo)?;
    let _ = stream.shutdown(Shutdown::Both);
    Ok(())
}

fn handle_http_client(mut stream: TcpStream) -> Result<(), ClientError> {
    stream
        .set_read_timeout(Some(FIRST_BYTE_TIMEOUT))
        .map_err(ClientError::HttpIo)?;
    stream
        .set_write_timeout(Some(FIRST_BYTE_TIMEOUT))
        .map_err(ClientError::HttpIo)?;

    let peer_addr = stream.peer_addr().map_err(ClientError::HttpIo)?;
    let local_addr = stream.local_addr().map_err(ClientError::HttpIo)?;

    match read_http_request(&mut stream) {
        Ok(request) => {
            let body = build_http_response(peer_addr, local_addr, &request)?;
            write_http_response(&mut stream, "200 OK", &body).map_err(ClientError::HttpIo)?;
            let _ = stream.shutdown(Shutdown::Both);
            Ok(())
        }
        Err(error) => {
            let body = build_error_response(
                "http",
                Some(peer_addr),
                Some(local_addr),
                &error.to_string(),
            )?;
            write_http_response(&mut stream, "400 Bad Request", &body)
                .map_err(ClientError::HttpIo)?;
            let _ = stream.shutdown(Shutdown::Both);
            Err(error.into())
        }
    }
}

fn read_payload_bytes(stream: &mut TcpStream) -> io::Result<Vec<u8>> {
    let mut payload = Vec::new();
    let mut buffer = [0_u8; 4096];
    let mut received_any = false;

    loop {
        match stream.read(&mut buffer) {
            Ok(0) => break,
            Ok(size) => {
                payload.extend_from_slice(&buffer[..size]);
                received_any = true;
                stream.set_read_timeout(Some(IDLE_TIMEOUT))?;
            }
            Err(error) if is_timeout(&error) && received_any => break,
            Err(error) if is_timeout(&error) => {
                return Err(io::Error::new(
                    ErrorKind::TimedOut,
                    "Timed out waiting for TCP request bytes.",
                ));
            }
            Err(error) => return Err(error),
        }
    }

    Ok(payload)
}

fn read_http_request(stream: &mut TcpStream) -> Result<HttpRequest, HttpRequestError> {
    let mut raw = Vec::new();
    let mut header_end = None;
    let mut buffer = [0_u8; 4096];

    while header_end.is_none() {
        let size = stream.read(&mut buffer).map_err(HttpRequestError::Io)?;
        if size == 0 {
            return Err(HttpRequestError::HeadersIncomplete);
        }

        raw.extend_from_slice(&buffer[..size]);
        if raw.len() > MAX_HTTP_HEADER_BYTES {
            return Err(HttpRequestError::HeadersTooLarge);
        }

        header_end = find_header_end(&raw);
    }

    let header_end = header_end.expect("header end must exist");
    let header_text = String::from_utf8_lossy(&raw[..header_end]);
    let mut lines = header_text.split("\r\n");
    let request_line = lines.next().ok_or(HttpRequestError::MissingRequestLine)?;

    let mut request_line_parts = request_line.split_whitespace();
    let method = request_line_parts
        .next()
        .ok_or(HttpRequestError::MissingMethod)?
        .to_string();
    let path = request_line_parts
        .next()
        .ok_or(HttpRequestError::MissingPath)?
        .to_string();
    let version = request_line_parts
        .next()
        .ok_or(HttpRequestError::MissingVersion)?
        .to_string();

    if request_line_parts.next().is_some() {
        return Err(HttpRequestError::TooManyRequestLineParts);
    }

    let mut headers = Vec::new();
    let mut content_length = None;

    for line in lines {
        if line.is_empty() {
            continue;
        }

        let (name, value) = line
            .split_once(':')
            .ok_or(HttpRequestError::MalformedHeader)?;
        let name = name.trim().to_string();
        let value = value.trim().to_string();

        if name.eq_ignore_ascii_case("content-length") {
            let parsed = value
                .parse::<usize>()
                .map_err(|_| HttpRequestError::InvalidContentLength)?;
            if parsed > MAX_HTTP_BODY_BYTES {
                return Err(HttpRequestError::BodyTooLarge);
            }
            content_length = Some(parsed);
        }

        headers.push(HttpHeader { name, value });
    }

    let body_start = header_end + 4;
    if let Some(content_length) = content_length {
        while raw.len() < body_start + content_length {
            let size = stream.read(&mut buffer).map_err(HttpRequestError::Io)?;
            if size == 0 {
                return Err(HttpRequestError::BodyTruncated);
            }
            raw.extend_from_slice(&buffer[..size]);
        }
        if raw.len() > body_start + MAX_HTTP_BODY_BYTES {
            return Err(HttpRequestError::BodyTooLarge);
        }
    } else if raw.len().saturating_sub(body_start) > MAX_HTTP_BODY_BYTES {
        return Err(HttpRequestError::BodyTooLarge);
    }

    let body = match content_length {
        Some(content_length) => raw[body_start..body_start + content_length].to_vec(),
        None => raw[body_start..].to_vec(),
    };

    Ok(HttpRequest {
        method,
        path,
        version,
        headers,
        body,
    })
}

fn write_http_response(stream: &mut TcpStream, status: &str, body: &[u8]) -> io::Result<()> {
    let header = format!(
        "HTTP/1.1 {status}\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: {}\r\nConnection: close\r\n\r\n",
        body.len()
    );
    stream.write_all(header.as_bytes())?;
    stream.write_all(body)?;
    stream.flush()
}

fn build_transport_response(
    protocol: &'static str,
    peer_addr: SocketAddr,
    local_addr: SocketAddr,
    payload: &[u8],
) -> Result<Vec<u8>, ResponseError> {
    serialize_json(&TransportResponse {
        server: SERVER_NAME,
        protocol,
        peer: socket_addr_info(peer_addr),
        local: socket_addr_info(local_addr),
        request: payload_info(payload),
        timestamp_unix_ms: unix_timestamp_ms(),
    })
}

fn build_http_response(
    peer_addr: SocketAddr,
    local_addr: SocketAddr,
    request: &HttpRequest,
) -> Result<Vec<u8>, ResponseError> {
    serialize_json(&HttpResponseBody {
        server: SERVER_NAME,
        protocol: "http",
        peer: socket_addr_info(peer_addr),
        local: socket_addr_info(local_addr),
        request: payload_info(&request.body),
        http: http_metadata(request),
        timestamp_unix_ms: unix_timestamp_ms(),
    })
}

fn build_error_response(
    protocol: &'static str,
    peer_addr: Option<SocketAddr>,
    local_addr: Option<SocketAddr>,
    message: &str,
) -> Result<Vec<u8>, ResponseError> {
    serialize_json(&ErrorResponse {
        server: SERVER_NAME,
        protocol,
        peer: peer_addr.map(socket_addr_info),
        local: local_addr.map(socket_addr_info),
        error: message.to_owned(),
        timestamp_unix_ms: unix_timestamp_ms(),
    })
}

fn serialize_json<T: Serialize>(value: &T) -> Result<Vec<u8>, ResponseError> {
    Ok(serde_json::to_vec_pretty(value)?)
}

fn socket_addr_info(addr: SocketAddr) -> SocketAddrInfo {
    let family = match addr {
        SocketAddr::V4(_) => "ipv4",
        SocketAddr::V6(_) => "ipv6",
    };

    SocketAddrInfo {
        ip: addr.ip().to_string(),
        port: addr.port(),
        socket: addr.to_string(),
        family,
    }
}

fn payload_info(payload: &[u8]) -> RequestPayload {
    RequestPayload {
        byte_length: payload.len(),
        utf8: String::from_utf8_lossy(payload).into_owned(),
        hex: hex_encode(payload),
        is_hello: payload == HELLO_BYTES,
    }
}

fn http_metadata(request: &HttpRequest) -> HttpMetadata {
    HttpMetadata {
        method: request.method.clone(),
        path: request.path.clone(),
        version: request.version.clone(),
        headers: request.headers.clone(),
    }
}

fn hex_encode(bytes: &[u8]) -> String {
    const HEX: &[u8; 16] = b"0123456789abcdef";
    let mut encoded = String::with_capacity(bytes.len() * 2);

    for byte in bytes {
        encoded.push(HEX[(byte >> 4) as usize] as char);
        encoded.push(HEX[(byte & 0x0f) as usize] as char);
    }

    encoded
}

fn unix_timestamp_ms() -> u128 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
}

fn find_header_end(bytes: &[u8]) -> Option<usize> {
    bytes.windows(4).position(|window| window == b"\r\n\r\n")
}

fn join_host_port(host: &str, port: u16) -> String {
    if host.contains(':') && !host.starts_with('[') {
        format!("[{host}]:{port}")
    } else {
        format!("{host}:{port}")
    }
}

fn is_timeout(error: &io::Error) -> bool {
    matches!(error.kind(), ErrorKind::WouldBlock | ErrorKind::TimedOut)
}

#[cfg(test)]
mod tests {
    use super::*;
    use clap::{Parser, error::ErrorKind as ClapErrorKind};
    use serde_json::Value;

    #[test]
    fn parse_custom_ports() {
        let config = ServerConfig::try_parse_from([
            "peer-info-server",
            "--bind",
            "::1",
            "--tcp-port",
            "19000",
            "--udp-port",
            "19001",
            "--http-port",
            "19002",
        ])
        .expect("config should parse");

        assert_eq!(
            config,
            ServerConfig {
                bind_addr: "::1".to_string(),
                tcp_port: 19000,
                udp_port: 19001,
                http_port: 19002,
            }
        );
        assert_eq!(config.tcp_bind_addr(), "[::1]:19000");
    }

    #[test]
    fn reject_unknown_argument() {
        let error =
            ServerConfig::try_parse_from(["peer-info-server", "--bogus"]).expect_err("must fail");
        assert_eq!(error.kind(), ClapErrorKind::UnknownArgument);
    }

    #[test]
    fn error_response_serializes_special_characters() {
        let response = build_error_response("udp", None, None, "quote\"\nslash\\")
            .expect("response should serialize");
        let value: Value =
            serde_json::from_slice(&response).expect("response should be valid json");

        assert_eq!(value["protocol"].as_str(), Some("udp"));
        assert!(value["peer"].is_null());
        assert_eq!(value["error"].as_str(), Some("quote\"\nslash\\"));
    }

    #[test]
    fn parse_http_request_with_body() {
        let request = parse_http_request_bytes(
            b"POST / HTTP/1.1\r\nHost: example\r\nContent-Length: 5\r\nX-Test: yes\r\n\r\nHello",
        )
        .expect("request should parse");

        assert_eq!(request.method, "POST");
        assert_eq!(request.path, "/");
        assert_eq!(request.version, "HTTP/1.1");
        assert_eq!(request.body, b"Hello");
        assert_eq!(
            request.headers,
            vec![
                HttpHeader {
                    name: "Host".to_string(),
                    value: "example".to_string(),
                },
                HttpHeader {
                    name: "Content-Length".to_string(),
                    value: "5".to_string(),
                },
                HttpHeader {
                    name: "X-Test".to_string(),
                    value: "yes".to_string(),
                }
            ]
        );
    }

    #[test]
    fn transport_response_marks_hello() {
        let peer = "127.0.0.1:40000".parse().expect("peer addr");
        let local = "127.0.0.1:9000".parse().expect("local addr");
        let response =
            build_transport_response("tcp", peer, local, b"Hello").expect("response should work");
        let value: Value =
            serde_json::from_slice(&response).expect("response should be valid json");

        assert_eq!(value["protocol"].as_str(), Some("tcp"));
        assert_eq!(value["peer"]["ip"].as_str(), Some("127.0.0.1"));
        assert_eq!(value["request"]["hex"].as_str(), Some("48656c6c6f"));
        assert_eq!(value["request"]["is_hello"].as_bool(), Some(true));
    }

    fn parse_http_request_bytes(bytes: &[u8]) -> Result<HttpRequest, HttpRequestError> {
        let header_end = find_header_end(bytes).ok_or(HttpRequestError::HeadersIncomplete)?;
        let header_text = String::from_utf8_lossy(&bytes[..header_end]);
        let mut lines = header_text.split("\r\n");
        let request_line = lines.next().ok_or(HttpRequestError::MissingRequestLine)?;
        let mut request_line_parts = request_line.split_whitespace();
        let method = request_line_parts
            .next()
            .ok_or(HttpRequestError::MissingMethod)?
            .to_string();
        let path = request_line_parts
            .next()
            .ok_or(HttpRequestError::MissingPath)?
            .to_string();
        let version = request_line_parts
            .next()
            .ok_or(HttpRequestError::MissingVersion)?
            .to_string();

        if request_line_parts.next().is_some() {
            return Err(HttpRequestError::TooManyRequestLineParts);
        }

        let mut headers = Vec::new();
        let mut content_length = 0;
        for line in lines {
            if line.is_empty() {
                continue;
            }
            let (name, value) = line
                .split_once(':')
                .ok_or(HttpRequestError::MalformedHeader)?;
            if name.eq_ignore_ascii_case("content-length") {
                content_length = value
                    .trim()
                    .parse::<usize>()
                    .map_err(|_| HttpRequestError::InvalidContentLength)?;
            }
            headers.push(HttpHeader {
                name: name.trim().to_string(),
                value: value.trim().to_string(),
            });
        }

        let body_start = header_end + 4;
        let body = bytes[body_start..body_start + content_length].to_vec();

        Ok(HttpRequest {
            method,
            path,
            version,
            headers,
            body,
        })
    }
}
