package com.krishagni.catissueplus.core.common.service;

import com.krishagni.catissueplus.core.common.domain.MessageLog;

public interface MessageHandler {
	String process(MessageLog log);
}
