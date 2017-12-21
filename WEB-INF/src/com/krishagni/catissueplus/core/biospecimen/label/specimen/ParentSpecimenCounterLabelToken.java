package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

public class ParentSpecimenCounterLabelToken extends AbstractSpecimenLabelToken {

	@Autowired
	private DaoFactory daoFactory;

	public ParentSpecimenCounterLabelToken() {
		this.name = "PSPEC_COUNTER";
	}

	@Override
	public String getLabel(Specimen specimen) {
		if (specimen.getParentSpecimen() == null) {
			return null;
		}

		String parentLabel = specimen.getParentSpecimen().getLabel();
		Matcher matcher = LAST_DIGIT_PATTERN.matcher(parentLabel);

		Long counter = 0L;
		int matchIdx = parentLabel.length();
		if (matcher.find()) {
			counter = Long.parseLong(matcher.group(0));
			matchIdx = matcher.start(0);
		}

		String pidStr = specimen.getParentSpecimen().getId().toString();
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(name, pidStr, counter);
		return parentLabel.substring(0, matchIdx) + uniqueId.toString();
	}

	private final static Pattern LAST_DIGIT_PATTERN = Pattern.compile("([0-9]+)$");
}
