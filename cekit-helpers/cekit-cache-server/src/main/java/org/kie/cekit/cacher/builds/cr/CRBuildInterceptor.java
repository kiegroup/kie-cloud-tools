package org.kie.cekit.cacher.builds.cr;

import org.kie.cekit.cacher.objects.PlainArtifact;

/**
 * A simple callback to notify the PullRequestAcceptor class
 * when a new rhdm/rhpam artifact is being downloaded and
 * after it is persisted (download is finished) to start the
 * Pull Request process
 */
public interface CRBuildInterceptor {

    /**
     * When a new artifact build is detected and a download starts,
     * then this interface notifies all classes that implements it.
     *
     * @param plainArtifact {@link PlainArtifact}
     */
    void onRequestReceived(PlainArtifact plainArtifact);

    /**
     * When a file is persisted, notify the callback and, if there is a
     * filename waiting for its checksum, update the hashMap
     * and {@link CRBuildsUpdater}
     *
     * @param fileName name of the persisted file
     * @param checkSum md5 checksum of the persisted file
     */
    void onFilePersisted(String fileName, String checkSum, int crBuild);

}
