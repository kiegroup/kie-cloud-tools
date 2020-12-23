package org.kie.cekit.cacher.builds.yaml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.kie.cekit.image.descriptors.module.Module;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class YamlFilesHelper {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * Receives a absolute file path or filename, it will first look on the classpath, if not found, if not found, look at filesystem.
     *
     * @param file {@link String}
     * @return {@link Module}
     */
    public Module load(String file) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file)) {
            return mapper.readValue(stream, Module.class);

        } catch (final Exception e) {
            log.fine("Failed to lookup " + file + " on Thread Context Class loader, trying from filesystem.");
            try (InputStream inputStream = new FileInputStream(file)) {
                return mapper.readValue(inputStream, Module.class);

            } catch (final Exception ex) {
                log.warning("Failed to load yaml file [" + file + "] from classloader and from filesystem");
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Receives a absolute file path or filename, it will first look on the classpath, if not found, look at filesystem.
     *
     * @param file {@link String} file
     * @return raw InputStream
     */
    public List<String> loadRawData(String file) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file)) {
            return IOUtils.readLines(stream, "UTF-8");

        } catch (final Exception e) {
            log.fine("Failed to lookup " + file + " on Thread Context Class loader, trying from filesystem.");
            try (InputStream inputStream = new FileInputStream(file)) {
                return IOUtils.readLines(inputStream, "UTF-8");

            } catch (final Exception ex) {
                log.warning("Failed to load yaml file [" + file + "] from classloader and from filesystem");
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Write the changes on the target yaml file.
     *
     * @param module {@link Module}
     * @param fileDestination {@link String}
     */
    public void writeModule(Module module, String fileDestination) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);

        try {
            mapper.writeValue(new File(fileDestination), module);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}