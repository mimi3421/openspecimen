package com.krishagni.catissueplus.core.init;

import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class AppPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
	protected String resolvePlaceholder(String placeholder, Properties props) {
		return AppProperties.getInstance().getProperties().getProperty(placeholder);
	}
}