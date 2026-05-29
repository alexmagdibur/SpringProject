package ru.bmstu;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import ru.bmstu.config.AppConfig;
import ru.bmstu.config.WebConfig;

import java.net.ServerSocket;
import java.util.logging.LogManager;

public class App {

    private static final int PORT = 8090;

    public static void main(String[] args) throws Exception {
        installJulBridge();
        checkPortAvailable(PORT);

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.getConnector();

        AnnotationConfigWebApplicationContext context =
                new AnnotationConfigWebApplicationContext();
        context.setClassLoader(App.class.getClassLoader());
        context.register(AppConfig.class, WebConfig.class);

        DispatcherServlet dispatcher = new DispatcherServlet(context);
        Context ctx = tomcat.addContext("", null);
        Wrapper servlet = Tomcat.addServlet(ctx, "dispatcher", dispatcher);
        servlet.setLoadOnStartup(1);
        servlet.setAsyncSupported(true);
        ctx.addServletMappingDecoded("/*", "dispatcher");

        tomcat.start();
        System.out.println("Server started at http://localhost:" + PORT);
        System.out.println("Swagger UI: http://localhost:" + PORT + "/swagger-ui/index.html");
        tomcat.getServer().await();
    }

    private static void installJulBridge() {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }

    private static void checkPortAvailable(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            // порт свободен — продолжаем
        } catch (Exception e) {
            System.err.println("========================================================");
            System.err.println(" ERROR: Port " + port + " is already in use.");
            System.err.println();
            System.err.println(" If you are using IntelliJ IDEA, disable its built-in");
            System.err.println(" web server on port " + port + ":");
            System.err.println("   Settings → Tools → Built-in Web Server → Port");
            System.err.println("   Change the port to any other value (e.g. 63342).");
            System.err.println();
            System.err.println(" Alternatively, stop the process using port " + port + " and");
            System.err.println(" restart the application.");
            System.err.println("========================================================");
            System.exit(1);
        }
    }
}
