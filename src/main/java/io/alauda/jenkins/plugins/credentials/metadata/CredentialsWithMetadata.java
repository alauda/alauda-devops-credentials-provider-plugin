package io.alauda.jenkins.plugins.credentials.metadata;

import com.cloudbees.plugins.credentials.common.IdCredentials;

import java.util.HashMap;
import java.util.Map;

public class CredentialsWithMetadata<C extends IdCredentials> {

    private C credentials;
    private Map<String, String> metadata = new HashMap<>();

    public CredentialsWithMetadata(C credentials) {
        this.credentials = credentials;
    }

    public C getCredentials() {
        return credentials;
    }

    public void setCredentials(C credentials) {
        this.credentials = credentials;
    }

    public void addMetadata(String key, String value) {
        metadata.put(key, value);
    }

    public String getMetadata(String key) {
        return metadata.get(key);
    }
}
