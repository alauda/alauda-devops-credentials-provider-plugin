package io.alauda.jenkins.plugins.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.google.gson.reflect.TypeToken;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.security.ACL;
import io.alauda.jenkins.devops.support.controller.Controller;
import io.alauda.jenkins.plugins.credentials.convertor.CredentialsConversionException;
import io.alauda.jenkins.plugins.credentials.convertor.SecretToCredentialConverter;
import io.alauda.jenkins.plugins.credentials.metadata.CredentialsWithMetadata;
import io.alauda.jenkins.plugins.credentials.metadata.MetadataProvider;
import io.alauda.jenkins.plugins.credentials.rule.KubernetesSecretRule;
import io.alauda.jenkins.plugins.credentials.scope.JenkinsRootScope;
import io.alauda.jenkins.plugins.credentials.scope.KubernetesSecretScope;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.models.V1Secret;
import io.kubernetes.client.models.V1SecretList;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class KubernetesCredentialsProvider extends CredentialsProvider implements Controller<V1Secret, V1SecretList> {

    private static final Logger LOG
            = Logger.getLogger(AlaudaKubernetesCredentialsStore.class.getName());
    private SharedIndexInformer<V1Secret> secretInformer;

    // Maps of credentials keyed by credentials ID
    private ConcurrentHashMap<String, CredentialsWithMetadata> credentials = new ConcurrentHashMap<>();

    @Override
    public void initialize(ApiClient apiClient, SharedInformerFactory sharedInformerFactory) {
        String labelSelector = KubernetesCredentialsProviderConfiguration.get().getLabelSelector();

        CoreV1Api coreV1Api = new CoreV1Api();

        secretInformer = sharedInformerFactory.sharedIndexInformerFor(
                callGeneratorParams -> {
                    try {
                        return coreV1Api.listSecretForAllNamespacesCall(
                                null,
                                null,
                                null,
                                labelSelector,
                                null,
                                null,
                                callGeneratorParams.resourceVersion,
                                callGeneratorParams.timeoutSeconds,
                                callGeneratorParams.watch,
                                null,
                                null);
                    } catch (ApiException e) {
                        throw new RuntimeException(e);
                    }
                }, V1Secret.class, V1SecretList.class);

        secretInformer.addEventHandler(new ResourceEventHandler<V1Secret>() {
            @Override
            public void onAdd(V1Secret secret) {
                IdCredentials cred = convertSecret(secret);
                if (cred != null) {
                    LOG.log(Level.FINE, "Secret Added - {0}", cred.getId());
                    CredentialsWithMetadata credWithMetadata = addMetadataToCredentials(secret, cred);
                    credentials.put(cred.getId(), credWithMetadata);
                }
            }

            @Override
            public void onUpdate(V1Secret oldSecret, V1Secret newSecret) {
                IdCredentials cred = convertSecret(newSecret);
                if (cred != null) {
                    LOG.log(Level.FINE, "Secret Modified - {0}", cred.getId());
                    CredentialsWithMetadata credWithMetadata = addMetadataToCredentials(newSecret, cred);
                    credentials.put(cred.getId(), credWithMetadata);
                }
            }

            @Override
            public void onDelete(V1Secret secret, boolean deletedFinalStateUnknown) {
                IdCredentials cred = convertSecret(secret);

                if (cred != null) {
                    if (credentials.containsKey(cred.getId())) {

                        LOG.log(Level.FINE, "Secret Deleted - {0}", cred.getId());
                        credentials.remove(cred.getId());
                    }
                }
            }
        });
    }

    @Override
    public void start() {
    }

    @Override
    public void shutDown(Throwable throwable) {

    }

    @Override
    public boolean hasSynced() {
        return secretInformer.hasSynced();
    }

    @Override
    public Type getType() {
        return new TypeToken<V1Secret>() {
        }.getType();
    }


    @Nonnull
    @Override
    public <C extends Credentials> List<C> getCredentials(@Nonnull Class<C> type, final ItemGroup itemGroup, Authentication authentication) {
        LOG.log(Level.FINEST, "getCredentials called with type {0} and authentication {1}", new Object[]{type.getName(), authentication});
        if (ACL.SYSTEM.equals(authentication)) {
            List<C> credentialsWithinScopes = getCredentialsWithinScope(type, itemGroup, authentication);

            List<KubernetesSecretScope> scopes = KubernetesSecretScope.matchedScopes(itemGroup);
            if (scopes.stream().anyMatch(s -> s.getClass().equals(JenkinsRootScope.class))) {
                return credentialsWithinScopes;
            }


            JenkinsRootScope rootScope = ExtensionList.lookup(JenkinsRootScope.class).get(0);
            credentials.forEach((s, credentialsWithMetadata) -> {
                if (rootScope.shouldShowInScope(Jenkins.getInstance(), credentialsWithMetadata)) {
                    if (type.isAssignableFrom(credentialsWithMetadata.getCredentials().getClass())) {
                        C c = type.cast(credentialsWithMetadata.getCredentials());
                        if (!credentialsWithinScopes.contains(c)) {
                            credentialsWithinScopes.add(c);
                        }
                    }
                }
            });
            return credentialsWithinScopes;
        }
        return Collections.emptyList();
    }

    public <C extends Credentials> List<C> getCredentialsWithinScope(@Nonnull Class<C> type, final ItemGroup itemGroup, Authentication authentication) {
        LOG.log(Level.FINEST, "getCredentials called with type {0} and authentication {1}", new Object[]{type.getName(), authentication});
        if (ACL.SYSTEM.equals(authentication)) {
            List<C> list = new ArrayList<>();
            Set<String> ids = new HashSet<>();

            List<KubernetesSecretScope> scopes = KubernetesSecretScope.matchedScopes(itemGroup);

            credentials.forEach((id, credentialsWithMetadata) -> {
                if (scopes.stream().anyMatch(scope -> scope.shouldShowInScope(itemGroup, credentialsWithMetadata))) {
                    if (type.isAssignableFrom(credentialsWithMetadata.getCredentials().getClass()) && ids.add(id)) {
                        list.add(type.cast(credentialsWithMetadata.getCredentials()));
                    }
                }
            });

            return list;
        }
        return Collections.emptyList();
    }


    private CredentialsWithMetadata addMetadataToCredentials(V1Secret s, IdCredentials cred) {
        CredentialsWithMetadata credWithMetadata = new CredentialsWithMetadata<>(cred);

        MetadataProvider.all().forEach(metadataProvider -> metadataProvider.attach(s, credWithMetadata));

        return credWithMetadata;
    }


    private IdCredentials convertSecret(V1Secret s) {
        if (KubernetesSecretRule.shouldExclude(s)) {
            return null;
        }

        String type = getSecretType(s);
        SecretToCredentialConverter lookup = SecretToCredentialConverter.lookup(type);
        if (lookup != null) {
            try {
                return lookup.convert(s);
            } catch (CredentialsConversionException ex) {
                // do not spam the logs with the stacktrace...
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Failed to convert Secret '" + SecretUtils.getCredentialId(s) + "' of type " + type, ex);
                } else {
                    LOG.log(Level.WARNING, "Failed to convert Secret ''{0}'' of type {1} due to {2}", new Object[]{SecretUtils.getCredentialId(s), type, ex.getMessage()});
                }
                return null;
            }
        }
        return null;
    }

    private String getSecretType(V1Secret s) {
        return s.getType();
    }

    @Override
    public CredentialsStore getStore(ModelObject object) {
        if (!(object instanceof ItemGroup)) {
            return null;
        }

        ItemGroup owner = (ItemGroup) object;
        if (!KubernetesSecretScope.hasMatchedScope(owner)) {
            return null;
        }

        return new AlaudaKubernetesCredentialsStore(this, owner);
    }


}
