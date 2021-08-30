package ai.evolv.android_sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EvolvParticipant {

    private String userId;

    public EvolvParticipant(String userId) {
        this.userId = userId;
    }

    String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
