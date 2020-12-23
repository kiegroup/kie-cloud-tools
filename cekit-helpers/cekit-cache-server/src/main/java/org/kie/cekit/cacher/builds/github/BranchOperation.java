package org.kie.cekit.cacher.builds.github;

/**
 * defines which branch operations can be made
 * NEW_BRANCH: will be called when all rhpam or rhdm files are ready for anew Pull Request
 * DELETE_BRANCH: will be called when the changes are pushed to git hub
 * PUSH_BRANCH: will be called after a new branch is created and all files are ready to push.
 * COMMIT_CHANGES: commit all changes before push
 *
 */
public enum BranchOperation {

    COMMIT_CHANGES,
    DELETE_BRANCH,
    NEW_BRANCH,
    PUSH_BRANCH,
}
