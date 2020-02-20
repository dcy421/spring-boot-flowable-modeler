package com.dcy;

import com.dcy.config.AppDispatcherServletConfiguration;
import com.dcy.config.ApplicationConfiguration;
import org.flowable.ui.modeler.conf.DatabaseConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Import(value={
        // 引入修改的配置
        ApplicationConfiguration.class,
        AppDispatcherServletConfiguration.class,
        })
//// 引入 DatabaseConfiguration 表更新转换
//        DatabaseConfiguration.class
// 移除 Security 自动配置
@SpringBootApplication(exclude={SecurityAutoConfiguration.class})
public class FlowableDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowableDemoApplication.class, args);
    }

}
