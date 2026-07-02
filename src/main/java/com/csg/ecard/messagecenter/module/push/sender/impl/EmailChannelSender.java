package com.csg.ecard.messagecenter.module.push.sender.impl;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.config.message.MessageSendProperties;
import com.csg.ecard.messagecenter.module.push.dto.EmailFileDTO;
import com.csg.ecard.messagecenter.module.push.sender.BatchChannelSender;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendRequest;
import com.csg.ecard.messagecenter.module.push.sender.ChannelSendResult;
import com.csg.ecard.messagecenter.module.push.sender.EmailAttachmentResolver;
import com.csg.ecard.messagecenter.module.push.sender.EmailAttachmentResource;
import com.csg.ecard.messagecenter.module.push.sender.MessageSendInfo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * 邮件发送适配器。
 */
@Component
public class EmailChannelSender implements BatchChannelSender {

    private final MessageSendProperties properties;
    private final EmailAttachmentResolver attachmentResolver;

    public EmailChannelSender(MessageSendProperties properties, EmailAttachmentResolver attachmentResolver) {
        this.properties = properties;
        this.attachmentResolver = attachmentResolver;
    }

    @Override
    public String channelType() {
        return ChannelType.EMAIL.getCode();
    }

    @Override
    public ChannelSendResult send(ChannelSendRequest request) {
        return sendBatch(List.of(request));
    }

    @Override
    public ChannelSendResult sendBatch(List<ChannelSendRequest> requests) {
        if (CollectionUtils.isEmpty(requests)) {
            return ChannelSendResult.succeeded();
        }
        for (List<ChannelSendRequest> group : groupByEmailId(requests).values()) {
            ChannelSendResult result = sendOneEmail(group);
            if (!result.success()) {
                return result;
            }
        }
        return ChannelSendResult.succeeded();
    }

    private Map<String, List<ChannelSendRequest>> groupByEmailId(List<ChannelSendRequest> requests) {
        return requests.stream()
                .filter(Objects::nonNull)
                .collect(LinkedHashMap::new,
                        (map, request) -> map.computeIfAbsent(groupKey(request.sendInfo()), key -> new java.util.ArrayList<>())
                                .add(request),
                        Map::putAll);
    }

    private String groupKey(MessageSendInfo info) {
        if (StringUtils.hasText(info.getEmailId())) {
            return info.getEmailId();
        }
        return StringUtils.hasText(info.getMsgInfoId()) ? info.getMsgInfoId() : info.getMsgId();
    }

    private ChannelSendResult sendOneEmail(List<ChannelSendRequest> requests) {
        MessageSendInfo first = requests.get(0).sendInfo();
        MessageSendProperties.Email email = properties.getEmail();
        EmailAccount account = resolveAccount(first, email);
        List<String> toEmails = requests.stream()
                .map(ChannelSendRequest::sendInfo)
                .map(MessageSendInfo::getReceiveEmail)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (toEmails.isEmpty()) {
            return ChannelSendResult.failedNonRetryable("邮件接收邮箱不能为空");
        }
        String configError = validateConfig(account);
        if (configError != null) {
            return ChannelSendResult.failedNonRetryable(configError);
        }

        try {
            JavaMailSenderImpl mailSender = buildMailSender(account, email);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(account.username());
            helper.setTo(toEmails.toArray(String[]::new));
            List<String> copyEmails = requests.stream()
                    .map(ChannelSendRequest::sendInfo)
                    .map(MessageSendInfo::getCopyEmails)
                    .filter(list -> !CollectionUtils.isEmpty(list))
                    .flatMap(List::stream)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();
            if (!copyEmails.isEmpty()) {
                helper.setCc(copyEmails.toArray(String[]::new));
            }
            helper.setSubject(StringUtils.hasText(first.getTitle()) ? first.getTitle() : "");
            helper.setText(first.getContent(), true);
            addAttachments(helper, first.getFiles());
            mailSender.send(message);
            return ChannelSendResult.succeeded();
        } catch (MailException | MessagingException ex) {
            return ChannelSendResult.failed("邮件发送失败：" + ex.getMessage());
        }
    }

