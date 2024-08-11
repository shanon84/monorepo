package com.example.mylib1;

import com.example.global.annotations.CucumberIT;
import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;

@CucumberIT
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.example.mylib1")
@ConfigurationParameter(key = Constants.FEATURES_PROPERTY_NAME, value = "src/test/resources/")
public class PetCucumberIT {
}
