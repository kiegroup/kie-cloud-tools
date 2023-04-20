package org.kie.cekit.cacher.utils;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;
import org.kie.cekit.cacher.properties.loader.CacherProperty;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CacherUtilsTest {

    @Inject
    CacherProperties cacherProperties;

    @Inject
    CacherUtils cacherUtils;

    @Inject
    @CacherProperty(name = "org.kie.cekit.cacher.preload.file")
    String preLoadFileLocation;

    @BeforeAll
    public void beforeTests() throws IOException {
        clean();

        cacherUtils.startupVerifications();

        File fout = new File(preLoadFileLocation);
        FileOutputStream fos = new FileOutputStream(fout);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        osw.write("https://repo1.maven.org/maven2/org/jboss/jandex/2.1.1.Final/jandex-2.1.1.Final-sources.jar\n");
        osw.write("https://repo1.maven.org/maven2/org/jboss/narayana/jta/cdi/5.9.5.Final/cdi-5.9.5.Final-sources.jar\n");
        osw.write("https://repo1.maven.org/maven2/org/jboss/remoting/jboss-remoting/5.0.9.Final/jboss-remoting-5.0.9.Final.jar\n");
        osw.close();
    }

    @AfterAll
    public void cleanCacherDirectory() {
        clean();
    }


    @Test
    @Order(1)
    public void cleanTmpFilesTest() throws IOException {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        Arrays.asList(
                cacherProperties.getArtifactsTmpDir() + "/file1-1day-old.txt",
                cacherProperties.getArtifactsTmpDir() + "/file2-1day-old.txt"
        ).stream()
                .forEach(file ->
                {
                    try {
                        File f = Files.createFile(Paths.get(file)).toFile();
                        f.setLastModified(yesterday.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        Files.createFile(Paths.get(cacherProperties.getArtifactsTmpDir() + "/file3.txt"));

        cacherUtils.cleanTmpFiles();

        // file-1 should be deleted
        Assertions.assertFalse(Paths.get(cacherProperties.getArtifactsTmpDir() + "/file1-1day-old.txt").toFile().exists());
        // file-2 should be deleted
        Assertions.assertFalse(Paths.get(cacherProperties.getArtifactsTmpDir() + "/file2-1day-old.txt").toFile().exists());
        // file-3 should not be deleted
        Assertions.assertTrue(Paths.get(cacherProperties.getArtifactsTmpDir() + "/file3.txt").toFile().exists());
        // tmp dir should exist after the files are deleted
        Assertions.assertTrue(Files.exists(Paths.get(cacherProperties.getArtifactsTmpDir()).toAbsolutePath()));

    }

    @Test
    @Order(2)
    public void preLoadFromFileAndFileExistsTest() throws InterruptedException {
        cacherUtils.preLoadFromFile();
        // wait a few seconds so the files are downloaded.
        // 10 seconds
        Thread.sleep(10000);

        Assertions.assertTrue(cacherUtils.fileExists("0d3955b0fed4a2d03d1adc29b3fd7c67"));
        Assertions.assertTrue(cacherUtils.fileExists("8039610bc1401c3c3c21c2fd75707c9b"));
        Assertions.assertTrue(cacherUtils.fileExists("dfe927040dbd33159b61b2c92e0b7ae2"));

        // by name
        Assertions.assertTrue(cacherUtils.fileExistsByNameExcludeTmp("jboss-remoting-5.0.9.Final.jar"));
        Assertions.assertTrue(cacherUtils.fileExistsByNameExcludeTmp("jandex-2.1.1.Final-sources.jar"));
        Assertions.assertTrue(cacherUtils.fileExistsByNameExcludeTmp("cdi-5.9.5.Final-sources.jar"));
    }

    @Test
    @Order(3)
    public void getFileTest() {
        Assertions.assertEquals(
                cacherProperties.getCacherArtifactsDir() + "/0d3955b0fed4a2d03d1adc29b3fd7c67/jboss-remoting-5.0.9.Final.jar",
                cacherUtils.getFile("0d3955b0fed4a2d03d1adc29b3fd7c67").get().toString());
    }

    @Test
    @Order(4)
    public void getPersistedArtifactsTest() {
        List<PlainArtifact> artifacts = cacherUtils.getPersistedArtifacts();
        // should be, at least greater than 2
        Assertions.assertTrue(artifacts.size() > 2);

        Assertions.assertTrue(artifacts.stream().map(PlainArtifact::getFileName).anyMatch("jboss-remoting-5.0.9.Final.jar"::equals));
        Assertions.assertTrue(artifacts.stream().map(PlainArtifact::getFileName).anyMatch("jandex-2.1.1.Final-sources.jar"::equals));
        Assertions.assertTrue(artifacts.stream().map(PlainArtifact::getFileName).anyMatch("cdi-5.9.5.Final-sources.jar"::equals));
    }

    @Test
    @Order(5)
    public void deleteArtifactTest() {
        Assertions.assertTrue(cacherUtils.deleteArtifact("0d3955b0fed4a2d03d1adc29b3fd7c67"));
        Assertions.assertTrue(cacherUtils.deleteArtifact("8039610bc1401c3c3c21c2fd75707c9b"));
        Assertions.assertTrue(cacherUtils.deleteArtifact("dfe927040dbd33159b61b2c92e0b7ae2"));

        Assertions.assertFalse(cacherUtils.fileExists("0d3955b0fed4a2d03d1adc29b3fd7c67"));
        Assertions.assertFalse(cacherUtils.fileExists("8039610bc1401c3c3c21c2fd75707c9b"));
        Assertions.assertFalse(cacherUtils.fileExists("dfe927040dbd33159b61b2c92e0b7ae2"));
    }


    private void clean() {
        if (preLoadFileLocation != null) {
            try {
                Files.walk(Paths.get(cacherProperties.getArtifactsTmpDir()))
                        .map(Path::toFile)
                        .forEach(file -> file.delete());
                Paths.get(preLoadFileLocation).toFile().delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

