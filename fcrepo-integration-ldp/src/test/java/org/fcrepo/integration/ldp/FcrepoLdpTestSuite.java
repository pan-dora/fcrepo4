package org.fcrepo.integration.ldp;

import static java.lang.Integer.parseInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.ldp.testsuite.LdpTestSuite;

abstract class FcrepoLdpTestSuite {

    static final int TESTNG_STATUS_HAS_FAILURE = 1;
    static final int TESTNG_STATUS_HAS_SKIPPED = 2;
    static final int TESTNG_STATUS_HAS_NO_TEST = 8;
    static Logger log = LoggerFactory.getLogger(FcrepoLdpTestSuite.class);
    static final int SERVER_PORT = parseInt(System.getProperty(
            "fcrepo.dynamic.test.port", "8080"));
    static final String HOSTNAME = "localhost";
    static final String PROTOCOL = "http";
    static final String CONTEXT = "/fcrepo/rest/";

    static String baseUrl;
    LdpTestSuite testSuite;

}
