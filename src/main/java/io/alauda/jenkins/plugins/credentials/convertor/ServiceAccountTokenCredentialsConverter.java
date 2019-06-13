package io.alauda.jenkins.plugins.credentials.convertor;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import hudson.util.Secret;
import io.alauda.jenkins.plugins.credentials.SecretUtils;
import io.kubernetes.client.models.V1Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

public class ServiceAccountTokenCredentialsConverter extends SecretToCredentialConverter {
    private static final String SERVICE_ACCOUNT_TOKEN_TYPE = "kubernetes.io/service-account-token";


    @Override
    public boolean canConvert(String type) {
        return SERVICE_ACCOUNT_TOKEN_TYPE.equals(type);
    }

    @Override
    public IdCredentials convert(V1Secret secret) throws CredentialsConversionException {
        SecretUtils.requireNonNull(secret.getData(), "kubernetes.io/service-account-token definition contains no data");
        String token = SecretUtils.getNonNullSecretData(secret, "token", "kubernetes.io/service-account-token credential is missing the token");

        return new StringCredentialsImpl(CredentialsScope.GLOBAL, SecretUtils.getCredentialId(secret), SecretUtils.getCredentialDescription(secret), Secret.fromString(token));
    }
}
