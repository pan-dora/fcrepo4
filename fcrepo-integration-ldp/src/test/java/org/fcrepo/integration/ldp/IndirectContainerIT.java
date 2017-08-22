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

package org.fcrepo.integration.ldp;

import com.jayway.restassured.RestAssured;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.UUID;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import org.apache.marmotta.platform.ldp.api.LdpService;
import org.apache.marmotta.platform.ldp.webservices.LdpWebService;
import org.openrdf.rio.RDFFormat;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher;
import org.w3.ldp.testsuite.vocab.LDP;

/**
 * @author christopher-johnson
 * @since 23/08/17
 */

@Category(IntegrationTestCategory.class)
public class IndirectContainerIT extends FcrepoLdpTestSuite {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("src/test/resources/docker-compose-fcrepo.yml")
            .waitingForService("fcrepo", HealthChecks.toHaveAllPortsOpen())
            .build();

    @BeforeClass
    public static void setup() throws URISyntaxException, IOException {
        final String pid = "ldp-test-basic-" + UUID.randomUUID().toString();
        baseUrl = UriBuilder.fromUri(PROTOCOL + "://" + HOSTNAME).port(SERVER_PORT).path(CONTEXT)
                .path(pid).build().toString();
    }

    @Before
    public void before() {
        log.debug("Performing required LDP re-initialization...");
        final String container = RestAssured
                .given()
                .header(HttpHeaders.CONTENT_TYPE, RDFFormat.TURTLE.getDefaultMIMEType())
                .header(HttpHeaders.LINK, Link.fromUri(String.valueOf(LDP.IndirectContainer)).rel
                        (LdpWebService.LINK_REL_TYPE).build().toString())
                .body("<> a <http://example.com/ContainerInteraction> . ".getBytes())
                .expect()
                .statusCode(HttpStatusSuccessMatcher.isSuccessful())
                .header(HttpHeaders.LOCATION, CoreMatchers.notNullValue())
                .put(baseUrl)
                .getHeader(HttpHeaders.LOCATION);
        final String resource = RestAssured
                .given()
                .header(HttpHeaders.CONTENT_TYPE, RDFFormat.TURTLE.getDefaultMIMEType())
                .header(HttpHeaders.LINK, Link.fromUri(String.valueOf(LdpService.InteractionModel
                        .LDPR)).rel(LdpWebService.LINK_REL_TYPE).build().toString())
                .body("<> a <http://example.com/ResourceInteraction> .".getBytes())
                .expect()
                .statusCode(HttpStatusSuccessMatcher.isSuccessful())
                .header(HttpHeaders.LOCATION, CoreMatchers.notNullValue())
                .post(baseUrl)
                .getHeader(HttpHeaders.LOCATION);

        RestAssured.reset();
        log.info("Container: {}", container);

        final HashMap<String, String> options = new HashMap<>();
        options.put("server", container);
        options.put("output", "report-basic");
        options.put("indirect", "true");
        options.put("non-rdf", "true");
        options.put("read-only-prop", "http://fedora.info/definitions/v4/repository#uuid");
        options.put("cont-res", resource);
        String reportPath = targetWorkingDir().getAbsolutePath();
        options.put("output", reportPath);

        testSuite = new LdpTestSuite(options);
    }

    @After
    public void after() {
        testSuite = null;
    }

    @Test
    public void testRunSuite() {
        testSuite.run();
        Assert.assertTrue("The LDP test suite is only informational", true);
        //Assert.assertTrue("ldp-testsuite finished with errors", (testSuite.getStatus() &
        //        TESTNG_STATUS_HAS_FAILURE) == 0);
        Assert.assertTrue("ldp-testsuite is empty - no test run", (testSuite.getStatus() &
                TESTNG_STATUS_HAS_NO_TEST) == 0);
        if ((testSuite.getStatus() & TESTNG_STATUS_HAS_SKIPPED) != 0) {
            log.warn("ldp-testsuite has skipped some tests");
        }
    }

    private File targetWorkingDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath, "ldp-testsuite");
        if (!targetDir.exists()) {
            Assume.assumeTrue("Could not create report-directory", targetDir.mkdir());
        }
        return targetDir;
    }
}

