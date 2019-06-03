package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.context.MessageSource;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.PositionAssigner;
import com.krishagni.catissueplus.core.administrative.domain.RowMajorPositionAssigner;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.services.ContainerMapExporter;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;

public class ContainerMapExporterImpl implements ContainerMapExporter {
	private MessageSource messageSource;
	
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	@PlusTransactional
	public File exportToFile(StorageContainer container) 
	throws IOException {
		File file = null;
		CsvWriter writer = null;
		
		try {
			file = File.createTempFile("container-", ".csv");
			file.deleteOnExit();
			
			writer = CsvFileWriter.createCsvFileWriter(file);
			
			exportContainerDetails(writer, container);
			exportOccupiedPositions(writer, container);
			writer.flush();
			
			return file;
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
		
	private void exportContainerDetails(CsvWriter writer, StorageContainer container) 
	throws IOException {
		writer.writeNext(new String[] { getMessage(CONTAINER_DETAILS) });
		writer.writeNext(new String[] { getMessage(CONTAINER_NAME), container.getName() });
		writer.writeNext(new String[] { getMessage(CONTAINER_HIERARCHY), container.getStringifiedAncestors() });
		
		writer.writeNext(new String[] { getMessage(CONTAINER_RESTRICTIONS) });
		
		List<String> cps = new ArrayList<>();
		cps.add(getMessage(CONTAINER_PROTOCOL));
		if (container.getCompAllowedCps().isEmpty()) {
			cps.add(getMessage(ALL));
		} else {
			for (CollectionProtocol cp : container.getCompAllowedCps()) {
				cps.add(cp.getTitle());
			}
		}
		writer.writeNext(cps.toArray(new String[0]));
		
		List<String> types = new ArrayList<>();
		types.add(getMessage(CONTAINER_SPECIMEN_TYPES));
		if (container.getCompAllowedSpecimenClasses().isEmpty() && 
			container.getCompAllowedSpecimenTypes().isEmpty()) {
			types.add(getMessage(ALL));
		} else {
			for (PermissibleValue specimenClass : container.getCompAllowedSpecimenClasses()) {
				types.add(specimenClass.getValue());
			}
			
			for (PermissibleValue type : container.getCompAllowedSpecimenTypes()) {
				types.add(type.getValue());
			}
		}
		writer.writeNext(types.toArray(new String[0]));
	}

	private void exportOccupiedPositions(CsvWriter writer, StorageContainer container) {
		PositionAssigner pa = container.getPositionAssigner();
		boolean rowMajor = pa instanceof RowMajorPositionAssigner;

		List<String> cells = new ArrayList<>();
		if (rowMajor) {
			cells.add("");
		}

		for (int i = 1; i <= container.getNoOfColumns(); ++i) {
			Pair<Integer, Integer> coord =  pa.fromMapIdx(container, 0, i - 1);

			if (rowMajor) {
				cells.add(container.toColumnLabelingScheme(coord.second()));
			} else {
				cells.add(container.toColumnLabelingScheme(coord.first()));
			}
		}

		if (!rowMajor) {
			cells.add("");
		}

		writer.writeNext(cells.toArray(new String[0]));

		Map<Integer, StorageContainerPosition> occupantsMap =
			container.getOccupiedPositions().stream().collect(
				Collectors.toMap(
					pos -> pa.toPosition(container, pos.getPosTwoOrdinal(), pos.getPosOneOrdinal()),
					pos -> pos
				)
			);

		for (int j = 1; j <= container.getNoOfRows(); ++j) {
			cells.clear();

			Pair<Integer, Integer> rowCoord = pa.fromMapIdx(container, j - 1, 0);
			String rowLabel;
			if (rowMajor) {
				rowLabel = container.toRowLabelingScheme(rowCoord.first());
				cells.add(rowLabel);
			} else {
				rowLabel = container.toRowLabelingScheme(rowCoord.second());
			}

			for (int i = 1; i <= container.getNoOfColumns(); ++i) {
				Pair<Integer, Integer> posCoord = pa.fromMapIdx(container, j - 1, i - 1);
				Integer pos = pa.toPosition(container, posCoord.first(), posCoord.second());
				StorageContainerPosition occupant = occupantsMap.get(pos);
				if (occupant != null) {
					if (occupant.getOccupyingContainer() != null) {
						cells.add(occupant.getOccupyingContainer().getName());
					} else if (occupant.getOccupyingSpecimen() != null) {
						cells.add(occupant.getOccupyingSpecimen().getLabel());
					} else if (occupant.isBlocked()) {
						cells.add(getMessage("storage_container_cell_blocked"));
					} else {
						cells.add("");
					}
				} else {
					cells.add("");
				}
			}

			if (!rowMajor) {
				cells.add(rowLabel);
			}

			writer.writeNext(cells.toArray(new String[0]));
		}
	}
		
	private String getMessage(String key) {
		return messageSource.getMessage(key, null, Locale.getDefault());
	}

	private static final String CONTAINER_DETAILS        = "storage_container_details";
	
	private static final String CONTAINER_NAME           = "storage_container_name";
	
	private static final String CONTAINER_HIERARCHY      = "storage_container_hierarchy";
	
	private static final String CONTAINER_RESTRICTIONS   = "storage_container_restrictions";
	
	private static final String CONTAINER_PROTOCOL       = "storage_container_restricted_protocols";
	
	private static final String CONTAINER_SPECIMEN_TYPES = "storage_container_specimen_types";
	
	private static final String ALL                      = "common_all";
}
