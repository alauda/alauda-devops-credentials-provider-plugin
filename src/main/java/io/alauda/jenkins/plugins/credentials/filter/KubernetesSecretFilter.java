package io.alauda.jenkins.plugins.credentials.filter;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.alauda.kubernetes.api.model.Secret;

public interface KubernetesSecretFilter extends ExtensionPoint {

    boolean filter(Secret secret);

    static ExtensionList<KubernetesSecretFilter> all() {
        return ExtensionList.lookup(KubernetesSecretFilter.class);
    }

    static boolean shouldFilter(Secret secret) {
        ExtensionList<KubernetesSecretFilter> filters = all();

        return filters.stream().anyMatch(f -> f.filter(secret));
    }

}
