package imageProcessing.tools;

import org.orangepalantir.leastsquares.fitters.MarquardtFitter;

import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackProcessor;

public class ImageProcessingTools {





	public static double getQuantile(double [] hist, double p)
	{

		// Normalize (in case)
		// Also, calculate cumulative sums, with 1 element more than
		// the histogram to have 0 and 1 in it
		double total = 0;
		double[] cumsum = new double[hist.length+1];
		for(int ind=0; ind<hist.length; ind++)
		{
			total=total+hist[ind];
			cumsum[ind]=0;
		}


		for(int ind=0; ind<hist.length; ind++)
		{
			hist[ind]=hist[ind]/total;
			cumsum[ind+1]=cumsum[ind]+hist[ind];
		}
		// To be sure it's really exactly 1 and some close value due to
		// rounding errors
		cumsum[hist.length]=1;

		// limiting cases and non-treatable values
		if(p<=0) { return -1; }
		if(p>=1) { return hist.length; }

		// nominal case





		int current_ind=0;
		while(cumsum[current_ind]<p && current_ind<=hist.length)
		{

			current_ind++;
		}

		// Now we dispose of the element where the cumulative sum is just bigger than the
		// desired p value

		// Do interpolation to get a finer estimate

		double p_upper = cumsum[current_ind];
		double p_lower = cumsum[current_ind-1];

		double q_upper = current_ind;
		double q_lower = current_ind-1;

		// Degenerate case where the is no entry into the histogram here
		if(p_upper==p_lower)
		{
			int index_to_lower = current_ind-1;
			while(index_to_lower>0 && p_lower==p_upper)
			{
				index_to_lower--;
				p_lower = cumsum[index_to_lower];
				q_lower = index_to_lower;
			}

		}

		// This still hasn't helped, return the mean of the associated quantiles
		if(p_lower==p_upper)
		{
			return (q_lower+q_upper)/2;
		}

		double linear_inter_q = q_lower + (q_upper-q_lower)/(p_upper-p_lower)*(p-p_lower);

		return(linear_inter_q);




	}

	public static ImageStack getThresholdedStack(ImageStack theStack, int lower,int upper)
	{
		ImageStack mask = ImageProcessingTools.getEmptyByteStack(theStack);

		// Get a clone
		for(int z=1; z<=mask.getSize(); z++)
		{

			mask.getProcessor(z).copyBits(theStack.getProcessor(z).convertToByte(false), 0, 0, Blitter.COPY);
		}





		int[] lut = new int[256];

		for(int ind=0; ind<lut.length;ind++)
		{
			if(ind>=lower && ind<=upper)
			{
				lut[ind]=255;
			}
		}

		for(int z=1; z<=mask.getSize(); z++)
		{

			mask.getProcessor(z).applyTable(lut);
		}


		return mask;



	}

	public static int[] stackHistogramInt(ImageStack theStack, ImageStack theMask)
	{
		// Initialize
		int[] hist = new int[256];


		for(int ind=0; ind<hist.length; ind++)
		{
			hist[ind]=0;

		}



		// Analyze the pixels and get histogram
		for(int z=1; z<=theStack.getSize(); z++)
		{

			ImageProcessor sp=theStack.getProcessor(z);

			sp.setMask(theMask.getProcessor(z));

			int[] sliceHistogram=sp.getHistogram();

			for(int pixel_value=0; pixel_value<hist.length; pixel_value++)
			{
				hist[pixel_value]+=sliceHistogram[pixel_value];



			}


		}



		return hist;

	}

