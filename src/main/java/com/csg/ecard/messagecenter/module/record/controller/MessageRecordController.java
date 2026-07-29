package com.csg.ecard.messagecenter.module.record.controller;

import com.csg.ecard.messagecenter.common.result.CommonResult;
import com.csg.ecard.messagecenter.module.record.dto.MessageRecordFilterDTO;
import com.csg.ecard.messagecenter.module.record.dto.MessageRecordPageQueryDTO;
import com.csg.ecard.messagecenter.module.record.enums.MessageRecordFilterType;
import com.csg.ecard.messagecenter.module.record.export.MessageRecordExcelExporter;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordExportRow;
import com.csg.ecard.messagecenter.module.record.service.MessageRecordService;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordDetailVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordFilterOptionVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordOverviewVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordPageResult;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordResendLogVO;
import com.csg.ecard.messagecenter.module.record.vo.MessageRecordResendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 消息记录查询、重发和导出接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "消息记录")
@RequestMapping("/api/msg/record")
public class MessageRecordController {

    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MessageRecordService messageRecordService;
    private final MessageRecordExcelExporter excelExporter;

    @GetMapping("/overview")
    @Operation(summary = "消息记录概览",
            description = "按服务器当前日期和记录创建时间统计今日、昨日数量及成功率")
    public CommonResult<MessageRecordOverviewVO> overview() {
        return CommonResult.success(messageRecordService.overview());
    }

    /**
     * 按需查询消息记录筛选项。
     *
     * @param type        筛选项类型编码
     * @param channelType 渠道类型，仅查询渠道选项时生效
     * @return 筛选项列表
     */
    @GetMapping("/filter-options")
    @Operation(summary = "消息记录筛选项查询",
            description = "下拉框首次展开时按需调用；type支持scene、channel、template")
    public CommonResult<List<MessageRecordFilterOptionVO>> filterOptions(
            @Parameter(description = "筛选项类型：scene、channel、template", required = true)
            @RequestParam String type,
            @Parameter(description = "渠道类型：SMS、EMAIL、ELINK、IN_APP；仅type=channel时生效")
            @RequestParam(required = false) String channelType) {
        return CommonResult.success(
                messageRecordService.filterOptions(
                        MessageRecordFilterType.fromCode(type), channelType));
    }

    @GetMapping("/list")
    @Operation(summary = "消息记录分页查询",
            description = "ID对外按字符串返回；sendTime为空表示异步消息尚未真正发送；"
                    + "messageContent为模板渲染后实际提交渠道发送器的最终推送内容；"
                    + "priority为消息业务优先级HIGH、NORMAL、LOW，与渠道匹配优先级无关；"
                    + "callType仅支持SYNC、ASYNC，表示消息最初由同步或异步推送入口进入，重试和手动重发不会改变")
    public CommonResult<MessageRecordPageResult> list(
            @ParameterObject @Valid MessageRecordPageQueryDTO query) {
        return CommonResult.success(messageRecordService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "消息记录详情",
            description = "返回历史记录中的完整最终推送内容和消息业务优先级；"
                    + "callType仅支持SYNC、ASYNC，与消息优先级和渠道优先级无关；"
                    + "errorStack仅用于技术排障，可能为空且仅详情接口返回；"
                    + "用户姓名和单位名称尚未接入员工中心")
    public CommonResult<MessageRecordDetailVO> detail(
            @Parameter(description = "消息记录ID，对外为字符串") @PathVariable Long id) {
        return CommonResult.success(messageRecordService.detail(id));
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "失败记录手工重发",
            description = "仅FAILED记录可重发；只发送当前记录对应渠道，使用历史记录原始完整messageContent，"
                    + "不重新渲染模板且不进入RabbitMQ自动重试；重发保持原消息业务优先级")
    public CommonResult<MessageRecordResendVO> resend(
            @Parameter(description = "消息记录ID，对外为字符串") @PathVariable Long id) {
        return CommonResult.success(messageRecordService.resend(id));
    }

    @GetMapping("/{id}/resend-logs")
    @Operation(summary = "查询消息记录手动重发日志",
            description = "按重发次数升序返回指定消息记录的每次手动重发结果")
    public CommonResult<List<MessageRecordResendLogVO>> resendLogs(
            @Parameter(description = "消息记录ID，对外为字符串") @PathVariable Long id) {
        return CommonResult.success(messageRecordService.resendLogs(id));
    }

    @GetMapping("/export")
    @Operation(summary = "导出消息记录",
            description = "筛选条件与列表一致，不分页，最大10000条；导出真正的xlsx文件，推送内容来自"
                    + "msg_record.message_content，消息优先级来自msg_record.priority；"
                    + "调用方式来自msg_record.call_type，仅支持SYNC、ASYNC")
    public void export(@ParameterObject @Valid MessageRecordFilterDTO query,
                       HttpServletResponse response) throws IOException {
        List<MessageRecordExportRow> rows = messageRecordService.exportRows(query);
        String fileName = "消息记录_" + FILE_TIME_FORMAT.format(LocalDateTime.now()) + ".xlsx";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"message-records.xlsx\"; filename*=UTF-8''" + encodedName);
        try (OutputStream outputStream = response.getOutputStream()) {
            excelExporter.write(rows, outputStream);
        }
    }
}
