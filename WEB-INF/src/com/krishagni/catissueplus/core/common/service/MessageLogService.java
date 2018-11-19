package com.krishagni.catissueplus.core.common.service;

public interface MessageLogService {
	void registerHandler(String extAppName, MessageHandler handler);

	void retryPendingMessages();
}
