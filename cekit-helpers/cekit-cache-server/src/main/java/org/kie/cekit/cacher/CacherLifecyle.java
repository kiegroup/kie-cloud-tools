package org.kie.cekit.cacher;

import java.lang.invoke.MethodHandles;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.kie.cekit.cacher.builds.github.GitRepository;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.CacherUtils;
import org.kie.cekit.cacher.utils.HttpRequestHandler;

@ApplicationScoped
public class CacherLifecyle {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    CacherUtils cacherUtils;

    @Inject
    GitRepository gitRepository;

    @Inject
    CacherProperties props;

    void onStart(@Observes StartupEvent ev) throws Exception {
        if (props.trustAllCerts()) {
            log.fine("Trusting all certs...");
            HttpRequestHandler.trustAllCertificates();
        }
        log.info("Quarkus CEKit Cacher is starting, performing startup verifications...");
        gitRepository.cleanGitRepos();
        cacherUtils.startupVerifications();
        gitRepository.prepareLocalGitRepo();
        cacherUtils.preLoadFromFile();
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("The application is stopping...");
    }


}
