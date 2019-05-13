package io.alauda.jenkins.plugins.credentials.scope;

import hudson.Extension;
import hudson.model.ItemGroup;
import io.alauda.jenkins.plugins.credentials.KubernetesCredentialsProviderConfiguration;
import io.alauda.jenkins.plugins.credentials.metadata.CredentialsWithMetadata;
import io.alauda.jenkins.plugins.credentials.metadata.NamespaceProvider;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

@Extension
public class JenkinsRootScope implements KubernetesSecretScope {
    @Override
    public boolean isInScope(ItemGroup owner) {
        return owner == Jenkins.getInstance();
    }

    @Override
    public boolean shouldShowInScope(ItemGroup owner, CredentialsWithMetadata credentialsWithMetadata) {
        if (!isInScope(owner)) {
            return false;
        }

        String namespace = credentialsWithMetadata.getMetadata(NamespaceProvider.NAMESPACE_METADATA);
        if (StringUtils.isEmpty(namespace)) {
            return false;
        }

        return namespace.equals(KubernetesCredentialsProviderConfiguration.get().getGlobalNamespace());

    }
}
