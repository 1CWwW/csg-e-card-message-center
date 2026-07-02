package com.csg.ecard.messagecenter.module.push.sender;

import com.csg.ecard.messagecenter.module.push.dto.EmailFileDTO;

import java.util.Optional;

/**
 * 邮件附件解析扩展点。
 */
public interface EmailAttachmentResolver {

    /**
     * 根据附件描述获取可发送的附件内容。
     *
     * @param file 附件描述
     * @return 附件内容
     */
    Optional<EmailAttachmentResource> resolve(EmailFileDTO file);
}
