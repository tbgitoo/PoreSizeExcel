package imageProcessing.tools;

import org.orangepalantir.leastsquares.Function;

public class gaussianHisto implements Function {
    
    
    public double evaluate(double[] values, double[] parameters) {
     // Get the variables
    	
    	double mean=parameters[0];
    	double sd=parameters[1];
    	double A=parameters[2];
    	
    	double x=values[0];
    	
    	
    	

    	return( A/sd/Math.sqrt(2.0*Math.PI)*Math.exp(-(x-mean)*(x-mean)/2/sd/sd) );
    	
    	
    }

    
    public int getNParameters() {
        return 3;
    }

    
    public int getNInputs() {
        return 1;
    }
}