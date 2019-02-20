package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;
import com.krishagni.catissueplus.core.common.util.PvUtil;

public class SpecimenTypeAbbrUniqueIdLabelToken extends AbstractUniqueIdToken<Specimen> {

	private static List<String> ALLOWED_TYPES = Arrays.asList("registration", "visit", "parent_specimen", "primary_specimen");

	@Autowired
	private DaoFactory daoFactory;

	public SpecimenTypeAbbrUniqueIdLabelToken() {
		this.name = "SP_TYPE_ABBR_UID";
	}

	public boolean areArgsValid(String... args) {
		if (!super.areArgsValid(args)) {
			return false;
		}

		String arg = getArg(1, args);
		if (StringUtils.isNotBlank(arg) && !arg.trim().equals("output_one") && !arg.trim().equals("default")) {
			return false;
		}

		arg = getArg(2, args);
		return StringUtils.isBlank(arg) || ALLOWED_TYPES.contains(arg.trim());
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		Long groupId = null;
		String keyType;

		String arg = getArg(2, args);
		if (StringUtils.isBlank(arg)) {
			arg = "visit";
		}

		arg = arg.trim();
		switch (arg) {
			case "registration":
				groupId = specimen.getRegistration().getId();
				keyType = "CPR_" + getName();
				break;

			case "parent_specimen":
				groupId = specimen.getParentSpecimen() != null ? specimen.getParentSpecimen().getId() : specimen.getId();
				keyType = "PARENT_SPMN_" + getName();
				break;

			case "primary_specimen":
				groupId = specimen.getPrimarySpecimen().getId();
				keyType = "PRIMARY_SPMN_" + getName();
				break;

			case "visit":
			default:
				groupId = specimen.getVisit().getId();
				keyType = "VISIT_" + getName();
				break;
		}

		String typeAbbr = getTypeAbbr(specimen);
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(keyType, groupId + "_" + typeAbbr);
		return uniqueId == 1L && !eqArg("output_one", 1, args) ? -1 : uniqueId;
	}

	private String getTypeAbbr(Specimen spmn) {
		return PvUtil.getInstance().getAbbr(PvAttributes.SPECIMEN_CLASS, spmn.getSpecimenType(), "");
	}
}
