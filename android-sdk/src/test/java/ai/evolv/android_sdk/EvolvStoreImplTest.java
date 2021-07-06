package ai.evolv.android_sdk;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

public class EvolvStoreImplTest {
    //private static final String rawConfiguration = "{\"_published\":1625448126.7474048,\"_client\":{\"browser\":\"unspecified\",\"device\":\"desktop\",\"location\":\"UA\",\"platform\":\"unspecified\"},\"_experiments\":[{\"web\":{},\"_predicate\":{\"id\":174,\"combinator\":\"and\",\"rules\":[{\"field\":\"Age\",\"operator\":\"equal\",\"value\":\"26\"},{\"combinator\":\"or\",\"rules\":[{\"field\":\"Sex\",\"operator\":\"equal\",\"value\":\"female\"},{\"field\":\"Student\",\"operator\":\"contains\",\"value\":\"High_school\"}]}]},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"id\":\"47d857cd5e\",\"_paused\":false},{\"web\":{},\"_predicate\":{\"id\":156,\"combinator\":\"and\",\"rules\":[{\"field\":\"signedin\",\"operator\":\"equal\",\"value\":\"yes\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"7789bf55d7\",\"_paused\":true},{\"web\":{},\"_predicate\":{\"id\":165,\"combinator\":\"and\",\"rules\":[{\"field\":\"authenticated\",\"operator\":\"equal\",\"value\":\"false\"},{\"field\":\"text\",\"operator\":\"contains\",\"value\":\"cancel\"}]},\"button_color\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"or\",\"rules\":[{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"mobile\"},{\"field\":\"device\",\"operator\":\"equal\",\"value\":\"desktop\"},{\"field\":\"platform\",\"operator\":\"equal\",\"value\":\"windows\"}]},\"_values\":true,\"_initializers\":true},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"id\":\"00436dee0b\",\"_paused\":true}]}";
    private static final String rawExperiment = "{\"web\":{},\"_predicate\":{},\"home\":{\"_is_entry_point\":true,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"home\"}]},\"cta_text\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"next\":{\"_is_entry_point\":false,\"_predicate\":{\"combinator\":\"and\",\"rules\":[{\"field\":\"view\",\"operator\":\"equal\",\"value\":\"next\"}]},\"layout\":{\"_is_entry_point\":false,\"_predicate\":null,\"_values\":true,\"_initializers\":true},\"_initializers\":true},\"_paused\":false}";

    @Mock
    private EvolvParticipant participant;
    @Mock
    private WaitForIt waitForIt;
    @Mock
    private EvolvConfig evolvConfig;

    JsonObject parseRawExperiment(String raw) {
        JsonParser parser = new JsonParser();
        return parser.parse(raw).getAsJsonObject();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        if (participant != null) {
            participant = null;
        }

        if (waitForIt != null) {
            waitForIt = null;
        }

        if (evolvConfig != null) {
            evolvConfig = null;
        }
    }

    @Test
    public void testGetValueFromRecurse(){

        ArrayList<String> keys = new ArrayList<>(Arrays.asList("home","home.cta_text",
        "next","next.layout"));

        try {
            EvolvStoreImpl evolvStore = new EvolvStoreImpl(evolvConfig,participant,waitForIt);
            JsonElement current = parseRawExperiment(rawExperiment);
            String parentKey = "";

            evolvStore.recurse(current, parentKey);

            evolvStore.endsWithFilter();

            Assert.assertEquals(evolvStore.getExpLoadedList(), keys);
        } catch (Error e) {
            Assert.fail(e.getMessage());
        }
    }
}
