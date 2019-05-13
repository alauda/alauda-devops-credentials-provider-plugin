package io.alauda.jenkins.plugins.credentials.metadata;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.alauda.kubernetes.api.model.Secret;

public interface MetadataProvider extends ExtensionPoint {

    void attach(Secret secret, CredentialsWithMetadata credentialsWithMetadata);

    static ExtensionList<MetadataProvider> all() {
        return ExtensionList.lookup(MetadataProvider.class);
    }

}
