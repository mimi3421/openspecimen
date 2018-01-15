
package com.krishagni.catissueplus.core.biospecimen.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenReturnEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.services.impl.FormUtil;

@Configurable
@Audited
public class Specimen extends BaseExtensionEntity {
	public static final String NEW = "New";
	
	public static final String ALIQUOT = "Aliquot";
	
	public static final String DERIVED = "Derived";
	
	public static final String COLLECTED = "Collected";
	
	public static final String MISSED_COLLECTION = "Missed Collection";
	
	public static final String PENDING = "Pending";
	
	public static final String ACCEPTABLE = "Acceptable";
	
	public static final String NOT_SPECIFIED = "Not Specified";

	public static final String EXTN = "SpecimenExtension";
	
	private static final String ENTITY_NAME = "specimen";

	private String tissueSite;

	private String tissueSide;

	private String pathologicalStatus;

	private String lineage;

	private BigDecimal initialQuantity;

	private String specimenClass;

	private String specimenType;

	private BigDecimal concentration;

	private String label;

	private String activityStatus;

	private String barcode;

	private String comment;

	private Date createdOn;

	private BigDecimal availableQuantity;

	private String collectionStatus;
	
	private Set<String> biohazards = new HashSet<>();

	private Integer freezeThawCycles;

	private CollectionProtocol collectionProtocol;

	private Visit visit;

	private SpecimenRequirement specimenRequirement;

	private StorageContainerPosition position;

	private Specimen parentSpecimen;

	private Set<Specimen> childCollection = new HashSet<>();

	private Specimen pooledSpecimen;

	private Set<Specimen> specimensPool = new HashSet<>();

	private Set<ExternalIdentifier> externalIdentifierCollection = new HashSet<>();

	//
	// records aliquot or derivative events that have occurred on this specimen
	//
	private Set<SpecimenChildrenEvent> childrenEvents = new LinkedHashSet<>();

	//
	// records the event through which this specimen got created
	//
	private SpecimenChildrenEvent parentEvent;

	//
	// collectionEvent and receivedEvent are valid only for primary specimens
	//
	private SpecimenCollectionEvent collectionEvent;
	
	private SpecimenReceivedEvent receivedEvent;

	//
	// Available for all specimens in hierarchy based on values set for primary specimens
	//
	private SpecimenCollectionReceiveDetail collRecvDetails;
	
	private List<SpecimenTransferEvent> transferEvents;
	
	private Set<SpecimenListItem> specimenListItems =  new HashSet<>();
	
	private boolean concentrationInit = false;

	@Autowired
	@Qualifier("specimenLabelGenerator")
	private LabelGenerator labelGenerator;
	
	private transient boolean forceDelete;
	
	private transient boolean printLabel;

	private transient boolean freezeThawIncremented;

	private transient String transferComments;

	private transient boolean autoCollectParents;

	//
	// Records the derivatives or aliquots created from this specimen in current action/transaction
	//
	private transient SpecimenChildrenEvent derivativeEvent;

	private transient SpecimenChildrenEvent aliquotEvent;

	public static String getEntityName() {
		return ENTITY_NAME;
	}
	
	public String getTissueSite() {
		return tissueSite;
	}

	public void setTissueSite(String tissueSite) {
		if (StringUtils.isNotBlank(this.tissueSite) && !this.tissueSite.equals(tissueSite)) {
			getChildCollection().stream()
				.filter(child -> child.isAliquot() || this.tissueSite.equals(child.getTissueSite()))
				.forEach(child -> child.setTissueSite(tissueSite));
			
			getSpecimensPool().forEach(poolSpmn -> poolSpmn.setTissueSite(tissueSite));
		}
		
		this.tissueSite = tissueSite;
	}

	public String getTissueSide() {
		return tissueSide;
	}

	public void setTissueSide(String tissueSide) {
		if (StringUtils.isNotBlank(this.tissueSide) && !this.tissueSide.equals(tissueSide)) {
			getChildCollection().stream()
				.filter(child -> child.isAliquot() || this.tissueSide.equals(child.getTissueSide()))
				.forEach(child -> child.setTissueSide(tissueSide));
			
			getSpecimensPool().forEach(poolSpmn -> poolSpmn.setTissueSide(tissueSide));
		}
		
		this.tissueSide = tissueSide;
	}

	public String getPathologicalStatus() {
		return pathologicalStatus;
	}

