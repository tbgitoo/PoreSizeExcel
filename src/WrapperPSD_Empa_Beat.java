import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.Window;

import excel_macro.tools.VersionIndicator;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import ij.process.AutoThresholder.Method;
import ij.text.TextPanel;
import ij.text.TextWindow;
import imageProcessing.tools.ImageProcessingTools;

// The idea of this plugin is to "wrap" the "Pore Size Distribution" plugin by 
// Beat Münch (EMPA, ftp://ftp.empa.ch/pub/empa/outgoing/BeatsRamsch/lib/, 
// xlib_.jar, inside the xlib_.jar, it's the xPore_Size_Distribution class in the default package
// Beat Münch's plugin evaluates the pore size distribution by fitting spheres
// into the pore space, and identifying each pixel with the greatest possible sphere; it is 
// meant to emulate mercury porosimetry in this way
// The problem with Beat Münch's plugin is that it has a bit unconventional output
// with which our MacroOnExcelFileList_ plugin cannot deal. The first output is a 
// cumulative distribution function in a graphical window that is not listed in ImageJ's WindowManager
// so it cannot be closed by a Macro, but needs to be closed separately. The second problem is that 
// rather than outputting to the Results table like most other plugins, Beat Münch's plugin writes a test in 
// a text window. 
// A problem of convenience is also that Beat Münch's plugin outputs the cumulative distribution function, 
// while our feretPore_ plugin outputs the probability density function. So here, we also evaluate
// the probability density function from the cumulative distribution function

public class WrapperPSD_Empa_Beat implements PlugInFilter,DialogListener {

	// The image associated with this plugin, provided by ImageJ
	protected ImagePlus imp;
	// The image processor associated with this plugin, provided by ImageJ
	protected ImageProcessor ip;


	public static double threshold=127;

	public static boolean useAutoThreshold = false;

	public static String[] outputTypes={"Histograms","Summary statistics only"};

	public static String chosenOutputType=outputTypes[0];

	public static boolean avoidEdgePores = true;
	
	public static boolean keep_pore_image_open = false;


	// To handle the input values
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {


		useAutoThreshold = gd.getNextBoolean();

		threshold = gd.getNextNumber();

		avoidEdgePores = gd.getNextBoolean();
		
		keep_pore_image_open = gd.getNextBoolean();

		chosenOutputType=gd.getNextChoice();

		return true;
	}


	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub

		this.imp=imp;

