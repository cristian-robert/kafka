package com.spring.kafkaproj.runner;


import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.spring.kafkaproj.steps"},
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json"
        }
)
public class CucumberTestNGRunner extends AbstractTestNGCucumberTests {

    @Override
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