	public void setPathologicalStatus(String pathologicalStatus) {
		if (StringUtils.isNotBlank(this.pathologicalStatus) && !this.pathologicalStatus.equals(pathologicalStatus)) {
			for (Specimen child : getChildCollection()) {
				if (child.isAliquot()) {
					child.setPathologicalStatus(pathologicalStatus);
				}
			}
			
			for (Specimen poolSpecimen : getSpecimensPool()) {
				poolSpecimen.setPathologicalStatus(pathologicalStatus);
			}
		}
				
		this.pathologicalStatus = pathologicalStatus;
	}

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		this.lineage = lineage;
	}

	public BigDecimal getInitialQuantity() {
		return initialQuantity;
	}

	public void setInitialQuantity(BigDecimal initialQuantity) {
		this.initialQuantity = initialQuantity;
	}

	public String getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(String specimenClass) {
		if (StringUtils.isNotBlank(this.specimenClass) && !this.specimenClass.equals(specimenClass)) {
			for (Specimen child : getChildCollection()) {
				if (child.isAliquot()) {
					child.setSpecimenClass(specimenClass);
				}				
			}
			
			for (Specimen poolSpecimen : getSpecimensPool()) {
				poolSpecimen.setSpecimenClass(specimenClass);
			}
		}
		
		this.specimenClass = specimenClass;
	}

	public String getSpecimenType() {
		return specimenType;
	}

	public void setSpecimenType(String specimenType) {
		if (StringUtils.isNotBlank(this.specimenType) && !this.specimenType.equals(specimenType)) {
			for (Specimen child : getChildCollection()) {
				if (child.isAliquot()) {
					child.setSpecimenType(specimenType);
				}				
			}
			
			for (Specimen poolSpecimen : getSpecimensPool()) {
				poolSpecimen.setSpecimenType(specimenType);
			}
		}
				
		this.specimenType = specimenType;
	}

	public BigDecimal getConcentration() {
		return concentration;
	}

	public void setConcentration(BigDecimal concentration) {
		if (concentrationInit) {
			if (this.concentration == concentration) {
				return;
			}

			if (this.concentration == null || !this.concentration.equals(concentration)) {
				for (Specimen child : getChildCollection()) {
					if (ObjectUtils.equals(this.concentration, child.getConcentration()) && child.isAliquot()) {
						child.setConcentration(concentration);
					}
				}
				
				for (Specimen poolSpecimen : getSpecimensPool()) {
					poolSpecimen.setConcentration(concentration);
				}
			}
		}
		
		this.concentration = concentration;
		this.concentrationInit = true;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		this.activityStatus = activityStatus;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Date getCreatedOn() {
		return  Utility.chopSeconds(createdOn);
	}

	public void setCreatedOn(Date createdOn) {
		// For all specimens, the created on seconds and milliseconds should be reset to 0
		this.createdOn = Utility.chopSeconds(createdOn);
	}

	public BigDecimal getAvailableQuantity() {
		return availableQuantity;
	}

	public void setAvailableQuantity(BigDecimal availableQuantity) {
		this.availableQuantity = availableQuantity;
	}

	public String getCollectionStatus() {
		return collectionStatus;
	}

	public void setCollectionStatus(String collectionStatus) {
		this.collectionStatus = collectionStatus;
	}

	public Set<String> getBiohazards() {
		return biohazards;
	}

	public void setBiohazards(Set<String> biohazards) {
		this.biohazards = biohazards;
	}
	
	public void updateBiohazards(Set<String> biohazards) {
		getBiohazards().addAll(biohazards);
		getBiohazards().retainAll(biohazards);
		
		for (Specimen child : getChildCollection()) {
			if (child.isAliquot()) {
				child.updateBiohazards(biohazards);
			}
		}
		
		for (Specimen poolSpecimen : getSpecimensPool()) {
			poolSpecimen.updateBiohazards(biohazards);
		}
	}

	public Integer getFreezeThawCycles() {
		return freezeThawCycles;
	}

	public void setFreezeThawCycles(Integer freezeThawCycles) {
		this.freezeThawCycles = freezeThawCycles;
	}

	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(CollectionProtocol collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public Visit getVisit() {
		return visit;
	}

	public void setVisit(Visit visit) {
		this.visit = visit;
	}

	public SpecimenRequirement getSpecimenRequirement() {
		return specimenRequirement;
	}

	public void setSpecimenRequirement(SpecimenRequirement specimenRequirement) {
		this.specimenRequirement = specimenRequirement;
	}

	public StorageContainerPosition getPosition() {
		return position;
	}

	public void setPosition(StorageContainerPosition position) {
		this.position = position;
	}

	public Specimen getParentSpecimen() {
		return parentSpecimen;
	}

	public void setParentSpecimen(Specimen parentSpecimen) {
		this.parentSpecimen = parentSpecimen;
	}

	@NotAudited
	public Set<Specimen> getChildCollection() {
		return childCollection;
	}

	public void setChildCollection(Set<Specimen> childSpecimenCollection) {
		this.childCollection = childSpecimenCollection;
	}

	public Specimen getPooledSpecimen() {
		return pooledSpecimen;
	}

	public void setPooledSpecimen(Specimen pooledSpecimen) {
		this.pooledSpecimen = pooledSpecimen;
	}

	@NotAudited
	public Set<Specimen> getSpecimensPool() {
		return specimensPool;
	}

	public void setSpecimensPool(Set<Specimen> specimensPool) {
		this.specimensPool = specimensPool;
	}

	@NotAudited
	public Set<ExternalIdentifier> getExternalIdentifierCollection() {
		return externalIdentifierCollection;
	}

	public void setExternalIdentifierCollection(Set<ExternalIdentifier> externalIdentifierCollection) {
		this.externalIdentifierCollection = externalIdentifierCollection;
	}

	@NotAudited
	public Set<SpecimenChildrenEvent> getChildrenEvents() {
		return childrenEvents;
	}

	public void setChildrenEvents(Set<SpecimenChildrenEvent> childrenEvents) {
		this.childrenEvents = childrenEvents;
	}

	@NotAudited
	public SpecimenChildrenEvent getParentEvent() {
		return parentEvent;
	}

	public void setParentEvent(SpecimenChildrenEvent parentEvent) {
		this.parentEvent = parentEvent;
	}

	@NotAudited
	public SpecimenCollectionEvent getCollectionEvent() {
		if (isAliquot() || isDerivative()) {
			return null;
		}
				
		if (this.collectionEvent == null) {
			this.collectionEvent = SpecimenCollectionEvent.getFor(this); 
		}
		
		if (this.collectionEvent == null) {
			this.collectionEvent = SpecimenCollectionEvent.createFromSr(this);
		}
		
		return this.collectionEvent;
	}

	public void setCollectionEvent(SpecimenCollectionEvent collectionEvent) {
		this.collectionEvent = collectionEvent;
	}

	@NotAudited
	public SpecimenReceivedEvent getReceivedEvent() {
		if (isAliquot() || isDerivative()) {
			return null;
		}
		
		if (this.receivedEvent == null) {
			this.receivedEvent = SpecimenReceivedEvent.getFor(this); 			 
		}
		
		if (this.receivedEvent == null) {
			this.receivedEvent = SpecimenReceivedEvent.createFromSr(this);
		}
		
		return this.receivedEvent; 
	}

	public void setReceivedEvent(SpecimenReceivedEvent receivedEvent) {
		this.receivedEvent = receivedEvent;
	}

	@NotAudited
	public SpecimenCollectionReceiveDetail getCollRecvDetails() {
		return collRecvDetails;
	}

	public void setCollRecvDetails(SpecimenCollectionReceiveDetail collRecvDetails) {
		this.collRecvDetails = collRecvDetails;
	}

	@NotAudited
	public List<SpecimenTransferEvent> getTransferEvents() {
		if (this.transferEvents == null) {
			this.transferEvents = SpecimenTransferEvent.getFor(this);
		}		
		return this.transferEvents;
	}
	
	@NotAudited
	public Set<SpecimenListItem> getSpecimenListItems() {
		return specimenListItems;
	}

	public void setSpecimenListItems(Set<SpecimenListItem> specimenListItems) {
		this.specimenListItems = specimenListItems;
	}

	public Set<DistributionProtocol> getDistributionProtocols() {
		return getCollectionProtocol().getDistributionProtocols();
	}

	public LabelGenerator getLabelGenerator() {
		return labelGenerator;
	}

	public void setLabelGenerator(LabelGenerator labelGenerator) {
		this.labelGenerator = labelGenerator;
	}

	@Override
	public String getEntityType() {
		return EXTN;
	}

	@Override
	public Long getCpId() {
		return getCollectionProtocol().getId();
	}

	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	public String getTransferComments() {
		return transferComments;
	}

	public void setTransferComments(String transferComments) {
		this.transferComments = transferComments;
	}

	public boolean isAutoCollectParents() {
		return autoCollectParents;
	}

	public void setAutoCollectParents(boolean autoCollectParents) {
		this.autoCollectParents = autoCollectParents;
	}

	public boolean isPrintLabel() {
		return printLabel;
	}

	public void setPrintLabel(boolean printLabel) {
		this.printLabel = printLabel;
	}

	public boolean isActive() {
		return Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(getActivityStatus());
	}
	
	public boolean isClosed() {
		return Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(getActivityStatus());
	}
	
	public boolean isActiveOrClosed() {
		return isActive() || isClosed();
	}
	
	public boolean isAliquot() {
		return ALIQUOT.equals(lineage);
	}
	
	public boolean isDerivative() {
		return DERIVED.equals(lineage);
	}
	
	public boolean isPrimary() {
		return NEW.equals(lineage);
	}
	
	public boolean isPoolSpecimen() {
		return getPooledSpecimen() != null;
	}
	
	public boolean isPooled() {
		return getSpecimenRequirement() != null && getSpecimenRequirement().isPooledSpecimenReq();
	}

	public boolean isCollected() {
		return isCollected(getCollectionStatus());
	}
	
	public boolean isPending() {
		return isPending(getCollectionStatus());
	}

	public boolean isMissed() {
		return isMissed(getCollectionStatus());
	}

	public Boolean isAvailable() {
		return getAvailableQuantity() == null || NumUtil.greaterThanZero(getAvailableQuantity());
	}

	public void disable() {
		disable(!isForceDelete());
	}

	public void disable(boolean checkChildSpecimens) {
		if (getActivityStatus().equals(Status.ACTIVITY_STATUS_DISABLED.getStatus())) {
			return;
		}

		if (checkChildSpecimens) {
			ensureNoActiveChildSpecimens();
		}
		
		for (Specimen child : getChildCollection()) {
			child.disable(checkChildSpecimens);
		}
		
		for (Specimen specimen : getSpecimensPool()) {
			specimen.disable(checkChildSpecimens);
		}

		virtualize(null, "Specimen deleted");
		setLabel(Utility.getDisabledValue(getLabel(), 255));
		setBarcode(Utility.getDisabledValue(getBarcode(), 255));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		FormUtil.getInstance().deleteRecords(getCpId(), Arrays.asList("Specimen", "SpecimenEvent", "SpecimenExtension"), getId());
	}
	
	public static boolean isCollected(String status) {
		return COLLECTED.equals(status);
	}
	
	public static boolean isPending(String status) {
		return PENDING.equals(status);
	}

	public static boolean isMissed(String status) {
		return MISSED_COLLECTION.equals(status);
	}

	public boolean isPrePrintEnabled() {
		return getSpecimenRequirement() != null &&
			getSpecimenRequirement().getLabelAutoPrintModeToUse() == CollectionProtocol.SpecimenLabelAutoPrintMode.PRE_PRINT;
	}

	public void close(User user, Date time, String reason) {
		if (!getActivityStatus().equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus())) {
			return;
		}
		
		virtualize(time, reason);
		addDisposalEvent(user, time, reason);		
		setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
	}
	
	public List<DependentEntityDetail> getDependentEntities() {
		return DependentEntityDetail.singletonList(Specimen.getEntityName(), getActiveChildSpecimens()); 
	}
		
	public void activate() {
		if (getActivityStatus().equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus())) {
			return;
		}
		
		setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());

		//
		// TODO: we need to add a reopen event here
		//
	}
		
	public CollectionProtocolRegistration getRegistration() {
		return getVisit().getRegistration();
	}

	public void update(Specimen specimen) {
		boolean wasCollected = isCollected();

		setForceDelete(specimen.isForceDelete());
		setAutoCollectParents(specimen.isAutoCollectParents());

		String reason = null;
		if (!StringUtils.equals(getComment(), specimen.getComment())) {
			reason = specimen.getComment();
		}

		updateStatus(specimen.getActivityStatus(), reason);
		if (!isActive()) {
			return;
		}
		
		setLabel(specimen.getLabel());
		setBarcode(specimen.getBarcode());
		setInitialQuantity(specimen.getInitialQuantity());
		setAvailableQuantity(specimen.getAvailableQuantity());
		setConcentration((isPoolSpecimen() ? getPooledSpecimen() : specimen).getConcentration());

		updateEvent(getCollectionEvent(), specimen.getCollectionEvent());
		updateEvent(getReceivedEvent(), specimen.getReceivedEvent());
		updateCollectionStatus(specimen.getCollectionStatus());
		updatePosition(specimen.getPosition(), null, specimen.getTransferComments());

		if (isCollected()) {
			if (isPrimary()) {
				updateCreatedOn(Utility.chopSeconds(getReceivedEvent().getTime()));
			} else {
				updateCreatedOn(specimen.getCreatedOn() != null ? specimen.getCreatedOn() : Calendar.getInstance().getTime());

				if (!wasCollected) {
					getParentSpecimen().addToChildrenEvent(this);
				}
			}
		} else {
			updateCreatedOn(null);
		}

		// TODO: Specimen class/type should not be allowed to change
		Specimen spmnToUpdateFrom = null;
		if (isAliquot()) {
			spmnToUpdateFrom = getParentSpecimen();
		} else if (isPoolSpecimen()) {
			spmnToUpdateFrom = getPooledSpecimen();
		} else {
			spmnToUpdateFrom = specimen;
		}

		setTissueSite(spmnToUpdateFrom.getTissueSite());
		setTissueSide(spmnToUpdateFrom.getTissueSide());
		setSpecimenClass(spmnToUpdateFrom.getSpecimenClass());
		setSpecimenType(spmnToUpdateFrom.getSpecimenType());
		updateBiohazards(spmnToUpdateFrom.getBiohazards());
		setPathologicalStatus(spmnToUpdateFrom.getPathologicalStatus());

		setComment(specimen.getComment());
		setExtension(specimen.getExtension());
		setPrintLabel(specimen.isPrintLabel());
		setFreezeThawCycles(specimen.getFreezeThawCycles());
	}
	
	public void updateStatus(String activityStatus, String reason){
		updateStatus(activityStatus, AuthUtil.getCurrentUser(), Calendar.getInstance().getTime(), reason, isForceDelete());
	}
	
	//
	// TODO: Modify to accommodate pooled specimens
	//	
	public void updateStatus(String activityStatus, User user, Date date, String reason, boolean isForceDelete) {
		if (this.activityStatus != null && this.activityStatus.equals(activityStatus)) {
			return;
		}
		
		if (Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(activityStatus)) {
			disable(!isForceDelete);
		} else if (Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(activityStatus)) {
			close(user, date, reason);
		} else if (Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(activityStatus)) {
			activate();
		}
	}
	
	public void updateCollectionStatus(String collectionStatus) {
		if (collectionStatus.equals(getCollectionStatus())) {
			//
			// no change in collection status; therefore nothing needs to be done
			//
			return;
		}
		
		if (isMissed(collectionStatus)) {
			if (!getVisit().isCompleted() && !getVisit().isMissed()) {
				throw OpenSpecimenException.userError(VisitErrorCode.COMPL_OR_MISSED_VISIT_REQ);
			} else if (getParentSpecimen() != null && !getParentSpecimen().isCollected() && !getParentSpecimen().isMissed()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.COLL_OR_MISSED_PARENT_REQ);
			} else {
				updateHierarchyStatus(collectionStatus);
				createMissedChildSpecimens();
			}
		} else if (isPending(collectionStatus)) {
			if (!getVisit().isCompleted() && !getVisit().isPending()) {
				throw OpenSpecimenException.userError(VisitErrorCode.COMPL_OR_PENDING_VISIT_REQ);
			} else if (getParentSpecimen() != null && !getParentSpecimen().isCollected() && !getParentSpecimen().isPending()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.COLL_OR_PENDING_PARENT_REQ);
			} else {
				updateHierarchyStatus(collectionStatus);
			}
		} else if (isCollected(collectionStatus)) {
			if (!getVisit().isCompleted()) {
				throw OpenSpecimenException.userError(VisitErrorCode.COMPL_VISIT_REQ);
			} else {
				if (getParentSpecimen() != null && !getParentSpecimen().isCollected()) {
					if (!autoCollectParents) {
						throw OpenSpecimenException.userError(SpecimenErrorCode.COLL_PARENT_REQ);
					}

					autoCollectParentSpecimens(this);
				}

				setCollectionStatus(collectionStatus);
				decAliquotedQtyFromParent();
				addOrUpdateCollRecvEvents();
			}
		}
		
		checkPoolStatusConstraints();
	}
		
	public void distribute(DistributionOrderItem item) {
		if (!isCollected() || isClosed()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_AVAILABLE_FOR_DIST, getLabel());
		}
		
		//
		// Deduct distributed quantity from available quantity
		//
		if (getAvailableQuantity() != null) {
			if (NumUtil.greaterThanEquals(getAvailableQuantity(), item.getQuantity())) {
				setAvailableQuantity(getAvailableQuantity().subtract(item.getQuantity()));
			} else {
				setAvailableQuantity(BigDecimal.ZERO);
			}
		}

		//
		// add distributed event
		//
		SpecimenDistributionEvent.createForDistributionOrderItem(item).saveRecordEntry();

		//
		// close specimen if explicitly closed or no quantity available
		//
		if (NumUtil.isZero(getAvailableQuantity()) || item.isDistributedAndClosed()) {
			close(item.getOrder().getDistributor(), item.getOrder().getExecutionDate(), "Distributed");
		}
	}

	public void updateCreatedOn(Date createdOn) {
		this.createdOn = createdOn;

		if (createdOn == null) {
			for (Specimen childSpecimen : getChildCollection()) {
				childSpecimen.updateCreatedOn(createdOn);
			}

			return;
		}

		if (createdOn.after(Calendar.getInstance().getTime())) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.CREATED_ON_GT_CURRENT);
		}

		// The below code is commented for now, so that there will not be any issue for the legacy data.
		// In legacy data created on was simple date field, but its been changed to timestamp in v20.
		// While migrating time part of the date is set as 00:00:00,
		// but the created on of primary specimen(fetched from received event time stamp) will have time part within.
		// So there is large possibility of below 2 exceptions.
		/*if (!isPrimary() && createdOn.before(getParentSpecimen().getCreatedOn())) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.CHILD_CREATED_ON_LT_PARENT);
		}

		for (Specimen childSpecimen : getChildCollection()) {
			if (childSpecimen.getCreatedOn() != null && createdOn.after(childSpecimen.getCreatedOn())) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.PARENT_CREATED_ON_GT_CHILDREN);
			}
		}*/
	}

	public void returnSpecimen(DistributionOrderItem item) {
		if (isClosed()) {
			setAvailableQuantity(item.getReturnedQuantity());
			activate();
		} else {
			if (getAvailableQuantity() == null) {
				setAvailableQuantity(item.getReturnedQuantity());
			} else {
				setAvailableQuantity(getAvailableQuantity().add(item.getReturnedQuantity()));
			}
		}

		StorageContainer container = item.getReturningContainer();
		if (container != null) {
			StorageContainerPosition position = container.createPosition(item.getReturningColumn(), item.getReturningRow());
			transferTo(position, item.getReturnDate(), "Specimen returned");
		}

		SpecimenReturnEvent.createForDistributionOrderItem(item).saveRecordEntry();
	}
	
	private void addDisposalEvent(User user, Date time, String reason) {
		SpecimenDisposalEvent event = new SpecimenDisposalEvent(this);
		event.setReason(reason);
		event.setUser(user);
		event.setTime(time);
		event.saveOrUpdate();
	}

	private void updatePosition(StorageContainerPosition newPosition, Date time, String comments) {
		if (!isCollected()) {
			return;
		}

		if (newPosition != null) {
			StorageContainer container = newPosition.getContainer();
			if (container == null || (!container.isDimensionless() && !newPosition.isSpecified())) {
				newPosition = null;
			}
		}

		transferTo(newPosition, time, comments);
	}

	private void virtualize(Date time, String comments) {
		transferTo(null, time, comments);
	}
	
	private void transferTo(StorageContainerPosition newPosition, Date time, String comments) {
		StorageContainerPosition oldPosition = getPosition();
		if (StorageContainerPosition.areSame(oldPosition, newPosition)) {
			return;
		}

		if (oldPosition != null) {
			AccessCtrlMgr.getInstance().ensureSpecimenStoreRights(oldPosition.getContainer());
		}

		if (newPosition != null) {
			AccessCtrlMgr.getInstance().ensureSpecimenStoreRights(newPosition.getContainer());
		}

		SpecimenTransferEvent transferEvent = new SpecimenTransferEvent(this);
		transferEvent.setUser(AuthUtil.getCurrentUser());
		transferEvent.setTime(time == null ? Calendar.getInstance().getTime() : time);
		transferEvent.setComments(comments);
		
		if (oldPosition != null && newPosition != null) {
			oldPosition.getContainer().retrieveSpecimen(this);
			newPosition.getContainer().storeSpecimen(this);

			transferEvent.setFromLocation(oldPosition);
			transferEvent.setToLocation(newPosition);

			oldPosition.update(newPosition);			
		} else if (oldPosition != null) {
			oldPosition.getContainer().retrieveSpecimen(this);
			transferEvent.setFromLocation(oldPosition);

			oldPosition.vacate();
			setPosition(null);
		} else if (newPosition != null) {
			newPosition.getContainer().storeSpecimen(this);
			transferEvent.setToLocation(newPosition);
			
			newPosition.setOccupyingSpecimen(this);
			newPosition.occupy();
			setPosition(newPosition);
		}
		
		transferEvent.saveOrUpdate();		
	}

	public void addChildSpecimen(Specimen specimen) {
		specimen.setParentSpecimen(this);

		if (!isCollected() && specimen.isCollected()) {
			if (!specimen.autoCollectParents) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.COLL_PARENT_REQ);
			}

			autoCollectParentSpecimens(specimen);
		}

		if (specimen.isAliquot()) {
			specimen.decAliquotedQtyFromParent();
		}

		if (specimen.getCreatedOn() != null && specimen.getCreatedOn().before(getCreatedOn())) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.CHILD_CREATED_ON_LT_PARENT);
		}

		specimen.occupyPosition();
		getChildCollection().add(specimen);
		addToChildrenEvent(specimen);
	}
	
	public void addPoolSpecimen(Specimen specimen) {
		specimen.setPooledSpecimen(this);
		specimen.occupyPosition();
		getSpecimensPool().add(specimen);
	}
	
	public void setPending() {
		updateCollectionStatus(PENDING);
	}

	public void decAliquotedQtyFromParent() {
		if (isCollected() && isAliquot()) {
			adjustParentSpecimenQty(initialQuantity);
		}		
	}
	
	public void occupyPosition() {
		if (position == null) {
			return;
		}
		
		if (!isCollected()) { 
			// Un-collected (pending/missed collection) specimens can't occupy space
			position = null;
			return;
		}

		position.getContainer().storeSpecimen(this);
		position.occupy();
	}
	
	public void addOrUpdateCollRecvEvents() {
		if (!isCollected()) {
			return;
		}
		
		addOrUpdateCollectionEvent();
		addOrUpdateReceivedEvent();
	}
	
	public void setLabelIfEmpty() {
		if (StringUtils.isNotBlank(label) || isMissed()) {
			return;
		}
		
		String labelTmpl = getLabelTmpl();				
		String label = null;
		if (StringUtils.isNotBlank(labelTmpl)) {
			label = labelGenerator.generateLabel(labelTmpl, this);
		} else if (isAliquot() || isDerivative()) {
			Specimen parentSpecimen = getParentSpecimen();
			int count = parentSpecimen.getChildCollection().size();
			label = parentSpecimen.getLabel() + "_" + (count + 1);
		}
		
		if (StringUtils.isBlank(label)) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.LABEL_REQUIRED);
		}
		
		setLabel(label);
	}
	
	public String getLabelTmpl() {
		String labelTmpl = null;
		
		SpecimenRequirement sr = getSpecimenRequirement();
		if (sr != null) { // anticipated specimen
			labelTmpl = sr.getLabelFormat();
		}
				
		if (StringUtils.isNotBlank(labelTmpl)) {
			return labelTmpl;
		}
		
		CollectionProtocol cp = getVisit().getCollectionProtocol();
		if (isAliquot()) {
			labelTmpl = cp.getAliquotLabelFormat();
		} else if (isDerivative()) {
			labelTmpl = cp.getDerivativeLabelFormat();
		} else {
			labelTmpl = cp.getSpecimenLabelFormat();
		}			
		
		return labelTmpl;		
	}
	
	public void updatePosition(StorageContainerPosition newPosition) {
		updatePosition(newPosition, null);
	}
	
	public void updatePosition(StorageContainerPosition newPosition, Date time) {
		updatePosition(newPosition, time, null);
	}

	public String getLabelOrDesc() {
		if (StringUtils.isNotBlank(label)) {
			return label;
		}
		
		return getDesc(specimenClass, specimenType);
	}

	public void incrementFreezeThaw(Integer incrementFreezeThaw) {
		if (freezeThawIncremented) {
			return;
		}

		if (incrementFreezeThaw == null || incrementFreezeThaw <= 0) {
			return;
		}

		if (getFreezeThawCycles() == null) {
			setFreezeThawCycles(incrementFreezeThaw);
		} else {
			setFreezeThawCycles(getFreezeThawCycles() + incrementFreezeThaw);
		}

		freezeThawIncremented = true;
	}

	public static String getDesc(String specimenClass, String type) {
		StringBuilder desc = new StringBuilder();
		if (StringUtils.isNotBlank(specimenClass)) {
			desc.append(specimenClass);
		}
		
		if (StringUtils.isNotBlank(type)) {
			if (desc.length() > 0) {
				desc.append("-");
			}
			
			desc.append(type);
		}
			
		return desc.toString();		
	}
	
	//
	// Useful for sorting specimens at same level
	//
	public static List<Specimen> sort(Collection<Specimen> specimens) {
		List<Specimen> result = new ArrayList<Specimen>(specimens);
		Collections.sort(result, new Comparator<Specimen>() {
			@Override
			public int compare(Specimen s1, Specimen s2) {
				Integer s1SortOrder = sortOrder(s1);
				Integer s2SortOrder = sortOrder(s2);

				Long s1ReqId = reqId(s1);
				Long s2ReqId = reqId(s2);

				if (s1SortOrder != null && s2SortOrder != null) {
					return s1SortOrder.compareTo(s2SortOrder);
				} else if (s1SortOrder != null) {
					return -1;
				} else if (s2SortOrder != null) {
					return 1;
				} else if (s1ReqId != null && s2ReqId != null) {
					if (!s1ReqId.equals(s2ReqId)) {
						return s1ReqId.compareTo(s2ReqId);
					} else {
						return compareById(s1, s2);
					}
				} else if (s1ReqId != null) {
					return -1;
				} else if (s2ReqId != null) {
					return 1;
				} else {
					return compareById(s1, s2);
				}
			}

			private int compareById(Specimen s1, Specimen s2) {
				if (s1.getId() != null && s2.getId() != null) {
					return s1.getId().compareTo(s2.getId());
				} else if (s1.getId() != null) {
					return -1;
				} else if (s2.getId() != null) {
					return 1;
				} else {
					return 0;
				}
			}

			private Integer sortOrder(Specimen s) {
				if (s.getSpecimenRequirement() != null) {
					return s.getSpecimenRequirement().getSortOrder();
				}

				return null;
			}

			private Long reqId(Specimen s) {
				if (s.getSpecimenRequirement() != null) {
					return s.getSpecimenRequirement().getId();
				}

				return null;
			}
		});

		return result;
	}

	public static List<Specimen> sortByLabels(Collection<Specimen> specimens, final List<String> labels) {
		List<Specimen> result = new ArrayList<Specimen>(specimens);
		Collections.sort(result, new Comparator<Specimen>() {
			@Override
			public int compare(Specimen s1, Specimen s2) {
				int s1Idx = labels.indexOf(s1.getLabel());
				int s2Idx = labels.indexOf(s2.getLabel());
				return s1Idx - s2Idx;
			}
		});

		return result;
	}

	public static List<Specimen> sortByIds(Collection<Specimen> specimens, final List<Long> ids) {
		List<Specimen> result = new ArrayList<Specimen>(specimens);
		Collections.sort(result, new Comparator<Specimen>() {
			@Override
			public int compare(Specimen s1, Specimen s2) {
				int s1Idx = ids.indexOf(s1.getId());
				int s2Idx = ids.indexOf(s2.getId());
				return s1Idx - s2Idx;
			}
		});

		return result;
	}

	public static List<Specimen> sortByBarcodes(Collection<Specimen> specimens, final List<String> barcodes) {
		List<Specimen> result = new ArrayList<>(specimens);
		result.sort(Comparator.comparingInt((s) -> barcodes.indexOf(s.getBarcode())));
		return result;
	}

	public static boolean isValidLineage(String lineage) {
		if (StringUtils.isBlank(lineage)) {
			return false;
		}

		return lineage.equals(NEW) || lineage.equals(DERIVED) || lineage.equals(ALIQUOT);
	}

	private void addToChildrenEvent(Specimen childSpmn) {
		if (!childSpmn.isCollected() || childSpmn.getParentSpecimen() == null) {
			return;
		}

		SpecimenChildrenEvent currentEvent = childSpmn.isAliquot() ? aliquotEvent : derivativeEvent;
		if (currentEvent == null) {
			currentEvent = new SpecimenChildrenEvent();
			currentEvent.setSpecimen(this);
			currentEvent.setLineage(childSpmn.getLineage());
			currentEvent.setUser(AuthUtil.getCurrentUser());
			currentEvent.setTime(childSpmn.getCreatedOn());
			getChildrenEvents().add(currentEvent);
		}

		currentEvent.addChild(childSpmn);

		if (childSpmn.isAliquot()) {
			aliquotEvent = currentEvent;
		} else if (childSpmn.isDerivative()) {
			derivativeEvent = currentEvent;
		}
	}

	private void ensureNoActiveChildSpecimens() {
		for (Specimen specimen : getChildCollection()) {
			if (specimen.isActiveOrClosed() && specimen.isCollected()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.REF_ENTITY_FOUND);
			}
		}

		if (isPooled()) {
			for (Specimen specimen : getSpecimensPool()) {
				if (specimen.isActiveOrClosed() && specimen.isCollected()) {
					throw OpenSpecimenException.userError(SpecimenErrorCode.REF_ENTITY_FOUND);
				}
			}
		}
	}
	
	private int getActiveChildSpecimens() {
		int count = 0;
		for (Specimen specimen : getChildCollection()) {
			if (specimen.isActiveOrClosed() && specimen.isCollected()) {
				++count;
			}
		}

		if (isPooled()) {
			for (Specimen specimen : getSpecimensPool()) {
				if (specimen.isActiveOrClosed() && specimen.isCollected()) {
					++count;
				}
			}
		}

		return count;
	}
			
	private void addOrUpdateCollectionEvent() {
		if (isAliquot() || isDerivative()) {
			return;
		}
		
		getCollectionEvent().saveOrUpdate();		
	}
	
	private void addOrUpdateReceivedEvent() {
		if (isAliquot() || isDerivative()) {
			return;
		}
		
		getReceivedEvent().saveOrUpdate();
		setCreatedOn(getReceivedEvent().getTime());
	}
	
	private void deleteEvents() {
		if (!isAliquot() && !isDerivative()) {
			getCollectionEvent().delete();
			getReceivedEvent().delete();
		}
		
		for (SpecimenTransferEvent te : getTransferEvents()) {
			te.delete();
		}
	}

	private void adjustParentSpecimenQty(BigDecimal qty) {
		BigDecimal parentQty = parentSpecimen.getAvailableQuantity();
		if (parentQty == null || NumUtil.isZero(parentQty)) {
			return;
		}

		parentQty = parentQty.subtract(qty);
		if (NumUtil.lessThanEqualsZero(parentQty)) {
			parentQty = BigDecimal.ZERO;
		}

		parentSpecimen.setAvailableQuantity(parentQty);
	}
	
	private void updateEvent(SpecimenEvent thisEvent, SpecimenEvent otherEvent) {
		if (isAliquot() || isDerivative()) {
			return;
		}
		
		thisEvent.update(otherEvent);
	}
	
	private void updateHierarchyStatus(String status) {
		setCollectionStatus(status);

		if (getId() != null && !isCollected(status)) {
			setAvailableQuantity(BigDecimal.ZERO);

			if (getPosition() != null) {
				getPosition().vacate();
			}
			setPosition(null);

			if (getParentEvent() != null) {
				getParentEvent().removeChild(this);
			}
				
			deleteEvents();
		}

		getChildCollection().forEach(child -> child.updateHierarchyStatus(status));
	}

	public void checkPoolStatusConstraints() {
		if (!isPooled() && !isPoolSpecimen()) {
			return;
		}

		Specimen pooledSpmn = null;
		if (isPooled()) {
			if (isMissed() || isPending()) {
				return;
			}

			pooledSpmn = this;
		} else if (isPoolSpecimen()) {
			if (isCollected()) {
				return;
			}

			pooledSpmn = getPooledSpecimen();
		}

		boolean atLeastOneColl = false;
		for (Specimen poolSpmn : pooledSpmn.getSpecimensPool()) {
			if (poolSpmn.isCollected()) {
				atLeastOneColl = true;
				break;
			}
		}

		if (!atLeastOneColl && pooledSpmn.isCollected()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NO_POOL_SPMN_COLLECTED, getLabel());
		}
	}

	private void createMissedChildSpecimens() {
		if (getSpecimenRequirement() == null) {
			return;
		}

		Set<SpecimenRequirement> anticipated = new HashSet<>(getSpecimenRequirement().getChildSpecimenRequirements());
		for (Specimen childSpecimen : getChildCollection()) {
			if (childSpecimen.getSpecimenRequirement() != null) {
				anticipated.remove(childSpecimen.getSpecimenRequirement());
				childSpecimen.createMissedChildSpecimens();
			}
		}

		for (SpecimenRequirement sr : anticipated) {
			Specimen specimen = sr.getSpecimen();
			specimen.setVisit(getVisit());
			specimen.setParentSpecimen(this);
			specimen.setCollectionStatus(Specimen.MISSED_COLLECTION);
			getChildCollection().add(specimen);

			specimen.createMissedChildSpecimens();
		}
	}

	private void autoCollectParentSpecimens(Specimen childSpmn) {
		Specimen parentSpmn = childSpmn.getParentSpecimen();
		while (parentSpmn != null && parentSpmn.isPending()) {
			parentSpmn.setCollectionStatus(COLLECTED);
			if (parentSpmn.isPrimary()) {
				parentSpmn.addOrUpdateCollRecvEvents();
			} else {
				parentSpmn.setCreatedOn(childSpmn.getCreatedOn());
			}

			parentSpmn.addToChildrenEvent(childSpmn);

			childSpmn = parentSpmn;
			parentSpmn = parentSpmn.getParentSpecimen();
		}

		if (parentSpmn != null) {
			//
			// this means the parent specimen was pre-collected.
			// therefore need to add a processing event for its
			// recently collected child specimen
			//
			parentSpmn.addToChildrenEvent(childSpmn);
		}
	}
}
