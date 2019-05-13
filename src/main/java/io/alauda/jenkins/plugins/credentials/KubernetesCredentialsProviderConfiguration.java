package io.alauda.jenkins.plugins.credentials;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

@Extension
public class KubernetesCredentialsProviderConfiguration extends GlobalConfiguration {

    private String globalNamespace = "global-credentials";
    private String labelSelector;

    public static KubernetesCredentialsProviderConfiguration get() {
        return GlobalConfiguration.all().get(KubernetesCredentialsProviderConfiguration.class);

    }

    public KubernetesCredentialsProviderConfiguration() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public String getGlobalNamespace() {
        return globalNamespace;
    }

    @DataBoundSetter
    public void setGlobalNamespace(String globalNamespace) {
        this.globalNamespace = globalNamespace;
    }

    public String getLabelSelector() {
        return labelSelector;
    }

    @DataBoundSetter
    public void setLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Alauda Credential Provider";
    }
}
