package io.alauda.jenkins.plugins.credentials.metadata;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.kubernetes.client.models.V1Secret;

/**
 * Provide metadata sources that implementations can get metadata from sources and add them to credentials.
 */
public interface MetadataProvider extends ExtensionPoint {

    /**
     * Get metadata from K8s secret and add them to its corresponding credentials
     * @param secret Secret in K8s
     * @param credentialsWithMetadata credentials that converted from Secret
     */
    void attach(V1Secret secret, CredentialsWithMetadata credentialsWithMetadata);

    static ExtensionList<MetadataProvider> all() {
        return ExtensionList.lookup(MetadataProvider.class);
    }

}
