package org.kie.cekit.cacher.builds.cr;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.Version;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.utils.BuildUtils;
import org.kie.cekit.cacher.utils.CacherUtils;
import org.kie.cekit.cacher.utils.UrlUtils;

/**
 * Triggers a CR-X build update on target repositories
 * Requires to set CR build number, target branch and version
 */
@ApplicationScoped
public class CRBuildsUpdater {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    CacherUtils cacherUtils;

    @Inject
    BuildUtils buildUtils;

    @Inject
    CacherProperties cacherProperties;

    @Inject
    CRBuildInterceptor callback;

    public String updateCRBuild(@NotNull String version, @NotNull String releaseBranch, @NotNull int crBuild) {
        StringBuilder propsResponse = new StringBuilder();
        String formattedRHDMURL = String.format(cacherProperties.rhdmCRUrl(), version, crBuild);
        String formattedRHPAMURL = String.format(cacherProperties.rhpamCRUrl(), version, crBuild);
        Version ve = buildUtils.getVersion(cacherProperties.version().split("[.]"));

        // rhdm handling properties
        Properties rhdmCRProps = cacherProperties.productPropertyFile(formattedRHDMURL);
        if (rhdmCRProps.size() == 0) {
            propsResponse.append("RHDM props not found, please check the URL [")
                    .append(formattedRHDMURL)
                    .append("]");
        }

        // rhpam handling properties
        Properties rhpamCRProps = cacherProperties.productPropertyFile(formattedRHPAMURL);
        if (rhpamCRProps.size() == 0) {
            propsResponse.append("\nRHPAM props not found, please check the URL [")
                    .append(formattedRHPAMURL)
                    .append("]");
        }

        if (propsResponse.length() > 0) {
            if (ve.compareTo(cacherProperties.pam713) < 0) {
                // 7.13+ does not have rhdm
                return propsResponse.toString();
            }
        }

        // set the kieVersion
        cacherProperties.setKieVersion(rhpamCRProps.get("KIE_VERSION").toString());

        // Download rhpam and rhdm artifacts
        if (ve.compareTo(cacherProperties.pam713) < 0) {
            cacherProperties.getRhdmFiles2DownloadPropName().forEach(fileProp -> {
                callback.onRequestReceived(new PlainArtifact(
                        UrlUtils.getFileName(rhdmCRProps.get(fileProp).toString()),
                        "",
                        "",
                        "",
                        version,
                        releaseBranch,
                        crBuild)
                );
                new Thread(() -> log.info(cacherUtils.fetchFile(rhdmCRProps.get(fileProp).toString(), Optional.of("cr"), crBuild))).start();
            });
        }

        // set the kieVersion
        cacherProperties.setKieVersion(rhpamCRProps.get("KIE_VERSION").toString());
        cacherProperties.getRhpamFiles2DownloadPropName().forEach(fileProp -> {
            callback.onRequestReceived(new PlainArtifact(
                    UrlUtils.getFileName(rhpamCRProps.get(fileProp).toString()),
                    "",
                    "",
                    "",
                    version,
                    releaseBranch,
                    crBuild)
            );
            new Thread(() -> log.info(cacherUtils.fetchFile(rhpamCRProps.get(fileProp).toString(), Optional.of("cr"), crBuild))).start();
        });

        return responseMessage(version, releaseBranch, crBuild);
    }

    private String responseMessage(String version, String releaseBranch, int crBuild) {
        return "A new request to update CR build " + crBuild + " \n" +
                "for product version [" + version + "] \n" +
                "on branch [" + releaseBranch + "] has been requested. \n" +
                "If the artifacts for the given CR build exists, it will be downloaded and updated on target branch.\n" +
                "Make sure there is no previous artifacts with the same name already available on cacher.";
    }
}
