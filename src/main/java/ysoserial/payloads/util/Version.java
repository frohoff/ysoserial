package ysoserial.payloads.util;

public class Version {
	
	private static Version singleton;
	
	public static Version getVersion() {
		if ( singleton == null ) {
			singleton = new Version();
		}
		
		return singleton;
	}
	
	public final double javaVersion;
	public final int build;
	
	public Version() { 
		String ver = System.getProperty( "java.version" );
		String[] components = ver.split( "_" );
		if ( components.length == 2 ) { 
			build = Integer.parseInt( components[1].replaceAll( "[^0-9]", "" ) );
		} else {
			build = 0;
		}
		
		javaVersion = Double.parseDouble( components[0].replace( "1.", "" ) );
	}
	
	public static boolean allowsDefaultAIH() { 
		Version version = Version.getVersion();
		return ( version.javaVersion < 7 ) || ( version.javaVersion == 7 && version.build < 72 ) || ( version.javaVersion == 8 && version.build < 71 );
	}

}
