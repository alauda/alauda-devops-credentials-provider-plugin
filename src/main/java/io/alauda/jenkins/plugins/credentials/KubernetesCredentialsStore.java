package io.alauda.jenkins.plugins.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.CredentialsStoreAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.security.ACL;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class KubernetesCredentialsStore extends CredentialsStore {
    private final KubernetesCredentialsProvider provider;
    private final AlaudaKubernetesCredentialsStoreAction action = new AlaudaKubernetesCredentialsStoreAction(this);

    private ItemGroup owner;

    public KubernetesCredentialsStore(KubernetesCredentialsProvider provider, ItemGroup owner) {
        super(KubernetesCredentialsProvider.class);
        this.provider = provider;
        this.owner = owner;
    }


    @Nonnull
    @Override
    public ModelObject getContext() {
        return owner;
    }

    @Override
    public boolean hasPermission(@Nonnull Authentication a, @Nonnull Permission permission) {
        return CredentialsProvider.VIEW.equals(permission) &&
                Jenkins.getInstance().getACL().hasPermission(a, permission);
    }

    @Nonnull
    @Override
    public List<Credentials> getCredentials(@Nonnull Domain domain) {
        if (Domain.global().equals(domain) && Jenkins.getInstance().hasPermission(CredentialsProvider.VIEW))
            return provider.getCredentials(Credentials.class, owner, ACL.SYSTEM);
        return Collections.emptyList();
    }

    @Override
    public boolean addCredentials(@Nonnull Domain domain, @Nonnull Credentials credentials) {
        return false;
    }

    @Override
    public boolean removeCredentials(@Nonnull Domain domain, @Nonnull Credentials credentials) {
        return false;
    }

    @Override
    public boolean updateCredentials(@Nonnull Domain domain, @Nonnull Credentials current, @Nonnull Credentials replacement) {
        return false;
    }

    @Nonnull
    @Override
    public CredentialsStoreAction getStoreAction() {
        return action;
    }

    /**
     * Expose the store.
     */
    @ExportedBean
    public static class AlaudaKubernetesCredentialsStoreAction extends CredentialsStoreAction {

        private final KubernetesCredentialsStore store;

        private AlaudaKubernetesCredentialsStoreAction(KubernetesCredentialsStore store) {
            this.store = store;
        }

        @Override
        @Nonnull
        public CredentialsStore getStore() {
            return store;
        }

        @Override
        public String getDisplayName() {
            return "Alauda DevOps";
        }
    }
}
