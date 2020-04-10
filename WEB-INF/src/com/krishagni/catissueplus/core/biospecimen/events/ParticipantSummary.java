
package com.krishagni.catissueplus.core.biospecimen.events;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;

public class ParticipantSummary {
	private Long id;

	private String source;

	private String firstName = "";

	private String lastName = "";
	
	private String empi;

	private String uid;

	private String emailAddress;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmpi() {
		return empi;
	}

	public void setEmpi(String empi) {
		this.empi = empi;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public static ParticipantSummary from(Participant p, boolean excludePhi) {
		ParticipantSummary result = new ParticipantSummary();
		result.setId(p.getId());
		result.setSource(p.getSource());
		if (excludePhi) {
			return result;
		}

		result.setFirstName(p.getFirstName());
		result.setLastName(p.getLastName());
		result.setEmpi(p.getEmpi());
		result.setUid(p.getUid());
		result.setEmailAddress(p.getEmailAddress());
		return result;
	}
}
