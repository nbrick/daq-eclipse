package org.eclipse.scanning.test.command;

import org.eclipse.scanning.api.device.IRunnableDeviceService;
import org.eclipse.scanning.api.event.scan.DeviceInformation;
import org.eclipse.scanning.command.Services;
import org.eclipse.scanning.example.detector.MandelbrotDetector;
import org.eclipse.scanning.example.detector.MandelbrotModel;
import org.eclipse.scanning.sequencer.RunnableDeviceServiceImpl;
import org.eclipse.scanning.test.scan.mock.MockScannableConnector;


public class DetectorCommandTest extends AbstractDetectorCommandTest {

	@Override
	public void setUpRunnableDeviceService() throws Exception {
		IRunnableDeviceService dservice = new RunnableDeviceServiceImpl(new MockScannableConnector());
		MandelbrotDetector mandy = new MandelbrotDetector();
		final DeviceInformation<MandelbrotModel> info = new DeviceInformation<MandelbrotModel>(); // This comes from extension point or spring in the real world.
		info.setName("mandelbrot");
		mandy.setDeviceInformation(info);
		((RunnableDeviceServiceImpl)dservice)._register("mandelbrot", mandy);
		Services.setRunnableDeviceService(dservice);
	}

}
