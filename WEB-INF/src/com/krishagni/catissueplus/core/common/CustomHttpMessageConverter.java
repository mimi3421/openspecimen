package com.krishagni.catissueplus.core.common;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class CustomHttpMessageConverter extends MappingJackson2HttpMessageConverter {

	public CustomHttpMessageConverter() {
		super(
			Jackson2ObjectMapperBuilder.json().filters(
				new SimpleFilterProvider()
					.addFilter("withoutId", SimpleBeanPropertyFilter.serializeAllExcept())
			).build()
		);
	}
}