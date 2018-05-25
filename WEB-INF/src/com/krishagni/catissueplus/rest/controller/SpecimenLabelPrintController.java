package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.events.LabelPrintJobSummary;
import com.krishagni.catissueplus.core.biospecimen.events.PrintSpecimenLabelDetail;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.LabelTokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

@Controller
@RequestMapping("/specimen-label-printer")
public class SpecimenLabelPrintController {
	
	@Autowired
	private SpecimenService specimenSvc;

	@RequestMapping(method = RequestMethod.GET, value = "/tokens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<LabelTokenDetail> getPrintTokens() {
		ResponseEvent<List<LabelTokenDetail>> resp = specimenSvc.getPrintLabelTokens();
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabelPrintJobSummary printLabel(@RequestBody PrintSpecimenLabelDetail detail) {
		RequestEvent<PrintSpecimenLabelDetail> req = new RequestEvent<>(detail);
		ResponseEvent<LabelPrintJobSummary> resp = specimenSvc.printSpecimenLabels(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/output-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getLabelPrintFiles(
		@RequestParam(value = "jobId")
		Long jobId,

		@RequestParam(value = "filename", required = false, defaultValue = "")
		String filename,

		HttpServletResponse httpResp) {

		String printJobDir = ConfigUtil.getInstance().getDataDir() + File.separator + "print-jobs";
		String outputFilePath = String.format(
			"%s%s%d_%d.csv",
			printJobDir, File.separator, AuthUtil.getCurrentUser().getId(), jobId);
		File result = new File(outputFilePath);
		if (!result.exists()) {
			throw OpenSpecimenException.userError(CommonErrorCode.FILE_NOT_FOUND, result.getName());
		}

		if (StringUtils.isBlank(filename)) {
			filename = result.getName();
		}

		httpResp.setContentType("application/csv");
		httpResp.setHeader("Content-Disposition", "attachment; filename=" + filename);

		InputStream in = null;
		try {
			in = new FileInputStream(result);
			IOUtils.copy(in, httpResp.getOutputStream());
		} catch (IOException e) {
			throw OpenSpecimenException.userError(CommonErrorCode.FILE_SEND_ERROR, e.getMessage());
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}
