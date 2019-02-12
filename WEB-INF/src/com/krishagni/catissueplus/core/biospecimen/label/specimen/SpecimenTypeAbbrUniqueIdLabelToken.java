package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.domain.AbstractUniqueIdToken;
import com.krishagni.catissueplus.core.common.util.PvUtil;

public class SpecimenTypeAbbrUniqueIdLabelToken extends AbstractUniqueIdToken<Specimen> {

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
		return StringUtils.isBlank(arg) || arg.trim().equals("registration") || arg.trim().equals("visit");
	}

	@Override
	public Number getUniqueId(Specimen specimen, String... args) {
		Long groupId = specimen.getVisit().getId();
		String keyType = "VISIT_" + getName();
		if (eqArg("registration", 2, args)) {
			keyType = "CPR_" + getName();
			groupId = specimen.getRegistration().getId();
		}

		String typeAbbr = getTypeAbbr(specimen);
		Long uniqueId = daoFactory.getUniqueIdGenerator().getUniqueId(keyType, groupId + "_" + typeAbbr);
		return uniqueId == 1L && !eqArg("output_one", 1, args) ? -1 : uniqueId;
	}

	private String getTypeAbbr(Specimen spmn) {
		return PvUtil.getInstance().getAbbr(PvAttributes.SPECIMEN_CLASS, spmn.getSpecimenType(), "");
	}
}
