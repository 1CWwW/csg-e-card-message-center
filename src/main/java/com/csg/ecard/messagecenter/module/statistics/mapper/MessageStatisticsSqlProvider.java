package com.csg.ecard.messagecenter.module.statistics.mapper;

/**
 * 消息统计聚合SQL构造器。
 */
public class MessageStatisticsSqlProvider {

    private static final String COUNT_COLUMNS = """
            COUNT(1) AS totalCount,
            COALESCE(SUM(CASE WHEN r.send_status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS successCount,
            COALESCE(SUM(CASE WHEN r.send_status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedCount
            """;

    public String selectOverview() {
        return """
                <script>
                SELECT
                COUNT(1) AS totalCount,
                COALESCE(SUM(CASE WHEN r.send_status = 'SUCCESS' THEN 1 ELSE 0 END), 0) AS successCount,
                COALESCE(SUM(CASE WHEN r.send_status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedCount,
                COALESCE(SUM(CASE WHEN r.call_type = 'SYNC' THEN 1 ELSE 0 END), 0) AS syncCount,
                COALESCE(SUM(CASE WHEN r.call_type = 'ASYNC' THEN 1 ELSE 0 END), 0) AS asyncCount,
                COUNT(DISTINCT CASE WHEN c.channel_type IS NOT NULL THEN c.channel_type ELSE NULL END) AS channelTypeCount,
                COUNT(DISTINCT CASE WHEN r.scene_code IS NOT NULL THEN r.scene_code ELSE NULL END) AS sceneCount,
                COUNT(DISTINCT CASE WHEN r.template_id IS NOT NULL THEN r.template_id ELSE NULL END) AS templateCount,
                COUNT(DISTINCT CASE WHEN r.user_org_id IS NOT NULL AND r.user_org_id &lt;&gt; '' THEN r.user_org_id ELSE NULL END) AS unitCount
                """ + baseFromWhere() + """
                </script>
                """;
    }

    public String selectSceneFilterOptions() {
        return """
                SELECT DISTINCT
                TO_CHAR(s.id) AS optionValue,
                COALESCE(s.scene_name, r.scene_code) AS optionLabel
                FROM msg_record r
                INNER JOIN msg_scene s ON s.scene_code = r.scene_code AND s.deleted = 0
                WHERE r.deleted = 0
                AND r.send_time IS NOT NULL
                AND r.send_status IN ('SUCCESS', 'FAILED')
                ORDER BY 2, 1
                """;
    }

    public String selectTemplateFilterOptions() {
        return """
                SELECT DISTINCT
                TO_CHAR(r.template_id) AS optionValue,
                t.template_name AS optionLabel
                FROM msg_record r
                INNER JOIN msg_template t ON t.id = r.template_id AND t.deleted = 0
                WHERE r.deleted = 0
                AND r.send_time IS NOT NULL
                AND r.send_status IN ('SUCCESS', 'FAILED')
                AND r.template_id IS NOT NULL
                ORDER BY 2, 1
                """;
    }

    public String selectSummary() {
        return """
                <script>
                SELECT
                """ + COUNT_COLUMNS + baseFromWhere() + """
                </script>
                """;
    }

    public String selectUnitSummary() {
        return """
                <script>
                SELECT
                """ + COUNT_COLUMNS + baseFromWhere() + """
                AND r.user_org_id IS NOT NULL AND r.user_org_id &lt;&gt; ''
                </script>
                """;
    }

    public String selectTimeDay() {
        return selectTime("TO_CHAR(r.send_time, 'YYYY-MM-DD')");
    }

    public String selectTimeWeek() {
        return selectTime("TO_CHAR(TRUNC(r.send_time, 'IW'), 'YYYY-MM-DD')");
    }

    public String selectTimeMonth() {
        return selectTime("TO_CHAR(r.send_time, 'YYYY-MM')");
    }

