package com.siyi;

import com.siyi.utils.SpringUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import tk.mybatis.spring.annotation.MapperScan;

@Controller
@SpringBootApplication
@MapperScan("com.siyi.mapper")
@ComponentScan(basePackages= {"com.siyi", "org.n3r.idworker"})
public class SiXinApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiXinApplication.class,args);
    }

    @Bean
    public SpringUtil getSpringUtil(){
        return new SpringUtil();
    }
}
