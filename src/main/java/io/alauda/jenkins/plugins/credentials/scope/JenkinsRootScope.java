package io.alauda.jenkins.plugins.credentials.scope;

import hudson.Extension;
import hudson.model.ItemGroup;
import io.alauda.jenkins.plugins.credentials.KubernetesCredentialsProviderConfiguration;
import io.alauda.jenkins.plugins.credentials.metadata.CredentialsWithMetadata;
import io.alauda.jenkins.plugins.credentials.metadata.NamespaceProvider;
import jenkins.model.Jenkins;

@Extension
public class JenkinsRootScope implements KubernetesSecretScope {
    @Override
    public boolean isScope(ItemGroup owner) {
        return owner == Jenkins.getInstance();
    }

    @Override
    public boolean isBelong(ItemGroup owner, CredentialsWithMetadata credentialsWithMetadata) {
        if (!isScope(owner)) {
            return false;
        }

        return credentialsWithMetadata.getMetadata(NamespaceProvider.NAMESPACE_METADATA).equals(
                KubernetesCredentialsProviderConfiguration.get().getGlobalNamespace());

    }
}
