package ai.evolv.example;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
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

import static ai.evolv.android_sdk.EvolvClientImpl.INITIALIZED;


public class MainActivity extends AppCompatActivity {

    private EvolvClient client;
    private EvolvContext evolvContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HttpClient httpClient = new OkHttpClient(TimeUnit.MILLISECONDS, 7000);

        // build config with custom timeout and custom allocation store
        // set client to use sandbox environment
        EvolvConfig config = EvolvConfig.builder("dbcf75051d", httpClient) //8b50696b6c
                .build();

        // initialize the client with a stored user
        client = EvolvClientFactory.init(
                config,
                new EvolvParticipant("79211876_16178796481581112"));
        //UserId"79211876_16178796481581112223331" - //"Default layout";"click here"
        //UserId("79211876_16178796481581112") - //"Layout 1";"click here"
        //UserId("79211876_16178796481581112223337") - //"Default layout"; "click here now!"
        //UserId("79211876_16178796481581112223339") - //"Default layout"; "best button"

        setContentView(R.layout.layout_default);

        evolvContext = ((EvolvClientImpl) client).getEvolvContext();
        //case 1
        //evolvContext.set("signedin", "yes", false);
        //case 2
        evolvContext.set("authenticated","false",false);
        evolvContext.set("device","mobile",false);
        //case 3
        evolvContext.set("Age", "26", false);
        evolvContext.set("Sex", "female", false);
        evolvContext.set("view", "home", false);

        client.subscribeGet("next.layout", "Default Layout", (EvolvAction<JsonElement>) value ->
                runOnUiThread(() -> {
                    switch (value.getAsString()) {
                        case "Layout 1":
                            setContentView(R.layout.layout_one);
                            ConstraintLayout constraintLayout_one = findViewById(R.id.constraintlayout_one);
                            constraintLayout_one.setBackgroundColor(Color.GREEN);
                            Log.d("evolv_subscribeGet_next", "element: " + value);

                            break;
                        case "Default Layout":
                            setContentView(R.layout.layout_default);
                            ConstraintLayout constraintLayout_default = findViewById(R.id.constraintlayout_default);
                            constraintLayout_default.setBackgroundColor(Color.MAGENTA);
                            Log.d("evolv_subscribeGet_next", "element: " + value);

                            break;
                        default:
                            setContentView(R.layout.layout_default);
                            Log.d("evolv_subscribeGet_next", "element: " + value);

                            break;
                    }
                }));

        client.subscribeGet("home.cta_text", "default text", (EvolvAction<JsonElement>) value ->
                runOnUiThread(() -> {
                    Log.d("evolv_subscribeGet_home", "element: " + value);
                    TextView textView = findViewById(R.id.homeButton);
                    textView.setText(value.getAsString());
                }));


        client.subscribeActiveKeys("", new EvolvAction<JsonObject>() {
            @Override
            public void apply(JsonObject value) {
                Log.d("evolv_subscribe_active_", "subscribeActiveKeys : " + value);
            }
        });

//        client.on(INITIALIZED,EvolvAction){
//            @Override
//            public void apply(JsonObject value) {
//                Log.d("evolv_subscribe_active_", "subscribeActiveKeys : " + value);
//            }
//        }

    }

    public void pressHome(View view) {
// test area -->

        JsonObject details = new JsonObject();
        details.addProperty("reason","error-thrown");
        details.addProperty("details","testing contamination");
        client.contaminate(details,false);


// TODO: 28.07.2021 commented data   -->
        //////////////////remove (context)//////////////////
        //evolvContext.remove("Age");
        //evolvContext.remove("Sex");

        //////////////////set (context)//////////////////
        //evolvContext.set("Age", "26", false);
        //evolvContext.set("view", "home", false);
        //evolvContext.set("view", "next", false);
        //evolvContext.set("signedin","yes",false);

        //////////////////confirm (client)//////////////////
        //client.confirm();

        //////////////////contaminate (client)//////////////////
        //JsonObject details = new JsonObject();
        //details.addProperty("reason","error-thrown");
        //details.addProperty("details","testing contamination");
        //client.contaminate(details,false);

        //////////////////getActiveKeys (client)//////////////////
        //JsonObject activeKeys = client.getActiveKeys();
        //Log.d("evolv_activeKeys", "activeKeys: " + activeKeys.toString());

        //////////////////get (client)//////////////////
        //JsonElement getValue = client.get("home");
        //Log.d("evolv_getvalue", "value: " + getValue.toString());

        //////////////////get (context)//////////////////
        //JsonElement element = evolvContext.get("experiments.allocations");
        //Log.d("evolv_context_get", "GET: " + element);

        //////////////////contains (context)//////////////////
        //boolean containValue = evolvContext.contains("Age");
        //Log.d("evolv_containValue", "GET: " + containValue);

        //////////////////isActive (client)//////////////////
        //boolean isActive = client.isActive("home");

        //////////////////subscribe (client)//////////////////
        //client.subscribeIsActive("next", new EvolvAction<Boolean>() {
        //    @Override
        //    public void apply(Boolean value) {
        //        Log.d("evolv_subscribe_Active", "subscribeActiveKeys : " + value);
        //    }
        //});

        //client.subscribeActiveKeys("", new EvolvAction<JsonObject>() {
        //    @Override
        //    public void apply(JsonObject value) {
        //        Log.d("evolv_subscribe_home", "subscribeActiveKeys : " + value);
        //    }
        //});

        //client.subscribeActiveKeys("button_color", new EvolvAction<JsonObject>() {
        //    @Override
        //    public void apply(JsonObject value) {
        //        Log.d("evolv_subscribe_btn", "subscribeActiveKeys (prefix): " + value);
        //    }
        //});

// test area <--
    }
}
