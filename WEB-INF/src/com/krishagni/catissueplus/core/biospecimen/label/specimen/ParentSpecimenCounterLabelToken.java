package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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

		String counter = "0";
		int matchIdx = parentLabel.length();
		if (matcher.find()) {
			counter = matcher.group(0);
			matchIdx = matcher.start(0);
		}

		String pidStr = specimen.getParentSpecimen().getId().toString();
		String uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(name, pidStr, Long.parseLong(counter)).toString();
		if (uniqueId.length() < counter.length()) {
			uniqueId = StringUtils.leftPad(uniqueId, counter.length(), "0");
		}

		return parentLabel.substring(0, matchIdx) + uniqueId;
	}

	private final static Pattern LAST_DIGIT_PATTERN = Pattern.compile("([0-9]+)$");
}
