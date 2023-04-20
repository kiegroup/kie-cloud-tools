package org.kie.cekit.cacher.builds.yaml;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.image.descriptors.module.Module;

import javax.inject.Inject;

@QuarkusTest
public class YamlFilesLoaderTest {

    @Inject
    YamlFilesHelper yamlFilesHelper;

    @Inject
    CacherProperties cacherProperties;

    @Test
    public void testModulesYamlFile() {
        Module modules = yamlFilesHelper.load("modules.yaml");

        Assertions.assertNotNull(modules.getSchemaVersion());
        Assertions.assertEquals(1, modules.getSchemaVersion());
        Assertions.assertEquals("rhpam-7-kieserver", modules.getName());
        Assertions.assertEquals("Red Hat Process Automation Manager KIE Server 7.13 installer", modules.getDescription());
        Assertions.assertEquals("[Label{name='org.jboss.product', value='rhpam-kieserver'}, Label{name='org.jboss.product.version', value='7.13.0'}, Label{name='org.jboss.product.rhpam-kieserver.version', value='7.13.0'}]", modules.getLabels().toString());
        Assertions.assertEquals("[Env{name='JBOSS_PRODUCT', value='rhpam-kieserver', description='null', example='null'}, Env{name='RHPAM_KIESERVER_VERSION', value='7.13.0', description='null', example='null'}, Env{name='PRODUCT_VERSION', value='7.13.0', description='null', example='null'}, Env{name='KIE_SERVER_DISTRIBUTION_ZIP', value='rhpam_kie_server_distribution.zip', description='null', example='null'}, Env{name='BUSINESS_CENTRAL_DISTRIBUTION_ZIP', value='rhpam_business_central_distribution.zip', description='null', example='null'}, Env{name='BUSINESS_CENTRAL_DISTRIBUTION_EAP', value='jboss-eap-7.4', description='null', example='null'}, Env{name='JBPM_WB_KIE_SERVER_BACKEND_JAR', value='jbpm-wb-kie-server-backend-7.62.0.redhat-211107.jar', description='null', example='null'}]", modules.getEnvs().toString());
        Assertions.assertEquals("[Artifact{name='rhpam_kie_server_distribution.zip', url='null', dest='null', target='null', md5='8f7e87bd95cb31fb355b2e6e6d304c77'}, Artifact{name='rhpam_business_central_distribution.zip', url='null', dest='null', target='null', md5='d4686b213a605e72b369b06e6fb93bbb'}, Artifact{name='slf4j-simple.jar', url='null', dest='null', target='null', md5='62cc6eeb72e2738e3acc8957ca95f37b'}, Artifact{name='kie-server-services-jbpm-cluster-7.62.0.redhat-211107.jar', url='null', dest='null', target='null', md5='becde72be53a9b4b6453ef9a79f9b4b2'}, Artifact{name='jbpm-event-emitters-kafka-7.62.0.redhat-211107.jar', url='null', dest='null', target='null', md5='1f877ff3ebd754cc1f78e8cf9736fdfb'}]", modules.getArtifacts().toString());
        Assertions.assertEquals("Run{user=185, cmd=[/opt/eap/bin/standalone.sh, -b, 0.0.0.0, -c, standalone-full.xml], workdir='null', entrypoint='null'}", modules.getRun().toString());
        Assertions.assertEquals("[Execute{script='install', user='null'}]", modules.getExecute().toString());
    }

    @Test
    public void loadRhpamModulesFromGitTest() {
        Module bcMonitoring = yamlFilesHelper.load(cacherProperties.getGitDir() +
                "/rhpam-7-image/businesscentral-monitoring/modules/businesscentral-monitoring/module.yaml");

        Module businessCentral = yamlFilesHelper.load(cacherProperties.getGitDir() +
                "/rhpam-7-image/businesscentral/modules/businesscentral/module.yaml");

        Module controller = yamlFilesHelper.load(cacherProperties.getGitDir() +
                "/rhpam-7-image/controller/modules/controller/module.yaml");

        Module kieserver = yamlFilesHelper.load(cacherProperties.getGitDir() +
                "/rhpam-7-image/kieserver/modules/kieserver/module.yaml");

        Module smartrouter = yamlFilesHelper.load(cacherProperties.getGitDir() +
                "/rhpam-7-image/smartrouter/modules/smartrouter/module.yaml");

        Module processMigration = yamlFilesHelper.load(cacherProperties.getGitDir() +
                                                           "/rhpam-7-image/process-migration/modules/process-migration/module.yaml");

        Assertions.assertNotNull(bcMonitoring);
        Assertions.assertEquals("rhpam-7-businesscentral-monitoring", bcMonitoring.getName());

        Assertions.assertNotNull(businessCentral);
        Assertions.assertEquals("rhpam-7-businesscentral", businessCentral.getName());

        Assertions.assertNotNull(controller);
        Assertions.assertEquals("rhpam-7-controller", controller.getName());

        Assertions.assertNotNull(kieserver);
        Assertions.assertEquals("rhpam-7-kieserver", kieserver.getName());

        Assertions.assertNotNull(smartrouter);
        Assertions.assertEquals("rhpam-7-smartrouter", smartrouter.getName());

        Assertions.assertNotNull(processMigration);
        Assertions.assertEquals("rhpam-7-process-migration", processMigration.getName());

    }
}
