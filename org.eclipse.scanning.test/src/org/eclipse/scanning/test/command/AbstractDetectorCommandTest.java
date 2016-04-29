package org.eclipse.scanning.test.command;

import static org.junit.Assert.*;

import org.eclipse.scanning.api.device.models.IDetectorModel;
import org.eclipse.scanning.test.command.AbstractJythonTest;
import org.junit.Before;
import org.junit.Test;


/**
 * This class tests that the detector command(s) of
 * mapping_scan_commands.py are able to retrieve detector
 * models from the device service.
 *
 * This requires that an IRunnableDeviceService be instantiated,
 * which is left to the concrete subclasses.
 */
public abstract class AbstractDetectorCommandTest extends AbstractJythonTest {

	@Before
	public abstract void setUpRunnableDeviceService() throws Exception;

	@Test
	public void testGenericDetectorFunction() {
		pi.exec("name, model = detector('mandelbrot', 0.1)");
		String detectorName = pi.get("name", String.class);
		IDetectorModel dModel = pi.get("model", IDetectorModel.class);

		assertEquals("mandelbrot", detectorName);
		assertEquals(0.1, dModel.getExposureTime(), 1e-8);
	}
}
