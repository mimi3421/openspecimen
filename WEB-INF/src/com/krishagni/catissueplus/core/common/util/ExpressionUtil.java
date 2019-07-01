package com.krishagni.catissueplus.core.common.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class ExpressionUtil {
	private static ExpressionUtil instance = new ExpressionUtil();

	private SpelExpressionParser parser = new SpelExpressionParser();

	private Map<String, Method> methods = new HashMap<>();

	public static ExpressionUtil getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public <T> T evaluate(String exprStr, Map<String, Object> variables) {
		StandardEvaluationContext ctxt = new StandardEvaluationContext();
		addMethods(ctxt);
		ctxt.setVariables(variables);

		SpelExpression expr = parse(exprStr);
		return (T) expr.getValue(ctxt);
	}

	private SpelExpression parse(String exprStr) {
		return parser.parseRaw(exprStr);
	}

	private void addMethods(StandardEvaluationContext ctxt) {
		if (methods.isEmpty()) {
			methods = getMethods();
		}

		methods.forEach(ctxt::registerFunction);
	}

	private Map<String, Method> getMethods() {
		try {
			Map<String, Method> methods = new HashMap<>();
			methods.put("containsAny", CollectionUtils.class.getDeclaredMethod("containsAny", Collection.class, Collection.class));
			methods.put("yearsBetween", Utility.class.getDeclaredMethod("yearsBetween", Date.class, Date.class));
			methods.put("monthsBetween", Utility.class.getDeclaredMethod("monthsBetween", Date.class, Date.class));
			methods.put("daysBetween", Utility.class.getDeclaredMethod("daysBetween", Date.class, Date.class));
			methods.put("cmp", Utility.class.getDeclaredMethod("cmp", Date.class, Date.class));
			return methods;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}
}
