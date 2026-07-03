package com.csg.ecard.messagecenter.module.statistics.controller;

import com.csg.ecard.messagecenter.common.result.ApiResult;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsExportQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.dto.MessageStatisticsTimeQueryDTO;
import com.csg.ecard.messagecenter.module.statistics.enums.StatisticsDimension;
import com.csg.ecard.messagecenter.module.statistics.export.MessageStatisticsExcelExporter;
import com.csg.ecard.messagecenter.module.statistics.service.MessageStatisticsService;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsChannelVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsOverviewVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsSceneVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTemplateVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTimeVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsUnitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 消息统计报表接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "消息统计报表")
@RequestMapping("/api/msg/statistics")
public class MessageStatisticsController {

    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String COMMON_DESCRIPTION = "统计基于msg_record，一条msg_record计为一次渠道发送；"
            + "统计时间统一使用send_time，startTime和endTime均可选，传入时包含边界；"
            + "只统计SUCCESS和FAILED，PENDING、ACCEPTED不计入完成发送统计；"
            + "successRate和percentage返回0到100之间的数字。";

    private final MessageStatisticsService messageStatisticsService;
    private final MessageStatisticsExcelExporter excelExporter;

    @GetMapping("/overview")
    @Operation(summary = "消息统计总览", description = COMMON_DESCRIPTION)
    public ApiResult<StatisticsOverviewVO> overview(
            @ParameterObject @Valid MessageStatisticsQueryDTO query) {
        return ApiResult.success(messageStatisticsService.overview(query));
    }

    @GetMapping("/time")
    @Operation(summary = "按时间统计消息发送量",
            description = COMMON_DESCRIPTION + "TIME维度支持DAY、WEEK、MONTH，缺失时间段由服务端补齐。")
    public ApiResult<StatisticsTimeVO> time(
            @ParameterObject @Valid MessageStatisticsTimeQueryDTO query) {
        return ApiResult.success(messageStatisticsService.time(query));
    }

    @GetMapping("/channel")
    @Operation(summary = "按渠道类型统计消息发送量",
            description = COMMON_DESCRIPTION + "当前按渠道类型统计，不按具体channelId统计。")
    public ApiResult<StatisticsChannelVO> channel(
            @ParameterObject @Valid MessageStatisticsQueryDTO query) {
        return ApiResult.success(messageStatisticsService.channel(query));
    }

    @GetMapping("/scene")
    @Operation(summary = "按场景统计消息发送量",
            description = COMMON_DESCRIPTION + "场景信息通过一次关联查询获取，已删除或停用场景不影响历史消息统计。")
    public ApiResult<StatisticsSceneVO> scene(
            @ParameterObject @Valid MessageStatisticsQueryDTO query) {
        return ApiResult.success(messageStatisticsService.scene(query));
    }

    @GetMapping("/unit")
    @Operation(summary = "按单位统计消息发送量",
            description = COMMON_DESCRIPTION + "单位统计当前仅基于msg_record.user_org_id，不支持组织层级和子单位，不接入员工中心。")
    public ApiResult<StatisticsUnitVO> unit(
            @ParameterObject @Valid MessageStatisticsQueryDTO query) {
        return ApiResult.success(messageStatisticsService.unit(query));
    }

    @GetMapping("/template")
    @Operation(summary = "按模板统计消息发送量",
            description = COMMON_DESCRIPTION + "模板和场景信息通过一次关联查询获取，已删除或停用数据不影响历史消息统计。")
    public ApiResult<StatisticsTemplateVO> template(
            @ParameterObject @Valid MessageStatisticsQueryDTO query) {
        return ApiResult.success(messageStatisticsService.template(query));
    }

    @GetMapping("/export")
    @Operation(summary = "导出消息统计报表",
            description = COMMON_DESCRIPTION + "导出格式为xlsx；ALL范围会忽略维度筛选但保留可选时间边界，"
                    + "不会导出消息明细、messageContent、sceneParams或errorStack。")
    public void export(@ParameterObject @Valid MessageStatisticsExportQueryDTO query,
                       HttpServletResponse response) throws IOException {
        StatisticsDimension dimension = messageStatisticsService.exportDimension(query);
        Object rows = exportRows(dimension, query);
        String fileName = "消息统计报表_" + dimensionName(dimension) + "_"
                + FILE_TIME_FORMAT.format(LocalDateTime.now()) + ".xlsx";
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"message-statistics.xlsx\"; filename*=UTF-8''" + encodedName);
        try (OutputStream outputStream = response.getOutputStream()) {
            excelExporter.write(dimension, rows, outputStream);
        }
    }

    private Object exportRows(StatisticsDimension dimension, MessageStatisticsExportQueryDTO query) {
        return switch (dimension) {
            case TIME -> messageStatisticsService.time(messageStatisticsService.exportTimeQuery(query)).getItems();
            case CHANNEL -> messageStatisticsService.channel(messageStatisticsService.exportCommonQuery(query)).getItems();
            case SCENE -> messageStatisticsService.scene(messageStatisticsService.exportCommonQuery(query)).getItems();
            case UNIT -> messageStatisticsService.unit(messageStatisticsService.exportCommonQuery(query)).getItems();
            case TEMPLATE -> messageStatisticsService.template(messageStatisticsService.exportCommonQuery(query)).getItems();
        };
    }

    private String dimensionName(StatisticsDimension dimension) {
        return switch (dimension) {
            case TIME -> "时间";
            case CHANNEL -> "渠道";
            case SCENE -> "场景";
            case UNIT -> "单位";
            case TEMPLATE -> "模板";
        };
    }
}
