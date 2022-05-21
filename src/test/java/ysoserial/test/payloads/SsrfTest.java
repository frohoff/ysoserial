package ysoserial.test.payloads;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.Assert;
import ysoserial.payloads.Scala;
import ysoserial.test.CustomTest;
import ysoserial.test.util.Randomized;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class SsrfTest implements CustomTest {
    int port = Randomized.randPort();
    String authority = "http://localhost:" + port;
    String uri = "/?" + Randomized.randUUID();

    @Override
    public String getPayloadArgs() {
        return authority + uri;
    }

    @Override
    public void run(Callable<Object> payload) throws Exception {
        final List<String> uris = new LinkedList<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println(exchange.getRequestURI());
                uris.add(exchange.getRequestURI().toString());
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            }
        });
        server.start();
        try {
            try {
                payload.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Assert.assertTrue(uris.contains(uri));
        } finally {
            server.stop(0);
        }
    }

    public static void main(String[] args) throws Exception {
        PayloadsTest.testPayload(Scala.ScalaSsrf.class, new Class[0]);
    }
}
