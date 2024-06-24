package com.ajaxjs.framework.embeded_tomcat;

import com.ajaxjs.framework.spring.filter.FileUploadHelper;
import com.ajaxjs.framework.spring.filter.UTF8CharsetFilter;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleState;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.IOException;
import java.util.Map;

/**
 * 嵌入式使用 Tomcat
 */
public class EmbeddedTomcatStarter extends TomcatStarter {
    /**
     * LifecycleState.STARTING_PREP 会执行两次，不知为何
     */
    public boolean isStatedSpring;

    /**
     * 配置类
     */
    Class<?>[] clz;

    public EmbeddedTomcatStarter(TomcatConfig cfg, Class<?>[] clz) {
        super(cfg);
        this.clz = clz;
    }

    @Override
    public void onContextReady(Context context) {
        context.addLifecycleListener((LifecycleEvent event) -> {
            if (isStatedSpring || (event.getLifecycle().getState() != LifecycleState.STARTING_PREP))
                return;

            ServletContext ctx = context.getServletContext();

            if (ctx == null) // 可能在测试
                return;

            // 通过注解的方式初始化 Spring 的上下文，注册 Spring 的配置类（替代传统项目中 xml 的 configuration）
            AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();
            ac.setServletContext(ctx);

            if (!ObjectUtils.isEmpty(clz))
                ac.register(clz);

            ac.refresh();
            ac.registerShutdownHook();
            ctx.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");
            ctx.addListener(new ContextLoaderListener()); // 监听器
            ctx.setAttribute("ctx", ctx.getContextPath()); // 为 JSP 提供 shorthands

            // 绑定 servlet
            ServletRegistration.Dynamic registration = ctx.addServlet("dispatcher", new DispatcherServlet(ac));
            registration.setLoadOnStartup(1);// 设置 tomcat 启动立即加载 servlet
            registration.addMapping("/"); // 浏览器访问 uri。注意不要设置 /*

            // 字符过滤器
            FilterRegistration.Dynamic filterReg = ctx.addFilter("InitMvcRequest", new UTF8CharsetFilter());
            filterReg.addMappingForUrlPatterns(null, true, "/*");

            if (cfg.getEnableLocalFileUpload()) {
                if (cfg.getLocalFileUploadDir() == null)
                    FileUploadHelper.initUpload(ctx, registration);
                else
                    FileUploadHelper.initUpload(ctx, registration, cfg.getLocalFileUploadDir());
            }

            if (cfg.isEnableJMX())
                connectMBeanServer();

            isStatedSpring = true;
            springTime = System.currentTimeMillis() - startedTime;
        });
    }

    public static void start(Class<?>... clz) {
        TomcatConfig cfg = new TomcatConfig();
        Map<String, Object> serverConfig = getServerConfig();
        int port = 8301; // default port

        if (serverConfig != null) {
            Object p = serverConfig.get("port");

            if (p != null)
                port = (int) p;

            String context = (String) serverConfig.get("context-path");

            if (StringUtils.hasText(context))
                cfg.setContextPath(context);

            Object upObj = serverConfig.get("localFileUpload");

            if (upObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> upCfg = (Map<String, Object>) upObj;

                if (upCfg.get("enable") != null)
                    cfg.setEnableLocalFileUpload((boolean) upCfg.get("enable"));

                cfg.setLocalFileUploadDir((String) upCfg.get("dir"));
            }
        }

        cfg.setPort(port);

        new EmbeddedTomcatStarter(cfg, clz).start();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> getServerConfig() {
        ClassPathResource resource = new ClassPathResource("application.yml");

        if (!resource.exists())
            return null;

        try {
            Map<String, Object> yamlMap = new Yaml().load(resource.getInputStream());

            return (Map<String, Object>) yamlMap.get("server");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 启动 Web 程序
     *
     * @param port 端口
     * @param clz  配置类列表
     */
    public static void start(int port, Class<?>... clz) {
        TomcatConfig cfg = new TomcatConfig();
        cfg.setPort(port);

        new EmbeddedTomcatStarter(cfg, clz).start();
    }

    /**
     * 启动 Web 程序
     *
     * @param port        端口
     * @param contextPath 程序上下文目录
     * @param clz         配置类列表
     */
    public static void start(int port, String contextPath, Class<?>... clz) {
        TomcatConfig cfg = new TomcatConfig();
        cfg.setPort(port);
        cfg.setContextPath(contextPath);

        new EmbeddedTomcatStarter(cfg, clz).start();
    }
}
