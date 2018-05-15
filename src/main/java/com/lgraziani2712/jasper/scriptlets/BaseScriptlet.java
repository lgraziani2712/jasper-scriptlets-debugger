package com.lgraziani2712.jasper.scriptlets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRScriptletException;
import net.sf.jasperreports.engine.JasperReport;

/**
 * This class its the main point for the Jasper Scriptlets. It's responsible for configure
 * everything before the real scriptlets are used, e.g., require the report's name, or instantiate
 * the custom DB connector.
 */
public class BaseScriptlet extends JRDefaultScriptlet {
  protected JasperConnection connection;
  protected String reportName;
  protected String scriptletName;
  protected String mainScriptletName;
  private static final Logger logger = new Logger(BaseScriptlet.class);

  public static final String DEBUG_QUERIES = "DEBUG_QUERIES";

  @Override
  public void beforeReportInit() throws JRScriptletException {
    JasperReport report = (JasperReport) this.getParameterValue(JRParameter.JASPER_REPORT);
    String mainQuery = report.getQuery().getText().replaceAll("[\\t\\n\\r]+", " ");

    this.mainScriptletName = report.getScriptletClass();
    this.mainScriptletName = this.mainScriptletName == null ? "" : this.mainScriptletName;
    this.scriptletName = this.getClass().getSimpleName();
    this.reportName = report.getName().toUpperCase();

    Map<String, Boolean> flags = this.debugFlags();

    if (flags.get(DEBUG_QUERIES) && this.mainScriptletName.contains(this.scriptletName)) {
      logger.debug(
          reportName + " (Scriplet: " + this.scriptletName + ") - MAIN QUERY: " + mainQuery);
    }

    this.connection =
        new JasperConnection(
            (Connection) this.getParameterValue(JRParameter.REPORT_CONNECTION),
            this.reportName,
            this.scriptletName,
            flags);

    if (flags.get(DEBUG_QUERIES) && this.mainScriptletName.contains(this.scriptletName)) {
      this.printOpenedCursors();
    }
  }

  @Override
  public void afterDetailEval() throws JRScriptletException {
    if (!this.debugFlags().get(DEBUG_QUERIES)
        || !this.mainScriptletName.contains(this.scriptletName)) {
      return;
    }

    this.printOpenedCursors();
  }

  private void printOpenedCursors() throws JRScriptletException {
    // FIXME: This query is Oracle dependent
    this.connection.query(
        "SELECT * FROM (SELECT ss.sid, ss.value FROM v$sesstat ss, v$statname sn "
            + "WHERE ss.statistic# = sn.statistic# AND sn.name like '%opened cursors current%' "
            + "ORDER BY value desc, sid desc) WHERE value > 0",
        new JasperOperation() {
          @Override
          public ResultSet query(PreparedStatement stmt) throws JRScriptletException, SQLException {
            ResultSet rs = stmt.executeQuery();

            logger.debug(reportName + " (Scriplet: " + scriptletName + ") - Opened cursors: ");

            while (rs.next()) {
              String sid = rs.getString("sid");
              String value = rs.getString("value");

              logger.cacheInline("[SID:" + sid + ", VAL:" + value + "]");
            }

            logger.flushInline(Logger.Methods.debug);

            return rs;
          }
        });
  }

  /**
   * Require debug flags. Are set to `false` by default.
   *
   * @return Map flag -> value
   */
  private Map<String, Boolean> debugFlags() {
    Map<String, Boolean> debugFlags = new HashMap<String, Boolean>();

    try {
      Object debugQueries = this.getParameterValue(DEBUG_QUERIES);

      debugQueries =
          debugQueries instanceof Boolean
              ? debugQueries
              : "S".equalsIgnoreCase((String) debugQueries);
      debugFlags.put(DEBUG_QUERIES, (Boolean) debugQueries);
    } catch (JRScriptletException e) {
      debugFlags.put(DEBUG_QUERIES, false);

      logger.debug(
          this.reportName
              + " (Scriplet: "
              + this.scriptletName
              + ") - DEBUG_QUERIES: not configured.");
    }

    return debugFlags;
  }
}
