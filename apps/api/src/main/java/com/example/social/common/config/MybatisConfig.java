package com.example.social.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.example.social")
public class MybatisConfig {

	@Bean
	public ConfigurationCustomizer mybatisConfigurationCustomizer() {
		return configuration -> {
			configuration.setMapUnderscoreToCamelCase(true);
			configuration.setCallSettersOnNulls(true);
			configuration.setDefaultStatementTimeout(30);
		};
	}
}
