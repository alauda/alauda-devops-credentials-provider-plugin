package io.alauda.jenkins.plugins.credentials.convertor;

import com.cloudbees.plugins.credentials.common.IdCredentials;
import hudson.Extension;
import io.alauda.jenkins.plugins.credentials.CredentialsUtils;
import io.alauda.jenkins.plugins.credentials.SecretUtils;
import io.kubernetes.client.models.V1Secret;

import java.util.Map;
import java.util.Optional;

@Extension
public class OpaqueCredentialsConverter extends SecretToCredentialConverter {
    private static final String OPAQUE_TYPE = "Opaque";

    @Override
    public boolean canConvert(String type) {
        return OPAQUE_TYPE.equals(type);
    }

    @Override
    public IdCredentials convert(V1Secret secret) throws CredentialsConversionException {
        Map<String, byte[]> data = secret.getData();
        if(data == null) {
            return null;
        }

        Optional<String> tokenOpt = SecretUtils.getOptionalSecretData(secret, "token", "");
        if(tokenOpt.isPresent()) {
            String token = tokenOpt.get();
            return CredentialsUtils.convertToStringCredentials(secret.getMetadata(), token);
        }

        return null;
    }
}
