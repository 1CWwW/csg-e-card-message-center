package com.csg.ecard.messagecenter.module.statistics.export;

import com.csg.ecard.messagecenter.module.statistics.enums.StatisticsDimension;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsChannelItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsSceneItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTemplateItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsTimeItemVO;
import com.csg.ecard.messagecenter.module.statistics.vo.StatisticsUnitItemVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * 消息统计报表Excel导出器。
 */
@Component
public class MessageStatisticsExcelExporter {

    private static final String[] TIME_HEADERS = {"时间", "总量", "成功量", "失败量", "成功率"};
    private static final String[] CHANNEL_HEADERS = {"渠道类型", "总量", "成功量", "失败量", "成功率"};
    private static final String[] SCENE_HEADERS = {"场景名称", "总量", "成功量", "失败量", "成功率", "占比"};
    private static final String[] UNIT_HEADERS = {"单位名称", "总量", "成功量", "失败量", "成功率"};
    private static final String[] TEMPLATE_HEADERS = {"模板名称", "所属场景", "渠道类型", "使用次数", "成功次数", "失败次数", "成功率"};

    /**
     * 将统计结果写为xlsx文件。
     */
    public void write(StatisticsDimension dimension, Object data, OutputStream outputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("消息统计");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            switch (dimension) {
                case TIME -> writeTime(sheet, headerStyle, textStyle, castList(data));
                case CHANNEL -> writeChannel(sheet, headerStyle, textStyle, castList(data));
                case SCENE -> writeScene(sheet, headerStyle, textStyle, castList(data));
                case UNIT -> writeUnit(sheet, headerStyle, textStyle, castList(data));
                case TEMPLATE -> writeTemplate(sheet, headerStyle, textStyle, castList(data));
            }
            sheet.createFreezePane(0, 1);
            autoWidth(sheet, headerCount(dimension));
            workbook.write(outputStream);
            outputStream.flush();
        }
    }

    private void writeTime(Sheet sheet, CellStyle headerStyle, CellStyle textStyle, List<StatisticsTimeItemVO> rows) {
        writeHeader(sheet, TIME_HEADERS, headerStyle);
        int index = 1;
        for (StatisticsTimeItemVO item : rows) {
            Row row = sheet.createRow(index++);
            writeText(row, 0, item.getPeriodLabel(), textStyle);
            writeNumber(row, 1, item.getTotalCount(), textStyle);
            writeNumber(row, 2, item.getSuccessCount(), textStyle);
            writeNumber(row, 3, item.getFailedCount(), textStyle);
            writeText(row, 4, percentText(item.getSuccessRate()), textStyle);
        }
    }

    private void writeChannel(Sheet sheet, CellStyle headerStyle, CellStyle textStyle, List<StatisticsChannelItemVO> rows) {
        writeHeader(sheet, CHANNEL_HEADERS, headerStyle);
        int index = 1;
        for (StatisticsChannelItemVO item : rows) {
            Row row = sheet.createRow(index++);
            writeText(row, 0, item.getChannelTypeDesc(), textStyle);
            writeNumber(row, 1, item.getTotalCount(), textStyle);
            writeNumber(row, 2, item.getSuccessCount(), textStyle);
            writeNumber(row, 3, item.getFailedCount(), textStyle);
            writeText(row, 4, percentText(item.getSuccessRate()), textStyle);
        }
    }

    private void writeScene(Sheet sheet, CellStyle headerStyle, CellStyle textStyle, List<StatisticsSceneItemVO> rows) {
        writeHeader(sheet, SCENE_HEADERS, headerStyle);
        int index = 1;
        for (StatisticsSceneItemVO item : rows) {
            Row row = sheet.createRow(index++);
            writeText(row, 0, item.getSceneName(), textStyle);
            writeNumber(row, 1, item.getTotalCount(), textStyle);
            writeNumber(row, 2, item.getSuccessCount(), textStyle);
            writeNumber(row, 3, item.getFailedCount(), textStyle);
            writeText(row, 4, percentText(item.getSuccessRate()), textStyle);
            writeText(row, 5, percentText(item.getPercentage()), textStyle);
        }
    }

    private void writeUnit(Sheet sheet, CellStyle headerStyle, CellStyle textStyle, List<StatisticsUnitItemVO> rows) {
        writeHeader(sheet, UNIT_HEADERS, headerStyle);
        int index = 1;
        for (StatisticsUnitItemVO item : rows) {
            Row row = sheet.createRow(index++);
            writeText(row, 0, item.getUnitName(), textStyle);
            writeNumber(row, 1, item.getTotalCount(), textStyle);
            writeNumber(row, 2, item.getSuccessCount(), textStyle);
            writeNumber(row, 3, item.getFailedCount(), textStyle);
            writeText(row, 4, percentText(item.getSuccessRate()), textStyle);
        }
    }

    private void writeTemplate(Sheet sheet, CellStyle headerStyle, CellStyle textStyle, List<StatisticsTemplateItemVO> rows) {
        writeHeader(sheet, TEMPLATE_HEADERS, headerStyle);
        int index = 1;
        for (StatisticsTemplateItemVO item : rows) {
            Row row = sheet.createRow(index++);
            writeText(row, 0, item.getTemplateName(), textStyle);
            writeText(row, 1, item.getSceneName(), textStyle);
            writeText(row, 2, item.getChannelTypeDesc(), textStyle);
            writeNumber(row, 3, item.getUsageCount(), textStyle);
            writeNumber(row, 4, item.getSuccessCount(), textStyle);
            writeNumber(row, 5, item.getFailedCount(), textStyle);
            writeText(row, 6, percentText(item.getSuccessRate()), textStyle);
        }
    }

    private void writeHeader(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        cell.setCellValue(safeText(value));
    }

    private void writeNumber(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
    }

    private String percentText(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return safe.toPlainString() + "%";
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

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTextStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("@"));
        return style;
    }

    private void autoWidth(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }
    }

    private int headerCount(StatisticsDimension dimension) {
        return switch (dimension) {
            case TIME -> TIME_HEADERS.length;
            case CHANNEL -> CHANNEL_HEADERS.length;
            case SCENE -> SCENE_HEADERS.length;
            case UNIT -> UNIT_HEADERS.length;
            case TEMPLATE -> TEMPLATE_HEADERS.length;
        };
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object data) {
        return (List<T>) data;
    }
}