    public String selectChannel() {
        return """
                <script>
                SELECT
                c.channel_type AS channelType,
                """ + COUNT_COLUMNS + baseFromWhere() + """
                AND c.channel_type IS NOT NULL
                GROUP BY c.channel_type
                ORDER BY totalCount DESC, c.channel_type ASC
                </script>
                """;
    }

    public String selectScene() {
        return """
                <script>
                SELECT
                s.id AS sceneId,
                r.scene_code AS sceneCode,
                s.scene_name AS sceneName,
                """ + COUNT_COLUMNS + baseFromWhere() + """
                AND r.scene_code IS NOT NULL
                GROUP BY s.id, r.scene_code, s.scene_name
                ORDER BY totalCount DESC, r.scene_code ASC
                </script>
                """;
    }

    public String selectUnit() {
        return """
                <script>
                SELECT
                r.user_org_id AS unitId,
                """ + COUNT_COLUMNS + baseFromWhere() + """
                AND r.user_org_id IS NOT NULL AND r.user_org_id &lt;&gt; ''
                GROUP BY r.user_org_id
                ORDER BY totalCount DESC, r.user_org_id ASC
                </script>
                """;
    }

    public String selectTemplate() {
        return """
                <script>
                SELECT
                r.template_id AS templateId,
                t.template_name AS templateName,
                s.id AS sceneId,
                COALESCE(s.scene_code, r.scene_code) AS sceneCode,
                s.scene_name AS sceneName,
                t.channel_type AS channelType,
                """ + COUNT_COLUMNS + baseFromWhere() + """
                AND r.template_id IS NOT NULL
                GROUP BY r.template_id, t.template_name, s.id, COALESCE(s.scene_code, r.scene_code), s.scene_name, t.channel_type
                ORDER BY totalCount DESC, t.template_name ASC
                </script>
                """;
    }

    private String selectTime(String periodExpression) {
        return "<script>\n"
                + "SELECT\n"
                + periodExpression + " AS period,\n"
                + COUNT_COLUMNS
                + baseFromWhere()
                + " GROUP BY " + periodExpression + "\n"
                + " ORDER BY period ASC\n"
                + "</script>";
    }

    private String baseFromWhere() {
        return """

                FROM msg_record r
                LEFT JOIN msg_channel c ON c.id = r.channel_id
                LEFT JOIN msg_scene s ON s.scene_code = r.scene_code AND s.deleted = 0
                LEFT JOIN msg_template t ON t.id = r.template_id
                WHERE r.deleted = 0
                AND r.send_time IS NOT NULL
                AND r.send_status IN ('SUCCESS', 'FAILED')
                <if test='query.startTime != null'>
                AND r.send_time &gt;= #{query.startTime}
                </if>
                <if test='query.endTime != null'>
                AND r.send_time &lt;= #{query.endTime}
                </if>
                <if test='query.channelTypes != null and query.channelTypes.size() &gt; 0'>
                AND c.channel_type IN
                <foreach collection='query.channelTypes' item='item' open='(' separator=',' close=')'>#{item}</foreach>
                </if>
                <if test='query.sceneIds != null and query.sceneIds.size() &gt; 0'>
                AND s.id IN
                <foreach collection='query.sceneIds' item='item' open='(' separator=',' close=')'>#{item}</foreach>
                </if>
                <if test='query.unitIds != null and query.unitIds.size() &gt; 0'>
                AND r.user_org_id IN
                <foreach collection='query.unitIds' item='item' open='(' separator=',' close=')'>#{item}</foreach>
                </if>
                <if test='query.templateIds != null and query.templateIds.size() &gt; 0'>
                AND r.template_id IN
                <foreach collection='query.templateIds' item='item' open='(' separator=',' close=')'>#{item}</foreach>
                </if>
                <if test='query.callTypes != null and query.callTypes.size() &gt; 0'>
                AND r.call_type IN
                <foreach collection='query.callTypes' item='item' open='(' separator=',' close=')'>#{item}</foreach>
                </if>
                """;
    }
}
