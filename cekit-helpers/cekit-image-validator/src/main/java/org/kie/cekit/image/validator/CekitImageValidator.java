package org.kie.cekit.image.validator;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.kie.cekit.image.validator.commands.MultipleFilesValidator;
import org.kie.cekit.image.validator.commands.SingleFileValidator;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

@QuarkusMain
public class CekitImageValidator implements QuarkusApplication {

    private static Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    SingleFileValidator sfv;
    @Inject
    MultipleFilesValidator mfv;

    /**
     * Main method
     *
     * @param args
     * @return result code
     * Result codes:
     *  0 - success
     *  1 - File validation failed
     *  5 - IOException errors
     *  6 - Generic Exceptions
     *  10 - provided file not found
     *  15 - Insufficient permissions
     */
    @Override
    public int run(String... args) {
        int returnCode = 0;
        Path fileToAnalyze;

        if (args.length > 0) {
            fileToAnalyze = FileSystems.getDefault().getPath(args[0]);

            if (!Files.exists(fileToAnalyze)) {
                log.severe("File not found: " + args[0]);
                return 10;

            } else if (!Files.isReadable(fileToAnalyze)) {
                log.severe("No permission to read the provided file: " + args[0]);
                return 15;

            } else if (Files.isDirectory(fileToAnalyze)) {
                log.info("Provided path is a directory, a recursive search of files will be performed..");
                return mfv.validate(fileToAnalyze);

            } else if (Files.isRegularFile(fileToAnalyze)) {
                log.info("Provided param is a regular file, analyzing it...");
                return sfv.validate(fileToAnalyze);
            }
        } else {
            log.warning("App requires at least one parameter, can be a single image.yaml file or a directory.");
        }

        return returnCode;
    }
}
