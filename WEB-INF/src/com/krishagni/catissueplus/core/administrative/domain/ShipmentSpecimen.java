package com.krishagni.catissueplus.core.administrative.domain;

import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenShipmentReceivedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenShipmentShippedEvent;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

@Configurable
@Audited
public class ShipmentSpecimen extends BaseEntity {
	private Shipment shipment;
	
	private Specimen specimen;

	private ShipmentContainer shipmentContainer;
	
	private Shipment.ItemReceiveQuality receivedQuality;

	@Autowired
	private SpecimenService spmnSvc;

	public Shipment getShipment() {
		return shipment;
	}

	public void setShipment(Shipment shipment) {
		this.shipment = shipment;
	}

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public ShipmentContainer getShipmentContainer() {
		return shipmentContainer;
	}

	public void setShipmentContainer(ShipmentContainer shipmentContainer) {
		this.shipmentContainer = shipmentContainer;
	}

	public Shipment.ItemReceiveQuality getReceivedQuality() {
		return receivedQuality;
	}

	public void setReceivedQuality(Shipment.ItemReceiveQuality receivedQuality) {
		this.receivedQuality = receivedQuality;
	}

	public void ship() {
		Shipment shipment = getShipment();
		if (shipment.isSpecimenShipment()) {
			StorageContainerPosition position = new StorageContainerPosition();
			position.setContainer(shipment.getReceivingSite().getContainer());
			position.setOccupyingSpecimen(getSpecimen());
			getSpecimen().updatePosition(position, shipment.getShippedDate());
		}

		shipment.addOnSaveProc(() -> addShippedEvent(this));
	}
	
	public void receive(ShipmentSpecimen other) {
		setReceivedQuality(other.getReceivedQuality());
		updateSpecimen(other);
		SpecimenShipmentReceivedEvent.createForShipmentItem(this).saveRecordEntry();
	}

	public void receive(Shipment.ItemReceiveQuality receivedQuality) {
		setReceivedQuality(receivedQuality);
		SpecimenShipmentReceivedEvent.createForShipmentItem(this).saveRecordEntry();
	}

	public static ShipmentSpecimen createShipmentSpecimen(Shipment shipment, Specimen specimen) {
		ShipmentSpecimen shipmentSpmn = new ShipmentSpecimen();
		shipmentSpmn.setShipment(shipment);
		shipmentSpmn.setSpecimen(specimen);
		return shipmentSpmn;
	}

	private void addShippedEvent(ShipmentSpecimen item) {
		SpecimenShipmentShippedEvent.createForShipmentSpecimen(item).saveRecordEntry();
	}

	private void updateSpecimen(ShipmentSpecimen other) {
		if (!getShipment().isSpecimenShipment() || getReceivedQuality() != Shipment.ItemReceiveQuality.ACCEPTABLE) {
			return;
		}

		spmnSvc.updateSpecimen(getSpecimen(), other.getSpecimen());
	}
}
