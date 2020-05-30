package com.krishagni.catissueplus.core.auth.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface UserRequestDataProvider {
	void setup(HttpServletRequest httpReq, HttpServletResponse httpResp);

	void teardown(HttpServletRequest httpReq, HttpServletResponse httpResp);
}
