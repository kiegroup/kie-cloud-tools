package org.kie.cloud.tests.logs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.kie.cloud.tests.core.resources.Project;
import org.kie.cloud.tests.core.util.PodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PodLogsCollectorRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PodLogsCollectorRunnable.class);

    private static final Integer DEFAULT_OBERVABLE_BUFFER_IN_SECONDS = 5;

    private Project project;
    private String logFolderName;

    protected ExecutorService executorService = Executors.newCachedThreadPool();
    protected Set<PodEntity> observedPods = Collections.synchronizedSet(new HashSet<>());

    public PodLogsCollectorRunnable(Project project, String logFolderName) {
        super();
        this.project = project;
        this.logFolderName = logFolderName;
    }

    @Override
    public void run() {
        // Check for new instances and observe on them
        getAllPods().stream()
                // Filter non observed instances
                .filter(pod -> !isPodObserved(pod))
                // Observe instance logs
                .forEach(this::observePodLogs);
    }

    protected List<PodEntity> getAllPods() {
        return project.getAllPods()
                .stream()
                .map(PodUtils::getPodName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(podName -> new PodEntity(project, podName))
                .collect(Collectors.toList());
    }

    public void closeAndFlushRemainingInstanceCollectors(int waitForCompletionInMs) {
        // Make a copy before stopping collector threads
        List<PodEntity> pods = new ArrayList<>(observedPods);

        // Stop all collectors
        executorService.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(waitForCompletionInMs, TimeUnit.MILLISECONDS)) {
                logger.warn("Log collector Threadpool cannot stop. Force shutdown ...");
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(waitForCompletionInMs, TimeUnit.MILLISECONDS))
                    logger.error("Log collector Threadpool did not terminate");
            } else {
                logger.debug("Log collector Threadpool stopped correctly");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        } finally {
            // Finally, flush logs to be sure we have the last state of running pods
            pods.forEach(this::flushInstanceLogs);
        }
    }

    private void observePodLogs(PodEntity podEntity) {
        Future<?> future = executorService.submit(() -> {
            try {
                podEntity.observeAllContainersLogs()
                        .entrySet()
                        .forEach(entry -> {
                            entry.getValue().buffer(DEFAULT_OBERVABLE_BUFFER_IN_SECONDS, TimeUnit.SECONDS)
                                    .subscribe(logLines -> podLogLines(podEntity.getName(), entry.getKey(), logLines), error -> {
                                        throw new RuntimeException(error);
                                    });
                        });

            } catch (Exception e) {
                logger.error("Problem observing logs for instance " + podEntity.getName(), e);
            } finally {
                removePodObserved(podEntity);
            }
        });
        setPodAsObserved(podEntity, future);
    }

    private void podLogLines(String podName, String containerName, Collection<String> logLines) {
        logger.trace("Write log lines {}", logLines);
        LogsUtil.appendPodLogLines(getName(podName, containerName), logFolderName, logLines);
    }

    private void flushInstanceLogs(PodEntity podEntity) {
        logger.trace("Flushing logs from {}", podEntity.getName());
        // Check pod exists
        if (podEntity.exists()) {
            logger.trace("Flush logs from {}", podEntity.getName());
            podEntity.getAllContainerLogs()
                    .entrySet()
                    .forEach(entry -> {
                        writePodLogs(podEntity.getName(), entry.getKey(), entry.getValue());
                    });

        } else {
            logger.trace("Ignoring pod {} as not running", podEntity.getName());
        }
    }

    private void writePodLogs(String podName, String containerName, String logs) {
        logger.trace("Write log lines {}", logs);
        LogsUtil.writeLogs(getName(podName, containerName), logFolderName, logs);
    }

    private boolean isPodObserved(PodEntity podEntity) {
        synchronized (observedPods) {
            return this.observedPods.stream()
                    .map(PodEntity::getName)
                    .anyMatch(name -> podEntity.getName().equals(name));
        }
    }

    private void setPodAsObserved(PodEntity podEntity, Future<?> future) {
        logger.trace("Observe instance {}", podEntity.getName());
        this.observedPods.add(podEntity);
    }

    private void removePodObserved(PodEntity podEntity) {
        logger.trace("finished observing instance {}", podEntity.getName());
        this.observedPods.remove(podEntity);
    }

    private static String getName(String podName, String containerName) {
        return podName + "-" + containerName;
    }

}
