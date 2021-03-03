

import ij.IJ;
import ij.plugin.PlugIn;

public class PoreSizeExcel_Credits implements PlugIn {

	
	public void run(String arg) {
		IJ.showMessage("PoreSizeExcel","Credits\n\n"+
				"Code by Thomas Braschler, Microniche lab, University of Geneva, Switzerland"+
				"\n \nThis plugin internally uses functionality of:\n"
				+ "- the xlib plugin by Beat Münch, EMPA, Switzerland for the maximal sphere algorithm"+
				"\n- the POI Java-Excel bridge for reading from Excel files "+
				"\n- the Jama Java Math library for various mathematical utility functions"+
				"\n- and the least-squares fitting library by Matthew B. Smith, available at "+
				"\n  http://orangepalantir.org for automatic background-foreground thresholding in "+
				"\n  autothresholding mode.\n\n\n"+
				"The plugin was tested by and debugged with the help of Fabien Bonini and Joé Brefie-Guth"
				);
		
	}

}


