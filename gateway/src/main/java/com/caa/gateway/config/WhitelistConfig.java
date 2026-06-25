package com.caa.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Whitelist configuration loaded from gateway.whitelist property.
 * Supports Nacos hot-reload via @RefreshScope.
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "gateway")
public class WhitelistConfig {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private List<String> whitelist = new ArrayList<>();

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    /**
     * Returns true if the given path matches any pattern in the whitelist.
     */
    public boolean isWhitelisted(String path) {
        if (path == null) {
            return false;
        }
        for (String pattern : whitelist) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
