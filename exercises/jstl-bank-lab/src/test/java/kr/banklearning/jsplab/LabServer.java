package kr.banklearning.jsplab;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/** 로컬 미리보기와 실제 JSP 렌더링 테스트에서 공유하는 내장 Tomcat. */
public final class LabServer implements AutoCloseable {
    private final Tomcat tomcat;

    public LabServer(int port) throws Exception {
        Path module = Path.of(System.getProperty("lab.module")).toAbsolutePath().normalize();
        Path views = Path.of(System.getProperty("lab.views")).toAbsolutePath().normalize();
        Path base = Files.createDirectories(module.resolve("target/tomcat-" + port));
        tomcat = new Tomcat();
        tomcat.setBaseDir(base.toString());
        tomcat.setHostname("127.0.0.1");
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context context = tomcat.addWebapp("", module.resolve("src/main/webapp").toString());
        context.setParentClassLoader(LabServer.class.getClassLoader());
        StandardRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/views", views.toString(), "/"));
        context.setResources(resources);
        tomcat.start();
    }

    public String url() {
        return "http://127.0.0.1:" + tomcat.getConnector().getLocalPort();
    }

    @Override
    public void close() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("lab.port", "8087"));
        LabServer server = new LabServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.close(); } catch (Exception ignored) { /* 종료 시 내부 예외를 출력하지 않는다. */ }
        }));
        System.out.println("JSTL lab: " + server.url() + "/accounts (Ctrl+C to stop)");
        server.tomcat.getServer().await();
    }
}
