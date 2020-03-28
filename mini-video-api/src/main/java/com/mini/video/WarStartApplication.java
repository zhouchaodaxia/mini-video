package com.mini.video;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

/**
 * @description: web容器启动类
 * @author: ZHOUCHAO
 * @create: 2020/3/21 1:28
 */
public class WarStartApplication extends SpringBootServletInitializer {

    /**
     * 重写配置
     * @param builder
     * @return
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // 使用web.xml运行程序,指向Application,最后启动springboot
        return builder.sources(Application.class);
    }
}
