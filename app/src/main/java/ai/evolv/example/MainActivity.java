package ai.evolv.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.concurrent.TimeUnit;

import ai.evolv.android_sdk.evolvinterface.EvolvClient;
import ai.evolv.android_sdk.EvolvClientFactory;
import ai.evolv.android_sdk.EvolvConfig;
import ai.evolv.android_sdk.EvolvParticipant;
import ai.evolv.android_sdk.httpclients.HttpClient;
import ai.evolv.android_sdk.httpclients.OkHttpClient;


public class MainActivity extends AppCompatActivity {

    private EvolvClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HttpClient httpClient = new OkHttpClient(TimeUnit.MILLISECONDS, 3000);

        // build config with custom timeout and custom allocation store
        // set client to use sandbox environment
        EvolvConfig config = EvolvConfig.builder("8b50696b6c", httpClient)
                .build();

        // initialize the client with a stored user
        client = EvolvClientFactory.init(config,  EvolvParticipant.builder()
                //.setUserId("79211876_16178796481581112223331").build()); //"Default layout";"click here"
                .setUserId("79211876_16178796481581112223332").build()); //"Layout 1";"click here"
                //.setUserId("79211876_16178796481581112223337").build()); //"Default layout"; "click here now!"
                //.setUserId("79211876_16178796481581112223339").build()); //"Default layout"; "best button"

        // initialize the client with a new user
        //client = EvolvClientFactory.init(config);

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

        client.subscribe("home.cta_text", "Default Message", buttonText -> {
            runOnUiThread(() -> {
                TextView showCountTextView = findViewById(R.id.homeButton);
                showCountTextView.setText(buttonText);
            });
        });

        //Get the value of a specified key.
        Log.d("get_log", client.get("home.cta_text", "default key"));

        //Check all active keys that start with the specified prefix.
        Log.d("getActiveKeys_log",client.getActiveKeys("", new JsonArray()).toString());

        client.confirm();
    }

    public void pressHome(View view) {
        client.emitEvent("conversion");
        Toast convMessage = Toast.makeText(this, "Сlicked",
                Toast.LENGTH_SHORT);
        convMessage.show();
    }

}
