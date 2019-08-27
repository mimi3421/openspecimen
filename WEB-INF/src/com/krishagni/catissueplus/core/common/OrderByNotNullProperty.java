package com.krishagni.catissueplus.core.common;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Order;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

public class OrderByNotNullProperty extends Order {
	private String[] propertyNames;

	protected OrderByNotNullProperty(String propertyName, boolean ascending) {
		super(propertyName, ascending);
	}

	protected OrderByNotNullProperty(boolean ascending, String... propertyNames) {
		super(propertyNames[0], ascending);
		this.propertyNames = propertyNames;
	}

	@Override
	public String toString() {
		return "(" + String.join(",", propertyNames) + ") " + ( isAscending() ? "asc" : "desc" );
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		List<String> columns = new ArrayList<>();
		Type type = null;

		for (String property : propertyNames) {
			String[] propColumns = criteriaQuery.getColumnsUsingProjection(criteria, property);
			columns.addAll(Arrays.asList(propColumns));

			if (type == null) {
				type = criteriaQuery.getTypeUsingProjection(criteria, property);
			}
		}

		SessionFactoryImplementor factory = criteriaQuery.getFactory();
		StringBuilder fragment = new StringBuilder("case");
		for (int i = 0; i < columns.size(); ++i) {
			if (i == columns.size() - 1) {
				fragment.append(" else ");
			} else {
				fragment.append(" when ").append(columns.get(i)).append(" is not null then ");
			}

			boolean lower = false;
			if (isIgnoreCase()) {
				int sqlType = type.sqlTypes(factory)[0];
				lower = sqlType == Types.VARCHAR || sqlType == Types.CHAR || sqlType == Types.LONGVARCHAR;
			}

			if (lower) {
				fragment.append( factory.getDialect().getLowercaseFunction() ).append('(');
			}

			fragment.append(columns.get(i));

			if (lower) {
				fragment.append(')');
			}
		}

		fragment.append(" end");
		return factory.getDialect().renderOrderByElement(
			fragment.toString(),
			null,
			isAscending() ? "asc" : "desc",
			factory.getSettings().getDefaultNullPrecedence());
	}

	public static OrderByNotNullProperty asc(String... propertyNames) {
		return new OrderByNotNullProperty(true, propertyNames);
	}

	public static OrderByNotNullProperty desc(String... propertyNames) {
		return new OrderByNotNullProperty(false, propertyNames);
	}
}
