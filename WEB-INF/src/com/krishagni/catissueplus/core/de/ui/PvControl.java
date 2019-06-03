package com.krishagni.catissueplus.core.de.ui;

import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import edu.common.dynamicextensions.domain.nui.AbstractLookupControl;
import edu.common.dynamicextensions.ndao.JdbcDaoFactory;

public class PvControl extends AbstractLookupControl implements Serializable {
	private static final long serialVersionUID = 1L;

	private String attribute;

	private boolean leafNode;

	private boolean rootNode;

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public boolean isLeafNode() {
		return leafNode;
	}

	public void setLeafNode(boolean leafNode) {
		this.leafNode = leafNode;
	}

	public boolean isRootNode() {
		return rootNode;
	}

	public void setRootNode(boolean rootNode) {
		this.rootNode = rootNode;
	}

	@Override
	public Long fromString(String s) {
		if (StringUtils.isBlank(s)) {
			return null;
		}

		try {
			return Long.parseLong(s);
		} catch (NumberFormatException nfe) {
			return getIdByValue(s);
		}
	}

	@Override
	public String getCtrlType() {
		return "pvField";
	}

	@Override
	public void getProps(Map<String, Object> props) {
		props.put("apiUrl", "rest/ng/permissible-values");
		props.put("dataType", getDataType());
		props.put("attribute", attribute);
		props.put("leafValue", leafNode);
		props.put("rootValue", rootNode);
	}

	@Override
	public void serializeToXml(Writer writer, Properties props) {
		super.serializeToXml("pvField", writer, props);
	}

	@Override
	public String getTableName() {
		return PV_TABLE;
	}

	@Override
	public String getValueColumn() {
		return VALUE_COLUMN;
	}

	@Override
	public String getAltKeyColumn() {
		return ALT_KEY;
	}

	@Override
	public Properties getPvSourceProps() {
		Map<String, Object> filters = new HashMap<>();
		filters.put("attribute", getAttribute());
		filters.put("includeOnlyLeafValue", isLeafNode());
		filters.put("includeOnlyRootValue", isRootNode());

		Properties props = new Properties();
		props.put("apiUrl", "rest/ng/permissible-values");
		props.put("searchTermName", "searchString");
		props.put("resultFormat", "{{value}}");
		props.put("filters", filters);
		return props;
	}

	private Long getIdByValue(String value) {
		return JdbcDaoFactory.getJdbcDao().getResultSet(
			GET_ID_BY_VALUE,
			Arrays.asList(attribute, value, value),
			(rs) -> rs.next() ? rs.getLong(1) : null
		);
	}

	private static final String PV_TABLE = "CATISSUE_PERMISSIBLE_VALUE";

	private static final String VALUE_COLUMN = "VALUE";

	private static final String ALT_KEY = "VALUE";

	private static final String GET_ID_BY_VALUE =
		"select " +
		"  identifier " +
		"from " +
		"  catissue_permissible_value pv " +
		"where " +
		"  pv.public_id = ? and (pv.value = ? or pv.concept_code = ?)";
}
