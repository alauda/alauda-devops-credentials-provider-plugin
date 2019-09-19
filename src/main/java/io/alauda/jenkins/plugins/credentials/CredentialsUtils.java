package io.alauda.jenkins.plugins.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.util.Secret;
import io.kubernetes.client.models.V1ObjectMeta;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

public abstract class CredentialsUtils {

    private CredentialsUtils() {}

    public static StringCredentials convertToStringCredentials(V1ObjectMeta meta, String token) {
        return new StringCredentialsImpl(CredentialsScope.GLOBAL, SecretUtils.getCredentialId(meta),
                SecretUtils.getCredentialDescription(meta), Secret.fromString(token));
    }
}
