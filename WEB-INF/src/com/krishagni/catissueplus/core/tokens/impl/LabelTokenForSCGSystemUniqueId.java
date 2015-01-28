
package com.krishagni.catissueplus.core.tokens.impl;

import org.springframework.context.ApplicationContext;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.util.KeyGenFactory;
import com.krishagni.catissueplus.core.tokens.LabelToken;

public class LabelTokenForSCGSystemUniqueId implements LabelToken<Visit> {

	private static String SCG_UNIQUE_ID = "SCG_UID";

	@Override
	public String getTokenValue(Visit scg) {
		ApplicationContext appCtx = OpenSpecimenAppCtxProvider.getAppCtx();
		KeyGenFactory keyFactory = (KeyGenFactory) appCtx.getBean("keyFactory");
		Long value = keyFactory.getValueByKey(SCG_UNIQUE_ID, SCG_UNIQUE_ID);
		return value.toString();
	}

}
