/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.likealook.api.impl;

import dk.kb.likealook.config.ServiceConfig;
import dk.kb.util.yaml.YAML;
import dk.kb.webservice.exception.InternalServiceException;
import dk.kb.webservice.exception.InvalidArgumentServiceException;
import dk.kb.webservice.exception.NotFoundServiceException;
import org.apache.log4j.lf5.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simplified file server. Restricted to files directly under given roots: No relative paths, ..-tricks or similar.
 */
public class ResourceHandler {
    private static final Logger log = LoggerFactory.getLogger(ResourceHandler.class);

    private static final String ROOTS_KEY = ".likealook.resources.roots";

    public static final String RESOURCE_URL_PREFIX_KEY = ".likealook.resources.urlprefix";
    public static final String RESOURCE_URL_PREFIX_DEFAULT = "/like-a-look/api/resource/";

    private static final String EPHEMERAL_TIMEOUT_KEY = ".likealook.resources.ephemeral.timeout";
    private static final int EPHEMERAL_TIMEOUT_DEFAULT = 5*60;
    private static final String EPHEMERAL_ENTRIES_KEY = ".likealook.resources.ephemeral.entries";
    private static final int EPHEMERAL_ENTRIES_DEFAULT = 100;

    public static final String EPHEMERAL = "ephemeral"; // Special collection with temporary content

    private static final ResourceHandler instance = new ResourceHandler();

    private final Map<String, Path> roots = new HashMap<>();
    private final String resourceURLPrefix;

    private final Map<String, Ephemeral> ephemerals;
    private final int ephemeralMaxAgeSeconds;
    private final int ephemeralMaxEntries;

    public ResourceHandler() {
        YAML conf = ServiceConfig.getConfig();

        if (ServiceConfig.getConfig().containsKey(ROOTS_KEY)) {
            List<YAML> rootConfigs = conf.getYAMLList(ROOTS_KEY);
            for (YAML rootConfig : rootConfigs) {
                String name = rootConfig.getString(".name");
                String pathString = rootConfig.getString(".path");
                Path path = Path.of(pathString);
                if (!Files.exists(path)) {
                    log.warn("The root '{}' with path '{}' does not exist", name, pathString);
                }
                roots.put(name, path);
            }
        }
        resourceURLPrefix = conf.getString(RESOURCE_URL_PREFIX_KEY, RESOURCE_URL_PREFIX_DEFAULT);

        ephemeralMaxAgeSeconds = conf.getInteger(EPHEMERAL_TIMEOUT_KEY, EPHEMERAL_TIMEOUT_DEFAULT);
        ephemeralMaxEntries = conf.getInteger(EPHEMERAL_ENTRIES_KEY, EPHEMERAL_ENTRIES_DEFAULT);
        ephemerals = new EphemeralCache(ephemeralMaxAgeSeconds, ephemeralMaxEntries);

        log.info("Created ResourceHandler with " + roots.size() + " roots");
    }

    public static InputStream getResource(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return id.contains("/") ? instance.getQualifiedResource(id) : instance.getUnqualifiedResource(id);
    }

    /**
     * Return an URL for the given resource, expanded using the urlprefix from the configuration.
     * @param id the ID for a resource.
     * @return the expanded URL for the resources. This should ideally be an absolute URL.
     */
    public static String getResourceURL(String id) {
        return instance.resourceURLPrefix .endsWith("/") ?
                instance.resourceURLPrefix + id :
                instance.resourceURLPrefix + "/" + id;
    }

