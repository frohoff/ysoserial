package ysoserial.payloads.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class GadgetsTest {

	@Test
	public void testMakeExecCommand() {
		String execCommand = Gadgets.makeExecCommand(new String[]{
				"cmd.exe", "/C", "copy NULL test.txt"});
		assertEquals("java.lang.Runtime.getRuntime().exec(new String[]{\"cmd.exe\", \"/C\", \"copy NULL test.txt\"});", execCommand);
	}

}
