package io.alauda.jenkins.plugins.credentials.filter;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.alauda.kubernetes.api.model.Secret;

/**
 * Matcher matches secrets that should be converted to credentials.
 *
 * If there are multiple matchers, secrets match one of them will be converted.
 */
public interface KubernetesSecretMatcher extends ExtensionPoint {

    boolean match(Secret secret);

    static ExtensionList<KubernetesSecretMatcher> all() {
        return ExtensionList.lookup(KubernetesSecretMatcher.class);
    }

    /**
     * Check if a secret can pass match
     * @param secret secret will be check
     * @return return true is a secret matches one of the matcher
     */
    static boolean isMatch(Secret secret) {
        ExtensionList<KubernetesSecretMatcher> matchers = all();

        for (KubernetesSecretMatcher matcher : matchers) {
            if (matcher.match(secret)) {
                return true;
            }
        }

        return false;
    }

}
