package io.alauda.jenkins.plugins.credentials;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1SecretList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Extension
public class SecretConnectionAliveDetectTask extends AsyncPeriodicWork {
    private static final Logger logger = LoggerFactory.getLogger(SecretConnectionAliveDetectTask.class);

    private AtomicInteger heartbeatLostCount = new AtomicInteger();

    public SecretConnectionAliveDetectTask() {
        super("Kubernetes watch connection detect task");
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        LocalDateTime now = LocalDateTime.now();

        logger.debug("Starting to check if the watch connection of resource Secret is alive");

        ExtensionList<KubernetesCredentialsProvider> credentialsProviders = ExtensionList.lookup(KubernetesCredentialsProvider.class);
        if (credentialsProviders.size() == 0) {
            logger.info("Unable to find KubernetesCredentialsProvider instance, will skip this check");
            return;
        }

        LocalDateTime lastEventComingTime = credentialsProviders.get(0).getLastEventComingTime();
        // controller might not be initialized, or no resource exist in k8s so that we cannot receive event
        if (lastEventComingTime == null) {
            try {
                // if there has resource exists but we didn't receive any event, the watch connection might be broken
                if (hasSecretExistsInK8s()) {
                    int count = heartbeatLostCount.incrementAndGet();
                    logger.warn("The watch connection of resource Secret seems broken, retry count {}",  count);
                } else {
                    logger.debug("There are no resource Secret exists in k8s, will skip this check for it");
                    return;
                }
            } catch (ApiException e) {
                logger.warn("Unable to check if resource Secret exists in k8s, will skip this check for it, reason: {}",  e.getMessage());
                return;
            }
        } else {
            Duration elapsed = Duration.between(lastEventComingTime, now);

            // the apiserver will use heartbeat to update resource per 30 seconds, so if we didn't receive an update event in last 1 minute,
            // the watch connection might broken
            if (!elapsed.minus(Duration.ofMinutes(1)).isNegative()) {
                int count = heartbeatLostCount.incrementAndGet();
                logger.warn("The watch connection of resource Secret seems broken, " +
                        "last event coming at {}, time since last event coming {}s, retry count {}",
                    lastEventComingTime, elapsed.getSeconds(), count);
            } else {
                heartbeatLostCount.set(0);
            }
        }

        if (heartbeatLostCount.get() > 3) {
            heartbeatLostCount.set(0);
            credentialsProviders.get(0).restart();
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    public boolean hasSecretExistsInK8s() throws ApiException {
        CoreV1Api api = new CoreV1Api();
        V1SecretList secretList = api.listSecretForAllNamespaces(
                null,
                null,
                null,
                1,
                null,
                "0",
                null,
                null);

        if (secretList == null || secretList.getItems() == null || secretList.getItems().size() == 0) {
            return false;
        }

        return true;
    }
}
