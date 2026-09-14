package io.github.johneliud.api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthorizationFilterTest {

    private final AuthorizationFilter filter = new AuthorizationFilter();

    @Test
    @DisplayName("Config accepts required role via shortcut field order")
    void configAcceptsRequiredRole() {
        AuthorizationFilter.Config config = new AuthorizationFilter.Config();
        config.setRequiredRole("ADMIN");

        assertThat(config.getRequiredRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Shortcut field order contains requiredRole")
    void shortcutFieldOrderContainsRequiredRole() {
        List<String> order = filter.shortcutFieldOrder();
        assertThat(order).containsExactly("requiredRole");
    }

    @Test
    @DisplayName("Filter factory creates config class")
    void filterFactoryCreatesConfig() {
        assertThat(filter.getConfigClass()).isEqualTo(AuthorizationFilter.Config.class);
    }
}
