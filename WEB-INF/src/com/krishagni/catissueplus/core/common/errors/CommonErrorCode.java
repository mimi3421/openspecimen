package com.krishagni.catissueplus.core.common.errors;

public enum CommonErrorCode implements ErrorCode {
	INVALID_INPUT,

	INVALID_REQUEST,

	SQL_EXCEPTION,

	FILE_NOT_FOUND,

	EXCEPTION_NOT_FOUND,

	CUSTOM_FIELD_LEVEL_REQ,

	CUSTOM_FIELD_NAME_REQ;

	@Override
	public String code() {
		return "COMMON_" + this.name();
	}
}
