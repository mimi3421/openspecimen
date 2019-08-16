package com.krishagni.catissueplus.core.administrative.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

@ListenAttributeChanges
public class DistributionProtocolSummary extends AttributeModifiedSupport {
	private Long id;
	
	private String title;

	private String shortTitle;
	
	private UserSummary principalInvestigator;
	
	private Date startDate;
	
	private Date endDate;

	private String defReceivingSiteName;

	private int distributedSpecimensCount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getShortTitle() {
		return shortTitle;
	}

	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	public UserSummary getPrincipalInvestigator() {
		return principalInvestigator;
	}

	public void setPrincipalInvestigator(UserSummary principalInvestigator) {
		this.principalInvestigator = principalInvestigator;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getDefReceivingSiteName() {
		return defReceivingSiteName;
	}

	public void setDefReceivingSiteName(String defReceivingSiteName) {
		this.defReceivingSiteName = defReceivingSiteName;
	}

	public int getDistributedSpecimensCount() {
		return distributedSpecimensCount;
	}

	public void setDistributedSpecimensCount(int distributedSpecimensCount) {
		this.distributedSpecimensCount = distributedSpecimensCount;
	}
	
	public static DistributionProtocolSummary from(DistributionProtocol dp) {
		DistributionProtocolSummary summary = new DistributionProtocolSummary();
		copy(dp, summary);
		return summary;
	}
	
	public static List<DistributionProtocolSummary> from(Collection<DistributionProtocol> dps) {
		return Utility.nullSafeStream(dps).map(DistributionProtocolSummary::from).collect(Collectors.toList());
	}
	
	public static void copy(DistributionProtocol dp, DistributionProtocolSummary detail) {
		detail.setId(dp.getId());
		detail.setTitle(dp.getTitle());
		detail.setShortTitle(dp.getShortTitle());
		detail.setPrincipalInvestigator(UserSummary.from(dp.getPrincipalInvestigator()));
		detail.setStartDate(dp.getStartDate());
		detail.setEndDate(dp.getEndDate());
		if (dp.getDefReceivingSite() != null) {
			detail.setDefReceivingSiteName(dp.getDefReceivingSite().getName());
		}
	}
}
