package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.repository.LabelPrintJobDao;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.events.LabelPrintStat;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class LabelPrintJobDaoImpl extends AbstractDao<LabelPrintJob> implements LabelPrintJobDao {

	@Override
	public Class<LabelPrintJob> getType() {
		return LabelPrintJob.class;
	}

	@Override
	public List<LabelPrintStat> getPrintStats(String type, Date start, Date end) {
		if (!type.equals("specimen")) {
			throw new IllegalArgumentException("Not supported for type = " + type);
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_SPMN_PRINT_STATS)
			.setParameter("startDate", start)
			.setParameter("endDate", end)
			.list();

		List<LabelPrintStat> stats = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			LabelPrintStat stat = new LabelPrintStat();
			stat.setUserFirstName((String) row[++idx]);
			stat.setUserLastName((String)row[++idx]);
			stat.setUserEmailAddress((String)row[++idx]);
			stat.setProtocol((String) row[++idx]);
			stat.setCount((Integer) row[++idx]);
			stats.add(stat);
		}

		return stats;
	}

	private static final String FQN = LabelPrintJob.class.getName();

	private static final String GET_SPMN_PRINT_STATS = FQN + ".getSpecimenPrintStats";
}
