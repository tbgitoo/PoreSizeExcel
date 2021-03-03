

import ij.IJ;
import ij.plugin.PlugIn;

public class PoreSizeExcel_Display_Depencies implements PlugIn {

	
	public void run(String arg) {
		IJ.showMessage("PoreSizeExcel","Installation dependencies\n \nPlace the following .jar file in the plugins folder\n"+
				"of your ImageJ or Fiji installation:\n"+
				"- \"xlib_.jar\": Plugin by Beat Münch, EMPA, Switzerland, available at\n"
				+ "    https://sites.imagej.net/Xlib/plugins/, basically the most recent xlib_.jar \n"
				+"     We use this plugin to do the actual maximal sphere fitting, but it offers other \n"
				+"     functions as well, check it out at >Plugins>Beat in your Fiji/ImageJ installation\n \n"
				+ "If not already present due to other plugins, \n"
				+ "place the following .jar files in the \"jars\" folder of your ImageJ or Fiji installation:\n"
				+ "- \"poi-3.17.jar\" or higher: Java-Excel interface, Apache library, from \n"+
				"      https://mvnrepository.com/artifact/org.apache.poi/poi\n"
				+ "- \"poi-ooxml-3.17.jar\" or higher: Java-Excel interface, Apache library, from \n"+
				"      https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml\n"
				+ "- \"poi-ooxml-schemas-3.17.jar\" or higher: Java-Excel interface, Apache library, from \n"
				+ "- \"jama-1.0.3.jar\": Generic Java math library, from https://math.nist.gov/javanumerics/jama/"
				);
		
	}

}

