package metrics_influxdb;

public class HttpInfluxdbProtocol2 implements InfluxdbProtocol {
	public final static String DEFAULT_HOST = "127.0.0.1";
	public final static int DEFAULT_PORT = 8086;
	public final static String DEFAULT_BUCKET = "metrics";
	public final static long DEFAULT_CONNECT_TIMEOUT_SECONDS = 2;
	public final static long DEFAULT_READ_TIMEOUT_SECONDS = 2;

	public final String scheme;
	public final String org;
	public final String token;
	public final String host;
	public final int port;
	public final String bucket;
	public final long connectTimeout;
	public final long readTimeout;

	public HttpInfluxdbProtocol2(String scheme, String host, int port, String org, String token, String bucket, long connectTimeout, long readTimeout) {
		super();
		this.scheme = scheme;
		this.host = host;
		this.port = port;
		this.org = org;
		this.token = token;
		this.bucket = bucket;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	public HttpInfluxdbProtocol2(String scheme, String host, int port, String org, String token, String bucket) {
	    this(scheme, host, port, org, token, bucket, DEFAULT_CONNECT_TIMEOUT_SECONDS, DEFAULT_READ_TIMEOUT_SECONDS);
	}

	public HttpInfluxdbProtocol2(String host, int port, String org, String token, String bucket) {
		this("http", host, port, org, token, bucket);
	}

	public HttpInfluxdbProtocol2(String host, int port, String org, String token) {
		this(host, port, org, token, DEFAULT_BUCKET);
	}
}
