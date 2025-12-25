package com.ssafy.yumcoach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvCheck {

    public EnvCheck(
            @Value("${app.mail.from}") String from,
            @Value("${spring.mail.username}") String user
    ) {
        System.out.println("====== ENV CHECK ======");
        System.out.println("FROM = " + from);
        System.out.println("USER = " + user);
        System.out.println("=======================");
    }
}
