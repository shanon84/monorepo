package com.example.global.annotations;

import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.platform.suite.api.Suite;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@CucumberContextConfiguration
@IT
@Suite
public @interface CucumberIT {
}
