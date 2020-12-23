package org.kie.cekit.cacher;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.kie.cekit.cacher.builds.github.GitRepository;
import org.kie.cekit.cacher.utils.CacherUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

@ApplicationScoped
public class CacherLifecyle {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    CacherUtils cacherUtils;

    @Inject
    GitRepository gitRepository;

    void onStart(@Observes StartupEvent ev) throws Exception {
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
