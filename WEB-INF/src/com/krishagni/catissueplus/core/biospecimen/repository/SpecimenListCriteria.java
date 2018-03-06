package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class SpecimenListCriteria extends AbstractListCriteria<SpecimenListCriteria> {
	private Long cpId;

	private String cpShortTitle;

	private String[] lineages;

	private String[] collectionStatuses;

	private List<Pair<Long, Long>> siteCps;
	
	private List<String> labels;

	private List<String> barcodes;
	
	private Long specimenListId;
	
	private boolean useMrnSites;

	private String storageLocationSite;

	private String ppid;

	private String anatomicSite;

	private String type;

	private String container;

	private Long containerId;

	private Long ancestorContainerId;

	private boolean available;

	private boolean noQty;

	private Long reservedForDp;

	public SpecimenListCriteria() {
		exactMatch(true);
	}
	
	@Override
	public SpecimenListCriteria self() {		
		return this;
	} 

	public Long cpId() {
		return cpId;
	}

	public SpecimenListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public String cpShortTitle() {
		return cpShortTitle;
	}

	public SpecimenListCriteria cpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
		return self();
	}

	public String[] lineages() {
		return lineages;
	}

	public SpecimenListCriteria lineages(String[] lineages) {
		this.lineages = lineages;
		return self();
	}

	public String[] collectionStatuses() {
		return collectionStatuses;
	}

	public SpecimenListCriteria collectionStatuses(String[] collectionStatuses) {
		this.collectionStatuses = collectionStatuses;
		return self();
	}

	public List<Pair<Long, Long>> siteCps() {
		return siteCps;
	}
	
	public SpecimenListCriteria siteCps(List<Pair<Long, Long>> siteCps) {
		this.siteCps = siteCps;
		return self();
	}
	
	public List<String> labels() {
		return labels;
	}
	
	public SpecimenListCriteria labels(List<String> labels) {
		this.labels = labels;
		return self();
	}

	public List<String> barcodes() {
		return barcodes;
	}

	public SpecimenListCriteria barcodes(List<String> barcodes) {
		this.barcodes = barcodes;
		return self();
	}

	public Long specimenListId() {
		return specimenListId;
	}

	public SpecimenListCriteria specimenListId(Long specimenListId) {
		this.specimenListId = specimenListId;
		return self();
	}
	
	public boolean useMrnSites() {
		return this.useMrnSites;
	}
	
	public SpecimenListCriteria useMrnSites(boolean useMrnSites) {
		this.useMrnSites = useMrnSites;
		return self();
	}

	public String storageLocationSite() {
		return storageLocationSite;
	}

	public SpecimenListCriteria storageLocationSite(String storageLocationSite) {
		this.storageLocationSite = storageLocationSite;
		return self();
	}

	public String ppid() {
		return ppid;
	}

	public SpecimenListCriteria ppid(String ppid) {
		this.ppid = ppid;
		return self();
	}

	public String anatomicSite() {
		return anatomicSite;
	}

	public SpecimenListCriteria anatomicSite(String anatomicSite) {
		this.anatomicSite = anatomicSite;
		return self();
	}

	public String type() {
		return type;
	}

	public SpecimenListCriteria type(String type) {
		this.type = type;
		return self();
	}

	public String container() {
		return container;
	}

	public SpecimenListCriteria container(String container) {
		this.container = container;
		return self();
	}

	public Long containerId() {
		return containerId;
	}

	public SpecimenListCriteria containerId(Long containerId) {
		this.containerId = containerId;
		return self();
	}

	public Long ancestorContainerId() {
		return ancestorContainerId;
	}

	public SpecimenListCriteria ancestorContainerId(Long ancestorContainerId) {
		this.ancestorContainerId = ancestorContainerId;
		return self();
	}

	public boolean available() {
		return this.available;
	}

	public SpecimenListCriteria available(boolean available) {
		this.available = available;
		return self();
	}

	public boolean noQty() {
		return this.noQty;
	}

	public SpecimenListCriteria noQty(boolean noQty) {
		this.noQty = noQty;
		return self();
	}

	public Long reservedForDp() {
		return reservedForDp;
	}

	public SpecimenListCriteria reservedForDp(Long reservedForDp) {
		this.reservedForDp = reservedForDp;
		return self();
	}
}