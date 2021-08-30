package ai.evolv.example;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import ai.evolv.android_sdk.EvolvClientFactory;
import ai.evolv.android_sdk.EvolvClientImpl;
import ai.evolv.android_sdk.EvolvConfig;
import ai.evolv.android_sdk.EvolvParticipant;
import ai.evolv.android_sdk.evolvinterface.EvolvAction;
import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.evolvinterface.EvolvContext;
import ai.evolv.android_sdk.httpclients.HttpClient;
import ai.evolv.android_sdk.httpclients.OkHttpClient;

import static ai.evolv.android_sdk.EvolvContextImpl.CONTEXT_CHANGED;


public class MainActivity extends AppCompatActivity {

    private EvolvClient client;
    private EvolvContext evolvContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HttpClient httpClient = new OkHttpClient(TimeUnit.MILLISECONDS, 7000);

        // build config with custom timeout and custom allocation store
        // set client to use sandbox environment
        EvolvConfig config = EvolvConfig.builder("dbcf75051d", httpClient)
                .build();

        // initialize the client with a stored user
        client = EvolvClientFactory.init(
                config,
                new EvolvParticipant("79211876_16178796481581112"));

        setContentView(R.layout.layout_default);

        evolvContext = ((EvolvClientImpl) client).getEvolvContext();
        //case 1
        //evolvContext.set("signedin", "yes", false);
        //case 2
        evolvContext.set("authenticated", "false", false);
        evolvContext.set("device", "mobile", false);
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
                            break;
                        case "Default Layout":
                            setContentView(R.layout.layout_default);
                            ConstraintLayout constraintLayout_default = findViewById(R.id.constraintlayout_default);
                            constraintLayout_default.setBackgroundColor(Color.MAGENTA);
                            break;
                        default:
                            setContentView(R.layout.layout_default);
                            break;
                    }
                }));

        client.subscribeGet("home.cta_text", "default text", (EvolvAction<JsonElement>) value ->
                runOnUiThread(() -> {
                    TextView textView = findViewById(R.id.homeButton);
                    textView.setText(value.getAsString());
                }));

        client.subscribeActiveKeys("", (EvolvAction<JsonObject>) value ->
                Log.d("evolv_subscribe_active_", "subscribeActiveKeys : " + value));

        client.subscribeActiveKeys("next", (EvolvAction<JsonObject>) value ->
                Log.d("evolv_subsc_active_1", "subscribeActiveKeys : " + value));

        client.subscribeGet("next", "default next", (EvolvAction<JsonElement>) value ->
                runOnUiThread(() -> Log.d("evolv_subscribeGet_next", "element: " + value)));

        client.on(CONTEXT_CHANGED, value ->
                Log.d("evolv_on_invoke", "CONTEXT_CHANGED " + value));

    }

    public void pressHome(View view) {
        evolvContext.set("view", "next", false);
    }
}