	public static double[] stackHistogram(ImageStack theStack, ImageStack theMask)
	{




		// Initialize
		long[] hist = new long[256];
		double[] Dhist = new double[256];

		for(int ind=0; ind<hist.length; ind++)
		{
			hist[ind]=0;
			Dhist[ind]=0;
		}

		double total=0;


		double total_slice=0;

		// Analyze the pixels and get histogram
		for(int z=1; z<=theStack.getSize(); z++)
		{

			ImageProcessor sp=theStack.getProcessor(z);

			sp.setMask(theMask.getProcessor(z));

			int[] sliceHistogram=sp.getHistogram();

			for(int pixel_value=0; pixel_value<hist.length; pixel_value++)
			{
				hist[pixel_value]+=sliceHistogram[pixel_value];
				total_slice++;


			}
			total=total+total_slice;

		}
		// Normalize


		for(int ind=0; ind<hist.length; ind++)
		{
			Dhist[ind]=((double)hist[ind])/total;
		}


		return Dhist;

	}

	// Helper function to get a new stack of the same dimensions as 
	// given stack for creating masks
	public static ImageStack getEmptyByteStack(ImageStack source)
	{
		ImageStack theStack = new ImageStack(source.getWidth(), 
				source.getHeight(), source.getSize());



		for(int ind=1; ind<=theStack.getSize(); ind++)
		{
			theStack.setProcessor(
					new ByteProcessor(theStack.getWidth(), 
							theStack.getHeight()), ind);
		}

		return(theStack);


	}

	public static ImageStack getClonedByteStack(ImageStack source)
	{
		ImageStack theStack=getEmptyByteStack(source);
		for(int z=1; z<=theStack.getSize(); z++)
		{
			theStack.getProcessor(z).copyBits(source.getProcessor(z).convertToByte(false), 0, 0, Blitter.COPY);
		}
		return theStack;
	}

	// Helper function to get a new stack of the same dimensions as 
	// given stack for creating masks
	public static ImageStack getEmptyFloatStack(ImageStack source)
	{
		ImageStack theStack = getEmptyByteStack(source);


		return(theStack.convertToFloat());


	}

	public static ImageStack getMask(double target, double tolerance, ImageStack source)
	{



		return getThresholdedStack(source, (int)Math.round(target-tolerance),(int)Math.round(target+tolerance));



	}
	
	
	
	
	
	
	/**
     * 3D filter using threads, rectangular volume
     *
     * @param out
     * @param radx Radius of  filter in x
     * @param rady Radius of  filter in y
     * @param radz Radius of  filter in z
      * @param filter: filter constants of the StackProcessor class
     */
    public static ImageStack filter3D(ImageStack stack, float radx, float rady, float radz, int filter) {
       
    	ImageStack out = stack.duplicate().convertToFloat();
    	
    	 StackProcessor sp = new StackProcessor(stack, stack.getProcessor(1));
    	 
    	 sp.filter3D(out, radx, rady, radz, 0, stack.getSize(), filter);
    	
        
        return(out);
    }

