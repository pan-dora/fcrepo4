/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.query.Dataset;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static com.gargoylesoftware.htmlunit.BrowserVersion.FIREFOX_24;
import static java.lang.Integer.MAX_VALUE;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.*;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Base class for ITs
 *
 * @author awoods
 * @author escowles
 **/

public abstract class AbstractResourceIT implements IntegrationTestCategory {

    protected Logger logger;

    WebClient webClient;
    WebClient javascriptlessWebClient;

    @Before
    public void setup() {
        logger = getLogger(this.getClass());
        webClient = getDefaultWebClient();
        javascriptlessWebClient = getDefaultWebClient();
        javascriptlessWebClient.getOptions().setJavaScriptEnabled(false);
    }

    @After
    public void cleanUp() {
        webClient.closeAllWindows();
        javascriptlessWebClient.closeAllWindows();
    }

    public WebClient getDefaultWebClient() {

        final WebClient webClient = new WebClient(FIREFOX_24);
        webClient.addRequestHeader(ACCEPT, "text/html");

        webClient.waitForBackgroundJavaScript(1000);
        webClient.waitForBackgroundJavaScriptStartingBefore(10000);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        //Suppress warning from IncorrectnessListener
        webClient.setIncorrectnessListener(new FedoraHtmlResponsesIT.SuppressWarningIncorrectnessListener());

        //Suppress css warning with the silent error handler.
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;

    }

    public static int SERVER_PORT = Integer.parseInt("8080");

    public static String CONTEXT_PATH = "/fcrepo/rest/";

    protected static final String HOSTNAME = "localhost";

    protected static final String PROTOCOL = "http";

    protected final String serverAddress = PROTOCOL + "://" + HOSTNAME + ":" + SERVER_PORT + CONTEXT_PATH;

    protected static HttpClient client = createClient();

    protected static HttpClient createClient() {
        return HttpClientBuilder.create().setMaxConnPerRoute(MAX_VALUE).setMaxConnTotal(MAX_VALUE).build();
    }

    protected HttpPost postObjMethod(final String pid) {
        return new HttpPost(serverAddress + pid);
    }

    protected HttpPut putObjMethod(final String pid) {
        return new HttpPut(serverAddress + pid);
    }

    protected HttpPost postObjMethod(final String pid, final String query) {
        if (query.equals("")) {
            return new HttpPost(serverAddress + pid);
        }
        return new HttpPost(serverAddress + pid + "?" + query);
    }

    protected HttpPost postDSMethod(final String pid, final String ds, final String content)
            throws UnsupportedEncodingException {
        final HttpPost post = new HttpPost(serverAddress + pid + "/" + ds + "/fcr:content");
        post.setEntity(new StringEntity(content));
        return post;
    }

    protected HttpPut putDSMethod(final String pid, final String ds, final String content)
            throws UnsupportedEncodingException {
        final HttpPut put = new HttpPut(serverAddress + pid + "/" + ds + "/fcr:content");

        put.setEntity(new StringEntity(content));
        return put;
    }

    protected HttpResponse execute(final HttpUriRequest method) throws ClientProtocolException, IOException {
        logger.debug("Executing: " + method.getMethod() + " to " + method.getURI());
        return client.execute(method);
    }

    // Executes requests with preemptive basic authentication
    protected HttpResponse executeWithBasicAuth(final HttpUriRequest request, final String username,
                                                final String password) throws IOException {
        final HttpHost target = new HttpHost(HOSTNAME, SERVER_PORT, PROTOCOL);
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials(username, password));
        try (final CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
                .build()) {

            final AuthCache authCache = new BasicAuthCache();
            final BasicScheme basicAuth = new BasicScheme();
            authCache.put(target, basicAuth);

            final HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            final CloseableHttpResponse response = httpclient.execute(request, localContext);
            return response;
        }
    }

    protected int getStatus(final HttpUriRequest method) throws ClientProtocolException, IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        if (!(result > 199) || !(result < 400)) {
            logger.warn(EntityUtils.toString(response.getEntity()));
        }
        return result;
    }

    protected String getContentType(final HttpUriRequest method) throws ClientProtocolException, IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        assertEquals(OK.getStatusCode(), result);
        return response.getFirstHeader(CONTENT_TYPE).getValue();
    }

    protected Dataset getDataset(final HttpClient client, final HttpUriRequest method) throws IOException {

        if (method.getFirstHeader(ACCEPT) == null) {
            method.addHeader(ACCEPT, "application/n-triples");
        } else {
            logger.debug("Retrieving RDF in mimeType: {}", method.getFirstHeader(ACCEPT));
        }

        final HttpResponse response = client.execute(method);
        assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
        final Dataset result = parseTriples(response.getEntity());
        logger.trace("Retrieved RDF: {}", result);
        return result;

    }

    protected Dataset getDataset(final HttpResponse response) throws IOException {
        assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
        final Dataset result = parseTriples(response.getEntity());
        logger.trace("Retrieved RDF: {}", result);
        return result;
    }

    protected Dataset getDataset(final HttpUriRequest method) throws IOException {
        return getDataset(client, method);
    }

    protected HttpResponse createObject(final String pid) throws IOException {
        final HttpPost httpPost = postObjMethod("/");
        if (pid.length() > 0) {
            httpPost.addHeader("Slug", pid);
        }
        final HttpResponse response = client.execute(httpPost);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return response;
    }

    protected HttpResponse createDatastream(final String pid, final String dsid, final String content)
            throws IOException {
        logger.trace("Attempting to create datastream for object: {} at datastream ID: {}", pid, dsid);
        final HttpResponse response = client.execute(postDSMethod(pid, dsid, content));
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        return response;
    }

    protected HttpResponse setProperty(final String pid, final String propertyUri, final String value)
            throws IOException {
        return setProperty(pid, null, propertyUri, value);
    }

    protected HttpResponse setProperty(final String pid, final String txId, final String propertyUri,
                                       final String value) throws IOException {
        final HttpPatch postProp = new HttpPatch(serverAddress + (txId != null ? txId + "/" : "") + pid);
        postProp.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT { <" + serverAddress + pid + "> <" + propertyUri + "> \"" + value + "\" } WHERE { }";
        postProp.setEntity(new StringEntity(updateString));
        final HttpResponse dcResp = execute(postProp);
        assertEquals(dcResp.getStatusLine().toString(), 204, dcResp.getStatusLine().getStatusCode());
        postProp.releaseConnection();
        return dcResp;
    }

    protected void addMixin(final String pid, final String mixinUrl) throws IOException {
        final HttpPatch updateObjectGraphMethod = new HttpPatch(serverAddress + pid);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();

        e.setContent(new ByteArrayInputStream(
                ("INSERT DATA { <> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + mixinUrl + "> . } ")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    /**
     * Gets a random (but valid) pid for use in testing.  This pid
     * is guaranteed to be unique within runs of this application.
     *
     * @return a random UUID
     */
    protected static String getRandomUniquePid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets a random (but valid) property name for use in testing.
     *
     * @return a random property name
     */
    protected static String getRandomPropertyName() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets a random (but valid) property value for use in testing.
     *
     * @return a random property value
     */
    protected static String getRandomPropertyValue() {
        return UUID.randomUUID().toString();
    }

}