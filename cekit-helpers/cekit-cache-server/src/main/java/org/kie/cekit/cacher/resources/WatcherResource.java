package org.kie.cekit.cacher.resources;

import org.kie.cekit.cacher.builds.nightly.NightlyBuildsWatcher;
import org.kie.cekit.cacher.properties.CacherProperties;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.logging.Logger;

@Path("/watcher")
public class WatcherResource {
    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    NightlyBuildsWatcher nightlyBuildsWatcher;

    @Inject
    CacherProperties cacherProperties;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/retry/{force}")
    public Response forceWatcherRetry(@PathParam("force") boolean force) {
        if (cacherProperties.isWatcherEnabled()) {
            nightlyBuildsWatcher.nightlyProductBuildsWatcher(force);
            return Response.ok().entity("New retry on verifying nightly builds made, check the artifacts list to see if new artifacts will be added, try to reload the page").build();
        }
        return Response.ok().entity("Watcher is disabled").build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{buildDate}")
    public Response retryNightlyBuild(@PathParam("buildDate") String buildDate,
                                      @QueryParam("force") boolean force) {

        if (cacherProperties.isWatcherEnabled()) {
            nightlyBuildsWatcher.verifyNightlyBuild(Optional.empty(), Optional.empty(), Optional.of(buildDate), force);
            return Response.ok().entity(responseMessage(buildDate)).build();
        }
        return Response.ok().entity("Watcher is disabled").build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/try/{version}/{branch}/{buildDate}")
    public Response retryNightlyBuildWithBranch(@PathParam("version") String version,
                                                @PathParam("branch") String branch,
                                                @PathParam("buildDate") String buildDate) {

        if (cacherProperties.isWatcherEnabled()) {
            nightlyBuildsWatcher.verifyNightlyBuild(Optional.of(version), Optional.of(branch), Optional.of(buildDate), false);
            return Response.ok().entity(responseMessage(buildDate)).build();
        }
        return Response.ok().entity("Watcher is disabled").build();
    }

    private String responseMessage(String buildDate) {
        StringBuilder responseMsg = new StringBuilder();
        responseMsg.append("A new request to search for nightly builds\n");
        responseMsg.append("using the build date [" + buildDate + "] was made\n");
        responseMsg.append("If found it will be showed on the artifact list with\n");
        responseMsg.append("Downloading status.");
        return responseMsg.toString();

    }
}
