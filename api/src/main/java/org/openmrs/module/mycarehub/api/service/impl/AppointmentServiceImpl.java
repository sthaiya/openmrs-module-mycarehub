package org.openmrs.module.mycarehub.api.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.appointmentscheduling.Appointment;
import org.openmrs.module.mycarehub.api.db.AppointmentDao;
import org.openmrs.module.mycarehub.api.service.AppointmentService;
import org.openmrs.module.mycarehub.api.service.MyCareHubSettingsService;
import org.openmrs.module.mycarehub.model.AppointmentRequests;
import org.openmrs.module.mycarehub.model.MyCareHubSetting;

import static org.openmrs.module.mycarehub.utils.Constants.MyCareHubSettingType.PATIENT_APPOINTMENTS_REQUESTS_GET;
import static org.openmrs.module.mycarehub.utils.Constants.MyCareHubSettingType.PATIENT_APPOINTMENTS_REQUESTS_POST;
import static org.openmrs.module.mycarehub.utils.MyCareHubUtil.fetchPatientAppointments;
import static org.openmrs.module.mycarehub.utils.MyCareHubUtil.getDefaultLocationMflCode;
import static org.openmrs.module.mycarehub.utils.MyCareHubUtil.getcccPatientIdentifierType;
import static org.openmrs.module.mycarehub.utils.MyCareHubUtil.uploadPatientAppointments;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.openmrs.module.mycarehub.utils.Constants.MyCareHubSettingType.PATIENT_APPOINTMENTS;

public class AppointmentServiceImpl extends BaseOpenmrsService implements AppointmentService {
	
	AppointmentDao dao;
	
	SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	
	public AppointmentServiceImpl(AppointmentDao dao) {
		this.dao = dao;
	}
	
	@Override
	public List<Appointment> getAppointmentsByLastSyncDate(Date lastSyncDate) {
		return dao.getAppointmentsByLastSyncDate(lastSyncDate);
	}
	
	@Override
	public List<AppointmentRequests> getAllAppointmentRequests() {
		return dao.getAllAppointmentRequests();
	}
	
	@Override
	public AppointmentRequests getAppointmentRequestByUuid(String uuid) {
		return dao.getAppointmentRequestByUuid(uuid);
	}
	
	@Override
	public List<AppointmentRequests> getAllAppointmentRequestsByLastSyncDate(Date lastSyncDate) {
		return dao.getAllAppointmentRequestsByLastSyncDate(lastSyncDate);
	}
	
	@Override
	public List<AppointmentRequests> saveAppointmentRequests(List<AppointmentRequests> appointmentRequests) {
		return dao.saveAppointmentRequests(appointmentRequests);
	}
	
	@Override
	public AppointmentRequests saveAppointmentRequests(AppointmentRequests appointmentRequest) {
		return dao.saveAppointmentRequests(appointmentRequest);
	}
	
	@Override
	public AppointmentRequests getAppointmentRequestByMycarehubId(String mycarehubId) {
		return dao.getAppointmentRequestByMycarehubId(mycarehubId);
	}
	
	@Override
	public Number countAppointments() {
		return dao.countAppointments();
	}
	
	@Override
	public List<AppointmentRequests> getPagedAppointments(Integer pageNumber, Integer pageSize) {
		return dao.getPagedAppointments(pageNumber, pageSize);
	}
	
