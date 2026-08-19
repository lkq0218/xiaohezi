package com.xiaohezi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    String mode,
    String username,
    String password,
    String token,
    List<String> allowedOrigins
) {
}
