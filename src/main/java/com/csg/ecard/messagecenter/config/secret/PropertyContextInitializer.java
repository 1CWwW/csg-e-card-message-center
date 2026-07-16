package com.csg.ecard.messagecenter.config.secret;

import com.csg.ecard.messagecenter.common.utils.EncryptUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解密使用 decrypt(...) 包裹的配置属性。
 */
@Slf4j
public class PropertyContextInitializer extends PropertySecretContextInitializer {

    private static final String SECRET_SUFFIX = "mobile_xy_secret";
    private static final Pattern DECODE_PATTERN = Pattern.compile("decrypt\\((.*?)\\)");
    private static final Map<String, String> LEGACY_ENCRYPTION_KEYS = Map.of(
            "spring.data.redis.password", "spring.redis.password",
            "app.message-send.elink.secret", "elink.secret"
    );

    @Override
    public String decode(String key, String input) {
        if (input == null) {
            return null;
        }
        Matcher matcher = DECODE_PATTERN.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        try {
            String encoded = matcher.group(1);
            String encryptionKey = LEGACY_ENCRYPTION_KEYS.getOrDefault(key, key) + SECRET_SUFFIX;
            String decoded = EncryptUtil.aesDecryptFromHex(encoded, encryptionKey);
            log.info("Decoded encrypted property. item={}", key);
            return decoded;
        } catch (RuntimeException ex) {
            log.error("Failed to decode encrypted property. item={}, cause={}", key, ex.getMessage());
            throw ex;
        }
    }
}
