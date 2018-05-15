package com.lgraziani2712.jasper.scriptlets;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.jasperreports.engine.JRScriptletException;

public class JasperOperation {
  public ResultSet query(PreparedStatement stmt) throws JRScriptletException, SQLException {
    throw new JRScriptletException("`JasperOperation@query` must be overwritten");
  }

  public void callProcedure(CallableStatement callableStmt)
      throws JRScriptletException, SQLException {
    throw new JRScriptletException("`JasperOperation@callProcedure` must be overwritten");
  }
}
