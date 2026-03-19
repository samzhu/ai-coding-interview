package exam.question.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/cp2-hint-provider.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "exam.question.bdd")
public class CP2Test {
}
