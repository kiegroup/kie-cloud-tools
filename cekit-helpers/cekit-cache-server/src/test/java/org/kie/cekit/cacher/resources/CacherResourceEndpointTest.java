package org.kie.cekit.cacher.resources;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.kie.cekit.cacher.utils.CacherUtils;

import javax.inject.Inject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class CacherResourceEndpointTest {

    @Inject
    CacherUtils cacherUtils;

    String url = "https://repo1.maven.org/maven2/io/quarkus/quarkus-arc/0.15.0/quarkus-arc-0.15.0-javadoc.jar";
    final String wrongUrl = "test.testing";

    @Test
    @Order(1)
    public void testCacherEndpoints() throws URISyntaxException, UnsupportedEncodingException {
        URI uri = new URI(url);

        String protocol = uri.getScheme();
        String host = uri.getHost();
        String encodedPath = URLEncoder.encode(uri.getPath(), "UTF-8");

        String uncodedUrl = String.format("%s%s%s%s", protocol, ":%2F%2F", host, encodedPath);

        given()
                .when().post("/resource/fetch/" + uncodedUrl)
                .then()
                .statusCode(200)
                .body(is("File quarkus-arc-0.15.0-javadoc.jar persisted."));

        given()
                .when().post("/resource/fetch/" + uncodedUrl)
                .then()
                .statusCode(200)
                .body(is("File quarkus-arc-0.15.0-javadoc.jar already exists."));

        given()
                .when().post("/resource/fetch/" + wrongUrl)
                .then()
                .statusCode(200)
                .body(is("Failed to fetch artifact, please check the url and try again"));

        given()
                .when().get("/resource/query/quarkus")
                .then()
                .statusCode(200)
                .body(containsString("[{\"checksum\":\"bccc8db65cb5eae41084222c82a6131c\",\"crBuild\":0,\"fileName\":\"quarkus-arc-0.15.0-javadoc.jar\",\"timestamp\""));

        given()
                .when().get("/resource/query/test")
                .then()
                .statusCode(204);

        Assertions.assertTrue(cacherUtils.fileExists("bccc8db65cb5eae41084222c82a6131c"));

        List<String> checksums = Arrays.asList("bccc8db65cb5eae41084222c82a6131c");

        given()
                .when()
                .body(checksums)
                .contentType("application/json")
                .delete("/resource")
                .then()
                .statusCode(200)
                .body(containsString("File bccc8db65cb5eae41084222c82a6131c deleted."));

        Assertions.assertFalse(cacherUtils.fileExists("bccc8db65cb5eae41084222c82a6131c"));
    }

    @Test
    @Order(2)
    public void testUploadFile() {
        given()
                .header(new Header("content-type", "multipart/form-data"))
                .multiPart("file", new File(getClass().getClassLoader().getResource("pre-load-test.txt").getFile()))
                .formParam("fileName", "pre-load-test.txt")
                .when().post("/resource/file/upload")
                .then()
                .statusCode(200)
                .body(containsString("File persisted, checksum is: 1b983479e22fedd1b6dde4cb1fb83d2d"));

        Assertions.assertTrue(cacherUtils.fileExists("1b983479e22fedd1b6dde4cb1fb83d2d"));

        given()
                .header(new Header("content-type", "multipart/form-data"))
                .multiPart("file", new File(getClass().getClassLoader().getResource("pre-load-test.txt").getFile()))
                .formParam("fileName", "pre-load-test.txt")
                .when().post("/resource/file/upload")
                .then()
                .statusCode(200)
                .body(containsString("File pre-load-test.txt already exists"));

        cacherUtils.deleteArtifact("1b983479e22fedd1b6dde4cb1fb83d2d");
    }

}