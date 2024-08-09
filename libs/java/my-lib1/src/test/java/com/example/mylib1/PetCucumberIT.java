package com.example.mylib1;

import io.cucumber.junit.platform.engine.Constants;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

@Suite
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.example.mylib1")
@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME, value = "src/test/resources/")
@CucumberContextConfiguration
@SpringBootTest
public class PetCucumberIT {
}
