package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.AsyncTaskExecutor;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder.Status;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.SpecimenRequestErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderDetail;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemDetail;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderSummary;
import com.krishagni.catissueplus.core.administrative.events.ReturnedSpecimenDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.services.DistributionOrderService;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenListErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.EntityCrudListener;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.SessionUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.Filter.Op;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.query.WideRowMode;

public class DistributionOrderServiceImpl implements DistributionOrderService, ObjectAccessor {
	private static final Log logger = LogFactory.getLog(DistributionOrderServiceImpl.class);

	private static final long ASYNC_CALL_TIMEOUT = 5000;

	private DaoFactory daoFactory;

	private DistributionOrderFactory distributionFactory;
	
	private QueryService querySvc;
	
	private EmailService emailService;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	private AsyncTaskExecutor taskExecutor;

	private List<EntityCrudListener<DistributionOrderDetail, DistributionOrder>> listeners = new ArrayList<>();

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDistributionFactory(DistributionOrderFactory distributionFactory) {
		this.distributionFactory = distributionFactory;
	}
	
	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}
	
	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionOrderSummary>> getOrders(RequestEvent<DistributionOrderListCriteria> req) {
		try {
			DistributionOrderListCriteria crit = addOrderListCriteria(req.getPayload());
			return ResponseEvent.response(daoFactory.getDistributionOrderDao().getOrders(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getOrdersCount(RequestEvent<DistributionOrderListCriteria> req) {
		try {
			DistributionOrderListCriteria crit = addOrderListCriteria(req.getPayload());
			return ResponseEvent.response(daoFactory.getDistributionOrderDao().getOrdersCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionOrderDetail> getOrder(RequestEvent<Long> req) {
		try {
			DistributionOrder order = getOrder(req.getPayload(), null);
			AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

			DistributionOrderDetail output = DistributionOrderDetail.from(order);
			listeners.forEach(listener -> listener.onRead(output, order));
			return ResponseEvent.response(output);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<DistributionOrderDetail> createOrder(RequestEvent<DistributionOrderDetail> req) {
		return saveOrUpdateOrder(this::createOrder, req.getPayload());
	}
	
	@Override
	public ResponseEvent<DistributionOrderDetail> updateOrder(RequestEvent<DistributionOrderDetail> req) {
		return saveOrUpdateOrder(this::updateOrder, req.getPayload());
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionOrderDetail> deleteOrder(RequestEvent<Long> req) {
		try {
			DistributionOrder order = getOrder(req.getPayload(), null);
			AccessCtrlMgr.getInstance().ensureDeleteDistributionOrderRights(order);
			order.delete();
			return ResponseEvent.response(DistributionOrderDetail.from(order));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportReport(RequestEvent<Long> req) {
		try {
			Long orderId = req.getPayload();
			DistributionOrder order = daoFactory.getDistributionOrderDao().getById(orderId);
			if (order == null) {
				return ResponseEvent.userError(DistributionOrderErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

			SavedQuery query = getReportQuery(order);
			if (query == null) {
				return ResponseEvent.userError(DistributionOrderErrorCode.RPT_TMPL_NOT_CONFIGURED, order.getDistributionProtocol().getShortTitle());
			}
			
			return new ResponseEvent<>(exportReport(order, query));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionOrderItemDetail>> getOrderItems(RequestEvent<DistributionOrderItemListCriteria> req) {
		try {
			DistributionOrderItemListCriteria crit = req.getPayload();

			DistributionOrder order = getOrder(crit.orderId(), null);
			AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

			List<DistributionOrderItem> items = daoFactory.getDistributionOrderDao().getOrderItems(crit);
			return ResponseEvent.response(DistributionOrderItemDetail.from(items));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionOrderItemDetail>> getDistributedSpecimens(RequestEvent<SpecimenListCriteria> req) {
		try {
			List<Specimen> specimens = getReadAccessSpecimens(req.getPayload());
			if (CollectionUtils.isEmpty(specimens)) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<Long> ids = specimens.stream().map(Specimen::getId).collect(Collectors.toList());
			List<DistributionOrderItem> items = daoFactory.getDistributionOrderDao().getDistributedOrderItems(ids);
			Set<DistributionOrder> accessAllowed = new HashSet<>();
			for (DistributionOrderItem item : items) {
				if (accessAllowed.contains(item.getOrder())) {
					continue;
				}

				AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(item.getOrder());
				accessAllowed.add(item.getOrder());
			}

			return ResponseEvent.response(DistributionOrderItemDetail.from(items));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> returnSpecimens(RequestEvent<List<ReturnedSpecimenDetail>> req) {
		try {
			Map<String, DistributionOrder> ordersMap = new HashMap<>();
			Map<String, StorageContainer> containersMap = new HashMap<>();

			List<Specimen> result = new ArrayList<>();
			for (ReturnedSpecimenDetail returnedSpmn : req.getPayload()) {
				result.add(returnSpecimen(returnedSpmn, ordersMap, containersMap));
			}

			return ResponseEvent.response(SpecimenInfo.from(result));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void addListener(EntityCrudListener<DistributionOrderDetail, DistributionOrder> listener) {
		listeners.add(listener);
	}

	@Override
	@PlusTransactional
	public void validateSpecimens(DistributionProtocol dp, List<Specimen> specimens) {
		List<Pair<Long, Long>> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidSpecimens(specimens, dp, siteCps, ose);
		ose.checkAndThrow();
	}

	@Override
	public String getObjectName() {
		return DistributionOrder.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getDistributionOrderDao().getOrderIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "OS_ORDERS_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		DistributionOrder order = getOrder(id, null);
		AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);
	}

	private DistributionOrderListCriteria addOrderListCriteria(DistributionOrderListCriteria crit) {
		Set<Long> siteIds = AccessCtrlMgr.getInstance().getReadAccessDistributionOrderSites();
		if (siteIds != null && siteIds.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (siteIds != null) {
			crit.siteIds(siteIds);
		}

		return crit;
	}

	@PlusTransactional
	private ResponseEvent<DistributionOrderDetail> createOrder(DistributionOrderDetail input) {
		long t1 = System.currentTimeMillis();
		try {
			DistributionOrder order = distributionFactory.createDistributionOrder(input, Status.PENDING);

			AccessCtrlMgr.getInstance().ensureCreateDistributionOrderRights(order);

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueConstraints(null, order, ose);
			ensureValidSpecimenList(order, ose);

			Status inputStatus = null;
			try {
				inputStatus = Status.valueOf(input.getStatus());
			} catch (IllegalArgumentException iae) {
				ose.addError(DistributionOrderErrorCode.INVALID_STATUS, input.getStatus());
			}

			ose.checkAndThrow();

			SpecimenRequest request = order.getRequest();
			if (request != null && request.isClosed()) {
				return ResponseEvent.userError(SpecimenRequestErrorCode.CLOSED, request.getId());
			}

			List<Pair<Long, Long>> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			daoFactory.getDistributionOrderDao().saveOrUpdate(order, true);

			ensureValidSpecimens(order, siteCps, ose);
			ose.checkAndThrow();

			order = daoFactory.getDistributionOrderDao().getById(order.getId());
			distributeOrder(order, siteCps, inputStatus);

			DistributionOrder savedOrder = daoFactory.getDistributionOrderDao().getById(order.getId());
			DistributionOrderDetail output = DistributionOrderDetail.from(savedOrder);
			listeners.forEach(listener -> listener.onSave(input, output, savedOrder));

			notifySaveOrUpdateOrder(savedOrder, null, t1);
			return ResponseEvent.response(output);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			logger.info("Time taken to save distribution order is " + (System.currentTimeMillis() - t1) + " ms");
		}
	}

	@PlusTransactional
	private ResponseEvent<DistributionOrderDetail> updateOrder(DistributionOrderDetail input) {
		long t1 = System.currentTimeMillis();
		try {
			DistributionOrder existingOrder = getOrder(input.getId(), input.getName());
			if (existingOrder.isOrderExecuted()) {
				return ResponseEvent.userError(DistributionOrderErrorCode.CANT_UPDATE_EXEC_ORDER, existingOrder.getName());
			}

			AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights(existingOrder);
			DistributionOrder newOrder = distributionFactory.createDistributionOrder(input, null);
			AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights(newOrder);

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueConstraints(existingOrder, newOrder, ose);
			ensureValidSpecimenList(newOrder, ose);
			ose.checkAndThrow();

			List<Pair<Long, Long>> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			Status oldStatus = existingOrder.getStatus();
			existingOrder.update(newOrder);
			daoFactory.getDistributionOrderDao().saveOrUpdate(existingOrder, true);

			ensureValidSpecimens(existingOrder, siteCps, ose);
			ose.checkAndThrow();

			existingOrder = daoFactory.getDistributionOrderDao().getById(existingOrder.getId());
			distributeOrder(existingOrder, siteCps, newOrder.getStatus());

			DistributionOrder savedOrder = daoFactory.getDistributionOrderDao().getById(existingOrder.getId());
			DistributionOrderDetail output = DistributionOrderDetail.from(savedOrder);
			listeners.forEach(listener -> listener.onSave(input, output, savedOrder));

			notifySaveOrUpdateOrder(savedOrder, oldStatus, t1);
			return ResponseEvent.response(output);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			logger.info("Time taken to save distribution order is " + (System.currentTimeMillis() - t1) + " ms");
		}
	}

	private ResponseEvent<DistributionOrderDetail> saveOrUpdateOrder(Function<DistributionOrderDetail, ResponseEvent<DistributionOrderDetail>> workFn, DistributionOrderDetail input) {
		User currentUser = AuthUtil.getCurrentUser();
		Future<ResponseEvent<DistributionOrderDetail>> result = taskExecutor.submit(
				() -> {
					try {
						AuthUtil.setCurrentUser(currentUser);
						return workFn.apply(input);
					} finally {
						AuthUtil.clearCurrentUser();
					}
				}
		);

		return unwrap(result, input.isAsync() ? ASYNC_CALL_TIMEOUT : 0);
	}

	private ResponseEvent<DistributionOrderDetail> unwrap(Future<ResponseEvent<DistributionOrderDetail>> result, long timeout) {
		try {
			if (timeout > 0) {
				return result.get(timeout, TimeUnit.MILLISECONDS);
			} else {
				return result.get();
			}
		} catch (TimeoutException e) {
			DistributionOrderDetail output = new DistributionOrderDetail();
			output.setCompleted(false);
			return ResponseEvent.response(output);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void ensureUniqueConstraints(DistributionOrder existingOrder, DistributionOrder newOrder, OpenSpecimenException ose) {
		if (existingOrder == null || !newOrder.getName().equals(existingOrder.getName())) {
			DistributionOrder order = daoFactory.getDistributionOrderDao().getOrder(newOrder.getName());
			if (order != null) {
				ose.addError(DistributionOrderErrorCode.DUP_NAME, newOrder.getName());
			}
		}
	}

	private void ensureValidSpecimenList(DistributionOrder order, OpenSpecimenException ose) {
		SpecimenList specimenList = order.getSpecimenList();
		if (specimenList == null) {
			return;
		}

		if (!AuthUtil.isAdmin() && !specimenList.canUserAccess(AuthUtil.getCurrentUser().getId())) {
			ose.addError(SpecimenListErrorCode.ACCESS_NOT_ALLOWED);
		}
	}

	private List<Specimen> getReadAccessSpecimens(SpecimenListCriteria crit) {
		List<Pair<Long, Long>> siteCpPairs = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCpPairs != null && siteCpPairs.isEmpty()) {
			return null;
		}

		return daoFactory.getSpecimenDao().getSpecimens(crit.siteCps(siteCpPairs));
	}

	private void ensureValidSpecimens(DistributionOrder order, List<Pair<Long, Long>> siteCps, OpenSpecimenException ose) {
		if (order.getSpecimenList() != null) {
			Long orderId = order.getId();

			int startAt = 0, maxSpmns = 100;
			boolean endOfSpecimens = false;
			while (!endOfSpecimens) {
				if (order == null) {
					order = daoFactory.getDistributionOrderDao().getById(orderId);
				}

				List<Specimen> specimens = getSpecimens(order.getSpecimenList(), siteCps, startAt, maxSpmns);
				if (specimens.isEmpty() && startAt == 0) {
					ose.addError(DistributionOrderErrorCode.NO_SPMNS_IN_LIST, order.getSpecimenList().getName());
				}

				ensureValidSpecimens(specimens, order.getDistributionProtocol(), siteCps, ose);

				startAt += specimens.size();
				endOfSpecimens = (specimens.size() < maxSpmns);

				specimens.clear();
				order = null;
				SessionUtil.getInstance().clearSession();
			}
		} else {
			List<Specimen> specimens = Utility.collect(order.getOrderItems(), "specimen");
			ensureValidSpecimens(specimens, order.getDistributionProtocol(), siteCps, ose);
		}
	}

	private List<Specimen> getSpecimens(SpecimenList specimenList, List<Pair<Long, Long>> siteCps, int startAt, int maxResults) {
		SpecimenListCriteria crit = new SpecimenListCriteria()
			.specimenListId(specimenList.getId()).siteCps(siteCps)
			.startAt(startAt).maxResults(maxResults)
			.limitItems(true);
		return daoFactory.getSpecimenDao().getSpecimens(crit);
	}

	private void ensureValidSpecimens(
		List<Specimen> inputSpecimens, DistributionProtocol dp, List<Pair<Long, Long>> siteCpPairs, OpenSpecimenException ose) {

		if (CollectionUtils.isEmpty(inputSpecimens)) {
			return;
		}

		List<Long> specimenIds = inputSpecimens.stream().map(Specimen::getId).collect(Collectors.toList());
		SpecimenListCriteria crit = new SpecimenListCriteria().siteCps(siteCpPairs).ids(specimenIds);
		String nonCompliantSpmnLabels = daoFactory.getSpecimenDao().getNonCompliantSpecimens(crit)
			.stream().collect(Collectors.joining(", "));
		if (!nonCompliantSpmnLabels.isEmpty()) {
			ose.addError(DistributionOrderErrorCode.SPECIMEN_DOES_NOT_EXIST, nonCompliantSpmnLabels);
		}

		List<String> closedSpmns = inputSpecimens.stream()
			.filter(spmn -> !spmn.isActive()).map(Specimen::getLabel)
			.collect(Collectors.toList());
		if (!closedSpmns.isEmpty()) {
			ose.addError(DistributionOrderErrorCode.CLOSED_SPECIMENS, closedSpmns);
			return;
		}

		ensureDpValidity(inputSpecimens, dp, ose);

		int stmtsCount = dp.getConsentTiers().size();
		if (stmtsCount > 0) {
			List<String> nonConsentingLabels = daoFactory.getDistributionProtocolDao()
				.getNonConsentingSpecimens(dp.getId(), specimenIds, stmtsCount);
			if (!nonConsentingLabels.isEmpty()) {
				ose.addError(DistributionOrderErrorCode.NON_CONSENTING_SPECIMENS, nonConsentingLabels);
			}
		}
	}

	private void ensureDpValidity(List<Specimen> specimens, DistributionProtocol dp, OpenSpecimenException ose) {
		List<Specimen> spmnWithDps = specimens.stream()
			.filter(spmn -> !spmn.getDistributionProtocols().isEmpty())
			.collect(Collectors.toList());

		List<String> resvForOthDps = spmnWithDps.stream()
			.filter(spmn -> !spmn.getDistributionProtocols().contains(dp))
			.map(Specimen::getLabel)
			.collect(Collectors.toList());

		if (!resvForOthDps.isEmpty()) {
			ose.addError(DistributionOrderErrorCode.SPMN_RESV_FOR_OTH_DPS, resvForOthDps, resvForOthDps.size());
		}

		List<Specimen> spmnWithoutDps = new ArrayList<>(CollectionUtils.removeAll(specimens, spmnWithDps));
		if (spmnWithoutDps.isEmpty()) {
			return;
		}

		Map<Long, Specimen> specimenMap = specimens.stream().collect(Collectors.toMap(Specimen::getId, s -> s));
		Set<Long> allowedSites = AccessCtrlMgr.getInstance().getDistributionOrderAllowedSites(dp);
		Map<Long, Set<Long>> spmnSitesMap = daoFactory.getSpecimenDao().getSpecimenSites(Utility.collect(spmnWithoutDps, "id", true));
		String errorLabels = spmnSitesMap.entrySet().stream()
			.filter(spmnSites -> CollectionUtils.intersection(spmnSites.getValue(), allowedSites).isEmpty())
			.map(spmnSites -> specimenMap.get(spmnSites.getKey()).getLabel())
			.collect(Collectors.joining(", "));

		if (StringUtils.isNotBlank(errorLabels)) {
			ose.addError(DistributionOrderErrorCode.INVALID_SPECIMENS_FOR_DP, errorLabels);
		}
	}

	private void distributeOrder(DistributionOrder order, List<Pair<Long, Long>> siteCps, Status status) {
		if (!Status.EXECUTED.equals(status)) {
			return;
		}

		order.distribute();
		daoFactory.getDistributionOrderDao().saveOrUpdate(order);
		if (order.getSpecimenList() == null) {
			return;
		}

		boolean endOfSpecimens = false;
		int starAt = 0, maxSpmns = 100;
		List<Specimen> specimens;
		while (!endOfSpecimens) {
			specimens = getSpecimens(order.getSpecimenList(), siteCps, starAt, maxSpmns);
			specimens.forEach(spmn -> distributeSpecimen(order, spmn));

			starAt += specimens.size();
			endOfSpecimens = (specimens.size() < maxSpmns);

			specimens.clear();
			SessionUtil.getInstance().clearSession();
		}
	}

	private void distributeSpecimen(DistributionOrder order, Specimen specimen) {
		DistributionOrderItem item = DistributionOrderItem.createOrderItem(order, specimen);
		daoFactory.getDistributionOrderDao().saveOrUpdateOrderItem(item);
		item.distribute();
	}

	private SavedQuery getReportQuery(DistributionOrder order) {
		SavedQuery query = order.getDistributionProtocol().getReport();
		if (query != null) {
			return query;
		}

		Integer queryId = ConfigUtil.getInstance().getIntSetting("common", "distribution_report_query", -1);
		if (queryId == -1) {
			return null;
		}

		return deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
	}

	private QueryDataExportResult exportReport(final DistributionOrder order, SavedQuery report) {
		Filter filter = new Filter();
		filter.setField("Specimen.specimenOrders.id");
		filter.setOp(Op.EQ);
		filter.setValues(new String[] { order.getId().toString() });
		
		ExecuteQueryEventOp execReportOp = new ExecuteQueryEventOp();
		execReportOp.setDrivingForm("Participant");
		execReportOp.setAql(report.getAql(new Filter[] { filter }));			
		execReportOp.setWideRowMode(WideRowMode.DEEP.name());
		execReportOp.setRunType("Export");
		return querySvc.exportQueryData(execReportOp, new QueryService.ExportProcessor() {
			@Override
			public String filename() {
				return "order_" + order.getId() + "_" + UUID.randomUUID().toString();
			}

			@Override
			public void headers(OutputStream out) {
				@SuppressWarnings("serial")
				Map<String, String> headers = new LinkedHashMap<String, String>() {{
					String notSpecified = msg("common_not_specified");
					DistributionProtocol dp = order.getDistributionProtocol();

					put(msg("dist_order_name"),     order.getName());
					put(msg("dist_dp_title"),       dp.getTitle());
					put(msg("dist_dp_short_title"), dp.getShortTitle());
					put(msg("dist_requestor_name"), order.getRequester().formattedName());
					put(msg("dist_requested_date"), Utility.getDateString(order.getExecutionDate()));
					put(msg("dist_receiving_site"), order.getSite() == null ? notSpecified : order.getSite().getName());
					put(msg("dist_tracking_url"),   StringUtils.isBlank(order.getTrackingUrl()) ? notSpecified : order.getTrackingUrl());
					put(msg("dist_comments"),       StringUtils.isBlank(order.getComments()) ? notSpecified : order.getComments());
					put(msg("dp_irb_id"),           StringUtils.isBlank(dp.getIrbId()) ? notSpecified : dp.getIrbId());
					put(msg("dist_exported_by"),    AuthUtil.getCurrentUser().formattedName());
					put(msg("dist_exported_on"),    Utility.getDateString(Calendar.getInstance().getTime()));

					User pi = dp.getPrincipalInvestigator();
					put(msg("dist_dp_pi_inst"),        pi.getInstitute().getName());
					put(msg("dist_dp_pi_email_addr"),  pi.getEmailAddress());
					put(msg("dist_dp_pi_cont_num"),    StringUtils.isBlank(pi.getPhoneNumber()) ? notSpecified : pi.getPhoneNumber());
					put(msg("dist_dp_pi_addr"),        StringUtils.isBlank(pi.getAddress()) ? notSpecified : pi.getAddress());

					DeObject extension = order.getDistributionProtocol().getExtension();
					if (extension != null) {
						putAll(extension.getLabelValueMap());
					}

					put("", ""); // blank line
				}};

				Utility.writeKeyValuesToCsv(out, headers);
			}
		});
	}

	private String msg(String code) {
		return MessageUtil.getInstance().getMessage(code);
	}
	
	private void notifySaveOrUpdateOrder(DistributionOrder order, Status oldStatus, Long startTime) {
		long timeTaken = (System.currentTimeMillis() - startTime) + 500;

		Status newStatus = order.getStatus();
		if (timeTaken < ASYNC_CALL_TIMEOUT && (!newStatus.equals(Status.EXECUTED) || newStatus.equals(oldStatus))) {
			return;
		}

		Set<User> rcpts = new HashSet<>();
		if (newStatus.equals(Status.EXECUTED)) {
			rcpts.add(order.getDistributor());
			rcpts.add(order.getRequester());
			rcpts.add(order.getDistributionProtocol().getPrincipalInvestigator());
			rcpts.addAll(order.getDistributionProtocol().getCoordinators());

			if (order.getSite() != null && CollectionUtils.isNotEmpty(order.getSite().getCoordinators())) {
				rcpts.addAll(order.getSite().getCoordinators());
			}
		}

		if (!rcpts.contains(AuthUtil.getCurrentUser())) {
			rcpts.add(AuthUtil.getCurrentUser());
		}

		Object[] subjectParams = { order.getName(), newStatus.equals(Status.EXECUTED) ? 1 : 2 };

		// Send email notification
		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", subjectParams);
		emailProps.put("order", order);
		for (User rcpt : rcpts) {
			emailProps.put("rcpt", rcpt);
			emailService.sendEmail(ORDER_DISTRIBUTED_EMAIL_TMPL, new String[] { rcpt.getEmailAddress() }, null, emailProps);
		}

		// UI notification
		String msg = MessageUtil.getInstance().getMessage(ORDER_DISTRIBUTED_EMAIL_TMPL + "_subj", subjectParams);
		Notification notif = new Notification();
		notif.setEntityType(DistributionOrder.getEntityName());
		notif.setEntityId(order.getId());
		notif.setOperation(oldStatus == null ? "CREATE" : "UPDATE");
		notif.setCreatedBy(AuthUtil.getCurrentUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setMessage(msg);
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("order-overview", rcpts));
	}

	private DistributionOrder getOrder(Long orderId, String orderName) {
		DistributionOrder order = null;
		Object key = null;

		if (orderId != null) {
			order = daoFactory.getDistributionOrderDao().getById(orderId);
			key = orderId;
		} else if (StringUtils.isNotBlank(orderName)) {
			order = daoFactory.getDistributionOrderDao().getOrder(orderName);
			key = orderName;
		}

		if (order == null) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.NOT_FOUND, key);
		}

		return order;
	}

	private Specimen returnSpecimen(
			ReturnedSpecimenDetail detail,
			Map<String, DistributionOrder> ordersMap,
			Map<String, StorageContainer> containersMap) {

		String orderName = detail.getOrderName();
		if (StringUtils.isBlank(orderName)) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.NAME_REQUIRED);
		}

		String label = detail.getSpecimenLabel();
		if (StringUtils.isBlank(label)) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.LABEL_REQUIRED);
		}

		DistributionOrder order = ordersMap.get(orderName);
		if (order == null) {
			order = getOrder(null, detail.getOrderName());
			AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights(order);
			if (!order.isOrderExecuted()) {
				throw OpenSpecimenException.userError(DistributionOrderErrorCode.NOT_DISTRIBUTED, order.getName());
			}

			ordersMap.put(order.getName(), order);
		}

		DistributionOrderItem item = order.getItemBySpecimen(label);
		if (item == null) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.SPMN_NOT_FOUND, label, orderName);
		}

		returnSpecimen(detail, item, containersMap);
		return item.getSpecimen();
	}


	private void returnSpecimen(ReturnedSpecimenDetail detail, DistributionOrderItem item, Map<String, StorageContainer> containersMap) {
		ensureItemNotReturned(item);
		setItemReturnedQty(item, detail.getQuantity());
		setItemReturnDate(item, detail.getTime());
		setItemReturnedBy(item, detail.getUser());
		setItemReturningPosition(item, detail.getLocation(), containersMap);
		setItemFreezeThawIncrOnReturn(item, detail.getIncrFreezeThaw());
		item.setReturnComments(detail.getComments());
		item.returnSpecimen();
	}

	private void ensureItemNotReturned(DistributionOrderItem item) {
		if (item.isReturned()) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.SPECIMEN_ALREADY_RETURNED, item.getSpecimen().getLabel());
		}
	}

	private void setItemReturnedQty(DistributionOrderItem item, BigDecimal returnQty) {
		if (returnQty == null) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.RETURN_QTY_REQ, item.getSpecimen().getLabel());
		}


		if (NumUtil.lessThanEqualsZero(returnQty) || NumUtil.lessThan(item.getQuantity(), returnQty)) {
			raiseError(DistributionOrderErrorCode.INVALID_RETURN_QUANTITY, item.getSpecimen().getLabel(), returnQty);
		}

		item.setReturnedQuantity(returnQty);
	}

	private void setItemReturnDate(DistributionOrderItem item, Date returnDate) {
		if (returnDate == null) {
			raiseError(DistributionOrderErrorCode.RETURN_DATE_REQ, item.getSpecimen().getLabel());
		}

		if (item.getOrder().getExecutionDate().after(returnDate)) {
			raiseError(DistributionOrderErrorCode.INVALID_RETURN_DATE, item.getSpecimen().getLabel(), returnDate);
		}

		item.setReturnDate(returnDate);
	}

	private void setItemReturningPosition(DistributionOrderItem item, StorageLocationSummary location, Map<String, StorageContainer> containersMap) {
		if (location == null || StringUtils.isBlank(location.getName())) {
			return;
		}

		StorageContainer container = containersMap.get(location.getName());
		if (container == null) {
			Object key = null;
			if (location.getId() != null) {
				container = daoFactory.getStorageContainerDao().getById(location.getId());
				key = location.getId();
			} else {
				container = daoFactory.getStorageContainerDao().getByName(location.getName());
				key = location.getName();
			}

			if (container == null) {
				raiseError(StorageContainerErrorCode.NOT_FOUND, key, 1);
			}

			containersMap.put(location.getName(), container);
		}


		Specimen specimen = item.getSpecimen();
		if (!container.canContain(specimen)) {
			raiseError(StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, container.getName(), specimen.getLabelOrDesc());
		}

		//
		// TODO: This is duplicate code. Need to consolidate this with specimen/container objects
		//
		String row = null, column = null;
		if (!container.isDimensionless()) {
			row = location.getPositionY();
			column = location.getPositionX();
			if (container.usesLinearLabelingMode() && location.getPosition() != null && location.getPosition() != 0) {
				row = String.valueOf((location.getPosition() - 1) / container.getNoOfColumns() + 1);
				column = String.valueOf((location.getPosition() - 1) % container.getNoOfColumns() + 1);
			}
		}

		StorageContainerPosition position = null;
		if (StringUtils.isNotBlank(row) && StringUtils.isNotBlank(column)) {
			if (container.canSpecimenOccupyPosition(specimen.getId(), column, row)) {
				position = container.createPosition(column, row);
				container.setLastAssignedPos(position);
			} else {
				raiseError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
			}
		} else {
			position = container.nextAvailablePosition(true);
			if (position == null) {
				raiseError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
			}
		}

		item.setReturningContainer(container);
		item.setReturningRow(position.getPosTwo());
		item.setReturningColumn(position.getPosOne());
	}

	private void setItemFreezeThawIncrOnReturn(DistributionOrderItem item, Integer incrFreezeThaw) {
		item.getSpecimen().incrementFreezeThaw(incrFreezeThaw);
		item.setFreezeThawIncrOnReturn(incrFreezeThaw);
	}

	private void setItemReturnedBy(DistributionOrderItem item, UserSummary userDetail) {
		if (userDetail == null || (userDetail.getId() == null && StringUtils.isBlank(userDetail.getEmailAddress()))) {
			raiseError(DistributionOrderErrorCode.RETURNED_BY_REQ, item.getSpecimen().getLabel());
		}

		Object key = null;
		User user = null;
		if (userDetail.getId() != null) {
			key = userDetail.getId();
			user = daoFactory.getUserDao().getById(userDetail.getId());
		} else {
			key = userDetail.getEmailAddress();
			user = daoFactory.getUserDao().getUserByEmailAddress(userDetail.getEmailAddress());
		}

		if (user == null) {
			raiseError(UserErrorCode.NOT_FOUND, key);
		}

		item.setReturnedBy(user);
	}

	private void raiseError(ErrorCode error, Object ... args) {
		throw OpenSpecimenException.userError(error, args);
	}

	private static final String ORDER_DISTRIBUTED_EMAIL_TMPL = "order_distributed";
}
