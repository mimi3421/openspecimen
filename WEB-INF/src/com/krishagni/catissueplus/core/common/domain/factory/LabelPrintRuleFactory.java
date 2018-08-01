package com.krishagni.catissueplus.core.common.domain.factory;

import java.util.Map;

import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;

public interface LabelPrintRuleFactory {
	LabelPrintRule createLabelPrintRule(Map<String, String> ruleDef);
}
