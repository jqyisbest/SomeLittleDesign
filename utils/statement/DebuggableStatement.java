
/**
 * Title:        <p>
 * Description:  <p>
 * Copyright:    Copyright (c) Troy Thompson Bob Byron<p>
 * Company:      JavaUnderground<p>
 * @author       Troy Thompson Bob Byron
 * @version 1.1
 */
package net.gmcc.dg.acr.modules.reward.basedata.service.convert.statement;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.StringTokenizer;

/**
 * PreparedStatements have no way to retrieve the statement that was
 * executed on the database. This is due to the nature of prepared statements, which
 * are database driver specific. This class proxies for a PreparedStatement and
 * creates the SQL string that is created from the sets done on the
 * PreparedStatement.
 * <p>
   Some of the objects such as blob, clob, and Ref are only represented as
   Strings and are not the actual objects populating the database.
   Array is represented by the object type within the array.

   Example code:
        int payPeriod = 1;
        String name = "Troy Thompson";
        ArrayList employeePay = new ArrayList();
        ResultSet rs = null;
        PreparedStatement ps = null;
        Connection con = null;
        try{
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            String url = "jdbc:odbc:Employee";
            con = DriverManager.getConnection(url);
            String sql = "SELECT e.name,e.employee_number,e.pay_rate,e.type,"+
                        " e.hire_date,h.pay_period,h.hours,h.commissions"+
                        " FROM Employee_tbl e,hours_tbl h "+
                        " WHERE h.pay_period = ?"+
                        " AND e.name = ?"+
                        " AND h.employee_number = e.employee_number";
            ps = StatementFactory.getStatement(con,sql); // <-- insert this to debug
            //ps = con.prepareStatement(sql);
            ps.setInt(1,payPeriod);
            ps.setString(2,name);
            System.out.println();
            System.out.println(" debuggable statement= " + ps.toString());
            rs = ps.executeQuery();
        }catch(SQLException e){
            e.printStackTrace();
        }catch(ClassNotFoundException ce){
            ce.printStackTrace();
        }
        finally{
            try{
                if(rs != null){rs.close();}
                if(ps != null){ps.close();}
                if(!con.isClosed()) con.close();
            }catch(SQLException e){
                e.printStackTrace();
            }
        }
   </p>
 * *****notes*****
 *  One of the main differences between databases is how they handle dates/times.
 *  Since we use Oracle, the debug string for Dates, Times, Timestamps are using
 *  an Oracle specific SqlFormatter called OracleSqlFormatter.
 *
 *  The following is in our debug class:
 *  static{
 *      StatementFactory.setDefaultDebug(DebugLevel.ON);
 *      StatementFactory.setDefaultFormatter(new OracleSqlFormatter());
 *  }
 *
 */
public class DebuggableStatement implements PreparedStatement{

    private PreparedStatement ps;       //preparedStatement being proxied for.
    private String sql;                 //original statement going to database.
    private String filteredSql;         //statement filtered for rogue '?' that are not bind variables.
    private DebugObject[] variables;    //array of bind variables
    private SqlFormatter formatter;     //format for dates
    private long startTime;             //time that statement began execution
    private long executeTime;           //time elapsed while executing statement
    private DebugLevel debugLevel;      //level of debug

