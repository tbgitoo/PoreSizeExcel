import excel_macro.tools.VersionIndicator;
import ij.IJ;
import ij.plugin.PlugIn;

public class VersionDisplay implements PlugIn {

	
	public void run(String arg) {
		IJ.showMessage("PoreSizeExcel","PoreSizeExcel.jar version = "+VersionIndicator.versionJar);
		
	}

}
