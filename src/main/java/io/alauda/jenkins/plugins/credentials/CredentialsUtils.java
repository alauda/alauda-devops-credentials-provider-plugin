package io.alauda.jenkins.plugins.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import hudson.util.Secret;
import io.kubernetes.client.models.V1ObjectMeta;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

public abstract class CredentialsUtils {
    public static IdCredentials convert(V1ObjectMeta meta, String token) {
        return new StringCredentialsImpl(CredentialsScope.GLOBAL, SecretUtils.getCredentialId(meta),
                SecretUtils.getCredentialDescription(meta), Secret.fromString(token));
    }
}
