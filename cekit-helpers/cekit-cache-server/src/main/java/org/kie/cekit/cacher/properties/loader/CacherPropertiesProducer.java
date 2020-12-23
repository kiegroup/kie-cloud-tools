package org.kie.cekit.cacher.properties.loader;

import org.kie.cekit.cacher.exception.RequiredParameterMissingException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class CacherPropertiesProducer {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private final String PROPERTIES_FILE = "META-INF/resources/cekit-cacher.properties";
    final Properties prop = new Properties();

    @Dependent
    @CacherProperty(name = "")
    public boolean findBotPropertyBoolean(InjectionPoint injectionPoint) {
        String value = getInjectedProp(injectionPoint);
        if (null == value || value.isEmpty()) {
            CacherProperty p = injectionPoint.getAnnotated().getAnnotation(CacherProperty.class);
            log.finest("Property " + p.name() + " is not set, defaulting to false.");
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    @Dependent
    @CacherProperty(name = "")
    public String findBotProperty(InjectionPoint injectionPoint) {
        return getInjectedProp(injectionPoint);
    }

    private String getInjectedProp(InjectionPoint injectionPoint) {
        CacherProperty prop = injectionPoint.getAnnotated().getAnnotation(CacherProperty.class);
        String property = readSysProperty(prop.name());
        log.finest("Injecting Property name: [" + prop.name() + "] value: [" + property + "] required [" + prop.required() + "]");
        if (prop.required() && (null == property) || property == "") {
            throw new RequiredParameterMissingException("The parameter " + prop.name() + " is required!");
        }
        return property;
    }

    /**
     * Read System Properties from
     * - system properties from command line
     * - properties file located on classpath
     * <p>
     * Supports environment variable substitution on the properties file.
     */
    private String readSysProperty(String propName) {
        Pattern pattern = Pattern.compile("\\$\\{.*?\\}");
        String property;

        String value = System.getProperty(propName);
        if (null != value) {
            return value;
        } else {

            try (final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
                prop.load(stream);
                Matcher matcher = pattern.matcher(prop.getProperty(propName));
                if (matcher.find()) {
                    String envVar = prop.getProperty(propName).substring(matcher.start() + 2, matcher.end() - 1);
                    property = System.getenv(envVar);
                    log.finest("Read environment variable [" + envVar + "] from properties file, new value [" + property + "]");
                    log.finest("Command line System properties takes precedence.");
                    return property;
                } else {
                    return System.getProperty(propName, prop.getProperty(propName));
                }
            } catch (final Exception e) {
                log.warning("Loading props file failed: " + e.getMessage());
                return System.getProperty(propName, prop.getProperty(propName));
            }
        }
    }
}