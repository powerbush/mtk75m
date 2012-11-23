package com.android.phone.tests;

import junit.framework.TestSuite;
import android.test.InstrumentationTestRunner;
import com.mediatek.android.performance.util.ServiceBindHelper;

public class CallTestRunner extends InstrumentationTestRunner {
	
	public TestSuite getAllTests() {
                ServiceBindHelper.setModuleName("Call_Module");
		TestSuite suite = new TestSuite();
		suite.addTestSuite(CallPerformanceTestCase.class);
                //suite.addTestSuite(InCallScreenTest2.class);
		return suite;
	}

}
