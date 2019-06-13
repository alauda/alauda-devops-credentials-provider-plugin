package io.alauda.jenkins.plugins.credentials.metadata;

import hudson.Extension;
import io.kubernetes.client.models.V1Secret;

@Extension
public class NamespaceProvider implements MetadataProvider {

    public static final String NAMESPACE_METADATA = "namespace";

    @Override
    public void attach(V1Secret secret, CredentialsWithMetadata credentialsWithMetadata) {
        credentialsWithMetadata.addMetadata(NAMESPACE_METADATA, secret.getMetadata().getNamespace());
    }
}
