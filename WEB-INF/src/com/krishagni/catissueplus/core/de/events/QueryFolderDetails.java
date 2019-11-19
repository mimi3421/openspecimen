package com.krishagni.catissueplus.core.de.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

public class QueryFolderDetails extends QueryFolderSummary {

	private List<UserSummary> sharedWith = new ArrayList<UserSummary>();

	private List<SavedQuerySummary> queries = new ArrayList<SavedQuerySummary>();

	public List<UserSummary> getSharedWith() {
		return sharedWith;
	}

	public void setSharedWith(List<UserSummary> sharedWith) {
		this.sharedWith = sharedWith;
	}

	public List<SavedQuerySummary> getQueries() {
		return queries;
	}

	public void setQueries(List<SavedQuerySummary> queries) {
		this.queries = queries;
	}

	public static QueryFolderDetails from(QueryFolder folder) {
		return from(folder, true);
	}

	public static QueryFolderDetails from(QueryFolder folder, boolean includeQueries) {
		QueryFolderDetails fd = new QueryFolderDetails();
		fd.setId(folder.getId());
		fd.setName(folder.getName());
		fd.setOwner(UserSummary.from(folder.getOwner()));
		fd.setSharedWith(fromUsers(folder.getSharedWith()));
		fd.setSharedWithAll(folder.isSharedWithAll());

		if (includeQueries) {
			fd.setQueries(fromSavedQueries(folder.getSavedQueries()));
		}
		return fd;
	}

	private static List<SavedQuerySummary> fromSavedQueries(Set<SavedQuery> savedQueries) {
		return savedQueries.stream().filter(q -> q.getDeletedOn() == null)
			.map(SavedQuerySummary::fromSavedQuery)
			.collect(Collectors.toList());
	}

	private static List<UserSummary> fromUsers(Set<User> users) {
		return users.stream().map(UserSummary::from).collect(Collectors.toList());
	}
}