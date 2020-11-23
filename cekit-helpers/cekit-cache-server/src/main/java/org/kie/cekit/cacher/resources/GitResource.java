package org.kie.cekit.cacher.resources;

import org.kie.cekit.cacher.builds.github.GitRepository;
import org.kie.cekit.cacher.utils.CacherUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("git")
@ApplicationScoped
public class GitResource {

    @Inject
    CacherUtils cacherUtils;

    @Inject
    GitRepository git;

    @GET
    @Path("pull")
    @Produces(MediaType.TEXT_PLAIN)
    public Response pull() {
        try {
            cacherUtils.startupVerifications();
            git.prepareLocalGitRepo();
            return Response.ok().status(Response.Status.CREATED).entity("Repositories pulled.").build();
        } catch (final Exception e) {
            return Response.serverError().entity(e.getCause()).build();
        }
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response clean() {
        try {
            git.cleanGitRepos();
            return Response.ok().entity("Git repositories cleaned.").build();
        } catch (final Exception e) {
            return Response.serverError().entity(e).build();
        }
    }
}
