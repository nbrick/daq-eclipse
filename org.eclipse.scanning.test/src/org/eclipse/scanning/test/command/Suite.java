package org.eclipse.scanning.test.command;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(org.junit.runners.Suite.class)
@SuiteClasses({
	// Only the first two tests have plugin equivalents.
	// (The others require no services.)
	SubmissionTest.class,
	DetectorCommandTest.class,
	ScanRequestCreationTest.class,
	PyExpresserTest.class
})
public class Suite { }