    /**
        Construct new DebugableStatement.
        Uses the SqlFormatter to format date, time, timestamp outputs
        @param con Connection to be used to construct PreparedStatement
        @param sqlStatement sql statement to be sent to database.
        @param debugLevel DebugLevel can be ON, OFF, VERBOSE.

    */
    protected DebuggableStatement(Connection con, String sqlStatement, SqlFormatter formatter, DebugLevel debugLevel) throws SQLException{
        //set values for member variables
        if (con == null) {
            throw new SQLException("Connection object is null");
        }
        this.ps = con.prepareStatement(sqlStatement);
        this.sql = sqlStatement;
        this.debugLevel = debugLevel;
        this.formatter = formatter;

        //see if there are any '?' in the statement that are not bind variables
        //and filter them out.
        boolean isString = false;
        char[] sqlString = sqlStatement.toCharArray();
        for (int i = 0; i < sqlString.length; i++){
            if (sqlString[i] == '\'') {
                isString = !isString;
            }
            //substitute the ? with an unprintable character if the ? is in a
            //string.
            if (sqlString[i] == '?' && isString){
                sqlString[i] = '\u0007';
            }
        }
        filteredSql = new String(sqlString);

        //find out how many variables are present in statement.
        int count = 0;
        int index = -1;
        while ((index = filteredSql.indexOf("?",index+1)) != -1){
            count++;
        }

        //show how many bind variables found
        if (debugLevel == DebugLevel.VERBOSE) {
            System.out.println("count= " + count);
        }

        //create array for bind variables
        variables = new DebugObject[count];

    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void addBatch() throws SQLException{
        ps.addBatch();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void addBatch(String sql) throws SQLException{
        ps.addBatch();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void cancel() throws SQLException{
        ps.cancel();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void clearBatch() throws SQLException{
        ps.clearBatch();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void clearParameters() throws SQLException{
        ps.clearParameters();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void clearWarnings() throws SQLException{
        ps.clearWarnings();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void close() throws SQLException{
        ps.close();
    }

    /**
     * Executes query and Calculates query execution time if DebugLevel = VERBOSE
     * @return results of query
     */
    @Override
    public boolean execute() throws SQLException{
        //execute query
        Boolean results = null;
        try{
            results = (Boolean)executeVerboseQuery("execute",null);
        }catch(Exception e){
           throw new SQLException("Could not execute sql command - Original message: " + e.getMessage());
        }
        return results.booleanValue();
    }

    /**
     * This method is only here for convenience. If a different sql string is executed
     * than was passed into Debuggable, unknown results will occur.
     * Executes query and Calculates query execution time if DebugLevel = VERBOSE
     * @param sql should be same string that was passed into Debuggable
     * @return results of query
     */
    @Override
    public boolean execute(String sql) throws SQLException{
        //execute query
        Boolean results = null;
        try{
            results = (Boolean)executeVerboseQuery("execute",new Class[]{sql.getClass()});
        }catch(Exception e){
            throw new SQLException("Could not execute sql command - Original message: " + e.getMessage());
        }
        return results.booleanValue();
    }

    /**
     * Executes query and Calculates query execution time if DebugLevel = VERBOSE
     * @return results of query
     */
    @Override
    public int[] executeBatch() throws SQLException{
        //execute query
        int[] results = null;
        try{
            results = (int[])executeVerboseQuery("executeBatch",null);
        }catch(Exception e){
            throw new SQLException("Could not execute sql command - Original message: " + e.getMessage());
        }
        return results;
    }

    /**
     * Executes query and Calculates query execution time if DebugLevel = VERBOSE
     * @return results of query
     */
    @Override
    public ResultSet executeQuery() throws SQLException{
        //execute query
        ResultSet results = null;
        try{
            results = (ResultSet)executeVerboseQuery("executeQuery",null);
        }catch(Exception e){
            throw new SQLException("Could not execute sql command - Original message: " + e.getMessage());
        }
        return results;
    }

    /**
     * This method is only here for convenience. If a different sql string is executed
     * than was passed into Debuggable, unknown results will occur.
     * Executes query and Calculates query execution time if DebugLevel = VERBOSE
     * @param sql should be same string that was passed into Debuggable
     * @return results of query
     */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException{
        //execute query
        ResultSet results = null;
        try{
            results = (ResultSet)executeVerboseQuery("executeQuery",new Class[]{sql.getClass()});
        }catch(Exception e){
            throw new SQLException("Could not execute sql command - Original message: " + e.getMessage());
        }
        return results;
    }

    /**
     * Executes query and Calculates query execution time if DebugLevel = VERBOSE
     * @return results of query
     */
    @Override
    public int executeUpdate() throws SQLException{
        //execute query
        Integer results = null;
        try{
            results = ps.executeUpdate();
        }catch(Exception e){
            throw new SQLException("Could not execute sql command - Original message: " + e.getMessage());
        }
        return results.intValue();
    }

    /**
     * This method is only here for convenience. If a different sql string is executed
     * than was passed into Debuggable, unknown results will occur.
     * Executes query and Calculates query execution time if DebugLevel = VERBOSE
     * @param sql should be same string that was passed into Debuggable
     * @return results of query
     */
    @Override
    public int executeUpdate(String sql) throws SQLException{
        //execute query
        Integer results = null;
        try{
            results = (Integer)executeVerboseQuery("executeUpdate",new Class[]{sql.getClass()});
        }catch(Exception e){
            throw new SQLException("Could not execute sql command - Original message: " + e.getMessage());
        }
        return results.intValue();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public Connection getConnection() throws SQLException{
        return ps.getConnection();
    }

    /**
     * Moves to this <code>Statement</code> object's next result, deals with
     * any current <code>ResultSet</code> object(s) according  to the instructions
     * specified by the given flag, and returns
     * <code>true</code> if the next result is a <code>ResultSet</code> object.
     *
     * <P>There are no more results when the following is true:
     * <PRE>{@code
     * // stmt is a Statement object
     * ((stmt.getMoreResults(current) == false) && (stmt.getUpdateCount() == -1))
     * }</PRE>
     *
     * @param current one of the following <code>Statement</code>
     *                constants indicating what should happen to current
     *                <code>ResultSet</code> objects obtained using the method
     *                <code>getResultSet</code>:
     *                <code>Statement.CLOSE_CURRENT_RESULT</code>,
     *                <code>Statement.KEEP_CURRENT_RESULT</code>, or
     *                <code>Statement.CLOSE_ALL_RESULTS</code>
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     * object; <code>false</code> if it is an update count or there are no
     * more results
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed <code>Statement</code> or the argument
     *                                         supplied is not one of the following:
     *                                         <code>Statement.CLOSE_CURRENT_RESULT</code>,
     *                                         <code>Statement.KEEP_CURRENT_RESULT</code> or
     *                                         <code>Statement.CLOSE_ALL_RESULTS</code>
     * @throws SQLFeatureNotSupportedException if
     *                                         <code>DatabaseMetaData.supportsMultipleOpenResults</code> returns
     *                                         <code>false</code> and either
     *                                         <code>Statement.KEEP_CURRENT_RESULT</code> or
     *                                         <code>Statement.CLOSE_ALL_RESULTS</code> are supplied as
     *                                         the argument.
     * @see #execute
     * @since 1.4
     */
    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }

    /**
     * Retrieves any auto-generated keys created as a result of executing this
     * <code>Statement</code> object. If this <code>Statement</code> object did
     * not generate any keys, an empty <code>ResultSet</code>
     * object is returned.
     *
     * <p><B>Note:</B>If the columns which represent the auto-generated keys were not specified,
     * the JDBC driver implementation will determine the columns which best represent the auto-generated keys.
     *
     * @return a <code>ResultSet</code> object containing the auto-generated key(s)
     * generated by the execution of this <code>Statement</code> object
     * @throws SQLException                    if a database access error occurs or
     *                                         this method is called on a closed <code>Statement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return null;
    }

    /**
     * Executes the given SQL statement and signals the driver with the
     * given flag about whether the
     * auto-generated keys produced by this <code>Statement</code> object
     * should be made available for retrieval.  The driver will ignore the
     * flag if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param sql               an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     *                          <code>DELETE</code>; or an SQL statement that returns nothing,
     *                          such as a DDL statement.
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *                          should be made available for retrieval;
     *                          one of the following constants:
     *                          <code>Statement.RETURN_GENERATED_KEYS</code>
     *                          <code>Statement.NO_GENERATED_KEYS</code>
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed <code>Statement</code>, the given
     *                                         SQL statement returns a <code>ResultSet</code> object,
     *                                         the given constant is not one of those allowed, the method is called on a
     *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.4
     */
    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return 0;
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param sql           an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     *                      <code>DELETE</code>; or an SQL statement that returns nothing,
     *                      such as a DDL statement.
     * @param columnIndexes an array of column indexes indicating the columns
     *                      that should be returned from the inserted row
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed <code>Statement</code>, the SQL
     *                                         statement returns a <code>ResultSet</code> object,the second argument
     *                                         supplied to this method is not an
     *                                         <code>int</code> array whose elements are valid column indexes, the method is called on a
     *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.4
     */
    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return 0;
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param sql         an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     *                    <code>DELETE</code>; or an SQL statement that returns nothing,
     *                    such as a DDL statement.
     * @param columnNames an array of the names of the columns that should be
     *                    returned from the inserted row
     * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>,
     * or <code>DELETE</code> statements, or 0 for SQL statements
     * that return nothing
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed <code>Statement</code>, the SQL
     *                                         statement returns a <code>ResultSet</code> object, the
     *                                         second argument supplied to this method is not a <code>String</code> array
     *                                         whose elements are valid column names, the method is called on a
     *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @since 1.4
     */
    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return 0;
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that any
     * auto-generated keys should be made available
     * for retrieval.  The driver will ignore this signal if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param sql               any SQL statement
     * @param autoGeneratedKeys a constant indicating whether auto-generated
     *                          keys should be made available for retrieval using the method
     *                          <code>getGeneratedKeys</code>; one of the following constants:
     *                          <code>Statement.RETURN_GENERATED_KEYS</code> or
     *                          <code>Statement.NO_GENERATED_KEYS</code>
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     * object; <code>false</code> if it is an update count or there are
     * no results
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed <code>Statement</code>, the second
     *                                         parameter supplied to this method is not
     *                                         <code>Statement.RETURN_GENERATED_KEYS</code> or
     *                                         <code>Statement.NO_GENERATED_KEYS</code>,
     *                                         the method is called on a
     *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     *                                         this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @see #getGeneratedKeys
     * @since 1.4
     */
    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return false;
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.  This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available.  The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * Under some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param sql           any SQL statement
     * @param columnIndexes an array of the indexes of the columns in the
     *                      inserted row that should be  made available for retrieval by a
     *                      call to the method <code>getGeneratedKeys</code>
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     * object; <code>false</code> if it is an update count or there
     * are no results
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed <code>Statement</code>, the
     *                                         elements in the <code>int</code> array passed to this method
     *                                         are not valid column indexes, the method is called on a
     *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @since 1.4
     */
    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return false;
    }

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval. This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available.  The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param sql         any SQL statement
     * @param columnNames an array of the names of the columns in the inserted
     *                    row that should be made available for retrieval by a call to the
     *                    method <code>getGeneratedKeys</code>
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     * object; <code>false</code> if it is an update count or there
     * are no more results
     * @throws SQLException                    if a database access error occurs,
     *                                         this method is called on a closed <code>Statement</code>,the
     *                                         elements of the <code>String</code> array passed to this
     *                                         method are not valid column names, the method is called on a
     *                                         <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @throws SQLTimeoutException             when the driver has determined that the
     *                                         timeout value that was specified by the {@code setQueryTimeout}
     *                                         method has been exceeded and has at least attempted to cancel
     *                                         the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @see #getGeneratedKeys
     * @since 1.4
     */
    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return false;
    }

    /**
     * Retrieves the result set holdability for <code>ResultSet</code> objects
     * generated by this <code>Statement</code> object.
     *
     * @return either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     * <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed <code>Statement</code>
     * @since 1.4
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    /**
     * Retrieves whether this <code>Statement</code> object has been closed. A <code>Statement</code> is closed if the
     * method close has been called on it, or if it is automatically closed.
     *
     * @return true if this <code>Statement</code> object is closed; false if it is still open
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    /**
     * Requests that a <code>Statement</code> be pooled or not pooled.  The value
     * specified is a hint to the statement pool implementation indicating
     * whether the application wants the statement to be pooled.  It is up to
     * the statement pool manager as to whether the hint is used.
     * <p>
     * The poolable value of a statement is applicable to both internal
     * statement caches implemented by the driver and external statement caches
     * implemented by application servers and other applications.
     * <p>
     * By default, a <code>Statement</code> is not poolable when created, and
     * a <code>PreparedStatement</code> and <code>CallableStatement</code>
     * are poolable when created.
     * <p>
     *
     * @param poolable requests that the statement be pooled if true and
     *                 that the statement not be pooled if false
     *                 <p>
     * @throws SQLException if this method is called on a closed
     *                      <code>Statement</code>
     *                      <p>
     * @since 1.6
     */
    @Override
    public void setPoolable(boolean poolable) throws SQLException {

    }

    /**
     * Returns a  value indicating whether the <code>Statement</code>
     * is poolable or not.
     * <p>
     *
     * @return <code>true</code> if the <code>Statement</code>
     * is poolable; <code>false</code> otherwise
     * <p>
     * @throws SQLException if this method is called on a closed
     *                      <code>Statement</code>
     *                      <p>
     * @see Statement#setPoolable(boolean) setPoolable(boolean)
     * @since 1.6
     * <p>
     */
    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    /**
     * Specifies that this {@code Statement} will be closed when all its
     * dependent result sets are closed. If execution of the {@code Statement}
     * does not produce any result sets, this method has no effect.
     * <p>
     * <strong>Note:</strong> Multiple calls to {@code closeOnCompletion} do
     * not toggle the effect on this {@code Statement}. However, a call to
     * {@code closeOnCompletion} does effect both the subsequent execution of
     * statements, and statements that currently have open, dependent,
     * result sets.
     *
     * @throws SQLException if this method is called on a closed
     *                      {@code Statement}
     * @since 1.7
     */
    @Override
    public void closeOnCompletion() throws SQLException {

    }

    /**
     * Returns a value indicating whether this {@code Statement} will be
     * closed when all its dependent result sets are closed.
     *
     * @return {@code true} if the {@code Statement} will be closed when all
     * of its dependent result sets are closed; {@code false} otherwise
     * @throws SQLException if this method is called on a closed
     *                      {@code Statement}
     * @since 1.7
     */
    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public int getFetchDirection() throws SQLException{
        return ps.getFetchDirection();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public int getFetchSize() throws SQLException{
        return ps.getFetchSize();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public int getMaxFieldSize() throws SQLException{
        return ps.getMaxFieldSize();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public int getMaxRows() throws SQLException{
        return ps.getMaxRows();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException{
        return ps.getMetaData();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public boolean getMoreResults() throws SQLException{
        return ps.getMoreResults();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public int getQueryTimeout() throws SQLException{
        return ps.getQueryTimeout();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public ResultSet getResultSet() throws SQLException{
        return ps.getResultSet();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public int getResultSetConcurrency() throws SQLException{
        return ps.getResultSetConcurrency();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public int getResultSetType() throws SQLException{
        return ps.getResultSetType();
    }

    /**
     * Facade for PreparedStatement
     */
    public String getStatement(){
        return sql;
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public int getUpdateCount() throws SQLException{
        return ps.getUpdateCount();
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public SQLWarning getWarnings() throws SQLException{
        return ps.getWarnings();
    }

    /**
     * Tests Object o for parameterIndex (which parameter is being set) and places
     * object in array of variables.
     * @param parameterIndex which PreparedStatement parameter is being set.
     * Sequence begins at 1.
     * @param o Object being stored as parameter
     * @exception throw if index exceeds number of variables.
     */
    private void saveObject(int parameterIndex, Object o)throws ParameterIndexOutOfBoundsException {
        if(parameterIndex > variables.length){
            throw new ParameterIndexOutOfBoundsException("Parameter index of " +
            parameterIndex + " exceeds actual parameter count of " + variables.length);
        }

        variables[parameterIndex-1] = new DebugObject(o);
    }

    /**
        Adds name of the Array's internal class type(by using x.getBaseTypeName())
         to the debug String. If x is null, NULL is added to debug String.
        @param i index of parameter
        @param x parameter Object
    */
    @Override
    public void setArray(int i, java.sql.Array x) throws SQLException{
        saveObject(i,x);
        ps.setArray(i,x);
    }

    /**
        Debug string prints NULL if InputStream is null, or adds "stream length = " + length
    */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException{
        saveObject(parameterIndex, (x==null?"NULL":"<stream length= " + length+">"));
        ps.setAsciiStream(parameterIndex,x,length);
    }

    /**
        Adds BigDecimal to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException{
        saveObject(parameterIndex, x);
        ps.setBigDecimal(parameterIndex,x);
    }

    /**
        Debug string prints NULL if InputStream is null, or adds "stream length= " + length.
        @param parameterIndex index of parameter
        @param x parameter Object
        @param length length of InputStream
    */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException{
        saveObject(parameterIndex, (x==null?"NULL":"<stream length= " + length+">"));
        ps.setBinaryStream(parameterIndex,x,length);
    }

    /**
        Adds name of the object's class type(Blob) to the debug String. If
        object is null, NULL is added to debug String.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException{
        saveObject(parameterIndex, x);
        ps.setBlob(parameterIndex, x);
    }

    /**
        Adds boolean to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException{
        saveObject(parameterIndex, new Boolean(x));
        ps.setBoolean(parameterIndex,x);
    }

    /**
        Adds byte to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException{
        saveObject(parameterIndex, new Byte(x));
        ps.setByte(parameterIndex,x);
    }

    /**
        Adds byte[] to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException{
        saveObject(parameterIndex, (x==null?"NULL":"byte[] length="+x.length));
        ps.setBytes(parameterIndex,x);
    }

    /**
        Debug string prints NULL if reader is null, or adds "stream length= " + length.
        @param parameterIndex index of parameter
        @param reader parameter Object
        @param length length of InputStream
    */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException{
        saveObject(parameterIndex, (reader==null?"NULL":"<stream length= " + length+">"));
        ps.setCharacterStream(parameterIndex,reader,length);
    }

    /**
        Adds name of the object's class type(Clob) to the debug String. If
        object is null, NULL is added to debug String.
        @param i index of parameter
        @param x parameter Object
    */
    @Override
    public void setClob(int i, Clob x) throws SQLException{
        saveObject(i, x);
        ps.setClob(i,x);
    }

    @Override
    public void setCursorName(String name) throws SQLException{
        ps.setCursorName(name);
    }

    /**
        Debug string displays date in YYYY-MM-DD HH24:MI:SS.# format.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException{
        saveObject(parameterIndex,x);
        ps.setDate(parameterIndex,x);
    }

    /**
        this implementation assumes that the Date has the date, and the
        calendar has the local info. For the debug string, the cal date
        is set to the date of x. Debug string displays date in YYYY-MM-DD HH24:MI:SS.# format.
        @param parameterIndex index of parameter
        @param x parameter Object
        @param cal uses x to set time
    */
    @Override
    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException{
        cal.setTime(new java.util.Date(x.getTime()));
        saveObject(parameterIndex,cal);
        ps.setDate(parameterIndex,x,cal);
    }

    /**
        Adds double to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException{
        saveObject(parameterIndex, new Double(x));
        ps.setDouble(parameterIndex,x);
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException{
        ps.setEscapeProcessing(enable);
    }

    /**
     * Facade for PreparedStatement
     */
    public void setFormatter(SqlFormatter formatter){
        this.formatter = formatter;
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void setFetchDirection(int direction) throws SQLException{
        ps.setFetchDirection(direction);
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void setFetchSize(int rows) throws SQLException{
        ps.setFetchSize(rows);
    }

    /**
        Adds float to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException{
        saveObject(parameterIndex, new Float(x));
        ps.setFloat(parameterIndex,x);
    }

    /**
        Adds int to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setInt(int parameterIndex, int x) throws SQLException{
        saveObject(parameterIndex, new Integer(x));
        ps.setInt(parameterIndex,x);
    }

    /**
        Adds long to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setLong(int parameterIndex, long x) throws SQLException{
        saveObject(parameterIndex, new Long(x));
        ps.setLong(parameterIndex,x);
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void setMaxFieldSize(int max) throws SQLException{
        ps.setMaxFieldSize(max);
    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void setMaxRows(int max) throws SQLException{
        ps.setMaxRows(max);
    }

    /**
        Adds a NULL to the debug String.
        @param parameterIndex index of parameter
        @param sqlType parameter Object
    */
    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException{
        saveObject(parameterIndex, "NULL");
        ps.setNull(parameterIndex,sqlType);
    }

    /**
        Adds a NULL to the debug String.
        @param parameterIndex index of parameter
        @param sqlType parameter Object
        @param typeName type of Object
    */
    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException{
        saveObject(parameterIndex, "NULL");
        ps.setNull(parameterIndex,sqlType,typeName);
    }

    /**
     * Sets the designated parameter to the given <code>java.net.URL</code> value.
     * The driver converts this to an SQL <code>DATALINK</code> value
     * when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the <code>java.net.URL</code> object to be set
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.4
     */
    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {

    }

    /**
     * Retrieves the number, types and properties of this
     * <code>PreparedStatement</code> object's parameters.
     *
     * @return a <code>ParameterMetaData</code> object that contains information
     * about the number, types and properties for each
     * parameter marker of this <code>PreparedStatement</code> object
     * @throws SQLException if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @see ParameterMetaData
     * @since 1.4
     */
    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The
     * driver converts this to a SQL <code>ROWID</code> value when it sends it
     * to the database
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the parameter value
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {

    }

    /**
     * Sets the designated parameter to the given <code>String</code> object.
     * The driver converts this to a SQL <code>NCHAR</code> or
     * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
     * (depending on the argument's
     * size relative to the driver's limits on <code>NVARCHAR</code> values)
     * when it sends it to the database.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value          the parameter value
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; if a database access error occurs; or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value          the parameter value
     * @param length         the number of characters in the parameter data.
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; if a database access error occurs; or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>java.sql.NClob</code> object. The driver converts this to a
     * SQL <code>NCLOB</code> value when it sends it to the database.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value          the parameter value
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; if a database access error occurs; or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader         An object that contains the data to set the parameter value to.
     * @param length         the number of characters in the parameter data.
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs; this method is called on
     *                                         a closed <code>PreparedStatement</code> or if the length specified is less than zero.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.  The inputstream must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     * This method differs from the <code>setBinaryStream (int, InputStream, int)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     *
     * @param parameterIndex index of the first parameter is 1,
     *                       the second is 2, ...
     * @param inputStream    An object that contains the data to set the parameter
     *                       value to.
     * @param length         the number of bytes in the parameter data.
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs;
     *                                         this method is called on a closed <code>PreparedStatement</code>;
     *                                         if the length specified
     *                                         is less than zero or if the number of bytes in the inputstream does not match
     *                                         the specified length.
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader         An object that contains the data to set the parameter value to.
     * @param length         the number of characters in the parameter data.
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if the length specified is less than zero;
     *                                         if the driver does not support national character sets;
     *                                         if the driver can detect that a data conversion
     *                                         error could occur;  if a database access error occurs or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    /**
     * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object.
     * The driver converts this to an
     * SQL <code>XML</code> value when it sends it to the database.
     * <p>
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param xmlObject      a <code>SQLXML</code> object that maps an SQL <code>XML</code> value
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs;
     *                                         this method is called on a closed <code>PreparedStatement</code>
     *                                         or the <code>java.xml.transform.Result</code>,
     *                                         <code>Writer</code> or <code>OutputStream</code> has not been closed for
     *                                         the <code>SQLXML</code> object
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

    }

    /**
        Adds name of the object's class type to the debug String. If
        object is null, NULL is added to debug String.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException{
        saveObject(parameterIndex, (x==null?"NULL":x.getClass().getName()));
        ps.setObject(parameterIndex, x);
    }

   /**
        Adds name of the object's class type to the debug String. If
        object is null, NULL is added to debug String.
        @param parameterIndex index of parameter
        @param x parameter Object
        @param targetSqlType database type
    */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException{
        saveObject(parameterIndex, (x==null?"NULL":x.getClass().getName()));
        ps.setObject(parameterIndex, x, targetSqlType);
    }

    /**
        Adds name of the object's class type to the debug String. If
        object is null, NULL is added to debug String.
        @param parameterIndex index of parameter
        @param x parameter Object
        @param targetSqlType database type
        @param scale see PreparedStatement
    */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException{
        saveObject(parameterIndex, (x==null?"NULL":x.getClass().getName()));
        ps.setObject(parameterIndex, x, targetSqlType, scale);
    }

    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the Java input stream that contains the ASCII parameter value
     * @param length         the number of bytes in the stream
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the java input stream which contains the binary parameter value
     * @param length         the number of bytes in the stream
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

    }

    /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param reader         the <code>java.io.Reader</code> object that contains the
     *                       Unicode data
     * @param length         the number of characters in the stream
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *                      marker in the SQL statement; if a database access error occurs or
     *                      this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

    }

    /**
     * Sets the designated parameter to the given input stream.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setAsciiStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the Java input stream that contains the ASCII parameter value
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

    }

    /**
     * Sets the designated parameter to the given input stream.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBinaryStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x              the java input stream which contains the binary parameter value
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

    }

    /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setCharacterStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param reader         the <code>java.io.Reader</code> object that contains the
     *                       Unicode data
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNCharacterStream</code> which takes a length parameter.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value          the parameter value
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if the driver does not support national
     *                                         character sets;  if the driver can detect that a data conversion
     *                                         error could occur; if a database access error occurs; or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setClob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader         An object that contains the data to set the parameter value to.
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs; this method is called on
     *                                         a closed <code>PreparedStatement</code>or if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.
     * This method differs from the <code>setBinaryStream (int, InputStream)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBlob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1,
     *                       the second is 2, ...
     * @param inputStream    An object that contains the data to set the parameter
     *                       value to.
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement; if a database access error occurs;
     *                                         this method is called on a closed <code>PreparedStatement</code> or
     *                                         if parameterIndex does not correspond
     *                                         to a parameter marker in the SQL statement,
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

    }

    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNClob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader         An object that contains the data to set the parameter value to.
     * @throws SQLException                    if parameterIndex does not correspond to a parameter
     *                                         marker in the SQL statement;
     *                                         if the driver does not support national character sets;
     *                                         if the driver can detect that a data conversion
     *                                         error could occur;  if a database access error occurs or
     *                                         this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support this method
     * @since 1.6
     */
    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {

    }

    /**
     * Facade for PreparedStatement
     */
    @Override
    public void setQueryTimeout(int seconds) throws SQLException{
        ps.setQueryTimeout(seconds);
    }

    /**
        From the javadocs:
            A reference to an SQL structured type value in the database.
            A Ref can be saved to persistent storage.
        The output from this method call in DebuggableStatement is a string representation
        of the Ref object by calling the Ref object's getBaseTypeName() method.
        Again, this will only be a String representation of the actual object
        being stored in the database.
        @param i index of parameter
        @param x parameter Object
    */

    @Override
    public void setRef(int i, Ref x) throws SQLException{
        saveObject(i, x);
        ps.setRef(i,x);
    }

    /**
        Adds short to debug string in parameterIndex position.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setShort(int parameterIndex, short x) throws SQLException{
        saveObject(parameterIndex, new Short(x));
        ps.setShort(parameterIndex, x);
    }

    /**
        Adds String to debug string in parameterIndex position.
        If String is null "NULL" is inserted in debug string.
        ****note****
        In situations where a single ' is in the string being
        inserted in the database. The debug string will need to be modified to
        reflect this when running the debug statement in the database.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setString(int parameterIndex, String x) throws SQLException{
        saveObject(parameterIndex,x);
        ps.setString(parameterIndex,x);
    }

    /**
        Debug string displays Time in HH24:MI:SS.# format.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException{
        saveObject(parameterIndex,x);
        ps.setTime(parameterIndex,x);
    }

   /**
        This implementation assumes that the Time object has the time and
        Calendar has the locale info. For the debug string, the cal time
        is set to the value of x. Debug string displays time in HH24:MI:SS.# format.
        @param parameterIndex index of parameter
        @param x parameter Object
        @param cal sets time based on x
    */
    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException{
        cal.setTime(new java.util.Date(x.getTime()));
        saveObject(parameterIndex, cal);
        ps.setTime(parameterIndex, x, cal);
    }

    /**
        Debug string displays timestamp in YYYY-MM-DD HH24:MI:SS.# format.
        @param parameterIndex index of parameter
        @param x parameter Object
    */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException{
        saveObject(parameterIndex,x);
        ps.setTimestamp(parameterIndex, x);
    }

    /**
        This implementation assumes that the Timestamp has the date/time and
        Calendar has the locale info. For the debug string, the cal date/time
        is set to the default value of Timestamp which is YYYY-MM-DD HH24:MI:SS.#.
        Debug string displays timestamp in DateFormat.LONG format.
        @param parameterIndex index of parameter
        @param x parameter Object
        @param cal sets time based on x
    */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException{
        cal.setTime(new java.util.Date(x.getTime()));
        saveObject(parameterIndex,cal);
        ps.setTimestamp(parameterIndex, x, cal);
    }

    /**
        Method has been deprecated in PreparedStatement interface.
        This method is present only to satisfy interface and does
        not do anything.
        Do not use...
        @deprecated
    */
    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException{
        //ps.setUnicodeStream(parameterIndex, x, length);
    }

    /**
        this toString is overidden to return a String representation of
        the sql statement being sent to the database. If a bind variable
        is missing then the String contains a ? + (missing variable #)
        @return the above string representation
    */
    @Override
    public String toString(){
        StringTokenizer st = new StringTokenizer(filteredSql,"?");
        int count = 1;
        StringBuffer statement = new StringBuffer();
        while(st.hasMoreTokens()){
            statement.append(st.nextToken());
            if(count <= variables.length){
                if(variables[count-1] != null && variables[count-1].isValueAssigned()){
                    try{
                         statement.append(formatter.format(variables[count-1].getDebugObject()));
                    }catch(SQLException e){
                         statement.append("SQLException");
                    }
                }else{
                    statement.append("? "+"(missing variable # " + count+" ) ");
                }
            }
            count++;
        }
        //unfilter the string in case there where rogue '?' in query string.
        char[] unfilterSql = statement.toString().toCharArray();
        for(int i = 0; i < unfilterSql.length; i++){
            if (unfilterSql[i] == '\u0007') {
                unfilterSql[i] = '?';
            }
        }

        //return execute time
        if (debugLevel == DebugLevel.ON) {
            return new String(unfilterSql);
        } else {
            return new String(unfilterSql) +
                                System.getProperty("line.separator") +
                                System.getProperty("line.separator") +
                                "query executed in " + executeTime + " milliseconds" +
                                System.getProperty("line.separator");
        }

    }

    private Object executeVerboseQuery(String methodName,Class[] parameters)
                                throws SQLException,NoSuchMethodException,
                                       InvocationTargetException,IllegalAccessException{
        //determine which method we have
        Method m = ps.getClass().getDeclaredMethod(methodName,parameters);
        //debug is set to on, so no times are calculated
        if (debugLevel == DebugLevel.ON) {
            return m.invoke(ps,parameters);
        }

        //calculate execution time for verbose debugging
        start();
        Object returnObject = m.invoke(ps,parameters);
        end();

        //return the executions return type
        return returnObject;
    }

    private void start(){
        startTime = System.currentTimeMillis();
    }

    private void end(){
        executeTime = System.currentTimeMillis()-startTime;
    }

    /**
     * Returns an object that implements the given interface to allow access to
     * non-standard methods, or standard methods not exposed by the proxy.
     * <p>
     * If the receiver implements the interface then the result is the receiver
     * or a proxy for the receiver. If the receiver is a wrapper
     * and the wrapped object implements the interface then the result is the
     * wrapped object or a proxy for the wrapped object. Otherwise return the
     * the result of calling <code>unwrap</code> recursively on the wrapped object
     * or a proxy for that result. If the receiver is not a
     * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
     *
     * @param iface A Class defining an interface that the result must implement.
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws SQLException If no object found that implements the interface
     * @since 1.6
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
     * for an object that does. Returns false otherwise. If this implements the interface then return true,
     * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
     * object. If this does not implement the interface and is not a wrapper, return false.
     * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
     * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
     * returns true then calling <code>unwrap</code> with the same argument should succeed.
     *
     * @param iface a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that does.
     * @throws SQLException if an error occurs while determining whether this is a wrapper
     *                      for an object with the given interface.
     * @since 1.6
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    private class DebugObject{
    private Object debugObject;
    private boolean valueAssigned;

    public DebugObject(Object debugObject){
        this.debugObject = debugObject;
        valueAssigned = true;
    }

    public Object getDebugObject(){
        return debugObject;
    }

    public boolean isValueAssigned(){
        return valueAssigned;
    }
}
}
