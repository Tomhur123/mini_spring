package com.example.spring.web;

import com.example.spring.Autowired;
import com.example.spring.Component;
import com.example.spring.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.io.File;
import java.io.IOException;
import java.util.logging.LogManager;

@Component
public class TomcatServer {

    @Autowired
    private DispatcherServlet dispatcherServlet;

    @PostConstruct
    public void start() throws Exception {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        int port =8080;
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        String contextPath="";
        String docBase = new File(".").getAbsolutePath();
        Context context = tomcat.addContext(contextPath, docBase);

        tomcat.addServlet(contextPath, "dispatcherServlet", dispatcherServlet);
        context.addServletMappingDecoded("/*", "dispatcherServlet");
        tomcat.start();
        System.out.println("Tomcat started....  port: "+port);
    }
}
