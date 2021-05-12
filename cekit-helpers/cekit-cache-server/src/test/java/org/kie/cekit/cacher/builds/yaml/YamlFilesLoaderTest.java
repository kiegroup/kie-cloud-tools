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
        Assertions.assertEquals("rhdm-7-kieserver", modules.getName());
        Assertions.assertEquals("Red Hat Decision Manager KIE Server 7.6 install", modules.getDescription());
        Assertions.assertEquals("[Label{name='org.jboss.product', value='rhdm-kieserver'}, Label{name='org.jboss.product.version', value='7.6.0'}, Label{name='org.jboss.product.rhdm-kieserver.version', value='7.6.0'}]", modules.getLabels().toString());
        Assertions.assertEquals("[Env{name='JBOSS_PRODUCT', value='rhdm-kieserver', description='null', example='null'}, Env{name='RHDM_KIESERVER_VERSION', value='7.6.0', description='null', example='null'}, Env{name='PRODUCT_VERSION', value='7.6.0', description='null', example='null'}, Env{name='KIE_SERVER_DISTRIBUTION_ZIP', value='kie_server_distribution.zip', description='null', example='null'}]", modules.getEnvs().toString());
        Assertions.assertEquals("[Artifact{name='KIE_SERVER_DISTRIBUTION_ZIP', url='null', dest='null', target='kie_server_distribution.zip', md5='03bc4e80d0945994c4e9beb9e756e0ab'}, Artifact{name='slf4j-simple.jar', url='null', dest='null', target='slf4j-simple.jar', md5='51c319582c16a07c21e41737e45cb03a'}, Artifact{name='jbpm-event-emitters-kafka-7.x.Final.jar', url='null', dest='/opt', target='null', md5='07e2ec53eb779fab36b357b145ba255b'}]", modules.getArtifacts().toString());
        Assertions.assertEquals("Run{user=185, cmd=[/opt/eap/bin/standalone.sh, -b, 0.0.0.0, -c, standalone-full.xml], workdir='null'}", modules.getRun().toString());
        Assertions.assertEquals("[Execute{script='install'}]", modules.getExecute().toString());
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

    @Test
    public void loadRhdmModulesFromGitTest() {

        Module controller = yamlFilesHelper.load(cacherProperties.getGitDir() +
                "/rhdm-7-image/controller/modules/controller/module.yaml");

        Module decisioncentral = yamlFilesHelper.load(cacherProperties.getGitDir() +
                "/rhdm-7-image/decisioncentral/modules/decisioncentral/module.yaml");

        Module kieserver = yamlFilesHelper.load(cacherProperties.getGitDir() +
                "/rhdm-7-image/kieserver/modules/kieserver/module.yaml");

        Assertions.assertNotNull(controller);
        Assertions.assertEquals("rhdm-7-controller", controller.getName());

        Assertions.assertNotNull(decisioncentral);
        Assertions.assertEquals("rhdm-7-decisioncentral", decisioncentral.getName());

        Assertions.assertNotNull(kieserver);
        Assertions.assertEquals("rhdm-7-kieserver", kieserver.getName());

    }

}
