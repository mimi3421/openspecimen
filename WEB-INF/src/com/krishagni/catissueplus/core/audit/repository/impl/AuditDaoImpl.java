package com.krishagni.catissueplus.core.audit.repository.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;

import com.krishagni.catissueplus.core.audit.domain.DeleteLog;
import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
import com.krishagni.catissueplus.core.audit.repository.AuditDao;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class AuditDaoImpl extends AbstractDao<UserApiCallLog> implements AuditDao {

	@Override
	@SuppressWarnings("unchecked")
	public AuditDetail getAuditDetail(String auditTable, Long objectId) {
		RevisionDetail createRev = getRevisionInfo(getLatestRevisionInfo(auditTable, objectId, 0));
		RevisionDetail updateRev = getRevisionInfo(getLatestRevisionInfo(auditTable, objectId, 1));

		AuditDetail result = new AuditDetail();
		result.setCreatedOn(createRev.getChangedOn());
		result.setCreatedBy(createRev.getChangedBy());
		result.setLastUpdatedOn(updateRev.getChangedOn());
		result.setLastUpdatedBy(updateRev.getChangedBy());

		if (result.getLastUpdatedOn() != null) {
			result.setRevisionsCount(getRevisionsCount(auditTable, objectId));
		} else {
			result.setRevisionsCount(1);
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<RevisionDetail> getRevisions(String auditTable, Long objectId) {
		List<Object[]> rows = getCurrentSession().createSQLQuery(String.format(GET_REV_INFO_SQL, auditTable, ""))
			.addScalar("rev", LongType.INSTANCE)
			.addScalar("revTime", TimestampType.INSTANCE)
			.addScalar("userId", LongType.INSTANCE)
			.addScalar("firstName", StringType.INSTANCE)
			.addScalar("lastName", StringType.INSTANCE)
			.addScalar("emailAddr", StringType.INSTANCE)
			.setParameter("objectId", objectId)
			.list();

		return rows.stream().map(this::getRevisionInfo).collect(Collectors.toList());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Date getLatestApiCallTime(Long userId, String token) {
		List<Date> result = getCurrentSession().getNamedQuery(GET_LATEST_API_CALL_TIME)
			.setLong("userId", userId)
			.setString("authToken", token)
			.list();

		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public void saveOrUpdate(DeleteLog log) {
		getCurrentSession().saveOrUpdate(log);
	}

	@SuppressWarnings("unchecked")
	private Object[] getLatestRevisionInfo(String auditTable, Long objectId, int revType) {
		String sql = String.format(GET_REV_INFO_SQL, auditTable, "and a.revtype = :revType");
		List<Object[]> rows = getCurrentSession().createSQLQuery(sql)
			.addScalar("rev", LongType.INSTANCE)
			.addScalar("revTime", TimestampType.INSTANCE)
			.addScalar("userId", LongType.INSTANCE)
			.addScalar("firstName", StringType.INSTANCE)
			.addScalar("lastName", StringType.INSTANCE)
			.addScalar("emailAddr", StringType.INSTANCE)
			.setParameter("objectId", objectId)
			.setParameter("revType", revType)
			.setMaxResults(1)
			.list();
		return CollectionUtils.isEmpty(rows) ? null : rows.iterator().next();
	}

	private RevisionDetail getRevisionInfo(Object[] row) {
		RevisionDetail detail = new RevisionDetail();
		if (row == null) {
			return detail;
		}

		int idx = 0;
		detail.setRevisionId((Long) row[idx++]);
		detail.setChangedOn((Date) row[idx++]);

		UserSummary user = new UserSummary();
		user.setId((Long)row[idx++]);
		user.setFirstName((String)row[idx++]);
		user.setLastName((String)row[idx++]);
		user.setEmailAddress((String)row[idx++]);
		detail.setChangedBy(user);
		return detail;
	}

	@SuppressWarnings("unchecked")
	private Integer getRevisionsCount(String auditTable, Long objectId) {
		List<Integer> result = getCurrentSession().createSQLQuery(String.format(GET_REV_COUNT_SQL, auditTable))
			.addScalar("revisions", IntegerType.INSTANCE)
			.setParameter("objectId", objectId)
			.list();

		return CollectionUtils.isEmpty(result) ? null : result.iterator().next();
	}

	private static final String FQN = UserApiCallLog.class.getName();

	private static final String GET_REV_INFO_SQL =
		"select " +
		"  r.rev as rev, r.revtstmp as revTime, r.user_id as userId, " +
		"  u.first_name as firstName, u.last_name as lastName, u.email_address as emailAddr " +
		"from " +
		"  os_revisions r " +
		"  inner join %s a on a.rev = r.rev " +
		"  inner join catissue_user u on u.identifier = r.user_id " +
		"where " +
		"  a.identifier = :objectId " +
		"  %s " +	// for additional constraints if any
		"order by " +
		"  r.revtstmp desc";

	private static final String GET_REV_COUNT_SQL = "select count(t.rev) as revisions from %s t where t.identifier = :objectId";

	private static final String GET_LATEST_API_CALL_TIME = FQN + ".getLatestApiCallTime";
}
