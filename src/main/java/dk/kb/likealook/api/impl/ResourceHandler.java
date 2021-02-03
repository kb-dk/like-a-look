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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified file server. Restricted to files directly under given roots: No relative paths, ..-tricks or similar.
 */
public class ResourceHandler {
    private static final Logger log = LoggerFactory.getLogger(ResourceHandler.class);

    private static final String ROOTS_KEY = ".likealook.resources.roots";

    private static final ResourceHandler instance = new ResourceHandler();

    private final Map<String, Path> roots = new HashMap<>();

    public ResourceHandler() {
        if (!ServiceConfig.getConfig().containsKey(ROOTS_KEY)) {
            log.info("Skipping setup of ResourceHandler as '{}' does not exist in configuration", ROOTS_KEY);
            return;
        }

        List<YAML> rootConfigs = ServiceConfig.getConfig().getYAMLList(ROOTS_KEY);
        for (YAML rootConfig: rootConfigs) {
            String name = rootConfig.getString(".name");
            String pathString = rootConfig.getString(".path");
            Path path = Path.of(pathString);
            if (!Files.exists(path)) {
                log.warn("The root '{}' with path '{}' does not exist", name, pathString);
            }
            roots.put(name, path);
        }
        log.info("Created ResourceHandler with " + roots.size() + " roots");
    }

    public static InputStream getResource(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return id.contains("/") ? instance.getQualifiedResource(id) : instance.getDirectResource(id);
    }

    private InputStream getDirectResource(String id) {
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
        Path root = roots.get(tokens[0]);
        if (root == null) {
            throw new NotFoundServiceException("Non-defined group '" + tokens[0] + "' for ID-lookup for '" + id + "'");
        }
        Path file = root.resolve(tokens[1]);
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
}
