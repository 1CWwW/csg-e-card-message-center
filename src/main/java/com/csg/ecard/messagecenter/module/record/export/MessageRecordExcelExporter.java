package com.csg.ecard.messagecenter.module.record.export;

import com.csg.ecard.messagecenter.common.enums.ChannelType;
import com.csg.ecard.messagecenter.common.enums.MessageCallType;
import com.csg.ecard.messagecenter.common.enums.MessagePriority;
import com.csg.ecard.messagecenter.module.push.enums.SendStatus;
import com.csg.ecard.messagecenter.module.record.mapper.MessageRecordExportRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 消息记录 Excel 导出器。
 */
@Component
public class MessageRecordExcelExporter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] HEADERS = {
            "消息ID", "业务单据ID", "场景编码", "场景名称", "模板名称",
            "渠道类型", "渠道名称", "用户ID", "用户单位ID", "消息优先级",
            "调用方式", "推送内容", "发送状态", "失败原因", "实际发送时间", "记录创建时间", "场景参数JSON"
    };

    /**
     * 将消息记录写为真正的 xlsx 文件。
     */
    public void write(List<MessageRecordExportRow> rows, OutputStream outputStream) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        try {
            Sheet sheet = workbook.createSheet("消息记录");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            writeHeader(sheet, headerStyle);
            int rowIndex = 1;
            for (MessageRecordExportRow item : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeText(row, 0, item.getMsgId(), textStyle);
                writeText(row, 1, item.getBizId(), textStyle);
                writeText(row, 2, item.getSceneCode(), textStyle);
                writeText(row, 3, item.getSceneName(), textStyle);
                writeText(row, 4, item.getTemplateName(), textStyle);
                writeText(row, 5, channelTypeText(item.getChannelType()), textStyle);
                writeText(row, 6, item.getChannelName(), textStyle);
                writeText(row, 7, item.getUserId(), textStyle);
                writeText(row, 8, item.getUserOrgId(), textStyle);
                writeText(row, 9, priorityText(item.getPriority()), textStyle);
                writeText(row, 10, callTypeText(item.getCallType()), textStyle);
                writeText(row, 11, item.getMessageContent(), textStyle);
                writeText(row, 12, sendStatusText(item.getSendStatus()), textStyle);
                writeText(row, 13, item.getErrorMsg(), textStyle);
                writeText(row, 14, formatTime(item.getSendTime()), textStyle);
                writeText(row, 15, formatTime(item.getCreatedAt()), textStyle);
                writeText(row, 16, item.getSceneParams(), textStyle);
            }
            sheet.createFreezePane(0, 1);
            setColumnWidths(sheet);
            workbook.write(outputStream);
            outputStream.flush();
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeText(Row row, int column, String value, CellStyle textStyle) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(textStyle);
        cell.setCellValue(safeText(value));
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : DATE_TIME_FORMATTER.format(time);
    }

    private String channelTypeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        try {
            return ChannelType.fromCode(value).getDesc();
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private String sendStatusText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        try {
            return SendStatus.valueOf(value) == SendStatus.SUCCESS ? "发送成功" : "发送失败";
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private String priorityText(String value) {
        if (!StringUtils.hasText(value)) {
            return MessagePriority.NORMAL.getDesc();
        }
        try {
            return MessagePriority.fromCode(value).getDesc();
        } catch (IllegalArgumentException ex) {
            return MessagePriority.NORMAL.getDesc();
        }
    }

    private String callTypeText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("消息记录缺少原始调用方式");
        }
        try {
            return MessageCallType.fromCode(value).getDescription();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("消息记录原始调用方式异常");
        }
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTextStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("@"));
        style.setWrapText(true);
        return style;
    }

    private void setColumnWidths(Sheet sheet) {
        int[] widths = {24, 24, 18, 18, 20, 14, 20, 20, 20, 14, 14, 50, 14, 40, 22, 22, 50};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }
}
