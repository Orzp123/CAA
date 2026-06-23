package com.caa.auth.repository;

import com.caa.auth.model.SystemConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SystemConfigRepositoryTest {

    @Autowired
    private SystemConfigRepository repository;

    @Test
    void saveAndFindById() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("site.name");
        config.setConfigValue("CAA Platform");
        config.setDescription("Platform display name");

        repository.save(config);

        Optional<SystemConfig> found = repository.findById("site.name");
        assertThat(found).isPresent();
        assertThat(found.get().getConfigValue()).isEqualTo("CAA Platform");
        assertThat(found.get().getDescription()).isEqualTo("Platform display name");
    }

    @Test
    void saveWithNullDescription() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("feature.flag.x");
        config.setConfigValue("true");

        repository.save(config);

        Optional<SystemConfig> found = repository.findById("feature.flag.x");
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isNull();
    }

    @Test
    void updateConfigValue() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("max.login.attempts");
        config.setConfigValue("5");
        repository.save(config);

        SystemConfig saved = repository.findById("max.login.attempts").orElseThrow();
        saved.setConfigValue("10");
        repository.save(saved);

        SystemConfig updated = repository.findById("max.login.attempts").orElseThrow();
        assertThat(updated.getConfigValue()).isEqualTo("10");
    }

    @Test
    void deleteById() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("temp.key");
        config.setConfigValue("temp.value");
        repository.save(config);

        repository.deleteById("temp.key");

        assertThat(repository.findById("temp.key")).isEmpty();
    }

    @Test
    void findByIdNotFound() {
        Optional<SystemConfig> result = repository.findById("nonexistent.key");
        assertThat(result).isEmpty();
    }
}
