package com.csg.ecard.messagecenter.module.push.sender;

import com.csg.ecard.messagecenter.module.push.dto.EmailFileDTO;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 默认附件解析器；真实文件中心接入前不返回模拟附件。
 */
@Component
public class DefaultEmailAttachmentResolver implements EmailAttachmentResolver {

    @Override
    public Optional<EmailAttachmentResource> resolve(EmailFileDTO file) {
        return Optional.empty();
    }
}
