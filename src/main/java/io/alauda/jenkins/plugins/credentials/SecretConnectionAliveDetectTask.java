package io.alauda.jenkins.plugins.credentials;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
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
            logger.debug("The KubernetesCredentialsProvider seems not start or no resource exists in k8s, will skip check for it");
            return;
        }

        Duration elapsed = Duration.between(lastEventComingTime, now);

        // the apiserver will use heartbeat to update resource per 30 seconds, so if we didn't receive an update event in last 1 minute,
        // the watch connection might broken
        if (!elapsed.minus(Duration.ofMinutes(1)).isNegative()) {
            int count = heartbeatLostCount.incrementAndGet();
            logger.warn("The watch connection of resource Secret seems broken, " +
                    "last event coming at {}, time since last event coming {}s, retry count {}", lastEventComingTime, elapsed.getSeconds(), count);
        } else {
            heartbeatLostCount.set(0);
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
}