package org.kie.cekit.cacher.utils;

import io.quarkus.scheduler.Scheduled;
import org.kie.cekit.cacher.builds.github.BuildDateUpdatesInterceptor;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class CacherUtils {

    private Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    CacherProperties cacherProperties;

    @Inject
    BuildDateUpdatesInterceptor buildCallback;

    /**
     * Clean 1 day old files under tmp directory
     */
    @Scheduled(every = "24h", delay = 12, delayUnit = TimeUnit.HOURS)
    public void cleanTmpFiles() {
        long elegibleForDeletion = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000);
        try {
            log.fine("Walking through tmp dir...");
            Files.walk(Paths.get(cacherProperties.getArtifactsTmpDir()))
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (file.lastModified() <= elegibleForDeletion && file.isFile()) {
                            if (file.delete()){
                                log.info("File Deleted --> " + file.getAbsolutePath());
                            }
                        }
                    });
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Pre load artifacts to the cacher using a txt located on filesystem
     * The file location can be configured with this system property:
     * org.kie.cekit.cacher.preload.file
     */
    public void preLoadFromFile() {
        if (Paths.get(cacherProperties.preLoadFileLocation()).toFile().exists() && null != cacherProperties.preLoadFileLocation()) {
            log.info("File " + cacherProperties.preLoadFileLocation() + " found. Starting the pre load.");

            try (BufferedReader br = Files.newBufferedReader(Paths.get(cacherProperties.preLoadFileLocation()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String finalLine = line;
                    if (!fileExistsByNameExcludeTmp(UrlUtils.getFileName(finalLine))) {
                        new Thread(() -> {
                            log.info(fetchFile(finalLine));
                        }).start();
                    }
                }
            } catch (IOException e) {
                log.warning("Failed to read file: " + e.getCause());
            }

        } else {
            log.info("File " + cacherProperties.preLoadFileLocation() + " not found.");
        }
    }

    /**
     * Creates the needed directories structure before starts
     */
    public void startupVerifications() {
        for (String location : cacherProperties.getCacherDirs()) {
            Path path = FileSystems.getDefault().getPath(location);
            if (Files.exists(path.toAbsolutePath())) {
                log.info(String.format("%s directory exists, skipping directory creation.", location));
            } else {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    log.severe(String.format("Failed to create %s directory: $s" + location, e.getLocalizedMessage()));
                }
            }
        }
    }

    /**
     * Download te given file and persit it locally
     *
     * @param url
     * @return the result of the operation
     */
    public String fetchFile(String url) {

        String fileName = UrlUtils.getFileName(url);
        String filePath = cacherProperties.getArtifactsTmpDir() + "/" + fileName;
        String fileChecksum = "";

        if (Files.exists(Paths.get(filePath))) {
            return "File " + fileName + " still being downloaded, skipping...";
        }

        try {
            log.info("Trying to fetch file: " + url);
            ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(url).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            fileChecksum = md5sum(filePath);

            try {
                Files.createDirectory(Paths.get(cacherProperties.getCacherArtifactsDir() + "/" + fileChecksum));
                Files.move(Paths.get(filePath), Paths.get(cacherProperties.getCacherArtifactsDir() + "/" + fileChecksum + "/" + fileName));

            } catch (FileAlreadyExistsException e) {
                try {
                    Files.delete(Paths.get(filePath));
                } catch (IOException ex) {
                    //ignore
                }
                return "File " + fileName + " already exists.";

            } finally {
                readableByteChannel.close();
                fileOutputStream.close();
            }

        } catch (final IOException e) {
            e.printStackTrace();
            try {
                Files.delete(Paths.get(filePath));
                Files.delete(Paths.get(cacherProperties.getCacherArtifactsDir() + "/" + fileChecksum));
            } catch (IOException ex) {
                // ignore
            }
            return e.getMessage();
        }
        buildCallback.onFilePersisted(fileName, fileChecksum);
        return "File " + fileName + " persisted.";
    }

    /**
     * If the given file exists, return its absolute pagh
     *
     * @param checksum
     * @return file's absolute path
     */
    public Optional<Path> getFile(String checksum) {

        Path path = FileSystems.getDefault().getPath(cacherProperties.getCacherArtifactsDir() + "/" + checksum);

        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile).findFirst();
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * verifies if the given checksum exists
     *
     * @param checksum
     * @return true if there is a directory with the given checksum
     */
    public boolean fileExists(String checksum) {
        return Files.exists(Paths.get(cacherProperties.getCacherArtifactsDir() + "/" + checksum));
    }

    /**
     * Verifies if the given file exists on the persisted files, excludes tmp dir from the search
     *
     * @param fileName
     * @return true if the given file exists
     */
    public boolean fileExistsByNameExcludeTmp(String fileName) {
        Path path = FileSystems.getDefault().getPath(cacherProperties.getCacherArtifactsDir());

        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile)
                    .filter(file -> !file.toAbsolutePath().getParent().toString().endsWith("tmp"))
                    .filter(file -> file.getFileName().toString().equals(fileName))
                    .findFirst().isPresent();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generates the given file checksum
     *
     * @param filePath
     * @return file checksum
     * @throws IOException
     */
    private String md5sum(String filePath) throws IOException {
        StringBuilder result = new StringBuilder();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            try (InputStream input = new FileInputStream(filePath)) {
                byte[] block = new byte[4096];
                int length;
                while ((length = input.read(block)) > 0) {
                    messageDigest.update(block, 0, length);
                }

                for (byte b : messageDigest.digest()) {
                    result.append(String.format("%02x", b));
                }

            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        return result.toString();
    }

    /**
     * @return all persisted files including the downloading ones.
     */
    public List<PlainArtifact> getPersistedArtifacts() {
        Path path = FileSystems.getDefault().getPath(cacherProperties.getCacherArtifactsDir());

        List<PlainArtifact> artifacts = new ArrayList<>();

        try {
            artifacts = Files.walk(path).filter(Files::isRegularFile)
                    .map(p -> new PlainArtifact(p.getFileName().toString(), p.getParent().getFileName().toString(), null, null, null))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return artifacts;
    }

    /**
     * delete artifacts by checksum
     *
     * @param checksum
     * @return true if delete, otherwise return false.
     */
    public boolean deleteArtifact(String checksum) {
        try {
            Path path = FileSystems.getDefault().getPath(cacherProperties.getCacherArtifactsDir() + "/" + checksum);
            try {
                Files.walk(path).map(Path::toFile)
                        .peek(f -> log.fine("Deleting " + f))
                        .forEach(File::delete);
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
