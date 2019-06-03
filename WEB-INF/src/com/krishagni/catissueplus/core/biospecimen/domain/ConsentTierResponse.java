package com.krishagni.catissueplus.core.biospecimen.domain;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;

@Audited
@AuditTable(value="CAT_CONSENT_TIER_RESPONSE_AUD")
public class ConsentTierResponse {

	private static final String ENTITY_NAME = "consent_response";

	private Long id;

	private PermissibleValue response;

	private ConsentTier consentTier;

	private CollectionProtocolRegistration cpr;
	
	public static String getEntityName() {
		return ENTITY_NAME;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getResponse() {
		return response;
	}

	public void setResponse(PermissibleValue response) {
		this.response = response;
	}

	@NotAudited
	public ConsentTier getConsentTier() {
		return consentTier;
	}

	public void setConsentTier(ConsentTier consentTier) {
		this.consentTier = consentTier;
	}

	@NotAudited
	public CollectionProtocolRegistration getCpr() {
		return cpr;
	}

	public void setCpr(CollectionProtocolRegistration cpr) {
		this.cpr = cpr;
	}

	public String getStatement() {
		return getConsentTier().getStatement().getStatement();
	}

	public String getStatementCode() {
		return getConsentTier().getStatement().getCode();
	}

	public ConsentTierResponse copy() {
		ConsentTierResponse copy = new ConsentTierResponse();
		BeanUtils.copyProperties(this, copy);
		return copy;
	}
}