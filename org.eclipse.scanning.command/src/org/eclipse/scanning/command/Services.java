package org.eclipse.scanning.command;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.IEventService;


public class Services {
	// OSGI calls the setters here, and we call the getters from
	// mapping_scan_commands.py. Thereby Python can talk to
	// dynamically injected services.
	// In non-plugin tests we call the setters manually instead.

	private static IEventService eventService;
	private static IRunnableDeviceService runnableDeviceService;

	public static void setEventService(IEventService eventService) {
		Services.eventService = eventService;
	}

	public static void setRunnableDeviceService(
			IRunnableDeviceService runnableDeviceService) {
		Services.runnableDeviceService = runnableDeviceService;
	}

	public static IEventService getEventService() {
		return eventService;
	}

	public static IRunnableDeviceService getRunnableDeviceService() {
		return runnableDeviceService;
	}
}
