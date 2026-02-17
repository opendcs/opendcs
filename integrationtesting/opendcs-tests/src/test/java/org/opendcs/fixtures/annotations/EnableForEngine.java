package org.opendcs.fixtures.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

/*
 * Only run this test if the test engine matches the specified engine
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnableForEngine.EngineCondition.class)
public @interface EnableForEngine
{
	String ENGINE = System.getProperty("opendcs.test.engine");

	String[] engines();

	class EngineCondition implements ExecutionCondition
	{
		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx)
		{
			ConditionEvaluationResult result = ConditionEvaluationResult.disabled("No compatible engine specified");
			var element = ctx.getElement();
			if (element.isPresent())
			{
				EnableForEngine engine = element.get().getAnnotation(EnableForEngine.class);
				if (engine != null)
				{
					String[] engines = engine.engines();
					if(engines.length != 0 && matches(engines, ENGINE))
					{
						result = ConditionEvaluationResult.enabled(String.format("Matches one of (%s)", String.join(",", engines)));
					}
				}
				else
				{
					result = ConditionEvaluationResult.enabled("No engine specified");
				}
			}
			return result;
		}

		private boolean matches(String[] allowed, String have)
		{
			for (String current : allowed)
			{
				if (current.equalsIgnoreCase(have))
				{
					return true;
				}
			}
			return false;
		}
	}
}
