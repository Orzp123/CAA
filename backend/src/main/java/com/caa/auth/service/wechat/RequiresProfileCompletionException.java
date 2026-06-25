package com.caa.auth.service.wechat;

/**
 * Thrown by {@link WechatOAuth2Service#findOrCreateAccount} when a WeChat
 * openid has no linked account yet and the user must complete their profile
 * (student number, name, account type) before a full account is created.
 *
 * <p>The caller should respond with an HTTP 202 / temp-token flow so the
 * frontend can collect the missing fields.
 */
public class RequiresProfileCompletionException extends RuntimeException {

    private final String openid;

    public RequiresProfileCompletionException(String openid) {
        super("No account linked to WeChat openid: " + openid
                + " — profile completion required");
        this.openid = openid;
    }

    public String getOpenid() {
        return openid;
    }
}
