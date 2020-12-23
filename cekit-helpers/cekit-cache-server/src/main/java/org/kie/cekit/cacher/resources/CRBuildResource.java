package org.kie.cekit.cacher.resources;

import org.kie.cekit.cacher.builds.cr.CRBuildsUpdater;
import org.kie.cekit.cacher.properties.CacherProperties;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

@Path("/crbuild")
public class CRBuildResource {
    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    CRBuildsUpdater crBuildsUpdater;

    @Inject
    CacherProperties cacherProperties;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/use/{version}/{releaseBranch}/{crBuild}")
    public Response retryNightlyBuildWithBranch(@PathParam("version") String version,
                                                @PathParam("releaseBranch") String releaseBranch,
                                                @PathParam("crBuild") int crBuild) {

        log.info("Trying to update te CR [" + crBuild + "] build for version [" + version + "] on branch [" + releaseBranch + "].");
        return Response.ok().entity(crBuildsUpdater.updateCRBuild(version, releaseBranch, crBuild)).build();
    }
}
