<%@ page import="java.sql.SQLException" %>
<%@ page import="custom.OracleInterceptor" %>
<%@ page import="java.sql.ResultSet" %>
<%@ include file="Common.jsp" %><%!
    //
//   Filename: Books.jsp
//   Generated with CodeCharge  v.1.2.0
//   JSP.ccp build 05/21/2001
//

    static final String sFileName = "Test.jsp";

%><%

    boolean bDebug = false;

    String sAction = getParam( request, "FormAction");
    String sForm = getParam( request, "FormName");
    String sResultsErr = "";
    String sSearchErr = "";
    String sAdvMenuErr = "";
    String sTotalErr = "";

    java.sql.Connection conn = null;
    java.sql.Statement stat = null;
    String sErr = loadDriver();
    conn = cn();
    stat = conn.createStatement();
    if ( ! sErr.equals("") ) {
        try {
            out.println(sErr);
        }
        catch (Exception e) {}
    }

%>
<html>
<head>
    <title>Book Store</title>
    <meta name="GENERATOR" content="YesSoftware CodeCharge v.1.2.0 / JSP.ccp build 05/21/2001"/>
    <meta http-equiv="pragma" content="no-cache"/>
    <meta http-equiv="expires" content="0"/>
    <meta http-equiv="cache-control" content="no-cache"/>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
</head>
<body style="background-color: #FFFFFF; color: #000000; font-family: Arial, Tahoma, Verdana, Helveticabackground-color: #FFFFFF; color: #000000; font-family: Arial, Tahoma, Verdana, Helvetica">
<jsp:include page="Header.jsp" flush="true"/>

TEST

<% test(request, response, session, out, sSearchErr, sForm, sAction, conn, stat); %>


<jsp:include page="Footer.jsp" flush="true"/>
<center><font face="Arial"><small>This dynamic site was generated with <a href="http://www.codecharge.com">CodeCharge</a></small></font></center>
</body>
</html>
<%%>
<%
    if ( stat != null ) stat.close();
    if ( conn != null ) conn.close();
%>
<%!
    void test (javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, javax.servlet.http.HttpSession session, javax.servlet.jsp.JspWriter out, String sSearchErr, String sForm, String sAction, java.sql.Connection conn, java.sql.Statement stat) throws java.io.IOException {

        String sWhere = "";
        int iCounter = 0;
        int iPage = 0;
        boolean bIsScroll = true;
        boolean hasParam = false;
        String sOrder = "";
        String sSQL = "";


        // Build full SQL statement

        sSQL ="SELECT employee.SSN,employee.SALARY from employee " ;


        java.sql.ResultSet rs = null;
        // Open recordset
        try {
            rs = openrs(stat, sSQL);
            System.out.println("ciao");
            int count=0;
            while (rs.next())
                count++;
            System.out.println(count+" tuples");
            if ( rs != null ) rs.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        /*
        try {

            ResultSet result=rs.preProcess(sSQL);

            int count=0;
            while (result.next())
                count++;
            System.out.println(count+" tuples");
            rs.close(result);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        java.sql.ResultSet rs = null;
        // Open recordset
        try {
            rs = openrs(stat, sSQL);
            int count=0;
            while (rs.next())
                count++;
            System.out.println(count+" tuples");
            if ( rs != null ) rs.close();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }*/
    }

%>