package metrics_influxdb.measurements;

import metrics_influxdb.HttpInfluxdbProtocol2;
import metrics_influxdb.misc.Miscellaneous;
import metrics_influxdb.serialization.line.Inliner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class HttpInlinerSender2 extends QueueableSender {
	private final static Logger LOGGER = LoggerFactory.getLogger(HttpInlinerSender2.class);
	private static int MAX_MEASURES_IN_SINGLE_POST = 5000;
	private final URL writeURL;
	private final Inliner inliner;
	private final long connectTimeout;
	private final long readTimeout;
	private final String authorization;

	public HttpInlinerSender2(HttpInfluxdbProtocol2 protocol) {
		super(MAX_MEASURES_IN_SINGLE_POST);
		URL toJoin;

		inliner = new Inliner(TimeUnit.MILLISECONDS);
		connectTimeout =  protocol.connectTimeout;
		readTimeout = protocol.readTimeout;
		authorization = "Token " + protocol.token;

		try {
				toJoin = new URL(protocol.scheme, protocol.host, protocol.port, "/api/v2/write?precision=ms&org=" + Miscellaneous.urlEncode(protocol.org) + "&bucket="
						+ Miscellaneous.urlEncode(protocol.bucket));
		} catch (MalformedURLException | UnsupportedEncodingException e) {
			toJoin = null;
		}

		writeURL = toJoin;
	}

	@Override
	protected boolean doSend(Collection<Measure> measures) {
		if (measures.isEmpty()) {
			return true;
		}

		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) writeURL.openConnection();
			con.setRequestMethod("POST");
			con.setConnectTimeout(Long.valueOf(TimeUnit.SECONDS.toMillis(connectTimeout)).intValue());
			con.setReadTimeout(Long.valueOf(TimeUnit.SECONDS.toMillis(readTimeout)).intValue());

			con.setRequestProperty("Authorization", authorization);

			// Send post request
			con.setDoOutput(true);
			OutputStream wr = con.getOutputStream();
			String measuresAsString = inliner.inline(measures);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Measures being sent:\n{}", measuresAsString);
			}
			wr.write(measuresAsString.getBytes(Miscellaneous.UTF8));

			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();

			switch (responseCode) {
			case HttpURLConnection.HTTP_NO_CONTENT:
				LOGGER.debug("{} Measures sent to {}://{}:{}", measures.size(), writeURL.getProtocol(), writeURL.getHost(), writeURL.getPort());
				break;
			case HttpURLConnection.HTTP_OK:
				LOGGER.info("{} Measures sent to {}://{}:{} but not saved by infludb, reason:\n{}", measures.size(), writeURL.getProtocol(), writeURL.getHost(), writeURL.getPort(), Miscellaneous.readFrom(con.getInputStream()));
				break;
			default:
				LOGGER.info("failed to send {} Measures to {}://{}:{}, HTTP CODE received: {}\n", measures.size(), writeURL.getProtocol(), writeURL.getHost(), writeURL.getPort(), responseCode,  Miscellaneous.readFrom(con.getInputStream()));
				break;
			}

			return true;
		} catch (IOException e) {
			// Here the influxdb is potentially temporary unreachable
			// we do not clear held measures so that we'll eb able to retry to post them
			LOGGER.warn("couldn't sent metrics to {}://{}:{}, reason: {}", writeURL.getProtocol(), writeURL.getHost(), writeURL.getPort(), e.getMessage(), e);
		} finally {
			// cleanup connection streams
			if (con != null) {
				try {
					con.getInputStream().close();
				} catch (Exception ignore) {
					// ignore
				}
			}
		}

		return false;
	}
}
