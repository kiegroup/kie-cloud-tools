package org.kie.cekit.cacher.properties;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class CacherPropertiesTest {

    @Inject
    CacherProperties cacherProperties;

    @Test
    public void verifyEmptyShortenedVersion() {
        Assertions.assertEquals("7.8", cacherProperties.shortenedVersion(""));
    }

    @Test
    public void verifyNullShortenedVersion() {
        Assertions.assertEquals("7.8", cacherProperties.shortenedVersion(null));
    }

    @Test
    public void verifyCustomShortenedVersion() {
        Assertions.assertEquals("7.9", cacherProperties.shortenedVersion("7.9"));
    }
}
