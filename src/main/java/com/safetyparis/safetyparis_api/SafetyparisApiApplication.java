package com.safetyparis.safetyparis_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 로그인은 JWT로만 하므로 Spring Security가 자동 생성하는 기본 계정(generated password)은 끔
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableJpaAuditing
@EnableCaching
public class SafetyparisApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafetyparisApiApplication.class, args);
	}

}
