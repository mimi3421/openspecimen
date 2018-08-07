package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.TransactionalThreadLocals;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

@Configurable
public class AutomatedContainerContext {
	private static AutomatedContainerContext instance = new AutomatedContainerContext();

	private ThreadLocal<Map<String, ContainerStoreList>> listsCtx = new ThreadLocal<Map<String, ContainerStoreList>>() {
		@Override
		protected Map<String, ContainerStoreList> initialValue() {
			TransactionalThreadLocals.getInstance().register(this);
			return new HashMap<>();
		}
	};

	@Autowired
	private DaoFactory daoFactory;

	public static AutomatedContainerContext getInstance() {
		return instance;
	}

	public void storeSpecimen(StorageContainer container, Specimen specimen) {
		addSpecimen(container, specimen, ContainerStoreList.Op.PUT);
	}

	public void retrieveSpecimen(StorageContainer container, Specimen specimen) {
		addSpecimen(container, specimen, ContainerStoreList.Op.PICK);
	}

	private void addSpecimen(StorageContainer container, Specimen specimen, ContainerStoreList.Op op) {
		if (!container.isAutomated()) {
			return;
		}

		ContainerStoreList storeList = listsCtx.get().get(listLookupKey(container, op));
		if (storeList == null) {
			storeList = createNewList(container, op);
		}

		ContainerStoreListItem item = new ContainerStoreListItem();
		item.setSpecimen(specimen);
		item.setStoreList(storeList);


		Runnable saveItem = () -> daoFactory.getContainerStoreListDao().saveOrUpdateItem(item);
		if (specimen.getId() == null) {
			specimen.addOnSaveProc(saveItem);
		} else {
			saveItem.run();
		}
	}

	private String listLookupKey(StorageContainer container, ContainerStoreList.Op op) {
		return container.getId() + "-" + op.name();
	}

	private ContainerStoreList createNewList(StorageContainer container, ContainerStoreList.Op op) {
		ContainerStoreList list = new ContainerStoreList();
		list.setContainer(container);
		list.setCreationTime(Calendar.getInstance().getTime());
		list.setOp(op);
		list.setUser(AuthUtil.getCurrentUser());

		daoFactory.getContainerStoreListDao().saveOrUpdate(list);
		listsCtx.get().put(listLookupKey(container, op), list);
		return list;
	}
}
