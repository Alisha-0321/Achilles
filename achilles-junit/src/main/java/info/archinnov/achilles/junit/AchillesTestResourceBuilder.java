/*
 * Copyright (C) 2012-2016 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.junit;

import static info.archinnov.achilles.embedded.CassandraEmbeddedConfigParameters.DEFAULT_KEYSPACE_NAME;
import static info.archinnov.achilles.embedded.CassandraEmbeddedConfigParameters.SCRIPT_LOCATIONS;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import com.datastax.driver.core.Cluster;

import info.archinnov.achilles.internals.cache.StatementsCache;
import info.archinnov.achilles.internals.runtime.AbstractManagerFactory;
import info.archinnov.achilles.junit.AchillesTestResource.Steps;
import info.archinnov.achilles.type.TypedMap;

/**
 * Builder class to create an instance of {@link AchillesTestResource}
 * <pre class="code"><code class="java">
 * AchillesTestResourceBuilder
 * .forJunit()
 * .withScript("script1.cql")
 * .withScript("script2.cql")
 * .tablesToTruncate("user", "account") // entityClassesToTruncate(UserEntity.class, AccountEntity.class)
 * .createAndUseKeyspace("unit_test")
 * .
 * ...
 * .build((cluster, statementsCache) -> ManagerFactoryBuilder
 * .builder(cluster)
 * .doForceSchemaCreation(true)
 * .withStatementCache(statementsCache)
 * .withDefaultKeyspaceName(DEFAULT_CASSANDRA_EMBEDDED_KEYSPACE_NAME)
 * .build()
 * );
 * </code></pre>
 */
public class AchillesTestResourceBuilder {

    private Steps cleanupSteps = Steps.BOTH;
    private List<Class<?>> entityClassesToCleanUp = new ArrayList<>();
    private Optional<String> keyspace = Optional.empty();
    private TypedMap cassandraParams = new TypedMap();
    private List<String> scriptLocations = new ArrayList<>();
    private List<String> tablesToTruncate = new ArrayList<>();

    private AchillesTestResourceBuilder() {
    }

    public static AchillesTestResourceBuilder forJunit() {
        return new AchillesTestResourceBuilder();
    }

    /**
     * /**
     * Load an CQL script in the class path and execute it upon initialization
     * of the embedded Cassandra server

     * <br/>

     * Call this method as many times as there are CQL scripts to be executed.
     * <br/>
     * Example:
     * <br/>
     * <pre class="code"><code class="java">
     * AchillesTestResourceBuilder
     * .forJunit()
     * .withScript("script1.cql")
     * .withScript("script2.cql")
     * ...
     * .build(cluster -> ManagerFactoryBuilder
     * .builder(cluster)
     * .doForceSchemaCreation(true)
     * .withDefaultKeyspaceName(DEFAULT_CASSANDRA_EMBEDDED_KEYSPACE_NAME)
     * .build());
     * </code></pre>
     *
     * @param scriptLocation location of the CQL script in the <strong>class path</strong>
     * @return AchillesTestResourceBuilder
     */
    public AchillesTestResourceBuilder withScript(String scriptLocation) {
        scriptLocations.add(scriptLocation);
        return this;
    }

    /**
     * Keyspace name to create
     *
     * @param keyspaceName keyspace name
     * @return AchillesTestResourceBuilder
     */
    public AchillesTestResourceBuilder createAndUseKeyspace(String keyspaceName) {
        this.keyspace = ofNullable(keyspaceName);
        this.keyspace.ifPresent(ks -> this.cassandraParams.put(DEFAULT_KEYSPACE_NAME, ks));
        return this;
    }

    /**
     * Entity classes whose table should be truncated during unit tests
     *
     * @param entityClassesToTruncate list of entity classes whose table should be truncated before and/or after tests
     * @return AchillesTestResourceBuilder
     */
    public AchillesTestResourceBuilder entityClassesToTruncate(Class<?>... entityClassesToTruncate) {
        this.entityClassesToCleanUp.addAll(asList(ofNullable(entityClassesToTruncate).orElse(new Class<?>[]{})));
        return this;
    }

    /**
     * Tables to be truncated during unit tests
     *
     * @param tablesToTruncate list of tables to truncate
     * @return AchillesTestResourceBuilder
     */
    public AchillesTestResourceBuilder tablesToTruncate(String... tablesToTruncate) {
        this.tablesToTruncate.addAll(asList(ofNullable(tablesToTruncate).orElse(new String[]{})));
        return this;
    }

    /**
     * Truncate tables BEFORE each test
     *
     * @return AchillesTestResourceBuilder
     */
    public AchillesTestResourceBuilder truncateBeforeTest() {
        this.cleanupSteps = Steps.BEFORE_TEST;
        return this;
    }

    /**
     * Truncate tables AFTER each test
     *
     * @return AchillesTestResourceBuilder
     */
    public AchillesTestResourceBuilder truncateAfterTest() {
        this.cleanupSteps = Steps.AFTER_TEST;
        return this;
    }

    /**
     * Truncate tables BEFORE and AFTER each test
     *
     * @return AchillesTestResourceBuilder
     */
    public AchillesTestResourceBuilder truncateBeforeAndAfterTest() {
        this.cleanupSteps = Steps.BOTH;
        return this;
    }

    /**
     * Provide a lambda function to build the ManagerFactory instance with the given Cluster object
     *
     * @param managerFactoryBuilder lambda function
     * @return ManagerFactory
     */
    public <T extends AbstractManagerFactory> AchillesTestResource<T> build(BiFunction<Cluster, StatementsCache, T> managerFactoryBuilder) {
        final TypedMap cassandraParams = buildCassandraParams();
        return new AchillesTestResource<>(managerFactoryBuilder, cassandraParams, keyspace, cleanupSteps, tablesToTruncate, entityClassesToCleanUp);
    }

    private TypedMap buildCassandraParams() {
        cassandraParams.put(SCRIPT_LOCATIONS, scriptLocations);
        return cassandraParams;
    }
}
