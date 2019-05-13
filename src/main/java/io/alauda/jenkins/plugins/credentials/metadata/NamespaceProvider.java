package io.alauda.jenkins.plugins.credentials.metadata;

import hudson.Extension;
import io.alauda.kubernetes.api.model.Secret;

@Extension
public class NamespaceProvider implements MetadataProvider {

    public static final String NAMESPACE_METADATA = "namespace";

    @Override
    public void attach(Secret secret, CredentialsWithMetadata credentialsWithMetadata) {
        credentialsWithMetadata.addMetadata(NAMESPACE_METADATA, secret.getMetadata().getNamespace());
    }
}
