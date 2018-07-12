package com.krishagni.catissueplus.core.common.access;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class SiteCpPair {
	private Long instituteId;

	private Long siteId;

	private Long cpId;

	public Long getInstituteId() {
		return instituteId;
	}

	public void setInstituteId(Long instituteId) {
		this.instituteId = instituteId;
	}

	public Long getSiteId() {
		return siteId;
	}

	public void setSiteId(Long siteId) {
		this.siteId = siteId;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public boolean isAllowed(SiteCpPair other) {
		return (getSiteId() != null && getSiteId().equals(other.getSiteId())) ||
			(getSiteId() == null && getInstituteId().equals(other.getInstituteId()));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SiteCpPair that = (SiteCpPair) o;
		return Objects.equals(getInstituteId(), that.getInstituteId()) &&
			Objects.equals(getSiteId(), that.getSiteId()) &&
			Objects.equals(getCpId(), that.getCpId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getInstituteId(), getSiteId(), getCpId());
	}

	public static SiteCpPair make(Long instituteId, Long siteId, Long cpId) {
		SiteCpPair result = new SiteCpPair();
		result.setInstituteId(instituteId);
		result.setSiteId(siteId);
		result.setCpId(cpId);
		return result;
	}

	public static boolean contains(Collection<SiteCpPair> domainSites, SiteCpPair testSite) {
		return contains(domainSites, Collections.singleton(testSite));
	}

	public static boolean contains(Collection<SiteCpPair> domainSites, Collection<SiteCpPair> testSites) {
		return contains(domainSites, testSites, false);
	}

	public static boolean containsAll(Collection<SiteCpPair> domainSites, Collection<SiteCpPair> testSites) {
		return contains(domainSites, testSites, true);
	}

	private static boolean contains(Collection<SiteCpPair> domainSites, Collection<SiteCpPair> testSites, boolean allSites) {
		boolean result = true;
		for (SiteCpPair testSite : testSites) {
			boolean allowed = false;
			for (SiteCpPair domainSite : domainSites) {
				if (testSite.getSiteId() == null) {
					allowed = (!allSites || domainSite.getSiteId() == null) && domainSite.getInstituteId().equals(testSite.getInstituteId());
				} else {
					allowed = (domainSite.getSiteId() != null && domainSite.getSiteId().equals(testSite.getSiteId())) ||
						(domainSite.getSiteId() == null && domainSite.getInstituteId().equals(testSite.getInstituteId()));
				}

				if (allowed) {
					break;
				}
			}

			if (allSites) {
				if (!allowed) {
					result = false;
					break;
				} else {
					result = true;
				}
			} else {
				if (allowed) {
					result = true;
					break;
				} else {
					result = false;
				}
			}
		}

		return result;
	}
}
