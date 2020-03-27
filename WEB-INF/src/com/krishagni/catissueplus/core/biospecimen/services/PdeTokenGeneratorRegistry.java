package com.krishagni.catissueplus.core.biospecimen.services;

public interface PdeTokenGeneratorRegistry {
	PdeTokenGenerator get(String type);

	void register(String type, PdeTokenGenerator generator);
}
