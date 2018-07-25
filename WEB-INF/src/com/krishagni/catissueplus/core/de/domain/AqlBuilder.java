package com.krishagni.catissueplus.core.de.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.de.domain.Filter.Op;
import com.krishagni.catissueplus.core.de.domain.QueryExpressionNode.LogicalOp;
import com.krishagni.catissueplus.core.de.domain.QueryExpressionNode.Parenthesis;
import com.krishagni.catissueplus.core.de.domain.SelectField.Function;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DataType;
import edu.common.dynamicextensions.domain.nui.LookupControl;

@Configurable
public class AqlBuilder {

	@Autowired
	private DaoFactory daoFactory;
	
	private AqlBuilder() {
		
	}
	
	public static AqlBuilder getInstance() {
		return new AqlBuilder();
	}
	
	public String getQuery(Object[] selectList, Filter[] filters, QueryExpressionNode[] queryExprNodes) {
		return getQuery(selectList, filters, StringUtils.EMPTY, queryExprNodes);
	}
	
	public String getQuery(Object[] selectList, Filter[] filters, Filter[] conjunctionFilters, QueryExpressionNode[] queryExprNodes) {
		Context ctx = new Context();

		StringBuilder conjunctionExpr = new StringBuilder();
		if (conjunctionFilters != null) {
			for (int i = 0; i < conjunctionFilters.length; ++i) {
				if (i > 0) {
					conjunctionExpr.append(" and ");
				}

				conjunctionExpr.append(buildFilterExpr(ctx, conjunctionFilters[i]));
			}
		}

		return getQuery(ctx, selectList, filters, conjunctionExpr.toString(), queryExprNodes);
	}

	public String getQuery(Object[] selectList, Filter[] filters, String conjunction, QueryExpressionNode[] queryExprNodes) {
		return getQuery(new Context(), selectList, filters, conjunction, queryExprNodes);
	}

	private String getQuery(Context ctx, Object[] selectList, Filter[] filters, String conjunction, QueryExpressionNode[] queryExprNodes) {
		Map<Integer, Filter> filterMap = new HashMap<>();
		for (Filter filter : filters) {
			filterMap.put(filter.getId(), filter);
		}

		String selectClause = buildSelectClause(filterMap, selectList);
		String whereClause = buildWhereClause(ctx, filterMap, queryExprNodes);
		if (StringUtils.isNotBlank(conjunction)) {
			whereClause = "(" + whereClause + ") and (" + conjunction + ")";
		}

		String query = "";
		if (StringUtils.isNotBlank(selectClause)) {
			query = "select " + selectClause + " where ";
		}

		return query + whereClause;
	}
	
	private String buildSelectClause(Map<Integer, Filter> filterMap, Object[] selectList) {
		if (selectList == null || selectList.length == 0) {
			return "";
		}

		StringBuilder select = new StringBuilder();
		for (Object field : selectList) {
			SelectField aggField = null;
			if (field instanceof String) {
				aggField = new SelectField();
				aggField.setName((String) field);
			} else if (field instanceof Map) {
				aggField = new ObjectMapper().convertValue(field, SelectField.class);
			} else if (field instanceof SelectField) {
				aggField = (SelectField) field;
			}

			if (aggField == null) {
				continue;
			}

			if (aggField.getAggFns() == null || aggField.getAggFns().isEmpty()) {
				select.append(getFieldExpr(filterMap, aggField, true)).append(", ");
			} else {
				String fieldExpr = getFieldExpr(filterMap, aggField, false);
					
				StringBuilder fnExpr = new StringBuilder("");
				for (Function fn : aggField.getAggFns()) {
					if (fnExpr.length() > 0) {
						fnExpr.append(", ");
					}
						
					if (fn.getName().equals("count")) {
						fnExpr.append("count(distinct ");
					} else {
						fnExpr.append(fn.getName()).append("(");
					}
						
					fnExpr.append(fieldExpr).append(") as \"").append(fn.getDesc()).append(" \"");
				}
					
				select.append(fnExpr.toString()).append(", ");
			}
		}
		
		int endIdx = select.length() - 2;		
		return select.substring(0, endIdx < 0 ? 0 : endIdx);
	}
	
	private String getFieldExpr(Map<Integer, Filter> filterMap, SelectField field, boolean includeDesc) {
		String fieldName = field.getName();
		if (!fieldName.startsWith("$temporal.")) {
			String alias = StringUtils.EMPTY;
			if (includeDesc && StringUtils.isNotBlank(field.getDisplayLabel())) {
				alias = " as \"" + field.getDisplayLabel() + "\"";
			}

			return fieldName + alias;
		}

		Integer filterId = Integer.parseInt(fieldName.substring("$temporal.".length()));
		Filter filter = filterMap.get(filterId);

		String expr = getLhs(filter.getExpr());
		if (includeDesc) {
			if (StringUtils.isNotBlank(field.getDisplayLabel())) {
				expr += " as \"" + field.getDisplayLabel() + "\"";
			} else {
				expr += " as \"" + filter.getDesc() + "\"";
			}
		}

		return expr;
	}	
	
