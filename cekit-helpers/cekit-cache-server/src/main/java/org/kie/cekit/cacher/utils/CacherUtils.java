package org.kie.cekit.cacher.utils;

import io.quarkus.scheduler.Scheduled;
import org.kie.cekit.cacher.builds.cr.CRBuildInterceptor;
import org.kie.cekit.cacher.builds.nightly.NightlyBuildUpdatesInterceptor;
import org.kie.cekit.cacher.objects.CacherUploadedFile;
import org.kie.cekit.cacher.objects.PlainArtifact;
import org.kie.cekit.cacher.properties.CacherProperties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.io.BufferedInputStream;
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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class CacherUtils {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Inject
    CacherProperties cacherProperties;

    @Inject
    BuildUtils buildUtils;

    @Inject
    NightlyBuildUpdatesInterceptor nightlyBuildUpdatesInterceptor;

    @Inject
    CRBuildInterceptor crBuildInterceptor;

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
                            if (file.delete()) {
                                log.info("File Deleted --> " + file.getAbsolutePath());
                            }
                        }
                    });
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clean RHPAM and RHDM nightly artifacts.
     * If there is no artifacts persisted, nothing will be done.
     * If there is one newer build date and all of the remaining ones are 3 days older than the most recent one, all
     * of them will be deleted keeping only the most recent .
     * Run once a day.
     */
    @Scheduled(every = "24h", delay = 12, delayUnit = TimeUnit.HOURS)
    public void cleanOldProductNightlyArtifacts() {
        log.fine("Trying to identify the latest nightly build based on filesystem artifacts to delete, this could take a while...");
        try {
            log.fine("Walking through data dir searching for old nightly product builds...");

            Map<File, LocalDate> nightlyBuildArtifacts = new HashMap<>();
            Files.walk(Paths.get(cacherProperties.getCacherArtifactsDir()))
                    .map(Path::toFile)
                    .filter(file -> buildUtils.buildDatePattern.matcher(file.getName()).find())
                    .filter(file -> file.getName().endsWith(".zip"))
                    .forEach(file -> {
                        Matcher test = buildUtils.buildDatePattern.matcher(file.getName());
                        if (test.find()) {
                            LocalDate d = LocalDate.parse(test.group(0), buildUtils.formatter(cacherProperties.getFormattedVersion()));
                            // collect all files that matches the build date pattern
                            nightlyBuildArtifacts.put(file, d);
                        }
                    });

            LocalDate mostRecentNightlyBuild = Collections.max(nightlyBuildArtifacts.values());
            log.fine("Latest nightly build date is -> " + mostRecentNightlyBuild);

            for (Map.Entry<File, LocalDate> entry : nightlyBuildArtifacts.entrySet()) {
                if (entry.getValue().plusDays(3).isBefore(mostRecentNightlyBuild)) {
                    log.fine("File [" + entry.getKey() + " is 3 days older than the latest build date [" + mostRecentNightlyBuild + "]. Deleting...");
                    Files.delete(entry.getKey().toPath());
                }
            }
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
                        new Thread(() -> log.info(fetchFile(finalLine, Optional.empty(), 0))).start();
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
     * Download and persist the given file locally
     *
     * @param url artifact url address
     * @return the result of the operation
     */
    public String fetchFile(String url, Optional<String> type, int crBuild) {

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

        switch (type.orElse("")) {
            case "nightly":
                nightlyBuildUpdatesInterceptor.onFilePersisted(fileName, fileChecksum);
                break;

            case "cr":
                crBuildInterceptor.onFilePersisted(fileName, fileChecksum, crBuild);
                break;

            default:
                log.finest("nothing has to be done.");
        }
        return "File " + fileName + " persisted.";
    }

    /**
     * If the given file exists, return its absolute path
     *
     * @param checksum to be searched
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
     * @param checksum for the given file
     * @return true if there is a directory with the given checksum
     */
    public boolean fileExists(String checksum) {
        return Files.exists(Paths.get(cacherProperties.getCacherArtifactsDir() + "/" + checksum));
    }

    /**
     * Verifies if the given file exists on the persisted files, excludes tmp dir from the search
     *
     * @param fileName of the file to be searched
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
     * Verifies if the given file exists on the persisted files, excludes tmp dir from the search
     *
     * @param fileName to be searched
     * @return List of found {@link PlainArtifact}
     */
    public List<PlainArtifact> getFilesByName(String fileName) {
        Path path = FileSystems.getDefault().getPath(cacherProperties.getCacherArtifactsDir());

        List<PlainArtifact> artifacts = new ArrayList<>();

        try {
            artifacts = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().contains(fileName))
                    .map(p -> {
                        try {
                            return new PlainArtifact(p.getFileName().toString(),
                                                     p.getParent().getFileName().toString(),
                                                     Files.getAttribute(p.toAbsolutePath(), "creationTime", LinkOption.NOFOLLOW_LINKS).toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return new PlainArtifact(p.getFileName().toString(),
                                                     p.getParent().getFileName().toString(), "00");
                        }
                    }).collect(Collectors.toList());
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return artifacts;
    }

    /**
     * Generates the given file checksum
     *
     * @param filePath for the file on the filesystem.
     * @return file checksum
     * @throws IOException for IO exceptions while calculating the md5 checksum
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
                    .map(p -> {
                        try {
                            return new PlainArtifact(p.getFileName().toString(),
                                                     p.getParent().getFileName().toString(),
                                                     Files.getAttribute(p.toAbsolutePath(), "creationTime", LinkOption.NOFOLLOW_LINKS).toString(),
                                                     null, null, null, 0);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }).collect(Collectors.toList());
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return artifacts;
    }

    /**
     * delete artifacts by checksum
     *
     * @param checksum of the artifact to be deleted.
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

    /**
     * @param jarName     the jar file to be searched
     * @param zipFileName source file
     * @return the version from the given .jar file
     */
    public String detectJarVersion(String jarName, String zipFileName) {

        Pattern versionp = Pattern.compile("\\d.\\d{1,2}.\\d");
        Pattern identifierp = Pattern.compile("\\d.\\d{1,2}.\\d.Final-redhat-\\d{5}");
        Optional<File> zipFile;
        try {
            zipFile = Files.walk(Paths.get(cacherProperties.getCacherArtifactsDir()))
                    .map(Path::toFile)
                    .filter(file -> file.getName().equals(zipFileName))
                    .findFirst();
        } catch (final Exception e) {
            e.printStackTrace();
            return "NONE";
        }

        try (FileInputStream fis = new FileInputStream(zipFile.get().getAbsoluteFile());
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream stream = new ZipInputStream(bis)) {

            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".jar") && entry.getName().contains(jarName)) {
                    log.fine("Inspecting file [" + entry.getName() + "]");

                    Matcher identifierm = identifierp.matcher(entry.getName());
                    if (identifierm.find()) {
                        log.fine("Release Version found, regex match is -> " + identifierm.group());
                        return identifierm.group();
                    }

                    Matcher versionm = versionp.matcher(entry.getName());
                    if (versionm.find()) {
                        log.fine("Nightly Version found, regex match is -> " + versionm.group());
                        return versionm.group();
                    }
                }
            }
        } catch (final Exception e) {
            log.warning("Failed to detect jar [" + jarName + "] version: " + e.getMessage());
        }

        return "NONE";
    }

    public String writeFileToDisk(CacherUploadedFile input) {

        String tmpFileLocation = cacherProperties.getArtifactsTmpDir() + "/" + input.fileName;
        String checksum = "";

        try (ReadableByteChannel readableByteChannel = Channels.newChannel(input.file);
             FileOutputStream fos = new FileOutputStream(tmpFileLocation)) {

            fos.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            checksum = md5sum(tmpFileLocation);

            try {
                Files.createDirectory(Paths.get(cacherProperties.getCacherArtifactsDir() + "/" + checksum));
                Files.move(Paths.get(tmpFileLocation),
                           Paths.get(cacherProperties.getCacherArtifactsDir() + "/" + checksum + "/" + input.fileName));
            } catch (FileAlreadyExistsException e) {
                Files.delete(Paths.get(tmpFileLocation));
                return "File " + input.fileName + " already exists";
            }

        } catch (final Exception e) {
            try {
                Files.delete(Paths.get(tmpFileLocation));
                Files.delete(Paths.get(cacherProperties.getCacherArtifactsDir() + "/" + checksum));
            } catch (IOException ex) {
                // ignore
            }
            return e.getMessage();
        }

        return "File persisted, checksum is: " + checksum;
    }
}
