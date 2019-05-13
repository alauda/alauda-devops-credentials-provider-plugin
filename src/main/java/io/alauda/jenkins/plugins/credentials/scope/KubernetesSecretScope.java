package io.alauda.jenkins.plugins.credentials.scope;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.ItemGroup;
import io.alauda.jenkins.plugins.credentials.metadata.CredentialsWithMetadata;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Scope defines where we can see credentials and what credentials we should display in these places.
 */
public interface KubernetesSecretScope extends ExtensionPoint {

    /**
     * Check if this scope should include ItemGroup.
     * @param owner ItemGroup will be checked.
     * @return true if the scope includes this ItemGroup.
     */
    boolean isInScope(ItemGroup owner);

    /**
     * Check if the credentials should show under the ItemGroup
     * @return true if the credentials should show in the ItemGroup
     */
    boolean shouldShowInScope(ItemGroup owner, CredentialsWithMetadata credentialsWithMetadata);

    static ExtensionList<KubernetesSecretScope> all() {
        return ExtensionList.lookup(KubernetesSecretScope.class);
    }

    static boolean hasMatchedScope(ItemGroup owner) {
        ExtensionList<KubernetesSecretScope> scopes = all();

        return scopes.stream().anyMatch(s -> s.isInScope(owner));
    }

    static List<KubernetesSecretScope> matchedScopes(ItemGroup owner) {
        ExtensionList<KubernetesSecretScope> scopes = all();

        return scopes.stream().filter(s -> s.isInScope(owner)).collect(Collectors.toList());
    }

}
