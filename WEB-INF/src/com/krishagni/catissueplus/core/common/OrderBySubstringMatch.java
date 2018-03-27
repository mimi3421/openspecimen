package com.krishagni.catissueplus.core.common;

import java.sql.Types;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Order;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

public class OrderBySubstringMatch extends Order {

	private String substring;

	protected OrderBySubstringMatch(String propertyName, String substring, boolean ascending) {
		super(propertyName, ascending);
		this.substring = substring;
	}

	@Override
	public String toString() {
		return "(" + getPropertyName() + ", " + substring + ") " + ( isAscending() ? "asc" : "desc" );
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, getPropertyName());
		Type type = criteriaQuery.getTypeUsingProjection(criteria, getPropertyName());

		StringBuilder fragment = new StringBuilder();
		for (int i = 0; i < columns.length; i++ ) {
			StringBuilder expression = new StringBuilder("instr(");
			SessionFactoryImplementor factory = criteriaQuery.getFactory();

			boolean lower = false;
			if (isIgnoreCase()) {
				int sqlType = type.sqlTypes( factory )[i];
				lower = sqlType == Types.VARCHAR || sqlType == Types.CHAR || sqlType == Types.LONGVARCHAR;
			}

			if (lower) {
				expression.append( factory.getDialect().getLowercaseFunction() ).append('(');
			}

			expression.append(columns[i]);

			if (lower) {
				expression.append(')');
			}

			expression.append(", '")
				.append(lower ? substring.toLowerCase() : substring)
				.append("')");

			fragment.append(
				factory.getDialect().renderOrderByElement(
					expression.toString(),
					null,
					isAscending() ? "asc" : "desc",
					factory.getSettings().getDefaultNullPrecedence()
				)
			);

			if (i < columns.length - 1) {
				fragment.append(", ");
			}
		}

		return fragment.toString();
	}

	public static OrderBySubstringMatch asc(String propertyName, String substring) {
		return new OrderBySubstringMatch(propertyName, substring, true);
	}

	public static OrderBySubstringMatch desc(String propertyName, String substring) {
		return new OrderBySubstringMatch(propertyName, substring, false);
	}
}
