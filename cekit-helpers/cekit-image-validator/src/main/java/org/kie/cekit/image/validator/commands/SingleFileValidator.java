package org.kie.cekit.image.validator.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.kie.cekit.image.descriptors.packages.ContentSets;
import org.kie.cekit.image.descriptors.container.Container;
import org.kie.cekit.image.descriptors.image.Image;
import org.kie.cekit.image.descriptors.module.Module;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class SingleFileValidator {
    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public int validate(Path path) {
        try {
            if (path.getFileName().toString().contains("image") || path.getFileName().toString().contains("overrides")) {
                log.info("Trying to validate file " + path.toString());

                // rhpam contains artifacts overrides, this file is based on the Module
                if (path.getFileName().toString().equals("artifact-overrides.yaml")) {
                    validate(path, "artifact-overrides", Module.class);
                } else {
                    List<Image> images;
                    try {
                        images = Arrays.asList(validate(path, "image", Image.class));
                    }catch (Exception e) {
                        log.info("Cannot parse as single image. Trying as an array of images ...");
                        images = Arrays.asList(validate(path, "multi images", Image[].class));
                    }
                }
            }

            if (path.getFileName().toString().equals("module.yaml")) {
                validate(path, "module", Module.class);
            }

            if (path.getFileName().toString().equals("container.yaml")) {
                validate(path, "container", Container.class);
            }

            if (path.getFileName().toString().contains("content_sets")) {
                validate(path, "content sets", ContentSets.class);
            }

            return 0;
        } catch (IOException e) {
            log.severe(e.getMessage());
            return 1;
        }
    }

    private <T> T validate(Path path, String fileTypeName, Class<T> valueType) throws IOException {
        log.info("Trying to validate file " + path.toString() + " as " + valueType.getName());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        T value;
        try (InputStream is = Files.newInputStream(path)) {
            value = mapper.readValue(is, valueType);
            log.info("File " + fileTypeName + " [" + path.toString() + "] loaded and validated");
        }
        return value;
    }
}
