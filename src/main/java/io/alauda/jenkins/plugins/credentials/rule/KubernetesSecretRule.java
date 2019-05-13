package io.alauda.jenkins.plugins.credentials.rule;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.alauda.kubernetes.api.model.Secret;

/**
 * Defines a series of rules that apply to secret
 */
public interface KubernetesSecretRule extends ExtensionPoint {

    /**
     * Check if the secret should be excluded.
     * @param secret secret will be checked.
     * @return true, if the secret should be excluded.
     */
    boolean exclude(Secret secret);

    static ExtensionList<KubernetesSecretRule> all() {
        return ExtensionList.lookup(KubernetesSecretRule.class);
    }

    static boolean shouldExclude(Secret secret) {
        ExtensionList<KubernetesSecretRule> filters = all();

        return filters.stream().anyMatch(e -> e.exclude(secret));
    }

}
