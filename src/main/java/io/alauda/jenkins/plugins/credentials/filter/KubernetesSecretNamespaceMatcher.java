package io.alauda.jenkins.plugins.credentials.filter;

import io.alauda.kubernetes.api.model.Secret;

import java.util.List;

public abstract class KubernetesSecretNamespaceMatcher implements KubernetesSecretMatcher {
    @Override
    public boolean match(Secret secret) {
        return getNamespaces().contains(secret.getMetadata().getNamespace());
    }

    public abstract List<String> getNamespaces();
}
