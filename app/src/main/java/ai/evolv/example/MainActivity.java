package ai.evolv.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import ai.evolv.android_sdk.EvolvClientImpl;
import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.EvolvClientFactory;
import ai.evolv.android_sdk.EvolvConfig;
import ai.evolv.android_sdk.EvolvParticipant;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.httpclients.HttpClient;
import ai.evolv.android_sdk.httpclients.OkHttpClient;


public class MainActivity extends AppCompatActivity {

    private EvolvClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HttpClient httpClient = new OkHttpClient(TimeUnit.MILLISECONDS, 7000);

        // build config with custom timeout and custom allocation store
        // set client to use sandbox environment
        EvolvConfig config = EvolvConfig.builder("8b50696b6c", httpClient) //e30a43d71c
                .build();

        // initialize the client with a stored user
        client = EvolvClientFactory.init(
                config,
                new EvolvParticipant("79211876_16178796481581112223331"));
                //UserId"79211876_16178796481581112223331" - //"Default layout";"click here"
                //UserId("79211876_16178796481581112223332") - //"Layout 1";"click here"
                //UserId("79211876_16178796481581112223337") - //"Default layout"; "click here now!"
                //UserId("79211876_16178796481581112223339") - //"Default layout"; "best button"

        setContentView(R.layout.layout_default);
        client.subscribe("next.layout", "Default Layout", layoutOption -> {
            runOnUiThread(() -> {
                switch (layoutOption) {
                    case "Layout 1":
                        setContentView(R.layout.layout_one);
                        break;
                    case "Default Layout":
                        setContentView(R.layout.layout_default);
                        break;
                    default:
                        setContentView(R.layout.layout_default);
                        break;
                }
            });
        });

        client.subscribe("home.cta_text", "Default Message", new EvolvAction<String>() {
            @Override
            public void apply(String buttonText) {
                MainActivity.this.runOnUiThread(() -> {
                    TextView showCountTextView = MainActivity.this.findViewById(R.id.homeButton);
                    showCountTextView.setText(buttonText);
                });
            }
        });

        EvolvContext evolvContext = ((EvolvClientImpl)client).getEvolvContext();
        //case 1
        //evolvContext.set("signedin","yes",false);
        //case 2
//        evolvContext.set("authenticated","false",false);
//        evolvContext.set("text","cancel",false);
//        evolvContext.set("device","mobile",false);
        //case 3
        evolvContext.set("Age","26",false);
        evolvContext.set("Sex","female",false);
        evolvContext.set("view","home",false);
        evolvContext.set("view","next",false);

        // TODO: 02.06.2021 allow adding third or more orders of keys to the remote context
        //evolvContext.set("key.test.test1","test_value",false);

    }

    public void pressHome(View view) {

        JsonObject activeKeys = client.getActiveKeys();
        JsonObject activeKeysPrefix = client.getActiveKeys("home");
        Log.d("activeKeys_", "activeKeys: " + activeKeys);
        Log.d("activeKeys_", "activeKeysPrefix: " + activeKeysPrefix);
        Log.d("activeKeys_", "isActive: " + client.isActive("cta_text"));
        Log.d("activeKeys_", "GET: " + client.get("home"));

    }
}