    private EmailAccount resolveAccount(MessageSendInfo info, MessageSendProperties.Email email) {
        EmailServerAddress address = parseEmailServer(firstText(info.getSenderEmailUrl(), email.getHost()));
        return new EmailAccount(
                address.host(),
                address.port() == null ? email.getPort() : address.port(),
                firstText(info.getSenderEmail(), email.getUsername()),
                firstText(info.getSenderEmailPassword(), email.getPassword()));
    }

    private EmailServerAddress parseEmailServer(String value) {
        if (!StringUtils.hasText(value)) {
            return new EmailServerAddress(null, null);
        }
        String trimmed = value.trim();
        URI uri = parseUri(trimmed);
        if (uri != null && StringUtils.hasText(uri.getHost())) {
            return new EmailServerAddress(uri.getHost(), uri.getPort() < 0 ? null : uri.getPort());
        }
        int separator = trimmed.lastIndexOf(':');
        if (separator > 0 && separator < trimmed.length() - 1) {
            String portText = trimmed.substring(separator + 1);
            try {
                return new EmailServerAddress(trimmed.substring(0, separator), Integer.valueOf(portText));
            } catch (NumberFormatException ignored) {
                return new EmailServerAddress(trimmed, null);
            }
        }
        return new EmailServerAddress(trimmed, null);
    }

    private URI parseUri(String value) {
        if (!value.contains("://")) {
            return null;
        }
        try {
            return new URI(value);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private void addAttachments(MimeMessageHelper helper, List<EmailFileDTO> files) throws MessagingException {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        for (EmailFileDTO file : files) {
            if (file == null || !StringUtils.hasText(file.getFileId())) {
                throw new MessagingException("邮件附件fileId不能为空");
            }
            EmailAttachmentResource resource = attachmentResolver.resolve(file)
                    .orElseThrow(() -> new MessagingException("邮件附件未找到或文件服务未接入：" + file.getFileId()));
            if (resource.content() == null || resource.content().length == 0) {
                throw new MessagingException("邮件附件内容为空：" + file.getFileId());
            }
            String fileName = StringUtils.hasText(file.getFileName()) ? file.getFileName() : resource.fileName();
            if (!StringUtils.hasText(fileName)) {
                throw new MessagingException("邮件附件文件名不能为空：" + file.getFileId());
            }
            helper.addAttachment(fileName, new ByteArrayResource(resource.content()));
        }
    }

    private String validateConfig(EmailAccount account) {
        if (!StringUtils.hasText(account.host())) {
            return "邮件服务器地址未配置";
        }
        if (account.port() == null) {
            return "邮件服务器端口未配置";
        }
        if (!StringUtils.hasText(account.username())) {
            return "发件邮箱未配置";
        }
        if (!StringUtils.hasText(account.password())) {
            return "发件邮箱密码未配置";
        }
        return null;
    }

    private JavaMailSenderImpl buildMailSender(EmailAccount account, MessageSendProperties.Email email) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.host());
        sender.setPort(account.port());
        sender.setUsername(account.username());
        sender.setPassword(account.password());
        sender.setProtocol(email.getProtocol());
        sender.setDefaultEncoding("UTF-8");

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", String.valueOf(email.isAuth()));
        mailProperties.put("mail.smtp.starttls.enable", String.valueOf(email.isStarttlsEnable()));
        mailProperties.put("mail.smtp.connectiontimeout", String.valueOf(millis(email.getConnectTimeout())));
        mailProperties.put("mail.smtp.timeout", String.valueOf(millis(email.getReadTimeout())));
        return sender;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : second;
    }

    private long millis(Duration duration) {
        return duration == null ? 0 : duration.toMillis();
    }

    private record EmailAccount(String host, Integer port, String username, String password) {
    }

    private record EmailServerAddress(String host, Integer port) {
    }
}
