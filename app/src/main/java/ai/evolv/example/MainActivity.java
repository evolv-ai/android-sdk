package ai.evolv.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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
    private EvolvContext evolvContext;

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


        evolvContext = ((EvolvClientImpl)client).getEvolvContext();
        //case 1
            //evolvContext.set("signedin","yes",false);
        //case 2
            //evolvContext.set("authenticated","false",false);
            //evolvContext.set("text","cancel",false);
            //evolvContext.set("device","mobile",false);
        //case 3
        evolvContext.set("Age","26",false);
        evolvContext.set("Sex","female",false);
        evolvContext.set("view","home",false);

        // TODO: 02.06.2021 allow adding third or more orders of keys to the remote context
        //evolvContext.set("key.test.test1","test_value",false);

        client.getActiveKeys(new EvolvAction<JsonObject>() {
            @Override
            public void apply(JsonObject activeKeys) {
                Log.d("evolvCallBack_", "5 MainActivity: " + activeKeys +" " + Thread.currentThread().getName());
            }
        });

        client.getActiveKeys("home",new EvolvAction<JsonObject>() {
            @Override
            public void apply(JsonObject activeKeysPrefix) {
                Log.d("evolvCallBack_", "6 MainActivity: " + activeKeysPrefix +" " + Thread.currentThread().getName());
            }
        });

        client.getActiveKeys("button_color",new EvolvAction<JsonObject>() {
            @Override
            public void apply(JsonObject activeKeysPrefix) {
                Log.d("evolvCallBack_", "7 MainActivity: " + activeKeysPrefix +" " + Thread.currentThread().getName());
            }
        });

        client.get("next.layout", new EvolvAction() {
            @Override
            public void apply(Object value) {
                Log.d("evolvCallBack_", "8 MainActivity: " + value +" " + Thread.currentThread().getName());
            }
        });



    }

    public void pressHome(View view) {
// test area -->


        //evolvContext.set("signedin","yes",false);
        evolvContext.set("view","next",false);


//        // TODO: 19.07.2021 uncomment
//        //JsonObject activeKeys = client.getActiveKeys();
//        JsonObject activeKeys = null;
//        for (Map.Entry<String, JsonElement> s : activeKeys.entrySet()) { Log.d("1_activeKeys_", "Active Keys: " +s.getValue()); }
//
////        JsonObject activeKeysPrefix = client.getActiveKeys("home");
////        for (Map.Entry<String, JsonElement> s : activeKeysPrefix.entrySet()) {  Log.d("activeKeys_", "activeKeysPrefix: " + s.getValue()); }
//
//        client.clearActiveKeys("home");
//
//        for (Map.Entry<String, JsonElement> s : activeKeys.entrySet()) { Log.d("1_activeKeys_", "Active Keys: " +s.getValue()); }
//
//        //client.reevaluateContext();
//
//        //String value = client.get("home.cta_text");
//        JsonElement value1 = client.get("home");
//        Log.d("1_activeKeys_", "GET: " +value1);
//
// test area <--
    }
}
