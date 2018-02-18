package org.imsglobal.lti.launch;

import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.headers.HeaderValueType;
import com.mastfrog.acteur.headers.Method;
import static com.mastfrog.acteur.headers.Method.GET;
import com.mastfrog.acteur.util.HttpMethod;
import com.mastfrog.url.Parameters;
import com.mastfrog.url.ParametersElement;
import com.mastfrog.url.ParsedParameters;
import com.mastfrog.url.Path;
import com.mastfrog.url.URL;
import com.mastfrog.util.Exceptions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLDecoder;
import org.apache.http.HttpRequest;

import java.util.*;
import org.apache.http.Header;

/**
 * @author Paul Gray
 */
public class BaseMockHttpServletRequest implements HttpEvent {

    HttpRequest req;
    URL url;

    public BaseMockHttpServletRequest(HttpRequest req) {
        this.req = req;
        url = URL.parse(req.getRequestLine().getUri());
    }

    protected static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    @Override
    public HttpMethod method() {
        Method result = Method.valueOf(req.getRequestLine().getMethod());
        return result == null ? GET : result;
    }

    @Override
    public String header(CharSequence nm) {
        Header h = req.getFirstHeader(nm.toString());
        return h == null ? null : h.getValue();
    }

    @Override
    public String urlParameter(String param) {
        Parameters p = url.getParameters();
        if (p == null) {
            return null;
        }
        ParsedParameters pp = p.toParsedParameters();
        String result = pp.getFirst(param);
        if (result == null) {
            // Mastfrog URL encodes _
            String encoded = param.replaceAll("_", "%5f");
            result = pp.getFirst(encoded);
        }
        return result;
    }

    @Override
    public Path path() {
        return Path.parse(req.getRequestLine().getUri().split("\\?")[0]);
    }

    @Override
    public <T> T header(HeaderValueType<T> value) {
        String h = header(value.name());
        return h == null ? null : value.toValue(h);
    }

    @Override
    public <T> List<T> headers(HeaderValueType<T> headerType) {
        List<T> result = new ArrayList<>();
        for (Header h : req.getHeaders(headerType.name().toString())) {
            result.add(headerType.toValue(h.getValue()));
        }
        return result;
    }

    @Override
    public Map<CharSequence, CharSequence> headersAsMap() {
        Map<CharSequence, CharSequence> m = new LinkedHashMap<>();
        for (Header h : req.getAllHeaders()) {
            m.put(h.getName(), h.getValue());
        }
        return m;
    }

    @Override
    public Map<String, String> urlParametersAsMap() {
        String pre = req.getRequestLine().getUri().startsWith("http") ? ""
                : req.getRequestLine().getUri().startsWith("/") ? "http://foo.com" : "http://foo.com/";
        URL url = URL.parse(pre + req.getRequestLine().getUri());
        Map<String, String> result = new HashMap<>();
        Parameters p = url.getParameters();
        if (p != null) {
            p.toParsedParameters().forEach((ParametersElement pe) -> {
                try {
                    String key = pe.getKey().replaceAll("%5f", "_");
                    String val = URLDecoder.decode(pe.getValue(), "UTF-8");
                    result.put(key, val);
                } catch (UnsupportedEncodingException ex) {
                    Exceptions.chuck(ex);
                }
            });
        }
        return result;
    }

    @Override
    public <T> T urlParametersAs(Class<T> type) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public com.google.common.base.Optional<Integer> intUrlParameter(String name) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public com.google.common.base.Optional<Long> longUrlParameter(String name) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String stringContent() throws IOException {
        return null;
    }

    @Override
    public boolean requestsConnectionStayOpen() {
        return false;
    }

    @Override
    public boolean isSsl() {
        return "https".equals(req.getFirstHeader("X-Forwarded-Proto"));
    }

    @Override
    public Channel channel() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public io.netty.handler.codec.http.HttpRequest request() {
        // just because we don't have enough layers of emulation in here...
        DefaultHttpRequest result = new DefaultHttpRequest(HttpVersion.valueOf(req.getRequestLine().getProtocolVersion().toString()),
                io.netty.handler.codec.http.HttpMethod.valueOf(req.getRequestLine().getMethod()),
                req.getRequestLine().getUri());
        for (Header h : req.getAllHeaders()) {
            result.headers().add(h.getName(), h.getValue());
        }
        return result;
    }

    @Override
    public SocketAddress remoteAddress() {
        return new InetSocketAddress("localhost", 8080);
    }

    @Override
    public <T> T jsonContent(Class<T> type) throws IOException {
        return null;
    }

    @Override
    public ChannelHandlerContext ctx() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public ByteBuf content() throws IOException {
        return Unpooled.EMPTY_BUFFER;
    }
}
