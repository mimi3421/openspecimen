package com.krishagni.catissueplus.core.common.service;

import java.util.Map;

public interface TemplateService {
	String render(String templateName, Map<String, Object> properties);
}
