package com.krishagni.catissueplus.core.query;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public class DefaultListService implements ListService {
	private Map<String, Function<Map<String, Object>, ListConfig>> listConfigFns = new HashMap<>();

	private ListGenerator listGenerator;

	public void setListGenerator(ListGenerator listGenerator) {
		this.listGenerator = listGenerator;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ListConfig> getListCfg(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> input = req.getPayload();
			String listName = (String)input.get("listName");
			Function<Map<String, Object>, ListConfig> configFn = listConfigFns.get(listName);
			if (configFn == null) {
				return ResponseEvent.response(null);
			}

			ListConfig cfg = configFn.apply(input);
			if (cfg == null) {
				return ResponseEvent.response(null);
			}

			cfg.setFilters(listGenerator.getFilters(cfg));
			return ResponseEvent.response(cfg);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ListDetail> getList(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> listReq = req.getPayload();
			String listName = (String)listReq.get("listName");
			Function<Map<String, Object>, ListConfig> configFn = listConfigFns.get(listName);
			if (configFn == null) {
				return ResponseEvent.response(null);
			}

			ListConfig cfg = configFn.apply(listReq);
			if (cfg == null) {
				return ResponseEvent.response(null);
			}

			return ResponseEvent.response(listGenerator.getList(cfg, (List<Column>)listReq.get("filters")));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getListSize(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> listReq = req.getPayload();
			String listName = (String)listReq.get("listName");
			Function<Map<String, Object>, ListConfig> configFn = listConfigFns.get(listName);
			if (configFn == null) {
				return ResponseEvent.response(null);
			}

			ListConfig cfg = configFn.apply(listReq);
			if (cfg == null) {
				return ResponseEvent.response(null);
			}

			return ResponseEvent.response(listGenerator.getListSize(cfg, (List<Column>)listReq.get("filters")));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Collection<Object>> getListExprValues(RequestEvent<Map<String, Object>> req) {
		try {
			Map<String, Object> listReq = req.getPayload();
			String listName = (String)listReq.get("listName");
			Function<Map<String, Object>, ListConfig> configFn = listConfigFns.get(listName);
			if (configFn == null) {
				return ResponseEvent.response(null);
			}

			ListConfig cfg = configFn.apply(listReq);
			if (cfg == null) {
				return ResponseEvent.response(null);
			}

			String expr = (String)listReq.get("expr");
			String searchTerm = (String)listReq.get("searchTerm");
			return ResponseEvent.response(listGenerator.getExpressionValues(cfg, expr, searchTerm));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void registerListConfigurator(String name, Function<Map<String, Object>, ListConfig> configFn) {
		listConfigFns.put(name, configFn);
	}
}
