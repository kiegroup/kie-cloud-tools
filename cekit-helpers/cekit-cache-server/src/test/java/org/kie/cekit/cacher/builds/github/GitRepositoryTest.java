package org.kie.cekit.cacher.builds.github;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.CacherUtils;

import javax.inject.Inject;
import java.io.IOException;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GitRepositoryTest {

    @Inject
    GitRepository gitRepository;

    @Inject
    CacherProperties cacherProperties;

    @Inject
    CacherUtils cacherUtils;

    @BeforeAll
    public void cleanGit() throws Exception {
        // reset git repo before this test
        gitRepository.cleanGitRepos();
        cacherUtils.startupVerifications();
        gitRepository.prepareLocalGitRepo();
    }

    /**
     * If this tests runs ok means that the GitHub repos
     * are correctly persisted on filesystem
     */
    @Test
    public void getCurrentProductBuildDateTest() throws IOException, InterruptedException {
        System.out.println("~asdasdasdasd " + cacherProperties.defaultBranch());
        Assertions.assertNotNull(gitRepository.getCurrentProductBuildDate(cacherProperties.defaultBranch()));
        Assertions.assertNotEquals("NONE", gitRepository.getCurrentProductBuildDate(cacherProperties.defaultBranch()));
    }

    @Test
    public void handleBranchTest() throws IOException, InterruptedException {
        gitRepository.handleBranch(BranchOperation.NEW_BRANCH, "myBranch", cacherProperties.defaultBranch(), "rhpam-7-image");
        gitRepository.handleBranch(BranchOperation.DELETE_BRANCH, "myBranch", null, "rhpam-7-image");
    }

    @Test
    public void handleCustomBranchTest() throws IOException, InterruptedException {
        gitRepository.handleBranch(BranchOperation.NEW_BRANCH, "myOtherBranch", "7.13.x", "rhpam-7-image");
        gitRepository.handleBranch(BranchOperation.DELETE_BRANCH, "myOtherBranch", null, "rhpam-7-image");
    }


}
