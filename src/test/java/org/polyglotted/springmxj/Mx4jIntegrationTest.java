package org.polyglotted.springmxj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-test/jmx-test-context.xml" })
public class Mx4jIntegrationTest {

    private static final String USERNAME = "jmxuser";
    private static final String PASSWORD = "jmxpasswd";
    private static final String JMX_PORT = "13124";

    @BeforeClass
    public static void initSetup() {
        System.setProperty("jmx.http.port", JMX_PORT);
    }

    @Test
    public void doTest() throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        try {
            Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    credentials);

            new BasicTestInvoker().doTest(httpClient);
            for (ClientTestInvoker invoker : getTests()) {
                invoker.doTest(httpClient);
            }
        }
        finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    protected List<ClientTestInvoker> getTests() {
        return Collections.emptyList();
    }

    protected static String getHostPort() {
        return getHostname() + ":" + JMX_PORT;
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static class BasicTestInvoker implements ClientTestInvoker {
        @Override
        public void doTest(HttpClient httpClient) throws Exception {
            HttpGet getMethod = new HttpGet("http://" + getHostPort() + "/");
            HttpResponse response = httpClient.execute(getMethod);
            assertEquals(200, response.getStatusLine().getStatusCode());

            String html = EntityUtils.toString(response.getEntity());
            assertTrue(html.contains("SpringMx4jHttpClient"));
        }
    }

    public interface ClientTestInvoker {
        void doTest(HttpClient client) throws Exception;
    }
}
