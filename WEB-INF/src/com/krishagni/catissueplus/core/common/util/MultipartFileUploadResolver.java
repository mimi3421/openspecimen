package com.krishagni.catissueplus.core.common.util;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class MultipartFileUploadResolver extends CommonsMultipartResolver {

	protected CommonsMultipartFile createMultipartFile(FileItem fileItem) {
		String fileType = FilenameUtils.getExtension(fileItem.getName());
		if (StringUtils.isBlank(fileType)) {
			throw OpenSpecimenException.userError(
				CommonErrorCode.INVALID_INPUT,
				"File '" + fileItem.getName() + "' has not extension");
		}


		String allowedTypesStr = ConfigUtil.getInstance().getStrSetting("common", "allowed_file_types", "");
		for (String allowedType : allowedTypesStr.split(",")) {
			if (allowedType.trim().equalsIgnoreCase(fileType)) {
				return super.createMultipartFile(fileItem);

			}
		}

		throw OpenSpecimenException.userError(
			CommonErrorCode.INVALID_INPUT,
			"Uploading files of type '" + fileType + "' is prohibited");
	}
}
