package io.alauda.jenkins.plugins.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.security.ACL;
import io.alauda.jenkins.devops.support.KubernetesCluster;
import io.alauda.jenkins.devops.support.KubernetesClusterConfiguration;
import io.alauda.jenkins.devops.support.KubernetesClusterConfigurationListener;
import io.alauda.jenkins.plugins.credentials.convertor.CredentialsConversionException;
import io.alauda.jenkins.plugins.credentials.convertor.SecretToCredentialConverter;
import io.alauda.jenkins.plugins.credentials.metadata.CredentialsWithMetadata;
import io.alauda.jenkins.plugins.credentials.metadata.MetadataProvider;
import io.alauda.jenkins.plugins.credentials.rule.KubernetesSecretRule;
import io.alauda.jenkins.plugins.credentials.scope.JenkinsRootScope;
import io.alauda.jenkins.plugins.credentials.scope.KubernetesSecretScope;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.builder.ControllerManagerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Extension
public class KubernetesCredentialsProvider extends CredentialsProvider implements KubernetesClusterConfigurationListener {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesCredentialsProvider.class);
    private static final String CONTROLLER_NAME = "SecretController";

    // Maps of credentials keyed by credentials ID
    private ConcurrentHashMap<String, CredentialsWithMetadata> credentials = new ConcurrentHashMap<>();
    private ControllerManager controllerManager;
    private ExecutorService controllerManagerThread;

    private LocalDateTime lastEventComingTime;

    @Override
    public void onConfigChange(KubernetesCluster cluster, ApiClient client) {
        shutDown(null);

        SharedInformerFactory factory = new SharedInformerFactory();
        ControllerManagerBuilder managerBuilder = ControllerBuilder
                .controllerManagerBuilder(factory);

        String labelSelector = KubernetesCredentialsProviderConfiguration.get().getLabelSelector();

        CoreV1Api coreV1Api = new CoreV1Api();

        SharedIndexInformer<V1Secret> secretInformer = factory.sharedIndexInformerFor(
                callGeneratorParams -> coreV1Api.listSecretForAllNamespacesCall(
                        null,
                        null,
                        null,
                        labelSelector,
                        null,
                        null,
                        callGeneratorParams.resourceVersion,
                        null,
                        callGeneratorParams.timeoutSeconds,
                        callGeneratorParams.watch,
                        null), V1Secret.class, V1SecretList.class);


        Controller controller = ControllerBuilder.defaultBuilder(factory).watch(
                (workQueue) ->
                        ControllerBuilder.controllerWatchBuilder(V1Secret.class, workQueue)
                                .withWorkQueueKeyFunc(secret ->
                                        new Request(secret.getMetadata().getNamespace(), secret.getMetadata().getName()))
                                .withOnAddFilter(secret -> {
                                    logger.debug("[{}] receives event: Add; Secret '{}/{}'",
                                            CONTROLLER_NAME,
                                            secret.getMetadata().getNamespace(), secret.getMetadata().getName());
                                    return true;
                                })
                                .withOnUpdateFilter((oldSecret, newSecret) -> {
                                    String namespace = oldSecret.getMetadata().getNamespace();
                                    String name = oldSecret.getMetadata().getName();

                                    logger.debug("[{}] receives event: Update; Secret '{}/{}'",
                                            CONTROLLER_NAME,
                                            namespace, name);

                                    lastEventComingTime = LocalDateTime.now();

                                    return true;
                                })
                                .withOnDeleteFilter((secret, aBoolean) -> {
                                    logger.debug("[{}] receives event: Delete; Secret '{}/{}'",
                                            CONTROLLER_NAME,
                                            secret.getMetadata().getNamespace(), secret.getMetadata().getName());
                                    return true;
                                }).build())
                .withReconciler(new SecretReconciler(new Lister<>(secretInformer.getIndexer())))
                .withName(CONTROLLER_NAME)
                .withWorkerCount(4)
                .build();

        controllerManager = managerBuilder.addController(controller).build();

        controllerManagerThread = Executors.newSingleThreadExecutor();
        controllerManagerThread.submit(() -> controllerManager.run());
    }

    @Override
    public void onConfigError(KubernetesCluster cluster, Throwable reason) {
        shutDown(reason);
    }

    private void shutDown(Throwable reason) {
        if (controllerManager != null) {
            controllerManager.shutdown();
            controllerManager = null;
        }

        if (controllerManagerThread != null && !controllerManagerThread.isShutdown()) {
            controllerManagerThread.shutdown();
        }

        if (reason != null) {
            logger.error("Alauda DevOps Credentials Provider is stopped, reason {}", reason.getMessage());
        } else {
            logger.error("Alauda DevOps Credentials Provider is stopped, reason is null, might be stopped by user");
        }
    }

    public LocalDateTime getLastEventComingTime() {
        return lastEventComingTime;
    }


    class SecretReconciler implements Reconciler {

        private Lister<V1Secret> secretLister;

        public SecretReconciler(Lister<V1Secret> secretLister) {
            this.secretLister = secretLister;
        }

        @Override
        public Result reconcile(Request request) {
            String namespace = request.getNamespace();
            String name = request.getName();

            V1Secret secret = secretLister.namespace(namespace).get(name);
            if (secret == null) {
                logger.debug("[{}] Unable to get Secret '{}/{}' from local list, will remove it", getControllerName(), namespace, name);
                String credId = SecretUtils.getCredentialId(new V1ObjectMeta().namespace(namespace).name(name));
                if (credentials.containsKey(credId)) {
                    logger.debug("Secret Deleted - {}", credId);
                    credentials.remove(credId);
                }
                return new Result(false);
            }

            IdCredentials cred = convertSecret(secret);
            if (cred != null) {
                logger.debug("Secret Added - {}", cred.getId());
                CredentialsWithMetadata credWithMetadata = addMetadataToCredentials(secret, cred);
                credentials.put(cred.getId(), credWithMetadata);
                return new Result(false);
            }

            return new Result(false);
        }

        public String getControllerName() {
            return CONTROLLER_NAME;
        }
    }


    @Nonnull
    @Override
    public <C extends Credentials> List<C> getCredentials(@Nonnull Class<C> type, final ItemGroup itemGroup, Authentication authentication) {
        logger.debug("getCredentials called with type {} and authentication {}", type.getName(), authentication);
        if (ACL.SYSTEM.equals(authentication)) {
            List<C> credentialsWithinScopes = getCredentialsWithinScope(type, itemGroup, authentication);

            List<KubernetesSecretScope> scopes = KubernetesSecretScope.matchedScopes(itemGroup);
            if (scopes.stream().anyMatch(s -> s.getClass().equals(JenkinsRootScope.class))) {
                return credentialsWithinScopes;
            }


            JenkinsRootScope rootScope = ExtensionList.lookup(JenkinsRootScope.class).get(0);
            credentials.forEach((s, credentialsWithMetadata) -> {
                if (rootScope.shouldShowInScope(Jenkins.getInstance(), credentialsWithMetadata)
                        && type.isAssignableFrom(credentialsWithMetadata.getCredentials().getClass())) {
                    C c = type.cast(credentialsWithMetadata.getCredentials());
                    if (!credentialsWithinScopes.contains(c)) {
                        credentialsWithinScopes.add(c);
                    }
                }

            });
            return credentialsWithinScopes;
        }
        return Collections.emptyList();
    }

    public <C extends Credentials> List<C> getCredentialsWithinScope(@Nonnull Class<C> type, final ItemGroup itemGroup, Authentication authentication) {
        logger.debug("getCredentials called with type {} and authentication {}", type.getName(), authentication);
        if (ACL.SYSTEM.equals(authentication)) {
            List<C> list = new ArrayList<>();
            Set<String> ids = new HashSet<>();

            List<KubernetesSecretScope> scopes = KubernetesSecretScope.matchedScopes(itemGroup);

            credentials.forEach((id, credentialsWithMetadata) -> {
                if (scopes.stream().anyMatch(scope -> scope.shouldShowInScope(itemGroup, credentialsWithMetadata))
                        && type.isAssignableFrom(credentialsWithMetadata.getCredentials().getClass()) && ids.add(id)) {
                    list.add(type.cast(credentialsWithMetadata.getCredentials()));
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
                logger.debug("Failed to convert Secret '" + SecretUtils.getCredentialId(s) + "' of type " + type, ex);
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

    @Override
    @Nonnull
    public String getDisplayName() {
        return "Alauda DevOps Credentials Provider";
    }

    @Override
    public String getIconClassName() {
        return "icon-credentials-alauda-store";
    }

    public void restart() {
        KubernetesCluster cluster = KubernetesClusterConfiguration.get().getCluster();
        this.onConfigChange(cluster, Configuration.getDefaultApiClient());
    }
}
