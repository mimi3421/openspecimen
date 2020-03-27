package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.services.PdeTokenGenerator;
import com.krishagni.catissueplus.core.biospecimen.services.PdeTokenGeneratorRegistry;

public class DefaultPdeTokenGeneratorRegistry implements PdeTokenGeneratorRegistry {
	private Map<String, PdeTokenGenerator> generators = new HashMap<>();

	@Override
	public PdeTokenGenerator get(String type) {
		return generators.get(type);
	}

	@Override
	public void register(String type, PdeTokenGenerator generator) {
		generators.put(type, generator);
	}
}
