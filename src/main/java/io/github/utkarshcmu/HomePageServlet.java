package io.github.utkarshcmu;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HomePageServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.getWriter().print("<html>\n"
                + "<head><title>Finagle Exporter</title></head>\n"
                + "<body>\n"
                + "<h1>Finagle Exporter</h1>\n"
                + "<p><a href=\"/_metrics\">Metrics</a></p>\n"
                + "</body>\n"
                + "</html>");
    }
}