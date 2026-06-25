package com.caa.auth.service.wechat;

/**
 * Deserialized response from the WeChat OAuth2 token endpoint.
 *
 * <p>WeChat returns a flat JSON object:
 * {@code {"access_token":"...","openid":"...","unionid":"...","errcode":0,"errmsg":"ok"}}
 *
 * <p>On success {@code errcode} is absent or 0; on failure it is a non-zero int
 * and {@code errmsg} carries a description.
 */
public record WechatTokenResult(
        String accessToken,
        String openid,
        String unionid,
        Integer errcode,
        String errmsg
) {
    /** @return true when WeChat returned a usable access_token */
    public boolean isSuccess() {
        return (errcode == null || errcode == 0) && openid != null && !openid.isBlank();
    }
}
