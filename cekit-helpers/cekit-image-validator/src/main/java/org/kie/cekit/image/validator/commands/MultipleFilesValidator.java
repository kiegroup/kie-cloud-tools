package org.kie.cekit.image.validator.commands;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class MultipleFilesValidator {
    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    SingleFileValidator sfv;

    public int validate(Path name) {
        List<Path> files;
        try {
            files = Files.walk(name)
                    .filter(Files::isRegularFile)
                    .filter(f -> !f.toString().contains("target"))
                    .filter(f -> f.getFileName().toString().equals("module.yaml") ||
                            f.getFileName().toString().equals("image.yaml") ||
                            f.getFileName().toString().contains("overrides.yaml") ||
                            f.getFileName().toString().contains("content_sets") ||
                            f.getFileName().toString().equals("container.yaml"))
                    .collect(Collectors.toList());

            log.info("Files collected (" + files.size() + "):");
            files.stream().map(f -> f.toString()).forEach(System.out::println);

            log.info("Processing files...");

            for (Path file : files) {
                int returnCode = sfv.validate(file);
                if (returnCode != 0) {
                    return returnCode;
                }
            }

        } catch (IOException e) {
            log.severe(e.getMessage());
            return 5;
        } catch (final Exception e) {
            log.severe(e.getMessage());
            return 6;
        }

        return 0;
    }
}