	public static ImageStack neighborhoodDeviationTransform(int u, int v, int w, ImageStack source)
	{

		

		
	   

	   
	

		
		
		ImageStack floatStack = source.duplicate().convertToFloat();
		
		
	    ImageStack maxStack = filter3D(floatStack, u, v, w, StackProcessor.FILTER_MAX);
	    
	    ImageStack minStack = filter3D(floatStack, u, v, w,  StackProcessor.FILTER_MIN);
	    
	    
	    StackProcessor sp = new StackProcessor(maxStack, maxStack.getProcessor(1));
	    
	    sp.copyBits(floatStack, 0, 0, Blitter.DIFFERENCE);
	    
	    sp = new StackProcessor(minStack, minStack.getProcessor(1));
	    
	    sp.copyBits(floatStack, 0, 0, Blitter.DIFFERENCE);
	    
	    sp.copyBits(maxStack, 0, 0, Blitter.MAX);

	    
	    return(minStack);
	 

}

public static ImageStack neighborhoodDeviationTransformRelative(int u, int v, int w, ImageStack source)
{
	return neighborhoodDeviationTransformRelative(u, v, w,source, 0);

}	

public static ImageStack neighborhoodDeviationTransformRelative(int u, int v, int w, ImageStack source,double background_value)
{
	
	ImageStack theStack=neighborhoodDeviationTransform(u,v,w,source);



	ImageStack originalDataCopy = getEmptyFloatStack(source);

	for(int z=1; z<=originalDataCopy.getSize(); z++)
	{
		originalDataCopy.getProcessor(z).copyBits(source.getProcessor(z).convertToFloat(), 0, 0, Blitter.COPY);

		

		if(background_value!=0)
		{
			originalDataCopy.getProcessor(z).add(-background_value);
		}

		theStack.getProcessor(z).copyBits(originalDataCopy.getProcessor(z), 0, 0, Blitter.DIVIDE);
		

	}

	return(theStack);

}

public static double getStackMean(ImageStack theStack, Calibration cal)
{

	double m=0;
	for(int z=1; z<=theStack.getSize(); z++)
	{
		ImageStatistics stats=ImageStatistics.getStatistics(
				theStack.getProcessor(z), ImageStatistics.MEAN, cal);

		m=m+stats.mean;

	}

	return (m/((double)theStack.getSize()));
}

public static double getStackSdMask(ImageStack theStack, ImageStack theMask)
{
	double[] hist=stackHistogram(theStack, theMask);
	double[] intensity=new double[hist.length];

	for(int ind=0; ind<hist.length; ind++)
	{
		intensity[ind]=ind;
	}

	return standard_deviation(intensity,hist);
}

public static double getStackMeanMask(ImageStack theStack, ImageStack theMask)
{

	double[] hist=stackHistogram(theStack, theMask);

	double total_hist=0;

	double total_intensity=0;

	for(int ind=0; ind<hist.length; ind++)
	{
		total_hist+=hist[ind];
		total_intensity+=(double)ind*hist[ind];
	}
	return(total_intensity/total_hist);

}

// Applies img2 to img1 according to the mode defined in the ij.Process.Blitter class
public static void divideStack(ImageStack stack1, ImageStack stack2)
{

	for(int z=1; z<=stack1.getSize(); z++)
	{
		
		stack1.getProcessor(z).copyBits(stack2.getProcessor(z), 0, 0, Blitter.DIVIDE);
		
	}

}


public static void normalize_stack_to_one(ImageStack theStack, boolean perSlide,Calibration cal)
{
	if(perSlide)
	{

		for(int z=1; z<=theStack.getSize(); z++)
		{
			ImageStatistics stats=ImageStatistics.getStatistics(
					theStack.getProcessor(z), ImageStatistics.MEAN, cal);

			theStack.getProcessor(z).multiply(1.0/stats.mean);

		}



	} else
	{
		double m=getStackMean(theStack,cal);
		for(int z=1; z<=theStack.getSize(); z++)
		{


			theStack.getProcessor(z).multiply(1.0/m);

		}

	}


}




public static ImageStack convertToGrey8(ImageStack stack1)
{
	ImageStack stack2=new ImageStack(stack1.getWidth(),stack1.getHeight());
	int nSlices=stack1.getSize();
	for(int i=1; i<=nSlices; i++) {
		String label = stack1.getSliceLabel(1);
		ImageProcessor ip = stack1.getProcessor(i);
		stack2.addSlice(label, ip.convertToByte(false));
		
	}
	return stack2;
}

public static double[] gaussian_fit_histogram_log(double[] histogram, double mean_guess, double sd_guess)
{

	// First of all, clean histogram from 0 values for which we cannot calculate a log
	int n_OK=0;
	for(int ind=0; ind<histogram.length; ind++)
	{
		if(histogram[ind]>0)
		{
			n_OK++;
		}
	}

	int[] indexes=new int[n_OK];
	double[] values=new double[n_OK];
	n_OK=0;
	for(int ind=0; ind<histogram.length; ind++)
	{
		if(histogram[ind]>0)
		{
			indexes[n_OK]=ind;
			values[n_OK]=Math.log(histogram[ind]);
			n_OK++;
		}
	}


	gaussianHistoLog theFun = new gaussianHistoLog();

	double[][] X = new double[values.length][1];



	for(int ind=0; ind<values.length; ind++)
	{
		X[ind][0]=indexes[ind];

	}

	double [] Z = values;

	for(int ind=0; ind<values.length; ind++)
	{
		Z[ind]=values[ind];

	}


	MarquardtFitter lft=new MarquardtFitter(theFun);
	lft.setData(X, Z);

	lft.setParameters(new double[]{mean_guess,Math.log(sd_guess),0});

	lft.fitData();

	double[] output = lft.getParameters();

	double[] final_output = new double[2];

	final_output[0]=output[0];
	final_output[1]=Math.exp(output[1]);





	return final_output;


}


public static double[] gaussian_fit_histogram(double[] histogram, double mean_guess, double sd_guess)
{
	gaussianHisto theFun = new gaussianHisto();





	double[][] X = new double[histogram.length][1];

	double sum=0;

	for(int ind=0; ind<histogram.length; ind++)
	{
		X[ind][0]=(double) ind;
		sum=sum+histogram[ind];
	}

	double [] Z = new double[histogram.length];

	for(int ind=0; ind<histogram.length; ind++)
	{
		Z[ind]=histogram[ind]/sum;

	}


	MarquardtFitter lft=new MarquardtFitter(theFun);
	lft.setData(X, Z);

	lft.setParameters(new double[]{mean_guess,sd_guess,1});

	lft.fitData();

	double[] output = lft.getParameters();

	double[] final_output = new double[2];

	final_output[0]=output[0];
	final_output[1]=output[1];





	return final_output;
}









// Helper function: weighted mean of the values in an array
// length of x and length of weights needs to be identical
public static double mean(double [] x, double [] weights)
{
	if(weights==null)
	{
		return mean(x);
	}
	double s = 0;
	double sum_weights=0;
	for(int ind=0; ind<x.length; ind++)
	{
		s = s + x[ind]*weights[ind];
		sum_weights = sum_weights+weights[ind];
	}
	if(sum_weights!=0)
	{
		return (s/sum_weights);
	}
	return 0;

}

// Helper function: Calculates the arithmetic mean of the values in the list
public static double  mean(double [] x)
{
	double s = 0;
	for(int ind=0; ind<x.length; ind++)
	{
		s = s + x[ind];
	}
	if(x.length>0)
	{
		return (s/(double)x.length);
	}
	return 0;

}

// Calculates the sum of the values in the list
public static double  sum(double [] x)
{
	double s = 0;
	for(int ind=0; ind<x.length; ind++)
	{
		s = s + x[ind];
	}
	if(x.length>0)
	{
		return (s);
	}
	return 0;

}


// Calculates a weighted standard deviation; the idea here is that if all the weights
// are equal to 1, this reduces to the standard N-1 formula
public static double standard_deviation(double [] x, double [] weights)
{

	if(weights==null)
	{
		return standard_deviation(x);
	}
	double s = 0;
	double sum_weights=0;

	double m = mean(x,weights);

	for(int ind=0; ind<x.length; ind++)
	{
		s = s + weights[ind]*(x[ind]-m)*(x[ind]-m);
		sum_weights = sum_weights+weights[ind];
	}
	if(x.length>1)
	{
		// If all the weights are 1, or in fact equal, 
		//  we should return the standard N-1 formula
		return Math.sqrt(s/(sum_weights*(double)(x.length-1)/(double)x.length));
	}
	return 0;

}

// Calculates the standard of the values in the list, uses
// the n-1 formula
public static double standard_deviation(double [] x)
{
	double s = 0;
	for(int ind=0; ind<x.length; ind++)
	{
		s = s + x[ind]*x[ind];
	}
	if(x.length>1)
	{
		double m=mean(x);
		return Math.sqrt((s-(double)x.length*m*m)/((double)x.length-1));
	}
	return 0;

}











}
