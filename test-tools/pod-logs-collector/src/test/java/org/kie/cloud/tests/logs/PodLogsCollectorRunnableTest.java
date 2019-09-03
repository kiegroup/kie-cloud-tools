package org.kie.cloud.tests.logs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.cloud.tests.core.resources.Project;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import rx.Observable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PodLogsCollectorRunnableTest {

    private static final String LOG_FOLDER_NAME = "LOG_FOLDER_NAME";
    private static final String LOG_OUTPUT_DIRECTORY = "instances";
    private static final String LOG_SUFFIX = ".log";
    private static final String CONTAINER_NAME = "container";

    private static final Integer DEFAULT_WAIT_FOR_COMPLETION_IN_MS = 5000;

    @Mock
    Project projectMock;

    PodLogsCollectorRunnable cut;

    @BeforeEach
    public void setUp() throws IOException {
        FileUtils.deleteDirectory(new File(LOG_OUTPUT_DIRECTORY));

        cut = Mockito.spy(new PodLogsCollectorRunnable(projectMock, LOG_FOLDER_NAME));
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File(LOG_OUTPUT_DIRECTORY));
    }

    @Test
    public void oneInstanceRunning() {
        mockObserveLogsCallable(setPodEntityMocks("BONJOUR"), null);
        ExecutorService executorService = retrieveExecutorService();

        cut.run();

        assertEquals(1, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances("BONJOUR");

        cut.closeAndFlushRemainingInstanceCollectors(DEFAULT_WAIT_FOR_COMPLETION_IN_MS);

        assertEquals(0, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances();

        checkLog("BONJOUR", true);

    }

    @Test
    public void oneInstanceRunningRunnableExecutedTwice() {
        mockObserveLogsCallable(setPodEntityMocks("BONJOUR"), null);
        ExecutorService executorService = retrieveExecutorService();

        cut.run();
        cut.run();

        assertEquals(1, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances("BONJOUR");

        cut.closeAndFlushRemainingInstanceCollectors(DEFAULT_WAIT_FOR_COMPLETION_IN_MS);

        assertEquals(0, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances();

        checkLog("BONJOUR", true);
    }

    @Test
    public void manyInstancesRunning() {
        List<PodEntity> pods = setPodEntityMocks("BONJOUR", "HELLO", "BUON GIORNO", "HALLO", "DOBRY DEN");
        mockObserveLogsCallable(pods, 1000); // Add small tempo to be sure the check after on the number of threads/observed instances is correct 

        ExecutorService executorService = retrieveExecutorService();

        cut.run();

        assertEquals(5, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances("BONJOUR", "HELLO", "BUON GIORNO", "HALLO", "DOBRY DEN");

        cut.closeAndFlushRemainingInstanceCollectors(DEFAULT_WAIT_FOR_COMPLETION_IN_MS);

        assertEquals(0, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances();

        checkLog("BONJOUR", true);
        checkLog("HELLO", true);
        checkLog("BUON GIORNO", true);
        checkLog("HALLO", true);
        checkLog("DOBRY DEN", true);
    }

    @Test
    public void killBeforeFinished() {
        mockObserveLogsCallable(setPodEntityMocks("BONJOUR"), 2000);
        ExecutorService executorService = retrieveExecutorService();

        cut.run();

        assertEquals(1, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances("BONJOUR");

        // Here we wait less than the time for the message to be delivered
        cut.closeAndFlushRemainingInstanceCollectors(1000);

        assertEquals(0, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances();

        checkLog("BONJOUR", false);
    }

    @Test
    public void killBeforeFinishedAndFlush() {
        List<PodEntity> pods = setPodEntityMocks("BONJOUR");
        mockObserveLogsCallable(pods, 2000);
        mockGetLogs(pods);
        pods.forEach(inst -> Mockito.when(inst.exists()).thenReturn(true));
        ExecutorService executorService = retrieveExecutorService();

        cut.run();

        assertEquals(1, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances("BONJOUR");

        // Here we wait less than the time for the message to be delivered
        cut.closeAndFlushRemainingInstanceCollectors(1000);

        assertEquals(0, ((ThreadPoolExecutor) executorService).getActiveCount());
        checkObservedInstances();

        checkLog("BONJOUR", true);
    }

    private List<PodEntity> setPodEntityMocks(String... podNames) {
        List<PodEntity> pods = Arrays.asList(podNames)
                .stream()
                .map(this::createPodMock)
                .collect(Collectors.toList());

        Mockito.doReturn(pods).when(cut).getAllPods();

        return pods;
    }

    private PodEntity createPodMock(String podName) {
        PodEntity podMock = Mockito.mock(PodEntity.class);
        Mockito.when(podMock.getName()).thenReturn(podName);
        return podMock;
    }

    private void mockObserveLogsCallable(List<PodEntity> pods, Integer waitForMessage) {
        pods.forEach(pod -> {
            Mockito.when(pod.observeAllContainersLogs()).then((invocation) -> {
                Map<String, Observable<String>> observes = new HashMap<>();
                observes.put(CONTAINER_NAME, Observable.fromCallable(() -> {
                    if (Objects.nonNull(waitForMessage)) {
                        Thread.sleep(waitForMessage);
                    }
                    return pod.getName();
                }));
                return observes;
            });
        });
    }

    private void mockGetLogs(List<PodEntity> pods) {
        pods.forEach(pod -> {
            Mockito.when(pod.getAllContainerLogs()).then((invocation) -> {
                Map<String, String> observes = new HashMap<>();
                observes.put(CONTAINER_NAME, pod.getName());
                return observes;
            });
        });
    }

    private ExecutorService retrieveExecutorService() {
        return cut.executorService;
    }

    private void checkObservedInstances(String... instanceNames) {
        assertEquals(instanceNames.length, cut.observedPods.size());
        Arrays.asList(instanceNames).forEach(podName -> {
            assertTrue(cut.observedPods.stream().map(PodEntity::getName).anyMatch(podName::equals), "Pod with name " + podName + " is not observed...");
        });
    }

    private void checkLog(String message, boolean exist) {
        assertEquals(exist, isLogExisting(message));
        if (exist) {
            assertEquals(message, readLog(message), "Log for " + message + " is wrong");
        }
    }

    private static boolean isLogExisting(String podName) {
        return getOutputFile(podName).exists();
    }

    private static String readLog(String instanceName) {
        try {
            return FileUtils.readFileToString(getOutputFile(instanceName), "UTF-8").trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File getOutputFile(String instanceName) {
        File outputDirectory = new File(LOG_OUTPUT_DIRECTORY, LOG_FOLDER_NAME);
        outputDirectory.mkdirs();
        return new File(outputDirectory, instanceName + "-" + CONTAINER_NAME + LOG_SUFFIX);
    }
}