		return(NO_UNDO+NO_CHANGES+DOES_ALL);


	}

	public boolean doDialog()
	{
		GenericDialog gd = new GenericDialog("WrapperPSD_Empa_Beat Plugin");

		gd.addCheckbox("Use autothreshold", useAutoThreshold);

		gd.addMessage("If not using autothreshold, indicate treshold for distinguishing wall and pore");
		gd.addNumericField("Upper threshold for pore pixel value" , threshold, 1);

		gd.addCheckbox("Ignore pores on image boundaries", avoidEdgePores);
		
		gd.addCheckbox("Keep pore image open", keep_pore_image_open);

		gd.addChoice("Output", outputTypes, chosenOutputType);

		gd.addDialogListener(this);

		gd.showDialog(); 

		// input by the user (or macro) happens here

		return !(gd.wasCanceled());
	}



	public void run(ImageProcessor ip) {
		// TODO Auto-generated method stub


		this.ip = ip;

		if(!doDialog())
		{
			return;
		}

		// if autothresholding is requested, we need to use the
		// corresponding imageJ plugin to get the autothreshold values
		if(useAutoThreshold)
		{
			threshold=getAutoThreshold(ip);
		}

		// Run the xPore_Size_Distribution plugin
		run_xPore_Size_Distribution();

		// Close the cumulative distribution function windows that
		// the xPore_Size_Distribution generates
		close_xGeomView_Windows();


		ImagePlus poreImage = null;


		
		poreImage=WindowManager.getImage("Pore size image");

		Calibration theCal=imp.getCalibration();
			
			
				
		poreImage.setCalibration(theCal);
			
		if(avoidEdgePores)
		{
		
			trim_edge_pores(poreImage);
		
		}

		poreImage.updateAndDraw();



		



		// Get the text Panel with the main results
		TextPanel resultText = getTextPanel("PSD of "+imp.getTitle());

		// Get the standard resultsTable
		ResultsTable rt = Analyzer.getResultsTable();

		// Take the output information of the xPore_Size_Distribution
		// plugin, given in a non-standard way
		// and transfer to standard resultsTable
		if(avoidEdgePores)
		{
			// Get pore size histogram without boundary from pore image

			double[] histogram_pore_image = get_float_histogram(poreImage);

			Calibration cal=imp.getCalibration();
			
			


			// Get the actual volume
			for(int i=0; i<histogram_pore_image.length; i++)
			{
				histogram_pore_image[i]=histogram_pore_image[i]*cal.pixelWidth*cal.pixelHeight;
			}

			// Take the output information of the xPore_Size_Distribution
			// plugin, given in a non-standard way
			// and transfer to standard resultsTable
			transfer_from_textPanel_and_pore_image_to_resultsWindow(resultText, histogram_pore_image,rt);

		} else
		{
			transfer_from_textPanel_to_resultsWindow(resultText, rt);
		}

		// Make sure we have enough precision for small numbers
		rt.setPrecision(-4);
		// Show the results Table
		rt.show("Results");

		// Close the window of the graphical display of the
		// cumulative PSD created by the xPore_Size_Distribution  plugin
		getNonImageFrame("PSD of "+imp.getTitle()).dispose();

		
		if(!keep_pore_image_open)
		{
			poreImage.close();
		}
		

	}



	public void run_xPore_Size_Distribution()
	{

		Calibration cal=imp.getCalibration();



		Interpreter interp = new Interpreter();

		String macro = "run(\"Pore Size Distribution\", \"";

		if(avoidEdgePores || keep_pore_image_open)
		{
			macro=macro+"gray";
		}

		macro=macro+"  psd=[Continuous PSD] intrusion=[0 (x, at low y)] border=none starting=none ending=none operation=[all connections from starting to ending face] "+
				"xsize="+cal.pixelWidth+
				" ysize="+cal.pixelHeight+
				" zsize="+cal.pixelDepth+
				" pore=0:999 directory=~ lower=0 upper="+
				threshold+" roi_low=NaN roi_high=NaN grid=1 maximal=-1 height=0 processes=1\");";

		// ImageJ has a locking system and we need to free the image to have the macro run on it
		imp.unlock();
		interp.run(macro);
		imp.lock();

	}

	// Intended to close the cumulative distribution function window 
	//generated by the Pore Size Distribution plugin
	// This is special, since somehow this window is not listed under imageJ image windows

	public static void close_xGeomView_Windows()
	{

		// As this window is not available through imageJ
		// standard mechanism of image and non-image windows, 
		// we need to go a step deeper and use the type of the 
		// window
		Window[] ww=Window.getWindows();
		for(int ind =0; ind<ww.length; ind++)
		{
			Window w=ww[ind];
			if(w.getClass().getName().equals("xjava.display.xGeomView"))

			{
				w.dispose();

			}
		}

	}

	public static Frame getNonImageFrame(String theTitle)
	{
		// First, get the correct window among the nonImage windows
		Frame[] f = WindowManager.getNonImageWindows();
		String[] titles = WindowManager.getNonImageTitles();
		int resultIndex=-1;
		for(int ind=0; ind<titles.length; ind++)
		{
			if(titles[ind].equals(theTitle))
			{
				resultIndex=ind;


			}

		}
		if(resultIndex==-1)
		{
			return null;
		}

		return f[resultIndex];

	}


	public static TextPanel getTextPanel(String theTitle)
	{
		// Get the information from the text window and transfer to results
		Frame f = getNonImageFrame(theTitle);

		if(f==null)
		{
			return null;
		}

		return(((TextWindow)f).getTextPanel());

	}

	// Reads the numerical results section from the text table
	// This yields a matrix of double values
	// result[0][] is the pore radii
	// result[1][] is the number-weighted cumulative frequency as output by xlib
	// result[2][] is the volume-weighted cumulative frequency as output by xlib
	public static double[][] readNumericResults(TextPanel resultText)
	{

		// The numeric results start at line 17
		double[][] retVals = new double[3][resultText.getLineCount()-17+1];

		// Use line 17 to know the number of entries (should be 3 but if changes one day ...)
		int n=resultText.getLine(17).trim().split("\\s+").length;

		for(int ind=17; ind<resultText.getLineCount(); ind++)
		{


			String[] values = resultText.getLine(ind).trim().split("\\s+");

			for(int ind_strings=0; ind_strings<n; ind_strings++)
			{	
				retVals[ind_strings][ind-17]=Double.parseDouble(values[ind_strings]);		
			}



		}

		return retVals;
	}

	public static double[] diff(double [] x)
	{

		return diff(x,1);



	}
	// Diff, multiplication with a number to allow sign reversal
	public static double[] diff(double [] x, double m)
	{

		double[] r=new double[x.length];

		for(int ind=0; ind<x.length; ind++)
		{
			if(ind<x.length-1)
			{
				r[ind]=(x[ind+1]-x[ind])*m;
			} else
			{
				r[ind]=0;
			}

		}
		return r;



	}

	// specific function to do the information transfer from the 
	// xPore_Size_Distribution text window to a standard ResultsTable
	public static void transfer_from_textPanel_to_resultsWindow(TextPanel resultText, ResultsTable rt)
	{
		String[] headers=resultText.getLine(14).trim().split("\\s{2,}");

		double[][] valuesTextWindow = readNumericResults(resultText);

		// Also get the differentials, this is going to be the histogram since the
		// xlib plugin outputs only the cummulative function
		double[][] diffValuesTextWindow = new double[valuesTextWindow.length][valuesTextWindow[0].length];

		for(int ind=0; ind<diffValuesTextWindow.length; ind++)
		{
			diffValuesTextWindow[ind]=diff(valuesTextWindow[ind],-1);
		}


		// Write down the values in a new table
		if(chosenOutputType.equals(outputTypes[0]))
		{
			for(int rowIndex=0; rowIndex<valuesTextWindow[0].length-1; rowIndex++)
			{
				rt.incrementCounter();


				// Direct transfer of the actual values
				for(int colIndex=0; colIndex<valuesTextWindow.length; colIndex++)
				{

					String h=headers[colIndex];

					h=h.replaceAll("#vox", "Nbr vox");
					h=h.replaceAll("#\\sPore", "Pore");
					rt.addValue(h.replaceAll("#", "Nbr"),
							valuesTextWindow[colIndex][rowIndex]);
				}	
				// Difference between the vox and pore volume values to have the histogram from
				// the cumulative distribution function

				String [] additional_headers = new String[2];
				additional_headers[0]="# Vox = radius";
				additional_headers[1]="Pore volume = radius";

				for(int colIndex=1; colIndex<diffValuesTextWindow.length; colIndex++)
				{
					double delta = diffValuesTextWindow[colIndex][rowIndex];
					rt.addValue(additional_headers[colIndex-1].replaceAll("#", "Nbr"),
							delta);



				}


				// Threshold value
				rt.addValue("Threshold", threshold);

				rt.addValue("Version PoreSizeExcel_.jar", VersionIndicator.versionJar);


			}

			rt.setDecimalPlaces(0,0);
			rt.setDecimalPlaces(1, 0);
			rt.setDecimalPlaces(2, 7);
			rt.setDecimalPlaces(3, 0);
			rt.setDecimalPlaces(5, 0);



		} 
		// Summary statistics
		if(chosenOutputType.equals(outputTypes[1]))
		{
			rt.incrementCounter();


			// Mean pore size: Arithmetic mean of the intersections lengths; since 
			// small intersections are by definition more frequent if involving the
			// same number of pixels, this is "number" weighting
			rt.addValue("Mean pore diameter (Number weighted)", 2*ImageProcessingTools.mean(valuesTextWindow[0],diffValuesTextWindow[1]));

			// Mean pore size: Weighted mean, using the length of the
			// intersections as the weight. This is technically "length weighted"
			// but can also be considered "pixel weighted", as gives each intersection
			// the weight it has due to its numbers of pixels			
			rt.addValue("Mean pore diameter (Volume weighted)", 2*ImageProcessingTools.mean(valuesTextWindow[0],diffValuesTextWindow[2]));


			// Standard deviations, with the two weighting methods
			rt.addValue("Std pore diameter (Number weighted)",2*ImageProcessingTools.standard_deviation(valuesTextWindow[0],diffValuesTextWindow[1]));

			rt.addValue("Std pore diameter (Volume weighted)",2*ImageProcessingTools.standard_deviation(valuesTextWindow[0],diffValuesTextWindow[2]));

			rt.addValue("Threshold", threshold);

			rt.addValue("Version PoreSizeExcel_.jar", VersionIndicator.versionJar);

		}




	}


	public void transfer_from_textPanel_and_pore_image_to_resultsWindow(TextPanel resultText,
			double[] histogram_pore_image, ResultsTable rt) {

		String[] headers=resultText.getLine(14).trim().split("\\s{2,}");

		double[][] valuesTextWindow = readNumericResults(resultText);

		// Also get the differentials, this is going to be the histogram since the
		// xlib plugin outputs only the cummulative function
		double[][] diffValuesTextWindow = new double[valuesTextWindow.length][valuesTextWindow[0].length];

		for(int ind=0; ind<diffValuesTextWindow.length; ind++)
		{
			diffValuesTextWindow[ind]=diff(valuesTextWindow[ind],-1);
		}


		// Write down the values in a new table
		if(chosenOutputType.equals(outputTypes[0]))
		{
			for(int rowIndex=0; rowIndex<valuesTextWindow[0].length-1; rowIndex++)
			{
				rt.incrementCounter();


				// Direct transfer of the actual values
				for(int colIndex=0; colIndex<valuesTextWindow.length; colIndex++)
				{

					String h=headers[colIndex];

					h=h.replaceAll("#vox", "Nbr vox");
					h=h.replaceAll("#\\sPore", "Pore");
					rt.addValue(h.replaceAll("#", "Nbr"),
							valuesTextWindow[colIndex][rowIndex]);
				}	
				// Difference between the vox and pore volume values to have the histogram from
				// the cumulative distribution function

				String [] additional_headers = new String[2];
				additional_headers[0]="# Vox = radius";
				additional_headers[1]="Pore volume = radius";

				for(int colIndex=1; colIndex<diffValuesTextWindow.length; colIndex++)
				{
					double delta = diffValuesTextWindow[colIndex][rowIndex];
					rt.addValue(additional_headers[colIndex-1].replaceAll("#", "Nbr"),
							delta);



				}


				// Threshold value
				rt.addValue("Threshold", threshold);

				rt.addValue("Version PoreSizeExcel_.jar", VersionIndicator.versionJar);

				double theValue = 0;

				if(rowIndex<histogram_pore_image.length && rowIndex>0)
				{
					theValue= histogram_pore_image[rowIndex];
				} 

				rt.addValue("Pore volume = radius (boundary_removed)", theValue);





			}

			rt.setDecimalPlaces(0,0);
			rt.setDecimalPlaces(1, 0);
			rt.setDecimalPlaces(2, 7);
			rt.setDecimalPlaces(3, 0);
			rt.setDecimalPlaces(5, 0);



		} 
		// Summary statistics
		if(chosenOutputType.equals(outputTypes[1]))
		{
			rt.incrementCounter();


			// Mean pore size: Arithmetic mean of the intersections lengths; since 
			// small intersections are by definition more frequent if involving the
			// same number of pixels, this is "number" weighting
			rt.addValue("Mean pore diameter (Number weighted)", 2*ImageProcessingTools.mean(valuesTextWindow[0],diffValuesTextWindow[1]));

			// Mean pore size: Weighted mean, using the length of the
			// intersections as the weight. This is technically "length weighted"
			// but can also be considered "pixel weighted", as gives each intersection
			// the weight it has due to its numbers of pixels			
			rt.addValue("Mean pore diameter (Volume weighted)", 2*ImageProcessingTools.mean(valuesTextWindow[0],diffValuesTextWindow[2]));


			// Standard deviations, with the two weighting methods
			rt.addValue("Std pore diameter (Number weighted)",2*ImageProcessingTools.standard_deviation(valuesTextWindow[0],diffValuesTextWindow[1]));

			rt.addValue("Std pore diameter (Volume weighted)",2*ImageProcessingTools.standard_deviation(valuesTextWindow[0],diffValuesTextWindow[2]));

			rt.addValue("Threshold", threshold);

			rt.addValue("Version PoreSizeExcel_.jar", VersionIndicator.versionJar);

			histogram_pore_image[0]=0;

			double[] radii=new double[histogram_pore_image.length];

			for(int r=0; r<histogram_pore_image.length; r++)
			{
				radii[r]=r;
			}

			rt.addValue("Mean pore diameter (Volume weighted, no boundary pores)",2*ImageProcessingTools.mean(radii,histogram_pore_image));

			rt.addValue("Std pore diameter (Volume weighted, no boundary pores)",2*ImageProcessingTools.standard_deviation(radii,histogram_pore_image));


		}

	}

	// Calculates an autothreshold to distinguish background and foreground
		// The algorithm is as follows:
		// on the 80% least bright pixels, estimate a mean and standard deviation
		// on a log-scaled histogram
		// From this, evaluate the boundraries for a symmetric interval of confidence
		// of 99%
		// Compare this to 5% of the maximum intensity
		// Whatever number is higher, take as the threshold
		// The idea is that if there is a strong background, we want to 
		// avoid counting background pixels as foreground by mistake
		// but if the background is very clean, the main danger is to attribute pixels
		// to the foreground, even though their brightness mainly results from 
		// out-of-plane contribution due to the finite size of the 
		// confocal detection volume, particularly in z-direction

		public static int getAutoThreshold(ImageProcessor ip)
		{
			
			
			
			int[] histo_int=ip.getHistogram();
			
			
			double[] histo_double = new double[histo_int.length];
			
			for(int ind=0; ind<histo_double.length; ind++)
			{
				histo_double[ind] = histo_int[ind];
			}

			int cutoff_background=(int)Math.ceil(ImageProcessingTools.getQuantile(histo_double, 0.8));

			if(cutoff_background <0 )
			{
				cutoff_background=0;
			}
			if(cutoff_background > 255)
			{
				cutoff_background=255;
			}

			double[] bg_histo = new double[cutoff_background];

			System.arraycopy(histo_double, 0, bg_histo, 0, cutoff_background);

			double[] bg_histo_for_mean = new double[bg_histo.length];

			for(int ind=0; ind<bg_histo.length; ind++)
			{
				bg_histo_for_mean[ind]=bg_histo[ind]*((double) ind);
			}

			double bg_mean_guess = ImageProcessingTools.sum(bg_histo_for_mean);

			double[] bg_histo_for_sd = new double[bg_histo.length];

			for(int ind=0; ind<bg_histo.length; ind++)
			{
				bg_histo_for_sd[ind]=bg_histo[ind]*((double) ind-bg_mean_guess)*((double) ind-bg_mean_guess);
			}

			double bg_sd_guess = Math.sqrt(ImageProcessingTools.sum(bg_histo_for_sd));

			// For fitting, use 2x the mean
			
			int to_use = (int)Math.min((int)Math.round(bg_mean_guess*2), histo_double.length);
			
			bg_histo = new double[to_use];

			System.arraycopy(histo_double, 0, bg_histo, 0, to_use);
			
			
			

			double[] m_sd= ImageProcessingTools.gaussian_fit_histogram_log(bg_histo, bg_mean_guess, bg_sd_guess); 
			
			// 99% interval:
			// Fromt the normal distribution
			double quantile_99p_symmetric=2.575829;
			
			double upper = m_sd[0]+m_sd[1]*quantile_99p_symmetric;
			
			
			int limit_from_background=(int)Math.ceil(upper);
			
			// Reasonably, we should also be above 0.1*the highest intensity
			// This condition is to avoid taking out-of-plane pixels as foreground in the
			// presence of a very weak background
			
			int max=0;
			for(int ind=0; ind<histo_double.length; ind++)
			{
				if(histo_double[ind]>0)
				{
					max=ind;
				}
			}
			
			
			
			return Math.max((int)Math.ceil((double)max*0.1), limit_from_background);

			

		}

	public static double[] get_float_histogram(ImagePlus poreImage) {

		ImageProcessor ipro=poreImage.getProcessor();

		int max_val=(int)(ipro.getMax());

		double[] ret = new double[max_val+1];


		for(int x=0; x<ipro.getWidth(); x++)
		{
			for(int y=0; y<ipro.getHeight(); y++)
			{
				ret[(int)(ipro.getPixelValue(x, y))]++;
			}
		}






		return ret;
	}


	// The idea here is to set pixels to zero where the value is inferior to the distance
	// For now, this is done slice-wise
	public static void trim_edge_pores(ImagePlus poreImage)
	{
		Calibration cal=poreImage.getCalibration();


		ImageProcessor proc = poreImage.getProcessor();

		for(int x=0; x<proc.getWidth(); x++)
		{
			for(int y=0; y<proc.getHeight(); y++)
			{
				double distance = proc.getWidth()*cal.pixelWidth+proc.getHeight()*cal.pixelHeight;
				if(cal.pixelWidth*x<distance)
				{
					distance=cal.pixelWidth*x;
				}
				if(cal.pixelHeight*y<distance)
				{
					distance=cal.pixelHeight*y;
				}
				if(cal.pixelWidth*(proc.getWidth()-1-x)<distance)
				{
					distance=cal.pixelWidth*(proc.getWidth()-1-x);
				}
				if(cal.pixelHeight*(proc.getHeight()-1-y)<distance)
				{
					distance=cal.pixelHeight*(proc.getHeight()-1-y);
				}
				if(distance<=proc.getPixelValue(x, y))
				{
					proc.putPixel(x, y, 0);
				}

			}
		}





	}



}


