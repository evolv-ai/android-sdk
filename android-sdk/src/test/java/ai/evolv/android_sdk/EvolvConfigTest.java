package ai.evolv.android_sdk;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ai.evolv.android_sdk.httpclients.HttpClient;

public class EvolvConfigTest {

    private static final String ENVIRONMENT_ID = "test_environment_id";

    @Mock
    private HttpClient mockHttpClient;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        if (mockHttpClient != null) {
            mockHttpClient = null;
        }
    }

    @Test
    public void testBuildDefaultConfig() {

        EvolvConfig evolvConfig = EvolvConfig.builder(ENVIRONMENT_ID,mockHttpClient).build();

        Assert.assertEquals(EvolvConfig.DEFAULT_HTTP_SCHEME,evolvConfig.getHttpScheme());
        Assert.assertEquals(EvolvConfig.DEFAULT_ENDPOINT + evolvConfig.getVersion(),evolvConfig.getEndpoint());
        Assert.assertEquals(EvolvConfig.DEFAULT_DOMAIN,evolvConfig.getDomain());
        Assert.assertEquals(ENVIRONMENT_ID,evolvConfig.getEnvironmentId());
        Assert.assertEquals(mockHttpClient,evolvConfig.getHttpClient());
        Assert.assertEquals(EvolvConfig.DEFAULT_VERSION,evolvConfig.getVersion());
        Assert.assertTrue(evolvConfig.isAnalytics());
        Assert.assertFalse(evolvConfig.isAutoConfirm());
        Assert.assertFalse(evolvConfig.isBufferEvents());
        Assert.assertNotNull(evolvConfig.getExecutionQueue());

    }

    @Test
    public void testBuildConfig() {
        String domain = "participants.evolv.ai.test";
        int version = 2;
        String httpScheme = "http";
        boolean isAnalytics = true;
        boolean isAutoConfirm = false;
        boolean isBufferEvents = false;

        EvolvConfig config = EvolvConfig.builder(ENVIRONMENT_ID, mockHttpClient)
                .setDomain(domain)
                .setVersion(version)
                .setHttpScheme(httpScheme)
                .build();

        Assert.assertEquals(ENVIRONMENT_ID, config.getEnvironmentId());
        Assert.assertEquals(domain, config.getDomain());
        Assert.assertEquals(version, config.getVersion());
        Assert.assertEquals(httpScheme, config.getHttpScheme());
        Assert.assertEquals(isAnalytics, config.isAnalytics());
        Assert.assertEquals(isAutoConfirm, config.isAutoConfirm());
        Assert.assertEquals(isBufferEvents, config.isBufferEvents());
    }
}
