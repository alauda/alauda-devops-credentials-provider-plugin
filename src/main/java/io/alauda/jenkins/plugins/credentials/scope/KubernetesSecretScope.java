package io.alauda.jenkins.plugins.credentials.scope;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.ItemGroup;
import io.alauda.jenkins.plugins.credentials.metadata.CredentialsWithMetadata;

import java.util.List;
import java.util.stream.Collectors;

public interface KubernetesSecretScope extends ExtensionPoint {

    boolean isScope(ItemGroup owner);

    boolean isBelong(ItemGroup owner, CredentialsWithMetadata credentialsWithMetadata);

    static ExtensionList<KubernetesSecretScope> all() {
        return ExtensionList.lookup(KubernetesSecretScope.class);
    }

    static boolean shouldBeScope(ItemGroup owner) {
        ExtensionList<KubernetesSecretScope> scopes = all();

        return scopes.stream().anyMatch(s -> s.isScope(owner));
    }

    static List<KubernetesSecretScope> matchScopes(ItemGroup owner) {
        ExtensionList<KubernetesSecretScope> scopes = all();

        return scopes.stream().filter(s -> s.isScope(owner)).collect(Collectors.toList());
    }

}