	private String buildWhereClause(Context ctx, Map<Integer, Filter> filterMap, QueryExpressionNode[] queryExprNodes) {
		StringBuilder whereClause = new StringBuilder();
		
		for (QueryExpressionNode node : queryExprNodes) {
			switch (node.getNodeType()) {
			  case FILTER:
				  int filterId;
				  if (node.getValue() instanceof Double) {
					  filterId = ((Double)node.getValue()).intValue();
				  } else {
					  filterId = (Integer)node.getValue();
				  }
				  
				  Filter filter = filterMap.get(filterId);
				  String filterExpr = buildFilterExpr(ctx, filter);
				  whereClause.append(filterExpr);				  				  
				  break;
				  
			  case OPERATOR:
				  LogicalOp op = null;
				  if (node.getValue() instanceof String) {
					  op = LogicalOp.valueOf((String)node.getValue());
				  } else if (node.getValue() instanceof LogicalOp) {
					  op = (LogicalOp)node.getValue();
				  } 
				  whereClause.append(op.symbol());
				  break;
				  
			  case PARENTHESIS:
				  Parenthesis paren = null;
				  if (node.getValue() instanceof String) {
					  paren = Parenthesis.valueOf((String)node.getValue());
				  } else if (node.getValue() instanceof Parenthesis) {
					  paren = (Parenthesis)node.getValue();
				  }				  
				  whereClause.append(paren.symbol());
				  break;				  				
			}
			
			whereClause.append(" ");
		}
		
		return whereClause.toString();
	}
	
	private String buildFilterExpr(Context ctx, Filter filter) {
		if (filter.getExpr() != null) {
			return filter.getExpr();
		}
		
		String field = filter.getField();
		String[] fieldParts = field.split("\\.");
		if (fieldParts.length <= 1) {
			throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "Invalid field: " + field);
		}
				
		StringBuilder filterExpr = new StringBuilder();
		filterExpr.append(field).append(" ").append(filter.getOp().symbol()).append(" ");
		if (filter.getOp().isUnary()) {
			return filterExpr.toString();
		}

		if (filter.getSubQueryId() != null) {
			SavedQuery query = ctx.getQuery(filter.getSubQueryId()).copy();

			ctx.addActiveQuery(filter.getSubQueryId());
			String subAql = getQuery(ctx, new Object[] { field }, query.getFilters(), null, query.getQueryExpression());
			ctx.removeActiveQuery(filter.getSubQueryId());

			return filterExpr.append("(").append(subAql).append(")").toString();
		}

		Container form = null;
		String ctrlName = null;
		Control ctrl = null;
		if (fieldParts[1].equals("extensions") || fieldParts[1].equals("customFields")) {
			if (fieldParts.length < 4) {
				return "";
			}
			
			form = getContainer(fieldParts[2]);
			ctrlName = StringUtils.join(fieldParts, ".", 3, fieldParts.length);
		} else {
			form = getContainer(fieldParts[0]);
			ctrlName = StringUtils.join(fieldParts, ".", 1, fieldParts.length);
		}

		if (form == null) {
			throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "Invalid field: " + field);
		}

		ctrl = form.getControlByUdn(ctrlName, "\\.");

		DataType type = ctrl.getDataType();
		if (ctrl instanceof LookupControl) {
			type = ((LookupControl)ctrl).getValueType();
		}
		
		String[] values = Arrays.copyOf(filter.getValues(), filter.getValues().length);
		quoteStrings(type, values);
		
		String value = values[0];
		if (filter.getOp() == Op.IN || filter.getOp() == Op.NOT_IN) {
			value = "(" + join(values) + ")";
		} else if (filter.getOp() == Op.BETWEEN) {
			value =  "(" + values[0] + ", " + values[1] + ")";
		}
		
		return filterExpr.append(value).toString();
	}
	
	private void quoteStrings(DataType type, String[] values) {
		if (type != DataType.STRING && type != DataType.DATE) {
			return;
		}
		
		for (int i = 0; i < values.length; ++i) {
			values[i] = "\"" + values[i] + "\"";   
		}		
	}
	
	private String join(String[] values) {
		StringBuilder result = new StringBuilder();
		for (String val : values) {
			result.append(val).append(", ");
		}
        
		int endIdx = result.length() - 2;
		return result.substring(0, endIdx < 0 ? 0 : endIdx);
	}
	
	private String getLhs(String temporalExpr) {
		String[] parts = temporalExpr.split("[<=>!]|\\sany\\s*$|\\sexists\\s*$|\\snot exists\\s*$|\\sbetween\\s");
		return parts[0];
	}
	
	public Container getContainer(String formName){
		return Container.getContainer(formName);
	}

	private class Context {
		private Map<Long, SavedQuery> queryMap = new HashMap<>();

		private Set<Long> activeQueries = new HashSet<>();

		SavedQuery getQuery(Long queryId) {
			SavedQuery query = queryMap.get(queryId);
			if (query == null) {
				query = daoFactory.getSavedQueryDao().getQuery(queryId);
				if (query == null) {
					throw OpenSpecimenException.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
				}

				queryMap.put(queryId, query);
			}

			return query;
		}

		void addActiveQuery(Long queryId) {
			boolean added = activeQueries.add(queryId);
			if (!added) {
				throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "One or more cyclic sub-queries found: " + queryId + "!");
			}
		}

		void removeActiveQuery(Long queryId) {
			activeQueries.remove(queryId);
		}
	}
}
