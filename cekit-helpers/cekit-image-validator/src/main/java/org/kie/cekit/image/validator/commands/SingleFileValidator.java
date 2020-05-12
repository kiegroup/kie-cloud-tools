package org.kie.cekit.image.validator.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.kie.cekit.image.descriptors.ContentSets;
import org.kie.cekit.image.descriptors.container.Container;
import org.kie.cekit.image.descriptors.image.Image;
import org.kie.cekit.image.descriptors.module.Modules;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

@ApplicationScoped
public class SingleFileValidator {
    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public int validate(Path name) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try (InputStream is = Files.newInputStream(name)) {

            Image image;
            Modules module;
            ContentSets contentSets;
            Container container;

            if (name.getFileName().toString().contains("image") || name.getFileName().toString().contains("overrides")) {
                log.info("Trying to validate file " + name.getFileName());

                // rhpam contains artifacts overrides, this file is based on the Module
                if (name.getFileName().toString().equals("artifact-overrides.yaml")) {
                    module = mapper.readValue(is, Modules.class);
                    log.info("Artifact-overrides file [" + name.toString() + "] loaded and validated");

                } else {
                    image = mapper.readValue(is, Image.class);
                    if (null == image.getName()) {
                        log.info("Image file [" + name.toString() + "] loaded and validated");
                    } else {
                        log.info("Image file [" + name.toString() + "] loaded and validated: " + image.getName());
                    }
                }
            }

            if (name.getFileName().toString().equals("module.yaml")) {
                log.info("Trying to validate file " + name.getFileName());
                module = mapper.readValue(is, Modules.class);
                log.info("Module file [" + name.toString() + "] loaded and validated: " + module.getName());
            }

            if (name.getFileName().toString().equals("container.yaml")) {
                log.info("Trying to validate file " + name.getFileName());
                container = mapper.readValue(is, Container.class);
                log.info("Container file [" + name.toString() + "] loaded and validated");
            }

            if (name.getFileName().toString().contains("content_sets")) {
                log.info("Trying to validate file " + name.getFileName());
                contentSets = mapper.readValue(is, ContentSets.class);
                log.info("Content sets file [" + name.toString() + "] loaded and validated");
            }

            return 0;
        } catch (IOException e) {
            log.severe(e.getMessage());
            return 1;
        }
    }
}
