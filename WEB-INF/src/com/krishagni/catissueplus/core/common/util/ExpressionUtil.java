package com.krishagni.catissueplus.core.common.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class ExpressionUtil {
	private static final Log logger = LogFactory.getLog(ExpressionUtil.class);

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
		SpelExpression expr = parser.parseRaw(exprStr);
		Set<String> variables = getVariables(expr.getAST(), new HashSet<>());

		String finalExpr = expr.toStringAST();
		for (String var : variables) {
			finalExpr = finalExpr.replaceAll(var + " ", "#" + var + " ");
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("The expression %s is rewritten as %s", exprStr, finalExpr));
		}
		return parser.parseRaw(finalExpr);
	}

	private Set<String> getVariables(SpelNode node, Set<String> result) {
		if (node instanceof CompoundExpression) {
			result.add(node.toStringAST());
		}

		for (int i = 0; i < node.getChildCount(); ++i) {
			getVariables(node.getChild(i), result);
		}

		return result;
	}
}
