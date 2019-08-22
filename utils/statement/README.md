**描述**

一个可以通过toString()获取最终SQL语句的PreparedStatement

[思路来源](https://www.javaworld.com/article/2073957/overpower-the-preparedstatement.html "javaworld")

[原始代码](https://huiminchen.iteye.com/blog/1097332)

原始代码在实际使用中存在多个错误点：

1. 多个方法未实现
2. 严重违背阿里巴巴代码规范
3. 不能兼容oracle driver

```java
DebuggableStatement can not access a member of class oracle.jdbc.driver.OraclePreparedStatementWrapper with modifiers "public"
```

---

**缺点**

1. oracle driver目前仅可以兼容executeUpdate()方法
2. 代码未完全合理修改

------

**使用**

```java
PreparedStatement preparedStatement = StatementFactory.getStatement(connection,baseSql, DebugLevel.ON);
```

**分析**

适配oracle的核心修改点在于避开通过反射调用driver里的方法，因为oracle的driver类都是包内可见...

问题点代码：

> DebuggableStatement.java

```java
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
```

executeUpdate等方法都调用了这个方法来实现

看源码的意思，做这个代理执行的目的只是为了做一个性能分析...

目前只修改了executeUpdate()，改为直接用preparedStatement对象执行就好了。

```java
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
```

