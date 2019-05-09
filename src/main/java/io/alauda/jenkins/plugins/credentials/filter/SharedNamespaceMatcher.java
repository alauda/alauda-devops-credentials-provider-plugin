package io.alauda.jenkins.plugins.credentials.filter;

import com.google.common.collect.Lists;
import hudson.Extension;
import io.alauda.jenkins.plugins.credentials.KubernetesCredentialsProviderConfiguration;

import java.util.List;

@Extension
public class SharedNamespaceMatcher extends KubernetesSecretNamespaceMatcher {
    @Override
    public List<String> getNamespaces() {
        return Lists.newArrayList(KubernetesCredentialsProviderConfiguration.get().getSharedNamespace());
    }
}
