package com.krishagni.catissueplus.core.common.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSite;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.Operation;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.services.impl.ExporterContextHolder;
import com.krishagni.catissueplus.core.importer.services.impl.ImporterContextHolder;
import com.krishagni.rbac.common.errors.RbacErrorCode;
import com.krishagni.rbac.domain.Subject;
import com.krishagni.rbac.domain.SubjectAccess;
import com.krishagni.rbac.domain.SubjectRole;
import com.krishagni.rbac.repository.DaoFactory;

@Configurable
public class AccessCtrlMgr {

	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory bsDaoFactory;

	private static AccessCtrlMgr instance;

	private AccessCtrlMgr() {
	}

	public static AccessCtrlMgr getInstance() {
		if (instance == null || instance.daoFactory == null || instance.bsDaoFactory == null) {
			instance = new AccessCtrlMgr();
		}

		return instance;
	}

	public void ensureUserIsAdmin() {
		User user = AuthUtil.getCurrentUser();

		if (!user.isAdmin()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          User object access control helper methods                               //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureCreateUserRights(User user) {
		ensureUserObjectRights(user, Operation.CREATE);
		ensureUserEximRights(user);
	}

	public void ensureUpdateUserRights(User user) {
		ensureUserObjectRights(user, Operation.UPDATE);
		ensureUserEximRights(user);
	}

	public void ensureDeleteUserRights(User user) {
		ensureUserObjectRights(user, Operation.DELETE);
		ensureUserEximRights(user);
	}

	public void ensureCreateUpdateUserRolesRights(User user, Site roleSite) {
		//
		// ensure the role site belongs to user's institute
		//
		if (roleSite != null && !roleSite.getInstitute().equals(user.getInstitute())) {
			throw OpenSpecimenException.userError(
				SiteErrorCode.INVALID_SITE_INSTITUTE, roleSite.getName(), user.getInstitute().getName());
		}

		if (AuthUtil.isAdmin()) {
			return;
		}
		
		if (AuthUtil.isInstituteAdmin() && user.getInstitute().equals(AuthUtil.getCurrentUserInstitute())) {
			return;
		}
		
		Set<Site> currentUserSites = getSites(Resource.USER, Operation.UPDATE);
		Set<Site> updateReqSites = null;
		if (roleSite == null) {				//this is case of all sites.
			updateReqSites = user.getInstitute().getSites();
		} else {
			updateReqSites = Collections.singleton(roleSite);
		}
		
		if (CollectionUtils.intersection(currentUserSites, updateReqSites).isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		ensureUserEximRights(user);
	}

	public List<User> getSuperAndSiteAdmins(Site site, CollectionProtocol cp) {
		List<User> result = getSuperAdmins();
		result.addAll(getSiteAdmins(site, cp));
		return result;
	}

	public List<User> getSuperAdmins() {
		UserListCriteria crit = new UserListCriteria().activityStatus("Active").type("SUPER");
		return bsDaoFactory.getUserDao().getUsers(crit);
	}

	public List<User> getSiteAdmins(Site site, CollectionProtocol cp) {
		List<User> result = new ArrayList<>();
		if (site != null) {
			result.addAll(site.getCoordinators());
		} else if (cp != null) {
			result.addAll(cp.getSites().stream()
				.map(CollectionProtocolSite::getSite)
				.flatMap(s -> s.getCoordinators().stream())
				.collect(Collectors.toList()));
		}

		return result;
	}

	private void ensureUserObjectRights(User user, Operation op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		if (user.isAdmin() && op != Operation.READ) {
			throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
		}

		if (!canUserPerformOp(AuthUtil.getCurrentUser().getId(), Resource.USER, new Operation[] {op})) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureUserEximRights(User user) {
		if (isImportOp() || isExportOp()) {
			ensureUserObjectRights(user, Operation.EXIM);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Site object access control helper methods                               //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureCreateUpdateDeleteSiteRights(Site site) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		boolean allowed = false;
		if (AuthUtil.isInstituteAdmin()) {
			allowed = AuthUtil.getCurrentUser().getInstitute().equals(site.getInstitute());
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.INST_ADMIN_RIGHTS_REQ, site.getInstitute().getName());
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Distribution Protocol object access control helper methods              //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public Set<Long> getReadAccessDistributionProtocolSites() {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		return Utility.collect(getSites(Resource.DP, Operation.READ), "id", true);
	}

	public void ensureReadDpRights(DistributionProtocol dp) {
		ensureDpObjectRights(dp, new Operation[] {Operation.READ}, false);
	}

	public void ensureCreateUpdateDpRights(DistributionProtocol dp) {
		ensureDpObjectRights(dp, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}

	public void ensureDeleteDpRights(DistributionProtocol dp) {
		ensureDpObjectRights(dp, new Operation[] {Operation.DELETE});
	}

	private void ensureDpObjectRights(DistributionProtocol dp, Operation[] ops) {
		ensureDpObjectRights(dp, ops, true);
	}

	private void ensureDpObjectRights(DistributionProtocol dp, Operation[] ops, boolean allSites) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<Site> allowedSites = getSites(Resource.DP, ops);
		if (allSites) {
			if (!allowedSites.containsAll(dp.getAllDistributingSites())) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
		} else {
			if (CollectionUtils.intersection(allowedSites, dp.getAllDistributingSites()).isEmpty()) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Collection Protocol object access control helper methods                //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public Set<Long> getReadableCpIds() {
		return getEligibleCpIds(Resource.CP.getName(), new String[] {Operation.READ.getName()}, null);
	}

	public Set<Long> getRegisterEnabledCpIds(List<String> siteNames) {
		return getEligibleCpIds(Resource.PARTICIPANT.getName(), new String[] {Operation.CREATE.getName()}, siteNames);
	}

	//
	// Returns list of IDs of users who can perform "ops" on "resource" belonging
	// to collection protocol identified by "cpId"
	//
	public List<Long> getUserIds(Long cpId, Resource resource, Operation[] ops) {
		String[] opsStr = new String[ops.length];
		for (int i = 0; i < ops.length; ++i) {
			opsStr[i] = ops[i].getName();
		}

		return daoFactory.getSubjectDao().getSubjectIds(cpId, resource.getName(), opsStr);
	}

	public void ensureCreateCpRights(CollectionProtocol cp) {
		ensureCpObjectRights(cp, Operation.CREATE);
	}

	public void ensureReadCpRights(CollectionProtocol cp) {
		ensureCpObjectRights(cp, Operation.READ);
	}

	public void ensureUpdateCpRights(Long cpId) {
		CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
		if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpId);
		}

		ensureUpdateCpRights(cp);
	}

	public void ensureUpdateCpRights(CollectionProtocol cp) {
		ensureCpObjectRights(cp, Operation.UPDATE);
	}

	public void ensureDeleteCpRights(CollectionProtocol cp) {
		ensureCpObjectRights(cp, Operation.DELETE);
	}

	private void ensureCpObjectRights(CollectionProtocol cp, Operation op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		String resource = Resource.CP.getName();
		String[] ops = {op.getName()};

		boolean allowed = false;
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		for (SubjectAccess access : accessList) {
			Site accessSite = access.getSite();
			CollectionProtocol accessCp = access.getCollectionProtocol();

			if (accessSite != null && accessCp != null && accessCp.equals(cp)) {
				//
				// Specific CP
				//
				allowed = true;
			} else if (accessSite != null && accessCp == null && cp.getRepositories().contains(accessSite)) {
				//
				// TODO: 
				// Current implementation is at least one site is CP repository. We do not check whether permission is
				// for all CP repositories.
				//
				// All CPs of a site
				//
				allowed = true;
			} else if (accessSite == null && (accessCp == null || accessCp.equals(cp))) {
				//
				// All CPs or specific CP 
				//
				
				allowed = true;
			}
			
			if (allowed) {
				break;
			}
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Participant object access control helper methods                        //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public static class ParticipantReadAccess {
		public boolean admin;

		//
		// phiSiteCps and siteCps is used for all accessible CPs
		// [{[site ids], cp id}]
		//
		public Set<Pair<Set<Long>, Long>> phiSiteCps;

		public Set<Pair<Set<Long>, Long>> siteCps;

		public boolean phiAccess;

		public boolean noAccessibleSites() {
			return CollectionUtils.isEmpty(siteCps);
		}
	}

	public ParticipantReadAccess getParticipantReadAccess() {
		ParticipantReadAccess result = new ParticipantReadAccess();
		result.phiAccess = true;

		if (AuthUtil.isAdmin()) {
			result.admin = true;
			return result;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		String[] ops = {Operation.READ.getName()};
		String resource = Resource.PARTICIPANT.getName();
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);

		resource = Resource.PARTICIPANT_DEID.getName();
		accessList.addAll(daoFactory.getSubjectDao().getAccessList(userId, resource, ops));

		Set<Pair<Set<Long>, Long>> phiSiteCps = new HashSet<>();
		Set<Pair<Set<Long>, Long>> siteCps = new HashSet<>();
		for (SubjectAccess access : accessList) {
			Set<Site> sites;
			if (access.getSite() != null) {
				sites = Collections.singleton(access.getSite());
			} else {
				sites = getUserInstituteSites(userId);
			}

			Set<Long> siteIds = sites.stream().map(Site::getId).collect(Collectors.toSet());
			Long cpId = access.getCollectionProtocol() != null ? access.getCollectionProtocol().getId() : null;
			if (Resource.PARTICIPANT.getName().equals(access.getResource())) {
				phiSiteCps.add(Pair.make(siteIds, cpId));
			}

			siteCps.add(Pair.make(siteIds, cpId));
		}

		result.phiSiteCps = phiSiteCps;
		result.siteCps = siteCps;
		result.phiAccess = CollectionUtils.isNotEmpty(phiSiteCps);
		return result;
	}

	public ParticipantReadAccess getParticipantReadAccess(Long cpId) {
		if (cpId == null || cpId == -1L) {
			return getParticipantReadAccess();
		}

		ParticipantReadAccess result = new ParticipantReadAccess();
		result.phiAccess = true;

		if (AuthUtil.isAdmin()) {
			result.admin = true;
			return result;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		String resource = Resource.PARTICIPANT.getName();
		String[] ops = {Operation.READ.getName()};
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
		if (accessList.isEmpty()) {
			resource = Resource.PARTICIPANT_DEID.getName();
			accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
			result.phiAccess = false;
		}

		Set<Pair<Set<Long>, Long>> siteCps = new HashSet<>();
		for (SubjectAccess access : accessList) {
			Site accessSite = access.getSite();

			if (accessSite != null) {
				siteCps.add(Pair.make(Collections.singleton(accessSite.getId()), cpId));
			} else {
				Set<Site> sites = getUserInstituteSites(userId);
				siteCps.add(Pair.make(sites.stream().map(Site::getId).collect(Collectors.toSet()), cpId));
			}
		}

		result.siteCps = siteCps;
		if (result.phiAccess) {
			result.phiSiteCps = result.siteCps;
		}

		return result;
	}
	
	public boolean ensurePhiRights(CollectionProtocolRegistration cpr, Operation op) {
		if (op == Operation.CREATE || op == Operation.UPDATE) {
			return ensureCprObjectRights(cpr, Operation.UPDATE);
		} else {
			return ensureCprObjectRights(cpr, Operation.READ);
		}
	}
	
	public boolean ensureReadParticipantRights(Long participantId) {
		return ensureParticipantObjectRights(participantId, Operation.READ);
	}

	public boolean ensureUpdateParticipantRights(Participant participant) {
		return ensureParticipantObjectRights(participant, Operation.UPDATE);
	}

	public List<CollectionProtocolRegistration> getAccessibleCprs(Collection<CollectionProtocolRegistration> cprs) {
		return getAccessibleCprs(cprs, Operation.READ);
	}

	public List<CollectionProtocolRegistration> getAccessibleCprs(Collection<CollectionProtocolRegistration> cprs, Operation op) {
		return cprs.stream().filter(cpr -> {
				try {
					ensureCprObjectRights(cpr, op);
					return true;
				} catch (OpenSpecimenException e) {
					return false;
				}
			}
		).collect(Collectors.toList());
	}

	public boolean ensureCreateCprRights(Long cprId) {
		return ensureCprObjectRights(cprId, Operation.CREATE);
	}

	public boolean ensureCreateCprRights(CollectionProtocolRegistration cpr) {
		boolean phiAccess = ensureCprObjectRights(cpr, Operation.CREATE);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	public boolean ensureReadCprRights(Long cprId) {
		return ensureCprObjectRights(cprId, Operation.READ);
	}

	public boolean ensureReadCprRights(CollectionProtocolRegistration cpr) {
		boolean phiAccess = ensureCprObjectRights(cpr, Operation.READ);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	public void ensureUpdateCprRights(Long cprId) {
		ensureCprObjectRights(cprId, Operation.UPDATE);
	}

	public boolean ensureUpdateCprRights(CollectionProtocolRegistration cpr) {
		boolean phiAccess = ensureCprObjectRights(cpr, Operation.UPDATE);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	public void ensureDeleteCprRights(Long cprId) {
		ensureCprObjectRights(cprId, Operation.DELETE);
	}

	public boolean ensureDeleteCprRights(CollectionProtocolRegistration cpr) {
		boolean phiAccess = ensureCprObjectRights(cpr, Operation.DELETE);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	private boolean ensureCprObjectRights(Long cprId, Operation op) {
		CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(cprId);
		if (cpr == null) {
			throw OpenSpecimenException.userError(CprErrorCode.NOT_FOUND);
		}

		boolean phiAccess = ensureCprObjectRights(cpr, op);
		ensureCprEximRights(cpr);
		return phiAccess;
	}

	private boolean ensureParticipantObjectRights(Long participantId, Operation op) {
		Participant participant = daoFactory.getParticipantDao().getById(participantId);
		return ensureParticipantObjectRights(participant, op);
	}

	private boolean ensureParticipantObjectRights(Participant p, Operation op) {
		for (CollectionProtocolRegistration cpr : p.getCprs()) {
			try {
				return ensureCprObjectRights(cpr, op);
			} catch (OpenSpecimenException ose) {

			}
		}

		throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
	}

	private boolean ensureCprObjectRights(CollectionProtocolRegistration cpr, Operation op) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		boolean phiAccess = true;
		Long cpId = cpr.getCollectionProtocol().getId();
		String resource = Resource.PARTICIPANT.getName();
		Long userId = AuthUtil.getCurrentUser().getId();
		String[] ops = {op.getName()};

		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
		Set<Site> cpSites = cpr.getCollectionProtocol().getRepositories();
		boolean allowed = isAccessAllowedOnAnySite(accessList, cpSites, userId);
		if (!allowed && op == Operation.READ) {
			phiAccess = false;
			resource = Resource.PARTICIPANT_DEID.getName();
			accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
			allowed = isAccessAllowedOnAnySite(accessList, cpSites, userId);
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
		
		if (!isAccessRestrictedBasedOnMrn()) {
			return phiAccess;
		}

		Set<Site> mrnSites = cpr.getParticipant().getMrnSites();
		if (mrnSites.isEmpty()) {
			return phiAccess;
		}

		allowed = isAccessAllowedOnAnySite(accessList, mrnSites, userId);
		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		return phiAccess;
	}

	private void ensureCprEximRights(CollectionProtocolRegistration cpr) {
		if (isImportOp() || isExportOp()) {
			ensureCprObjectRights(cpr, Operation.EXIM);
		}
	}

	public boolean canCreateUpdateParticipant() {
		return canUserPerformOp(Resource.PARTICIPANT, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Visit and Specimen object access control helper methods                 //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureCreateOrUpdateVisitRights(Long visitId, boolean checkPhiAccess) {
		ensureVisitObjectRights(visitId, Operation.UPDATE, checkPhiAccess);
	}

	public void ensureCreateOrUpdateVisitRights(Visit visit) {
		ensureCreateOrUpdateVisitRights(visit, visit.hasPhiFields());
	}

	public void ensureCreateOrUpdateVisitRights(Visit visit, boolean checkPhiAccess) {
		ensureVisitAndSpecimenObjectRights(visit.getRegistration(), Operation.UPDATE, checkPhiAccess);
		ensureVisitAndSpecimenEximRights(visit.getRegistration());
	}

	public boolean ensureReadVisitRights(Long visitId) {
		return ensureReadVisitRights(visitId, false);
	}

	public boolean ensureReadVisitRights(Long visitId, boolean checkPhiAccess) {
		return ensureVisitObjectRights(visitId, Operation.READ, checkPhiAccess);
	}

	public boolean ensureReadVisitRights(Visit visit) {
		return ensureReadVisitRights(visit, visit.hasPhiFields());
	}

	public boolean ensureReadVisitRights(Visit visit, boolean checkPhiAccess) {
		return ensureReadVisitRights(visit.getRegistration(), checkPhiAccess);
	}

	public boolean ensureReadVisitRights(CollectionProtocolRegistration cpr, boolean checkPhiAccess) {
		boolean phiAccess = ensureVisitAndSpecimenObjectRights(cpr, Operation.READ, checkPhiAccess);
		ensureVisitAndSpecimenEximRights(cpr);
		return phiAccess;
	}

	public void ensureDeleteVisitRights(Visit visit) {
		ensureVisitAndSpecimenObjectRights(visit.getRegistration(), Operation.DELETE, false);
		ensureVisitAndSpecimenEximRights(visit.getRegistration());
	}
	
	public void ensureCreateOrUpdateSpecimenRights(Long specimenId, boolean checkPhiAccess) {
		ensureSpecimenObjectRights(specimenId, Operation.UPDATE, checkPhiAccess);
	}

	public void ensureCreateOrUpdateSpecimenRights(Specimen specimen) {
		ensureCreateOrUpdateSpecimenRights(specimen, specimen.hasPhiFields());
	}

	public void ensureCreateOrUpdateSpecimenRights(Specimen specimen, boolean checkPhiAccess) {
		ensureVisitAndSpecimenObjectRights(specimen.getRegistration(), Operation.UPDATE, checkPhiAccess);
		ensureVisitAndSpecimenEximRights(specimen.getRegistration());
	}

	public boolean ensureReadSpecimenRights(Long specimenId) {
		return ensureReadSpecimenRights(specimenId, false);
	}

	public boolean ensureReadSpecimenRights(Long specimenId, boolean checkPhiAccess) {
		return ensureSpecimenObjectRights(specimenId, Operation.READ, checkPhiAccess);
	}

	public boolean ensureReadSpecimenRights(Specimen specimen) {
		return ensureReadSpecimenRights(specimen, specimen.hasPhiFields());
	}

	public boolean ensureReadSpecimenRights(Specimen specimen, boolean checkPhiAccess) {
		return ensureReadSpecimenRights(specimen.getRegistration(), checkPhiAccess);
	}

	public boolean ensureReadSpecimenRights(CollectionProtocolRegistration cpr, boolean checkPhiAccess) {
		boolean phiAccess = ensureVisitAndSpecimenObjectRights(cpr, Operation.READ, checkPhiAccess);
		ensureVisitAndSpecimenEximRights(cpr);
		return phiAccess;
	}

	public void ensureDeleteSpecimenRights(Specimen specimen) {
		ensureVisitAndSpecimenObjectRights(specimen.getRegistration(), Operation.DELETE, false);
		ensureVisitAndSpecimenEximRights(specimen.getRegistration());
	}

	public List<Pair<Long, Long>> getReadAccessSpecimenSiteCps() {
		return getReadAccessSpecimenSiteCps(null);
	}

	public List<Pair<Long, Long>> getReadAccessSpecimenSiteCps(Long cpId) {
		return getReadAccessSpecimenSiteCps(cpId, true);
	}

	public List<Pair<Long, Long>> getReadAccessSpecimenSiteCps(Long cpId, boolean addOrderSites) {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		String[] ops = {Operation.READ.getName()};
		List<Pair<Long, Long>> siteCpPairs = new ArrayList<>(getVisitAndSpecimenSiteCps(cpId, ops));
		if (addOrderSites) {
			siteCpPairs.addAll(getDistributionOrderSiteCps(ops));
			siteCpPairs = deDupSiteCpPairs(siteCpPairs);
		}

		return siteCpPairs;
	}

	private boolean ensureVisitObjectRights(Long visitId, Operation op, boolean checkPhiAccess) {
		Visit visit = daoFactory.getVisitDao().getById(visitId);
		if (visit == null) {
			throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND);
		}

		boolean phiAccess = ensureVisitObjectRights(visit, op, checkPhiAccess);
		ensureVisitAndSpecimenEximRights(visit.getRegistration());
		return phiAccess;
	}

	private boolean ensureVisitObjectRights(Visit visit, Operation op, boolean checkPhiAccess) {
		return ensureVisitAndSpecimenObjectRights(visit.getRegistration(), op, checkPhiAccess);
	}
	
	private boolean ensureSpecimenObjectRights(Long specimenId, Operation op, boolean checkPhiAccess) {
		Specimen specimen = daoFactory.getSpecimenDao().getById(specimenId);
		if (specimen == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, specimenId);
		}

		boolean phiAccess = ensureVisitAndSpecimenObjectRights(specimen.getRegistration(), op, checkPhiAccess);
		ensureVisitAndSpecimenEximRights(specimen.getRegistration());
		return phiAccess;
	}

	private boolean ensureVisitAndSpecimenObjectRights(CollectionProtocolRegistration cpr, Operation op, boolean checkPhiAccess) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		String[] ops = null;
		if (op == Operation.CREATE || op == Operation.UPDATE) {
			ops = new String[]{Operation.CREATE.getName(), Operation.UPDATE.getName()};
		} else {
			ops = new String[]{op.getName()};
		}

		ensureVisitAndSpecimenObjectRights(cpr, Resource.VISIT_N_SPECIMEN, ops);
		return checkPhiAccess ? ensurePhiRights(cpr, op) : false;
	}

	private void ensureVisitAndSpecimenEximRights(CollectionProtocolRegistration registration) {
		if (isImportOp() || isExportOp()) {
			ensureVisitAndSpecimenObjectRights(registration, Operation.EXIM, false);
		}
	}

	private Set<Pair<Long, Long>> getVisitAndSpecimenSiteCps(Long cpId, String[] ops) {
		return getSiteCps(Resource.VISIT_N_SPECIMEN.getName(), cpId, ops);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//         Container type object access control helper methods                      //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureReadContainerTypeRights() {
		if (!canUserPerformOp(Resource.STORAGE_CONTAINER, new Operation[] {Operation.READ})) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		} 
	}

	public void ensureCreateOrUpdateContainerTypeRights() {
		if (AuthUtil.isAdmin() || AuthUtil.isInstituteAdmin()) {
			return;
		}

		throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Storage container object access control helper methods                  //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public Set<Pair<Long, Long>> getReadAccessContainerSiteCps() {
		return getReadAccessContainerSiteCps(null);
	}

	public Set<Pair<Long, Long>> getReadAccessContainerSiteCps(Long cpId) {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		String[] ops = {Operation.READ.getName()};
		return getSiteCps(Resource.STORAGE_CONTAINER.getName(), cpId, ops);
	}

	public void ensureCreateContainerRights(StorageContainer container) {
		ensureStorageContainerObjectRights(container, Operation.CREATE);
		ensureStorageContainerEximRights(container);
	}

	public void ensureReadContainerRights(StorageContainer container) {
		ensureStorageContainerObjectRights(container, Operation.READ);
		ensureStorageContainerEximRights(container);
	}

	public void ensureSpecimenStoreRights(StorageContainer container) {
		ensureSpecimenStoreRights(container, null);
	}

	public void ensureSpecimenStoreRights(StorageContainer container, OpenSpecimenException result) {
		try {
			ensureReadContainerRights(container);
		} catch (OpenSpecimenException ose) {
			boolean throwError = false;
			if (result == null) {
				result = new OpenSpecimenException(ErrorType.USER_ERROR);
				throwError = true;
			}

			if (ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
				result.addError(SpecimenErrorCode.CONTAINER_ACCESS_DENIED, container.getName());
			} else {
				result.addErrors(ose.getErrors());
			}

			if (throwError) {
				throw result;
			}
		}
	}

	public void ensureUpdateContainerRights(StorageContainer container) {
		ensureStorageContainerObjectRights(container, Operation.UPDATE);
		ensureStorageContainerEximRights(container);
	}

	public void ensureDeleteContainerRights(StorageContainer container) {
		ensureStorageContainerObjectRights(container, Operation.DELETE);
		ensureStorageContainerEximRights(container);
	}

	private void ensureStorageContainerObjectRights(StorageContainer container, Operation op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		String resource = Resource.STORAGE_CONTAINER.getName();
		String[] ops = {op.getName()};

		boolean allowed = false;
		Set<CollectionProtocol> allowedCps = container.getCompAllowedCps();
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		for (SubjectAccess access : accessList) {
			if (isAccessAllowedOnSite(access.getSite(), container.getSite(), userId)) {
				CollectionProtocol accessCp = access.getCollectionProtocol();
				allowed = (accessCp == null || CollectionUtils.isEmpty(allowedCps) || allowedCps.contains(accessCp));
			}

			if (allowed) {
				break;
			}
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureStorageContainerEximRights(StorageContainer container) {
		if (isImportOp() || isExportOp()) {
			ensureStorageContainerObjectRights(container, Operation.EXIM);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Distribution order access control helper methods                        //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	
	public Set<Long> getReadAccessDistributionOrderSites() {
		if (AuthUtil.isAdmin()) {
			return null;
		}
		
		return Utility.<Set<Long>>collect(getSites(Resource.ORDER, Operation.READ), "id", true);
	}
	
	public boolean canCreateUpdateDistributionOrder() {
		return canUserPerformOp(Resource.ORDER, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}
	
	public Set<Long> getCreateUpdateAccessDistributionOrderSiteIds() {
		Set<Site> sites = getCreateUpdateAccessDistributionOrderSites();
		if (sites == null) {
			return null;
		}

		return Utility.collect(sites, "id", true);
	}

	public Set<Site> getCreateUpdateAccessDistributionOrderSites() {
		if (AuthUtil.isAdmin()) {
			return null;
		}
		
		return getSites(Resource.ORDER, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}
	
	@SuppressWarnings("unchecked")
	public Set<Long> getDistributionOrderAllowedSites(DistributionProtocol dp) {
		Set<Site> allowedSites = null;
		if (AuthUtil.isAdmin()) {
			allowedSites = dp.getAllDistributingSites();
		} else {
			Set<Site> userSites = getSites(Resource.ORDER, new Operation[]{Operation.CREATE, Operation.UPDATE});
			allowedSites = new HashSet<Site>(CollectionUtils.intersection(userSites, dp.getAllDistributingSites()));
		}
		
		return Utility.<Set<Long>>collect(allowedSites, "id", true);
	}

	public void ensureCreateDistributionOrderRights(DistributionOrder order) {
		ensureDistributionOrderObjectRights(order, Operation.CREATE);
		ensureDistributionOrderEximRights(order);
	}

	public void ensureReadDistributionOrderRights(DistributionOrder order) {
		ensureDistributionOrderObjectRights(order, Operation.READ);
		ensureDistributionOrderEximRights(order);
	}

	public void ensureUpdateDistributionOrderRights(DistributionOrder order) {
		ensureDistributionOrderObjectRights(order, Operation.UPDATE);
		ensureDistributionOrderEximRights(order);
	}

	public void ensureDeleteDistributionOrderRights(DistributionOrder order) {
		ensureDistributionOrderObjectRights(order, Operation.DELETE);
		ensureDistributionOrderEximRights(order);
	}

	private void ensureDistributionOrderEximRights(DistributionOrder order) {
		if (isImportOp() || isExportOp()) {
			ensureDistributionOrderObjectRights(order, Operation.EXIM);
		}
	}
	
	private void ensureDistributionOrderObjectRights(DistributionOrder order, Operation operation) {
		if (AuthUtil.isAdmin()) {
			return;
		}
		
		Set<Site> allowedSites = getSites(Resource.ORDER, operation);
		Set<Site> distributingSites = order.getDistributionProtocol().getAllDistributingSites();
		if (CollectionUtils.intersection(allowedSites, distributingSites).isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private Set<Pair<Long, Long>> getDistributionOrderSiteCps(String[] ops) {
		Long userId = AuthUtil.getCurrentUser().getId();
		String resource = Resource.ORDER.getName();

		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		Set<Pair<Long, Long>> siteCpPairs = new HashSet<Pair<Long, Long>>();
		for (SubjectAccess access : accessList) {
			Set<Site> sites = null;
			if (access.getSite() != null) {
				sites = access.getSite().getInstitute().getSites();
			} else {
				sites = getUserInstituteSites(userId);
			}

			for (Site site : sites) {
				siteCpPairs.add(Pair.make(site.getId(), (Long) null));
			}
		}

		return siteCpPairs;
	}

	public Set<Site> getRoleAssignedSites() {
		User user = AuthUtil.getCurrentUser();
		Subject subject = daoFactory.getSubjectDao().getById(user.getId());

		Set<Site> results = new HashSet<Site>();
		boolean allSites = false;
		for (SubjectRole role : subject.getRoles()) {
			if (role.getSite() == null) {
				allSites = true;
				break;
			}

			results.add(role.getSite());
		}

		if (allSites) {
			results.clear();
			results.addAll(getUserInstituteSites(user.getId()));
		}

		return results;
	}
	
	public Set<Site> getSites(Resource resource, Operation op) {
		return getSites(resource, new Operation[]{op});
	}

	public Set<Site> getSites(Resource resource, Operation[] operations) {
		return getSites(resource.getName(), operations);
	}

	public Set<Site> getSites(String resource, Operation[] operations) {
		User user = AuthUtil.getCurrentUser();

		//
		// Below conditional is an optimisation in that it is assumed an insitute admin will have
		// permissions to perform "operations" on "resource" on all sites...
		//
		if (AuthUtil.isInstituteAdmin()) {
			return getUserInstituteSites(user.getId());
		}

		String[] ops = new String[operations.length];
		for (int i = 0; i < operations.length; i++ ) {
			ops[i] = operations[i].getName();
		}

		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(user.getId(), resource, ops);
		Set<Site> results = new HashSet<Site>();
		boolean allSites = false;
		for (SubjectAccess access : accessList) {
			if (access.getSite() == null) {
				allSites = true;
				break;
			}

			results.add(access.getSite());
		}

		if (allSites) {
			results.clear();
			results.addAll(getUserInstituteSites(user.getId()));
		}

		return results;
	}

	public Set<Long> getEligibleCpIds(String resource, String op, List<String> siteNames) {
		return getEligibleCpIds(resource, new String[] {op}, siteNames);
	}

	public Set<Long> getEligibleCpIds(String resource, String[] ops, List<String> siteNames) {
		if (AuthUtil.isAdmin()) {
			return null;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		List<SubjectAccess> accessList = null;
		if (CollectionUtils.isEmpty(siteNames)) {
			accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		} else {
			accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops, siteNames.toArray(new String[0]));
		}

		Set<Long> cpIds = new HashSet<Long>();
		Set<Long> cpOfSites = new HashSet<Long>();
		for (SubjectAccess access : accessList) {
			if (access.getSite() != null && access.getCollectionProtocol() != null) {
				cpIds.add(access.getCollectionProtocol().getId());
			} else if (access.getSite() != null) {
				cpOfSites.add(access.getSite().getId());
			} else if (access.getCollectionProtocol() != null) {
				cpIds.add(access.getCollectionProtocol().getId());
			} else  {
				Collection<Site> sites = getUserInstituteSites(userId);
				for (Site site : sites) {
					if (CollectionUtils.isEmpty(siteNames) || siteNames.contains(site.getName())) {
						cpOfSites.add(site.getId());
					}
				}
			}
		}

		if (!cpOfSites.isEmpty()) {
			cpIds.addAll(daoFactory.getCollectionProtocolDao().getCpIdsBySiteIds(cpOfSites));
		}

		return cpIds;
	}

	private Set<Site> getUserInstituteSites(Long userId) {
		return getUserInstitute(userId).getSites();
	}

	private Institute getUserInstitute(Long userId) {
		User user = bsDaoFactory.getUserDao().getById(userId);
		return user.getInstitute();
	}

	private boolean canUserPerformOp(Resource resource, Operation[] ops) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		return canUserPerformOp(AuthUtil.getCurrentUser().getId(), resource, ops);
	}

	private boolean canUserPerformOp(Long userId, Resource resource, Operation[] operations) {
		List<String> ops = new ArrayList<>();
		for (Operation operation : operations) {
			ops.add(operation.getName());
		}

		return daoFactory.getSubjectDao().canUserPerformOps(
				userId,
				resource.getName(),
				ops.toArray(new String[0]));
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Surgical pathology report access control helper methods                 //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureCreateOrUpdateSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.UPDATE);
	}

	public void ensureDeleteSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.DELETE);
	}

	public void ensureReadSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.READ);
	}

	public void ensureLockSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.LOCK);
	}

	public void ensureUnlockSprRights(Visit visit) {
		ensureSprObjectRights(visit, Operation.UNLOCK);
	}

	private void ensureSprObjectRights(Visit visit, Operation op) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		if (op == Operation.LOCK || op == Operation.UNLOCK) {
			ensureVisitObjectRights(visit, Operation.UPDATE, false);
		} else {
			ensureVisitObjectRights(visit, op, false);
		}

		CollectionProtocolRegistration cpr = visit.getRegistration();
		String[] ops = {op.getName()};
		ensureVisitAndSpecimenObjectRights(cpr, Resource.SURGICAL_PATHOLOGY_REPORT, ops);
		ensureSprEximRights(visit);
	}

	private void ensureVisitAndSpecimenObjectRights(CollectionProtocolRegistration cpr, Resource resource, String[] ops) {
		Long userId = AuthUtil.getCurrentUser().getId();
		Long cpId = cpr.getCollectionProtocol().getId();
		List<SubjectAccess> accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource.getName(), ops);

		Set<Site> cpSites = cpr.getCollectionProtocol().getRepositories();
		if (!isAccessAllowedOnAnySite(accessList, cpSites, userId)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (!isAccessRestrictedBasedOnMrn()) {
			return;
		}

		Set<Site> mrnSites = cpr.getParticipant().getMrnSites();
		if (mrnSites.isEmpty()) {
			return;
		}

		if (!isAccessAllowedOnAnySite(accessList, mrnSites, userId)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureSprEximRights(Visit visit) {
		if (isImportOp() || isExportOp()) {
			String[] ops = {Operation.EXIM.getName()};
			ensureVisitAndSpecimenObjectRights(visit.getRegistration(), Resource.SURGICAL_PATHOLOGY_REPORT, ops);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	//                                                                                  //
	//          Scheduled Job object access control helper methods                      //
	//                                                                                  //
	//////////////////////////////////////////////////////////////////////////////////////
	public void ensureReadScheduledJobRights() {
		Operation[] ops = {Operation.READ};
		ensureScheduledJobRights(ops);
	}

	public void ensureRunJobRights() {
		Operation[] ops = {Operation.READ};
		ensureScheduledJobRights(ops);
	}

	public void ensureCreateScheduledJobRights() {
		Operation[] ops = {Operation.CREATE};
		ensureScheduledJobRights(ops);
	}

	public void ensureUpdateScheduledJobRights() {
		Operation[] ops = {Operation.UPDATE};
		ensureScheduledJobRights(ops);
	}

	public void ensureDeleteScheduledJobRights() {
		Operation[] ops = {Operation.DELETE};
		ensureScheduledJobRights(ops);
	}

	public void ensureScheduledJobRights(Operation[] ops) {
		if (!canUserPerformOp(Resource.SCHEDULED_JOB, ops)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}
	
	public Boolean isAccessRestrictedBasedOnMrn() {
		return ConfigUtil.getInstance().getBoolSetting(
				ConfigParams.MODULE,
				ConfigParams.MRN_RESTRICTION_ENABLED, 
				false);
	}
	
	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	//        Shipping and Tracking access control helper methods        //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////

	public Set<Long> getReadAccessShipmentSiteIds() {
		Set<Site> sites = getReadAccessShipmentSites();
		if (sites == null) {
			return null;
		}

		return Utility.collect(sites, "id", true);
	}

	public Set<Site> getReadAccessShipmentSites() {
		if (AuthUtil.isAdmin()) {
			return null;
		}
		
		return getSites(Resource.SHIPPING_N_TRACKING, Operation.READ);
	}

	public boolean canCreateUpdateShipment() {
		return canUserPerformOp(Resource.SHIPPING_N_TRACKING, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}
	
	public Set<Long> getCreateUpdateAccessShipmentSiteIds() {
		Set<Site> sites = getCreateUpdateAccessShipmentSites();
		if (sites == null) {
			return null;
		}

		return Utility.collect(sites, "id", true);
	}

	public Set<Site> getCreateUpdateAccessShipmentSites() {
		if (AuthUtil.isAdmin()) {
			return null;
		}
		
		return getSites(Resource.SHIPPING_N_TRACKING, new Operation[] {Operation.CREATE, Operation.UPDATE});
	}
	
	public void ensureReadShipmentRights(Shipment shipment) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<Site> allowedSites = getReadAccessShipmentSites();
		boolean sendSiteAccess = allowedSites.contains(shipment.getSendingSite());
		boolean recvSiteAccess = !shipment.isPending() && allowedSites.contains(shipment.getReceivingSite());
		if (!sendSiteAccess && !recvSiteAccess) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		ensureShipmentEximRights();
	}
	
	public void ensureCreateShipmentRights() {
		if (AuthUtil.isAdmin()) {
			return;
		}
		
		if (CollectionUtils.isEmpty(getSites(Resource.SHIPPING_N_TRACKING, Operation.CREATE))) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		ensureShipmentEximRights();
	}

	public void ensureUpdateShipmentRights(Shipment shipment) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		boolean allowed = false;
		Set<Site> allowedSites = getSites(Resource.SHIPPING_N_TRACKING, Operation.UPDATE);
		if (!shipment.isReceived() && allowedSites.contains(shipment.getSendingSite())) {
			allowed = true; // sender can update
		}
		
		if (shipment.isReceived() && allowedSites.contains(shipment.getReceivingSite())) {
			allowed = true; // receiver can update
		}

		if (!allowed) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		ensureShipmentEximRights();
	}

	private void ensureShipmentEximRights() {
		if ((isImportOp() || isExportOp()) && CollectionUtils.isEmpty(getSites(Resource.SHIPPING_N_TRACKING, Operation.EXIM))) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	//	Custom form access control helper methods                        //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////
	public void ensureFormUpdateRights() {
		User user = AuthUtil.getCurrentUser();
		if (!user.isAdmin() && !user.canManageForms()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	//	EXIM access control helper methods                        //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////
	public boolean hasCprEximRights(Long cpId) {
		boolean allowed = hasEximRights(cpId, Resource.PARTICIPANT.getName());
		if (!allowed) {
			allowed = hasEximRights(cpId, Resource.PARTICIPANT_DEID.getName());
		}

		return allowed;
	}

	public boolean hasVisitSpecimenEximRights(Long cpId) {
		return hasEximRights(cpId, Resource.VISIT_N_SPECIMEN.getName());
	}

	public boolean hasStorageContainerEximRights() {
		return hasEximRights(null, Resource.STORAGE_CONTAINER.getName());
	}

	public boolean hasUserEximRights() {
		return hasEximRights(null, Resource.USER.getName());
	}

	public boolean hasEximRights(Long cpId, String resource) {
		if (AuthUtil.isAdmin()) {
			return true;
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		String[] ops = {Operation.EXIM.getName()};

		List<SubjectAccess> acl;
		if (cpId != null && cpId != -1L) {
			acl = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
		} else {
			acl = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		}

		return CollectionUtils.isNotEmpty(acl);
	}

	///////////////////////////////////////////////////////////////////////
	//                                                                   //
	// Utility methods                                                   //
	//                                                                   //
	///////////////////////////////////////////////////////////////////////
	private Set<Pair<Long, Long>> getSiteCps(String resource, Long cpId, String[] ops) {
		Long userId = AuthUtil.getCurrentUser().getId();

		List<SubjectAccess> accessList = null;
		if (cpId != null) {
			accessList = daoFactory.getSubjectDao().getAccessList(userId, cpId, resource, ops);
		} else {
			accessList = daoFactory.getSubjectDao().getAccessList(userId, resource, ops);
		}

		Set<Pair<Long, Long>> siteCpPairs = new HashSet<>();
		for (SubjectAccess access : accessList) {
			Set<Site> sites = null;
			if (access.getSite() != null) {
				sites = Collections.singleton(access.getSite());
			} else {
				sites = getUserInstituteSites(userId);
			}

			CollectionProtocol cp = access.getCollectionProtocol();
			for (Site site : sites) {
				siteCpPairs.add(Pair.make(site.getId(), (cp != null) ? cp.getId() : null));
			}
		}

		return siteCpPairs;
	}

	private List<Pair<Long, Long>> deDupSiteCpPairs(Collection<Pair<Long, Long>> siteCpPairs) {
		Set<Long> sitesOfAllCps = new HashSet<>();
		List<Pair<Long, Long>> result = new ArrayList<>();
		for (Pair<Long, Long> siteCp : siteCpPairs) {
			if (siteCp.second() == null) {
				sitesOfAllCps.add(siteCp.first());
				result.add(siteCp);
			}
		}

		for (Pair<Long, Long> siteCp : siteCpPairs) {
			if (sitesOfAllCps.contains(siteCp.first())) {
				continue;
			}

			result.add(siteCp);
		}

		return result;
	}

	private boolean isAccessAllowedOnAnySite(List<SubjectAccess> accessList, Set<Site> sites, Long userId) {
		boolean allowed = false;
		for (SubjectAccess access : accessList) {
			Site accessSite = access.getSite();
			if (accessSite != null && sites.contains(accessSite)) { // Specific site
				allowed = true;
			} else if (accessSite == null) { // All user institute sites
				Set<Site> institueSites = getUserInstituteSites(userId);
				if (CollectionUtils.containsAny(institueSites, sites)) {
					allowed = true;
				}
			}

			if (allowed) {
				break;
			}
		}

		return allowed;
	}

	private boolean isAccessAllowedOnSite(Site accessSite, Site site, Long userId) {
		return (accessSite != null && accessSite.equals(site)) ||
				(accessSite == null && getUserInstituteSites(userId).contains(site));
	}

	private boolean isImportOp() {
		return ImporterContextHolder.getInstance().isImportOp();
	}

	private boolean isExportOp() {
		return ExporterContextHolder.getInstance().isExportOp();
	}
}
