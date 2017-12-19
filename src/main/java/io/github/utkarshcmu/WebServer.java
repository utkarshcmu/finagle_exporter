package io.github.utkarshcmu;

import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer {

    public static String host;
    public static int port;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: jar <host> <port> [exporter_port]");
            System.exit(1);
        }

        host = args[0];
        port = Integer.parseInt(args[1]);
        FinagleCollector collector = new FinagleCollector(host, port).register();
        
        int exporterPort;
        if (args.length == 3) {
        	exporterPort = Integer.parseInt(args[2]);
        } else {
        	exporterPort = 9991;
        }
        
        Server server = new Server(exporterPort);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MetricsServlet()), "/_metrics");
        context.addServlet(new ServletHolder(new HomePageServlet()), "/");
        server.start();
        server.join();
    }
}

