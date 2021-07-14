
package ai.evolv.android_sdk.httpclients;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;

import okhttp3.RequestBody;

public interface HttpClient {

    /**
     * Performs a GET request using the provided url.
     * <p>
     *     This call is asynchronous, the request is sent and a completable future
     *     is returned. The future is completed when the result of the request returns.
     *     The timeout of the request is determined in the implementation of the
     *     HttpClient.
     * </p>
     * @param url a valid url representing a call to the Participant API.
     * @return a response future
     */
    ListenableFuture<String> get(String url);

    ListenableFuture<String> post(String url, RequestBody requestBody);

}