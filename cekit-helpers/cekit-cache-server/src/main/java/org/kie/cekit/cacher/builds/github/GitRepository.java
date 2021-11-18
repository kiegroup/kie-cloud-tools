package org.kie.cekit.cacher.builds.github;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.Version;
import io.quarkus.scheduler.Scheduled;
import org.kie.cekit.cacher.builds.yaml.YamlFilesHelper;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.BuildUtils;

/**
 * Holds all Git operations
 */
@ApplicationScoped
public class GitRepository {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private LocalDateTime lastRebase;
    private boolean forceRebase;

    @Inject
    YamlFilesHelper yamlFilesHelper;

    @Inject
    CacherProperties cacherProperties;

    @Inject
    BuildUtils buildUtils;

    /**
     * Take care of the git repositories.
     * Clone rpham and rhdm -7-images repositories locally
     */
    public void prepareLocalGitRepo() throws IOException, InterruptedException {

        if (cacherProperties.isGHBotEnabled()) {

            log.info("Preparing git repositories...");
            lastRebase = LocalDateTime.now();
            forceRebase = true;
            String rhdmFork = cacherProperties.rhdmUpstream().replace(cacherProperties.rhdmUpstream().split("/")[3], cacherProperties.githubUsername());
            String rhpamFork = cacherProperties.rhpamUpstream().replace(cacherProperties.rhpamUpstream().split("/")[3], cacherProperties.githubUsername());

            if (Files.isExecutable(Paths.get(cacherProperties.getGitDir() + "/rhdm-7-image"))) {
                log.info("rhdm-image repo already exists.");
            } else {
                run(cacherProperties.getGitDir(), new String[]{"git", "clone", rhdmFork});

                // remove the origin remote to add the user credentials.
                run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "remote", "remove", "origin"});
                String origin = rhdmFork.replace("https://", "https://" + cacherProperties.githubUsername() + ":" + cacherProperties.oauthToken() + "@");
                run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "remote", "add", "origin", origin});

                //add bot gh email address
                run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "config", "user.email", cacherProperties.githubEmail()});

                run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "remote", "add", "upstream", cacherProperties.rhdmUpstream()});
            }

            if (Files.isExecutable(Paths.get(cacherProperties.getGitDir() + "/rhpam-7-image"))) {
                log.info("rhpam-image repo already exists.");
            } else {
                run(cacherProperties.getGitDir(), new String[]{"git", "clone", rhpamFork});

                // remove the origin remote to add the user credentials.
                run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "remote", "remove", "origin"});
                String origin = rhpamFork.replace("https://", "https://" + cacherProperties.githubUsername() + ":" + cacherProperties.oauthToken() + "@");
                run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "remote", "add", "origin", origin});

                //add bot gh email address
                run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "config", "user.email", cacherProperties.githubEmail()});

                run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "remote", "add", "upstream", cacherProperties.rhpamUpstream()});
            }
            gitRebase(cacherProperties.defaultBranch());
            checkoutDesiredBranch(cacherProperties.defaultBranch());
        } else {
            log.info("Github integration bot is disabled.");
        }
    }

    /**
     * make sure to checkout the default git branch
     */
    private void checkoutDesiredBranch(String branch) throws IOException, InterruptedException {
        run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "checkout", branch});
        run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "checkout", branch});
    }

    /**
     * Every 24h a rebase on OpenShfit repo will be made
     * or everytime a new file resulting from a nightly build
     * was added to the cacher's artifact dir. Once it is done
     * a new PR will be triggered updating the rhdm|rhpam-7-image
     * local repos.
     */
    @Scheduled(every = "24h", delay = 24, delayUnit = TimeUnit.HOURS)
    public void gitScheduledRebase() throws IOException, InterruptedException {
        gitRebase(cacherProperties.defaultBranch());
    }

    public void gitRebase(String branch) throws IOException, InterruptedException {
        // only rebase if the last rebase happened in the last hour, or force it
        if ((cacherProperties.isGHBotEnabled() && lastRebase.plusHours(1).isBefore(LocalDateTime.now())) || forceRebase) {
            log.info("Rebasing rhdm-7-image git repository...");
            run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "fetch", "upstream"});
            run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "rebase", "upstream/" + branch});

            log.info("Rebasing rhpam-7-image git repository...");
            run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "fetch", "upstream"});
            run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "rebase", "upstream/" + branch});

            lastRebase = LocalDateTime.now();
        } else {
            log.fine("Github integration bot is disabled or it was recently rebased.");
        }
    }

    /**
     * Verify one RHDM and RHPAM file that is usually updated:
     * CacherUtils.CACHER_GIT_DIR + "/rhpam-7-image/kieserver/modules/kieserver/module.yaml"
     * CacherUtils.CACHER_GIT_DIR + "/rhdm-7-image/kieserver/modules/kieserver/module.yaml"
     *
     * @return {@link String currentBuildDate}
     */
    public String getCurrentProductBuildDate(String branch, boolean force) throws IOException, InterruptedException {

        Version version = cacherProperties.getFormattedVersion();

        // rebase before
        forceRebase = false;
        checkoutDesiredBranch(branch);
        gitRebase(branch);

        String rhpamFilter = String.format("# rhpam-%s.redhat", cacherProperties.version());
        if (version.compareTo(cacherProperties.versionBeforeDMPAMPrefix) < 0) {
            rhpamFilter = String.format("# rhpam-%s.PAM-redhat", cacherProperties.version());
        }
        String finalRhpamFilter = rhpamFilter;
        log.fine("Using RHPAM filter " + finalRhpamFilter);

        String rhpamKieServerDateBuild = yamlFilesHelper
                .loadRawData(cacherProperties.getGitDir() + "/rhpam-7-image/kieserver/modules/kieserver/module.yaml")
                .stream()
                .filter(line -> line.contains(finalRhpamFilter))
                .findFirst().get();

        Matcher rhpamMatcher = null;

        try {
            rhpamMatcher = buildUtils.buildDatePattern.matcher(rhpamKieServerDateBuild);
        } catch (final Exception e) {
            log.warning("Failed to parse date pattern for " + rhpamKieServerDateBuild);
        }

        if (rhpamMatcher.find()) {
            log.fine("Build date validation succeed, current build date is: " + rhpamMatcher.group());
            return rhpamMatcher.group();

        } else {
            log.warning("Not able to identify upstream build date");
        }
        return "NONE";
    }

    /**
     * Add the file changes to be committed.
     *
     * @param repo - git repository name
     * @return true if the git add command was successfully executed, otherwise, false.
     */
    public boolean addChanges(String repo) {
        try {
            run(cacherProperties.getGitDir() + "/" + repo, new String[]{"git", "add", "--all"});
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * commit the changes to the current branch and push the commit to GitHub
     *
     * @param repo    - git repository name
     * @param message - Commit message
     * @return true if the git add command was successfully executed, otherwise, false.
     */
    public boolean commitChanges(String repo, String branch, String message) {
        try {
            run(cacherProperties.getGitDir() + "/" + repo, new String[]{"git", "commit", "-am", message});
            run(cacherProperties.getGitDir() + "/" + repo, new String[]{"git", "push", "origin", branch});
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handle branch see {@link BranchOperation} for all supported operations
     *
     * @param operation
     * @param branchName - branch  name
     * @param baseBranch - base branch to push a pull request
     * @param repo       - target repository, available are rhpam-7-image and rhdm-7-image
     */
    public void handleBranch(BranchOperation operation, String branchName, String baseBranch, String repo) throws IOException, InterruptedException {

        switch (operation) {
            case NEW_BRANCH:
                if (repo.equals("rhpam-7-image")) {
                    checkoutDesiredBranch(baseBranch);
                    gitRebase(baseBranch);
                    log.fine("Creating new branch for rhpam-7-image. Branch name ->  " + branchName);
                    run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "checkout", "-b", branchName, baseBranch});
                } else if (repo.equals("rhdm-7-image")) {
                    checkoutDesiredBranch(baseBranch);
                    gitRebase(baseBranch);
                    log.fine("Creating new branch for rhdm-7-image. Branch name ->  " + branchName);
                    run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "checkout", "-b", branchName, baseBranch});
                }
                break;

            case DELETE_BRANCH:
                if (repo.equals("rhpam-7-image")) {
                    log.fine("Deleting branch " + branchName + " for rhpam-7-image.");
                    run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "checkout", cacherProperties.defaultBranch()});
                    run(cacherProperties.getGitDir() + "/rhpam-7-image", new String[]{"git", "branch", "-D", branchName});
                } else if (repo.equals("rhdm-7-image")) {
                    log.fine("Deleting branch " + branchName + " for rhdm-7-image.");
                    run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "checkout", cacherProperties.defaultBranch()});
                    run(cacherProperties.getGitDir() + "/rhdm-7-image", new String[]{"git", "branch", "-D", branchName});
                }
                break;

            default:
                log.warning("Operation not recognized.");
                break;
        }
    }

    /**
     * Clean git repository
     * Can be called using the /git rest endpoint with DELETE method
     */
    public void cleanGitRepos() throws Exception {
        log.fine("Cleaning git repositories");
        Path path = Paths.get(cacherProperties.getGitDir());
        if (Files.exists(path)) {
            Files.walk(path).map(Path::toFile)
                    // sort it on the reverse order so directories can be deleted.
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .peek(f -> log.finest("Deleting " + f))
                    .forEach(File::delete);
            log.fine("Cleaning git repositories - done");
        }
    }

    /**
     * OS command executor, it executes commands on the base working dir
     *
     * @param workDir base dir
     * @param command git command to be executed
     */
    private void run(String workDir, String... command) throws IOException, InterruptedException {

        log.fine("Trying to execute the command: " + Arrays.asList(command) + " on work dir: " + workDir);

        ProcessBuilder builder = new ProcessBuilder().inheritIO();

        builder.directory(new File(workDir));
        if (!builder.directory().canWrite()) {
            throw new AccessDeniedException("Permission denied, can't write on : " + workDir);
        }
        builder.command(command);

        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to execute command " + Arrays.asList(command) + ", exit code is " + exitCode);
        }
    }
}

