package com.krishagni.catissueplus.core.common.util;

import java.util.Map;

import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class ExpressionUtil {
	private static ExpressionUtil instance = new ExpressionUtil();

	private SpelExpressionParser parser = new SpelExpressionParser();

	public static ExpressionUtil getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public <T> T evaluate(String exprStr, Map<String, Object> variables) {
		StandardEvaluationContext ctxt = new StandardEvaluationContext();
		ctxt.setVariables(variables);

		SpelExpression expr = parse(exprStr);
		return (T) expr.getValue(ctxt);
	}

	private SpelExpression parse(String exprStr) {
		return parser.parseRaw(exprStr);
	}
}
