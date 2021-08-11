package ai.evolv.android_sdk.httpclients;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import ai.evolv.android_sdk.httpclients.HttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class OkHttpClient implements HttpClient {

    private final okhttp3.OkHttpClient httpClient;

    /**
     * Initializes the OhHttp# httpClient.
     * <p>
     * Note: Default timeout is 1 second
     * </p>
     */
    public OkHttpClient() {
        this.httpClient = new okhttp3.OkHttpClient.Builder()
                .callTimeout(1, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(3, 1000, TimeUnit.MILLISECONDS))
                .build();
    }

    /**
     * Initializes the OhHttp# httpClient.
     *
     * @param timeUnit Specify the unit of the timeout value.
     * @param timeout  Specify a request timeout for the httpClient.
     */
    public OkHttpClient(TimeUnit timeUnit, long timeout) {
        this.httpClient = new okhttp3.OkHttpClient.Builder()
                .callTimeout(timeout, timeUnit)
                .connectionPool(new ConnectionPool(3, 1000, TimeUnit.MILLISECONDS))
                .build();
    }

    /**
     * Initializes the OhHttp# httpClient.
     *
     * @param httpClient An instance of okhttp3.OkHttpClient for Evolv to use.
     */
    public OkHttpClient(okhttp3.OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Performs a GET request with the given url using the httpClient from
     * okhttp3.
     *
     * @param url a valid url representing a call to the Participant API.
     * @return a Listenable future instance containing a response from
     * the API
     */
    public ListenableFuture<String> get(String url) {
        return getStringSettableFuture(url, httpClient);
    }

    private static SettableFuture<String> getStringSettableFuture(
            String url, okhttp3.OkHttpClient httpClient) {
        SettableFuture<String> responseFuture = SettableFuture.create();
        final Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                responseFuture.setException(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                String body = "";
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        body = responseBody.string();
                    }

                    if (!response.isSuccessful()) {
                        throw new IOException(String.format("Unexpected response "
                                        + "when making GET request: %s using url: %s with body: %s",
                                response, request.url(), body));
                    }

                    responseFuture.set(body);
                } catch (Exception e) {
                    responseFuture.setException(e);
                }
            }
        });

        return responseFuture;
    }

    public ListenableFuture<String> post(String url, RequestBody requestBody) {
        return postStringSettableFuture(url, httpClient, requestBody);
    }

    private static SettableFuture<String> postStringSettableFuture(
            String url, okhttp3.OkHttpClient httpClient, RequestBody requestBody) {
        SettableFuture<String> responseFuture = SettableFuture.create();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                responseFuture.setException(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                String body = "";
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        body = responseBody.string();
                    }

                    if (!response.isSuccessful()) {
                        throw new IOException(String.format("Unexpected response "
                                        + "when making POST request: %s using url: %s with body: %s",
                                response, request.url(), body));
                    }

                    responseFuture.set(body);
                } catch (Exception e) {
                    responseFuture.setException(e);
                }
            }
        });

        return responseFuture;
    }

}
