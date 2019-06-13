package io.alauda.jenkins.plugins.credentials.scope;

import hudson.Extension;
import hudson.model.ItemGroup;
import io.alauda.jenkins.plugins.credentials.KubernetesCredentialsProviderConfiguration;
import io.alauda.jenkins.plugins.credentials.metadata.CredentialsWithMetadata;
import io.alauda.jenkins.plugins.credentials.metadata.NamespaceProvider;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

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

        String globalNamespaces = KubernetesCredentialsProviderConfiguration.get().getGlobalNamespaces();
        if (globalNamespaces == null) {
            return false;
        }

        return Arrays.asList(globalNamespaces.split(",")).contains(namespace);
    }
}
