package com.csg.ecard.messagecenter.module.push.sender;

/**
 * 邮件附件内容。
 *
 * @param fileName 附件文件名
 * @param content  附件二进制内容
 */
public record EmailAttachmentResource(String fileName, byte[] content) {
}