    /**
     * Create and store an ephemeral entry with the given content.
     * @param content the content for the ephemeral entry.
     * @return a generated ID for the entry (an UUID). This can be used with {@code getResource(ephemeral/id)}.
     */
    public static String createEphemeral(InputStream content) throws IOException {
        if (instance.ephemeralMaxAgeSeconds <= 0 || instance.ephemeralMaxEntries == 0) {
            return "EphemeralsNotEnabled";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamUtils.copy(content, bos);
        return createEphemeral(bos.toByteArray());
    }

    /**
     * Create and store an ephemeral entry with the given content.
     * @param content the content for the ephemeral entry.
     * @param id the ID for the entry - this can be used with {@code getResource(ephemeral/id)}.
     * @return a generated ID for the entry (an UUID). This can be used with {@code getResource(ephemeral/id)}.
     * @return the given id or "EphemeralsNotEnabled" if ephemerals are not enabled.
     */
    public static String createEphemeral(String id, InputStream content) throws IOException {
        if (instance.ephemeralMaxAgeSeconds <= 0 || instance.ephemeralMaxEntries == 0) {
            return "EphemeralsNotEnabled";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamUtils.copy(content, bos);
        return createEphemeral(id, bos.toByteArray());
    }

    /**
     * Create and store an ephemeral entry with the given content.
     * @param content the content for the ephemeral entry.
     * @return a generated ID for the entry (an UUID). This can be used with {@code getResource(ephemeral/id)}.
     */
    public static String createEphemeral(byte[] content) {
        if (instance.ephemeralMaxAgeSeconds <= 0 || instance.ephemeralMaxEntries == 0) {
            return "EphemeralsNotEnabled";
        }
        Ephemeral e = new Ephemeral(content);
        instance.ephemerals.put(e.getId(), e);
        return e.getId();
    }

    /**
     * Create and store an ephemeral entry with the given id and content.
     * @param id the ID for the entry - this can be used with {@code getResource(ephemeral/id)}.
     * @param content the content for the ephemeral entry.
     * @return the given id or "EphemeralsNotEnabled" if ephemerals are not enabled.
     */
    public static String createEphemeral(String id, byte[] content) {
        if (instance.ephemeralMaxAgeSeconds <= 0 || instance.ephemeralMaxEntries == 0) {
            return "EphemeralsNotEnabled";
        }
        Ephemeral e = new Ephemeral(id, content);
        instance.ephemerals.put(e.getId(), e);
        return id;
    }

    /**
     * @param collectionName a root.
     * @return true if the name of one of the roots matches collectionName.
     */
    public static boolean hasCollection(String collectionName) {
        return instance.roots.containsKey(collectionName);
    }

    /**
     * True if the resource is available.
     * @param collection the collection to check for the resource.
     * @param resource the resource.
     * @return true if the resource is available.
     */
    public static boolean hasResource(String collection, String resource) {
        if (EPHEMERAL.equals(collection)) {
            return instance.ephemerals.containsKey(resource);
        }

        Path root = instance.roots.get(collection);
        return root != null && Files.isReadable(root.resolve(resource));
    }

    /**
     * Retrieve the ephemeral resource. Throws HTTP service exceptions if the resource could not be located.
     * @param ephemeralID the ID of the ephemeral.
     * @return the reseource.
     */
    public static Ephemeral getEphemeral(String ephemeralID) {
        return instance.ephemerals.get(ephemeralID);
    }

    private InputStream getUnqualifiedResource(String id) {
        if (ephemerals.containsKey(id)) { // Theoretically it could become too old between the check and the retrieval
            return new ByteArrayInputStream(ephemerals.get(id).content);
        }

        for (Path root: roots.values()) {
            Path file = root.resolve(id);
            if (Files.exists(file)) {
                try {
                    return Files.newInputStream(file);
                } catch (IOException e) {
                    log.warn("Exception trying to stream file '{}' for ID '{}'", file, id, e);
                    throw new InternalServiceException("Exception trying to stream '" + id + "'");
                }
            }
        }
        throw new NotFoundServiceException("Unable to locate '" + id + "' in any resource group");
    }

    private InputStream getQualifiedResource(String id) {
        String[] tokens = id.split("/");
        if (tokens.length > 2) {
            throw new InvalidArgumentServiceException("ID '" + id + "' contains more than 1 slash (/)");
        }
        return getResource(tokens[0], tokens[1]);
    }

    private InputStream getResource(String collection, String resource) {
        // Ephemerals are special case
        if (EPHEMERAL.equals(collection)) {
            return new ByteArrayInputStream(ephemerals.get(resource).getContent());
        }
        final String id = collection + "/" + resource;

        // Resolve file system content
        Path root = roots.get(collection);
        if (root == null) {
            throw new NotFoundServiceException("Non-defined group '" + collection + "' for ID-lookup for '" + id + "'");
        }
        Path file = root.resolve(resource);
        if (!Files.exists(file)) {
            throw new NotFoundServiceException("Unable to resolve resource '" + id + "'");
        }
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            log.warn("Exception trying to stream file '{}' for ID '{}'", file, id, e);
            throw new InternalServiceException("Exception trying to stream '" + id + "'");
        }
    }

    /**
     * Holds ephemeral data with a creation time.
     */
    public static class Ephemeral {
        private final String id;
        private final Instant creationTime;
        private final byte[] content;

        /**
         * Create an Ephemeral with a random ID (UUID).
         * @param content the content to store temporarily.
         */
        public Ephemeral(byte[] content) {
            this(UUID.randomUUID().toString(), content);
        }

        /**
         * Create an Ephemeral with the given ID.
         * @param id used for later retrieval of the content.
         * @param content the content to store temporarily.
         */
        public Ephemeral(String id, byte[] content) {
            this.id = id;
            this.creationTime = Instant.now();
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public Instant getCreationTime() {
            return creationTime;
        }

        public byte[] getContent() {
            return content;
        }

        public boolean isTooOld(int maxAgeSeconds) {
            return creationTime.plusSeconds(maxAgeSeconds).isBefore(Instant.now());
        }
    }

    /**
     * Holds Ephemerals and ensures that {@link LinkedHashMap#get(Object)} does not deliver entries that are too old.
     */
    private static class EphemeralCache extends LinkedHashMap<String, Ephemeral> {
        private final int maxSize;
        private final int maxAgeSeconds;

        public EphemeralCache(int maxAgeSeconds,int maxSize) {
            this.maxSize = maxSize;
            this.maxAgeSeconds = maxAgeSeconds;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Ephemeral> eldest) {
            return size() > maxSize || eldest.getValue().isTooOld(maxAgeSeconds);
        }

        @Override
        public Ephemeral get(Object key) {
            Ephemeral e = super.get(key);
            if (e == null) {
                throw new NotFoundServiceException("The ephemeral '" + key + "' could not be located");
            }
            if (e.isTooOld(maxAgeSeconds)) {
                remove(key);
                throw new NotFoundServiceException("The ephemeral '" + key + "' was expired");
            }
            return e;
        }

        @Override
        public boolean containsKey(Object key) {
            Ephemeral e = super.get(key);
            if (e == null) {
                return false;
            }
            if (e.isTooOld(maxAgeSeconds)) {
                remove(key);
                return false;
            }
            return true;
        }
    }
}