	@Override
	public void syncPatientAppointments() {
		MyCareHubSettingsService settingsService = Context.getService(MyCareHubSettingsService.class);
		MyCareHubSetting setting = settingsService.getMyCareHubSettingByType(PATIENT_APPOINTMENTS);
		if (setting != null) {
			List<Appointment> appointments = dao.getAppointmentsByLastSyncDate(setting.getLastSyncTime());
			Date newSyncDate = new Date();
			
			JsonObject containerObject = new JsonObject();
			JsonArray appointmentsArray = new JsonArray();
			JsonObject appointmentObject = new JsonObject();
			PatientIdentifierType cccPatientIdentifierType = getcccPatientIdentifierType();
			if (appointments.size() > 0) {
				for (Appointment appointment : appointments) {
					java.util.Date date = new java.util.Date();
					SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
					SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
					
					appointmentObject = new JsonObject();
					appointmentObject.addProperty("appointment_uuid", appointment.getUuid());
					appointmentObject.addProperty("appointment_date",
					    dateFormat.format(appointment.getTimeSlot().getStartDate()));
					appointmentObject.addProperty(
					    "time_slot",
					    timeFormat.format(timeFormat.format(appointment.getTimeSlot().getStartDate()) + " "
					            + timeFormat.format(appointment.getTimeSlot().getEndDate())));
					appointmentObject.addProperty("appointment_type", appointment.getAppointmentType().getName());
					appointmentObject.addProperty("status", appointment.getStatus().getName());
					appointmentObject.addProperty("ccc_number",
					    appointment.getPatient().getPatientIdentifier(cccPatientIdentifierType).getIdentifier());
					appointmentsArray.add(appointmentObject);
				}
				
				containerObject.addProperty("MFLCODE", getDefaultLocationMflCode());
				containerObject.add("appointments", appointmentsArray);
				
				uploadPatientAppointments(containerObject, newSyncDate);
				
			} else {
				settingsService.createMyCareHubSetting(PATIENT_APPOINTMENTS, newSyncDate);
			}
		} else {
			settingsService.createMyCareHubSetting(PATIENT_APPOINTMENTS, new Date());
		}
	}
	
	@Override
	public void syncPatientAppointmentRequests() {
		MyCareHubSettingsService settingsService = Context.getService(MyCareHubSettingsService.class);
		MyCareHubSetting setting = settingsService.getMyCareHubSettingByType(PATIENT_APPOINTMENTS_REQUESTS_POST);
		if (setting != null) {
			List<AppointmentRequests> appointmentRequests = dao.getAllAppointmentRequestsByLastSyncDate(setting
			        .getLastSyncTime());
			Date newSyncDate = new Date();
			
			JsonObject containerObject = new JsonObject();
			JsonArray appointmentsObject = new JsonArray();
			JsonObject appointmentObject = new JsonObject();
			PatientIdentifierType cccPatientIdentifierType = getcccPatientIdentifierType();
			if (appointmentRequests.size() > 0) {
				for (AppointmentRequests appointmentRequest : appointmentRequests) {
					java.util.Date date = new java.util.Date();
					
					appointmentObject = new JsonObject();
					appointmentObject.addProperty("ID", appointmentRequest.getMycarehubId());
					appointmentObject.addProperty("status", appointmentRequest.getStatus());
					if (appointmentRequest.getProgressDate() != null) {
						appointmentObject.addProperty("InProgressAt",
						    dateTimeFormat.format(appointmentRequest.getProgressDate()));
					} else {
						appointmentObject.addProperty("InProgressAt", "null");
					}
					appointmentObject.addProperty("InProgressBy", appointmentRequest.getProgressBy());
					if (appointmentRequest.getDateResolved() != null) {
						appointmentObject.addProperty("ResolvedAt",
						    dateTimeFormat.format(appointmentRequest.getDateResolved()));
					} else {
						appointmentObject.addProperty("ResolvedAt", "null");
					}
					appointmentObject.addProperty("ResolvedBy", appointmentRequest.getResolvedBy());
					
					appointmentsObject.add(appointmentObject);
				}
				
				containerObject.add("appointment-request", appointmentsObject);
				
				uploadPatientAppointments(containerObject, newSyncDate);
				
			} else {
				settingsService.createMyCareHubSetting(PATIENT_APPOINTMENTS_REQUESTS_POST, newSyncDate);
			}
		} else {
			settingsService.createMyCareHubSetting(PATIENT_APPOINTMENTS_REQUESTS_POST, new Date());
		}
	}
	
	@Override
	public void fetchPatientAppointmentRequests() {
		MyCareHubSettingsService settingsService = Context.getService(MyCareHubSettingsService.class);
		MyCareHubSetting setting = settingsService.getMyCareHubSettingByType(PATIENT_APPOINTMENTS_REQUESTS_GET);
		if (setting != null) {
			Date newSyncDate = new Date();
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("MFLCODE", getDefaultLocationMflCode());
			jsonObject.addProperty("lastSyncTime", dateTimeFormat.format(setting.getLastSyncTime()));
			
			fetchPatientAppointments(jsonObject, newSyncDate);
		} else {
			settingsService.createMyCareHubSetting(PATIENT_APPOINTMENTS_REQUESTS_GET, new Date());
		}
	}
	
	public AppointmentDao getDao() {
		return dao;
	}
	
	public void setDao(AppointmentDao dao) {
		this.dao = dao;
	}
}
