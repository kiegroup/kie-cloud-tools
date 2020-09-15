package org.kie.cekit.cacher.builds.github;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.kie.cekit.cacher.objects.PlainArtifact;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BuildUpdatesInterceptorTest {

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    String buildDate = LocalDate.now().plusDays(1).format(formatter);

    @Inject
    BuildDateUpdatesInterceptor buildDateUpdatesInterceptor;

    @Inject
    PullRequestAcceptor prAcceptor;

    @Inject
    GitRepository gitRepository;

    /**
     * Do not execute these tests on CI or automatically
     * it will simulate the entire process of Pull Request.
     * Ony use it in case of troubleshooting.
     */
    //@Test
    @Order(1)
    public void newBUildDateDetectedTest() {
        // add 7 files - number of the required files before submit a new pull request. build date is always plus one day
        buildDateUpdatesInterceptor.onNewBuildReceived(new PlainArtifact("rhpam-7.5.0.PAM-redhat-" + buildDate + "-add-ons.zip", null, buildDate, "", null, null), false);
        buildDateUpdatesInterceptor.onNewBuildReceived(new PlainArtifact("rhpam-7.5.0.PAM-redhat-" + buildDate + "-business-central-eap7-deployable.zip", null, buildDate, "", null, null), false);
        buildDateUpdatesInterceptor.onNewBuildReceived(new PlainArtifact("rhpam-7.5.0.PAM-redhat-" + buildDate + "-monitoring-ee7.zip", null, buildDate, "", null, null), false);
        buildDateUpdatesInterceptor.onNewBuildReceived(new PlainArtifact("rhpam-7.5.0.PAM-redhat-" + buildDate + "-kie-server-ee8.zip", null, buildDate, "", null, null), false);
        buildDateUpdatesInterceptor.onNewBuildReceived(new PlainArtifact("rhdm-7.5.0.DM-redhat-" + buildDate + "-add-ons.zip", null, buildDate, "", null, null), false);
        buildDateUpdatesInterceptor.onNewBuildReceived(new PlainArtifact("rhdm-7.5.0.DM-redhat-" + buildDate + "-decision-central-eap7-deployable.zip", null, buildDate, "", null, null), false);
        buildDateUpdatesInterceptor.onNewBuildReceived(new PlainArtifact("rhdm-7.5.0.DM-redhat-" + buildDate + "-kie-server-ee8.zip", null, buildDate, "", null, null), false);

        Assertions.assertNotNull(prAcceptor.getElements());
        Assertions.assertTrue(prAcceptor.getElements().size() == 7);

    }

    //@Test
    @Order(2)
    public void filePersistedTest() {
        buildDateUpdatesInterceptor.onFilePersisted("rhdm-7.5.0.DM-redhat-" + buildDate + "-add-ons.zip", "ab1");
        Assertions.assertEquals(prAcceptor.getElements().get("rhdm-7.5.0.DM-redhat-" + buildDate + "-add-ons.zip").getChecksum(), "ab1");
        buildDateUpdatesInterceptor.onFilePersisted("rhdm-7.5.0.DM-redhat-" + buildDate + "-decision-central-eap7-deployable.zip", "ab2");
        Assertions.assertEquals(prAcceptor.getElements().get("rhdm-7.5.0.DM-redhat-" + buildDate + "-decision-central-eap7-deployable.zip").getChecksum(), "ab2");
        buildDateUpdatesInterceptor.onFilePersisted("rhdm-7.5.0.DM-redhat-" + buildDate + "-kie-server-ee8.zip", "ab3");
        buildDateUpdatesInterceptor.onFilePersisted("rhpam-7.5.0.PAM-redhat-" + buildDate + "-add-ons.zip", "ab4");
        Assertions.assertEquals(prAcceptor.getElements().get("rhpam-7.5.0.PAM-redhat-" + buildDate + "-add-ons.zip").getChecksum(), "ab4");
        buildDateUpdatesInterceptor.onFilePersisted("rhpam-7.5.0.PAM-redhat-" + buildDate + "-business-central-eap7-deployable.zip", "ab5");
        Assertions.assertEquals(prAcceptor.getElements().get("rhpam-7.5.0.PAM-redhat-" + buildDate + "-business-central-eap7-deployable.zip").getChecksum(), "ab5");
        buildDateUpdatesInterceptor.onFilePersisted("rhpam-7.5.0.PAM-redhat-" + buildDate + "-monitoring-ee7.zip", "ab6");
        Assertions.assertEquals(prAcceptor.getElements().get("rhpam-7.5.0.PAM-redhat-" + buildDate + "-monitoring-ee7.zip").getChecksum(), "ab6");
        buildDateUpdatesInterceptor.onFilePersisted("rhpam-7.5.0.PAM-redhat-" + buildDate + "-kie-server-ee8.zip", "ab7");
    }

}
