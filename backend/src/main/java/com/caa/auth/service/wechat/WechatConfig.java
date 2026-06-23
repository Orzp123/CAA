package com.caa.auth.service.wechat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WeChat Mini-Program / Official Account OAuth2 configuration.
 * Bound to the {@code wechat.*} namespace in application.yml / Nacos.
 */
@Configuration
@ConfigurationProperties(prefix = "wechat")
public class WechatConfig {

    private String appid;
    private String secret;

    public String getAppid() { return appid; }
    public void setAppid(String appid) { this.appid = appid; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
