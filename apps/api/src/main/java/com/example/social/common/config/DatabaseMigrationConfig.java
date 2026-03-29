package com.example.social.common.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Bean
    public ApplicationRunner databaseMigrationRunner(
        DataSource dataSource,
        @Value("${spring.flyway.enabled:true}") boolean flywayEnabled,
        @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
        @Value("${spring.flyway.baseline-version:1}") String baselineVersion
    ) {
        return args -> {
            if (!flywayEnabled) {
                LOG.info("Flyway migration disabled by configuration.");
                return;
            }

            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(MigrationVersion.fromVersion(baselineVersion))
                .load();

            var result = flyway.migrate();
            LOG.info(
                "Flyway migration finished. Executed={}, InitialVersion={}, TargetVersion={}",
                result.migrationsExecuted,
                result.initialSchemaVersion,
                result.targetSchemaVersion
            );
        };
    }
}
