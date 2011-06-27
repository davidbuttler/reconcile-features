/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    LogisticBase.java
 *    Copyright (C) 2003 Niels Landwehr
 *
 */

package reconcile.weka.classifiers.trees.lmt;

import reconcile.weka.classifiers.Classifier;
import reconcile.weka.classifiers.Evaluation;
import reconcile.weka.classifiers.functions.SimpleLinearRegression;
import reconcile.weka.core.Attribute;
import reconcile.weka.core.Instance;
import reconcile.weka.core.Instances;
import reconcile.weka.core.Utils;
import reconcile.weka.core.WeightedInstancesHandler;


/**
 * Base/helper class for building logistic regression models with the LogitBoost algorithm.
 * Used for building logistic model trees (weka.classifiers.trees.lmt.LMT)
 * and standalone logistic regression (weka.classifiers.functions.SimpleLogistic).
 *
 * @author Niels Landwehr
 * @version $Revision: 1.1 $
 */

public class LogisticBase extends Classifier implements WeightedInstancesHandler{

    /** Header-only version of the numeric version of the training data*/
    protected Instances m_numericDataHeader;
    /** 
     * Numeric version of the training data. Original class is replaced by a numeric pseudo-class.
     */
    protected Instances m_numericData;
    
    /** Training data */
    protected Instances m_train;
    
    /** Use cross-validation to determine best number of LogitBoost iterations ?*/
    protected boolean m_useCrossValidation;

    /**Use error on probabilities for stopping criterion of LogitBoost? */
    protected boolean m_errorOnProbabilities;

    /**Use fixed number of iterations for LogitBoost? (if negative, cross-validate number of iterations)*/
    protected int m_fixedNumIterations;
    
    /**Use heuristic to stop performing LogitBoost iterations earlier?
     * If enabled, LogitBoost is stopped if the current (local) minimum of the error on a test set as 
     * a function of the number of iterations has not changed for m_heuristicStop iterations.
     */
    protected int m_heuristicStop = 50;
 
    /**The number of LogitBoost iterations performed.*/
    protected int m_numRegressions = 0;
    
    /**The maximum number of LogitBoost iterations*/
    protected int m_maxIterations;
    
    /**The number of different classes*/
    protected int m_numClasses;

    /**Array holding the simple regression functions fit by LogitBoost*/
    protected SimpleLinearRegression[][] m_regressions;
        
    /**Number of folds for cross-validating number of LogitBoost iterations*/
    protected static int m_numFoldsBoosting = 5;

    /**Threshold on the Z-value for LogitBoost*/
    protected static final float Z_MAX = 3;

    /**
     * Constructor that creates LogisticBase object with standard options.
     */
    public LogisticBase(){
	m_fixedNumIterations = -1;
	m_useCrossValidation = true;
	m_errorOnProbabilities = false;	
	m_maxIterations = 500;
    }
    
    /**
     * Constructor to create LogisticBase object.
     * @param numBoostingIterations fixed number of iterations for LogitBoost (if negative, use cross-validation or
     * stopping criterion on the training data).
     * @param useCrossValidation cross-validate number of LogitBoost iterations (if false, use stopping 
     * criterion on the training data).
     * @param errorOnProbabilities if true, use error on probabilities 
     * instead of misclassification for stopping criterion of LogitBoost
     */
    public LogisticBase(int numBoostingIterations, boolean useCrossValidation, boolean errorOnProbabilities){
	m_fixedNumIterations = numBoostingIterations;
	m_useCrossValidation = useCrossValidation;
	m_errorOnProbabilities = errorOnProbabilities;	
	m_maxIterations = 500;
    }    

    /**
     * Builds the logistic regression model usiing LogitBoost.
     * 
     * @param data the training data
     */
    
    public void buildClassifier(Instances data) throws Exception{			

	m_train = new Instances(data);
	
	m_numClasses = m_train.numClasses();
	
	//init the array of simple regression functions
	m_regressions = initRegressions();
	m_numRegressions = 0;

	//get numeric version of the training data (class variable replaced  by numeric pseudo-class)
	m_numericData = getNumericData(m_train);	
	
	//save header info
	m_numericDataHeader = new Instances(m_numericData, 0);
	
	
	if (m_fixedNumIterations > 0) {
	    //run LogitBoost for fixed number of iterations
	    performBoosting(m_fixedNumIterations);
	} else if (m_useCrossValidation) {
	    //cross-validate number of LogitBoost iterations
	    performBoostingCV();
	} else {
	    //run LogitBoost with number of iterations that minimizes error on the training set
	    performBoosting();
	}	
	
	//only keep the simple regression functions that correspond to the selected number of LogitBoost iterations
	m_regressions = selectRegressions(m_regressions);	
    }   

    /**
     * Runs LogitBoost, determining the best number of iterations by cross-validation.
     */
    protected void performBoostingCV() throws Exception{			
	
	//completed iteration keeps track of the number of iterations that have been
	//performed in every fold (some might stop earlier than others). 
	//Best iteration is selected only from these.
	int completedIterations = m_maxIterations;
	
	Instances allData = new Instances(m_train);
	
	allData.stratify(m_numFoldsBoosting);	      

	float[] error = new float[m_maxIterations + 1];	
	
	for (int i = 0; i < m_numFoldsBoosting; i++) {
	    //split into training/test data in fold
	    Instances train = allData.trainCV(m_numFoldsBoosting,i);
	    Instances test = allData.testCV(m_numFoldsBoosting,i);

	    //initialize LogitBoost
	    m_numRegressions = 0;
	    m_regressions = initRegressions();

	    //run LogitBoost iterations
	    int iterations = performBoosting(train,test,error,completedIterations);	    
	    if (iterations < completedIterations) completedIterations = iterations;	    
	}
	
	//determine iteration with minimum error over the folds
	int bestIteration = getBestIteration(error,completedIterations);
	
	//rebuild model on all of the training data
	m_numRegressions = 0;
	performBoosting(bestIteration);
    }    

    /**
     * Runs LogitBoost on a training set and monitors the error on a test set.
     * Used for running one fold when cross-validating the number of LogitBoost iterations.
     * @param train the training set
     * @param test the test set
     * @param error array to hold the logged error values
     * @param maxIterations the maximum number of LogitBoost iterations to run
     * @return the number of completed LogitBoost iterations (can be smaller than maxIterations 
     * if the heuristic for early stopping is active or there is a problem while fitting the regressions 
     * in LogitBoost).
     * 
     */
    protected int performBoosting(Instances train, Instances test, 
				  float[] error, int maxIterations) throws Exception{
	
	//get numeric version of the (sub)set of training instances
	Instances numericTrain = getNumericData(train);		

	//initialize Ys/Fs/ps 
	double[][] trainYs = getYs(train);
	double[][] trainFs = getFs(numericTrain);		
	double[][] probs = getProbs(trainFs);	

	int iteration = 0;

	float[] testErrors = new float[maxIterations+1];
	
 	int noMin = 0;
	float lastMin = Float.MAX_VALUE;	
	
	if (m_errorOnProbabilities) error[0] += getMeanAbsoluteError(test);
	else error[0] += getErrorRate(test);
	
	while (iteration < maxIterations) {
	  
	    //perform single LogitBoost iteration
	    boolean foundAttribute = performIteration(iteration, trainYs, trainFs, probs, numericTrain);
	    if (foundAttribute) {
		iteration++;
		m_numRegressions = iteration;
	    } else {
		//could not fit simple linear regression: stop LogitBoost
		break;
	    }
	    
	    if (m_errorOnProbabilities) error[iteration] += getMeanAbsoluteError(test);
	    else error[iteration] += getErrorRate(test);
	  
	    //heuristic: stop LogitBoost if the current minimum has not changed for <m_heuristicStop> iterations
	    if (noMin > m_heuristicStop) break;
	    if (error[iteration] < lastMin) {
		lastMin = error[iteration];
		noMin = 0;
	    } else {
		noMin++;
	    }	    	      	    
	}

	return iteration;
    }

    /**
     * Runs LogitBoost with a fixed number of iterations.
     * @param numIterations the number of iterations to run
     */
    protected void performBoosting(int numIterations) throws Exception{

	//initialize Ys/Fs/ps 
	double[][] trainYs = getYs(m_train);
	double[][] trainFs = getFs(m_numericData);		
	double[][] probs = getProbs(trainFs);
	
	int iteration = 0;

	//run iterations
	while (iteration < numIterations) {
	    boolean foundAttribute = performIteration(iteration, trainYs, trainFs, probs, m_numericData);
	    if (foundAttribute) iteration++;
	    else break;
	}
	
	m_numRegressions = iteration;
    }
    
    /**
     * Runs LogitBoost using the stopping criterion on the training set.
     * The number of iterations is used that gives the lowest error on the training set, either misclassification
     * or error on probabilities (depending on the errorOnProbabilities option).
     */
    protected void performBoosting() throws Exception{
	
	//initialize Ys/Fs/ps
	double[][] trainYs = getYs(m_train);
	double[][] trainFs = getFs(m_numericData);		
	double[][] probs = getProbs(trainFs);	

	int iteration = 0;

	float[] trainErrors = new float[m_maxIterations+1];
	trainErrors[0] = getErrorRate(m_train);
	
	int noMin = 0;
	float lastMin = Float.MAX_VALUE;
	
	while (iteration < m_maxIterations) {
	    boolean foundAttribute = performIteration(iteration, trainYs, trainFs, probs, m_numericData);
	    if (foundAttribute) {
		iteration++;
		m_numRegressions = iteration;
	    } else {		
		//could not fit simple regression
		break;
	    }
	    
	    trainErrors[iteration] = getErrorRate(m_train);	    
	 
	    //heuristic: stop LogitBoost if the current minimum has not changed for <m_heuristicStop> iterations
	    if (noMin > m_heuristicStop) break;	    
	    if (trainErrors[iteration] < lastMin) {
		lastMin = trainErrors[iteration];
		noMin = 0;
	    } else {
		noMin++;
	    }
	}
	
	//find iteration with best error
        m_numRegressions = getBestIteration(trainErrors, iteration);	
    }

    /**
     * Returns the misclassification error of the current model on a set of instances.
     * @param data the set of instances
     * @return the error rate
     */
    protected float getErrorRate(Instances data) throws Exception {
	Evaluation eval = new Evaluation(data);
	eval.evaluateModel(this,data);
	return (float)eval.errorRate();
    }

    /**
     * Returns the error of the probability estimates for the current model on a set of instances.
     * @param data the set of instances
     * @return the error
     */
    protected float getMeanAbsoluteError(Instances data) throws Exception {
	Evaluation eval = new Evaluation(data);
	eval.evaluateModel(this,data);
	return (float)eval.meanAbsoluteError();
    }

    /**
     * Helper function to find the minimum in an array of error values.
     */
    protected int getBestIteration(float[] errors, int maxIteration) {
	float bestError = errors[0];
	int bestIteration = 0;
	for (int i = 1; i <= maxIteration; i++) {	    
	    if (errors[i] < bestError) {
		bestError = errors[i];
		bestIteration = i;		
	    }
	} 
	return bestIteration;
    }

    /**
     * Performs a single iteration of LogitBoost, and updates the model accordingly.
     * A simple regression function is fit to the response and added to the m_regressions array.
     * @param iteration the current iteration 
     * @param trainYs the y-values (see description of LogitBoost) for the model trained so far
     * @param trainFs the F-values (see description of LogitBoost) for the model trained so far
     * @param probs the p-values (see description of LogitBoost) for the model trained so far
     * @param trainNumeric numeric version of the training data
     * @return returns true if iteration performed successfully, false if no simple regression function 
     * could be fitted.
     */
    protected boolean performIteration(int iteration, 
        double[][] trainYs,
        double[][] trainFs,
        double[][] probs,
        Instances trainNumeric) throws Exception {
	
	for (int j = 0; j < m_numClasses; j++) {
	    
	    //make copy of data (need to save the weights) 
	    Instances boostData = new Instances(trainNumeric);
	    
	    for (int i = 0; i < trainNumeric.numInstances(); i++) {
		
		//compute response and weight
	    double p = probs[i][j];
		double actual = trainYs[i][j];
		double z = getZ(actual, p);
		double w = (actual - p) / z;
		
		//set values for instance 
		Instance current = boostData.instance(i);
		current.setValue(boostData.classIndex(), (float)z);
		current.setWeight((float)(current.weight() * w));				
	    }
	    
	    //fit simple regression function
	    m_regressions[j][iteration].buildClassifier(boostData);
	    
	    boolean foundAttribute = m_regressions[j][iteration].foundUsefulAttribute();
	    if (!foundAttribute) {
		//could not fit simple regression function
		return false;
	    }
	    
	}
	
	// Evaluate / increment trainFs from the classifier
	for (int i = 0; i < trainFs.length; i++) {
	    float [] pred = new float [m_numClasses];
	    float predSum = 0;
	    for (int j = 0; j < m_numClasses; j++) {
		pred[j] = (float)m_regressions[j][iteration]
		    .classifyInstance(trainNumeric.instance(i));
		predSum += pred[j];
	    }
	    predSum /= m_numClasses;
	    for (int j = 0; j < m_numClasses; j++) {
		trainFs[i][j] += (pred[j] - predSum) * (m_numClasses - 1) 
		    / m_numClasses;
	    }
	}
	
	// Compute the current probability estimates
	for (int i = 0; i < trainYs.length; i++) {
	    probs[i] = probs(trainFs[i]);
	}
	return true;
    }    

    /**
     * Helper function to initialize m_regressions.
     */
    protected SimpleLinearRegression[][] initRegressions(){
	SimpleLinearRegression[][] classifiers =   
	    new SimpleLinearRegression[m_numClasses][m_maxIterations];
	for (int j = 0; j < m_numClasses; j++) {
	    for (int i = 0; i < m_maxIterations; i++) {
		classifiers[j][i] = new SimpleLinearRegression();
		classifiers[j][i].setSuppressErrorMessage(true);
	    }
	}
	return classifiers;
    }

    /**
     * Converts training data to numeric version. The class variable is replaced by a pseudo-class 
     * used by LogitBoost.
     */
    protected Instances getNumericData(Instances data) throws Exception{
	Instances numericData = new Instances(data);
	
	int classIndex = numericData.classIndex();
	numericData.setClassIndex(-1);
	numericData.deleteAttributeAt(classIndex);
	numericData.insertAttributeAt(new Attribute("'pseudo class'"), classIndex);
	numericData.setClassIndex(classIndex);
	return numericData;
    }
    
    /**
     * Helper function for cutting back m_regressions to the set of classifiers (corresponsing to the number of 
     * LogitBoost iterations) that gave the smallest error.
     */
    protected SimpleLinearRegression[][] selectRegressions(SimpleLinearRegression[][] classifiers){
	SimpleLinearRegression[][] goodClassifiers = 
	    new SimpleLinearRegression[m_numClasses][m_numRegressions];
	
	for (int j = 0; j < m_numClasses; j++) {
	    for (int i = 0; i < m_numRegressions; i++) {
		goodClassifiers[j][i] = classifiers[j][i];
	    }
	}
	return goodClassifiers;
    }		
    
    /**
     * Computes the LogitBoost response variable from y/p values (actual/estimated class probabilities).
     */
    protected double getZ(double actual, double p) {
      double z;
	if (actual == 1) {
	    z = 1.0 / p;
	    if (z > Z_MAX) { // threshold
		z = Z_MAX;
	    }
	} else {
	    z = (float)-1.0 / (float)(1.0 - p);
	    if (z < -Z_MAX) { // threshold
		z = -Z_MAX;
	    }
	}
	return z;
    }
    
    /**
     * Computes the LogitBoost response for an array of y/p values (actual/estimated class probabilities).
     */
    protected double[][] getZs(double[][] probs, double[][] dataYs) {
	
	double[][] dataZs = new double[probs.length][m_numClasses];
	for (int j = 0; j < m_numClasses; j++) 
	    for (int i = 0; i < probs.length; i++) dataZs[i][j] = getZ(dataYs[i][j], probs[i][j]);
	return dataZs;
    }
    
    /**
     * Computes the LogitBoost weights from an array of y/p values (actual/estimated class probabilities).
     */
    protected double[][] getWs(double[][] probs, double[][] dataYs) {
	
	double[][] dataWs = new double[probs.length][m_numClasses];
	for (int j = 0; j < m_numClasses; j++) 
	    for (int i = 0; i < probs.length; i++){
	    double z = getZ(dataYs[i][j], probs[i][j]);
	    dataWs[i][j] = (dataYs[i][j] - probs[i][j]) / z;
	    }
	return dataWs;
    }

    /**
     * Computes the p-values (probabilities for the classes) from the F-values of the logistic model.
     */
    protected double[] probs(double[] Fs) {
	
	double maxF = -Double.MAX_VALUE;
	for (int i = 0; i < Fs.length; i++) {
	    if (Fs[i] > maxF) {
		maxF = Fs[i];
	    }
	}   
	double sum = 0;
	double[] probs = new double[Fs.length];
	for (int i = 0; i < Fs.length; i++) {
	    probs[i] = (float)Math.exp(Fs[i] - maxF);   
	    sum += probs[i];
	}
	
	Utils.normalize(probs, sum);
	return probs;
    }

    /**
     * Computes the Y-values (actual class probabilities) for a set of instances.
     */
    protected double[][] getYs(Instances data){
	
	double [][] dataYs = new double [data.numInstances()][m_numClasses];
	for (int j = 0; j < m_numClasses; j++) {
	    for (int k = 0; k < data.numInstances(); k++) {
		dataYs[k][j] = (data.instance(k).classValue() == j) ? 
		    (float)1.0: (float)0.0;
	    }
	}
	return dataYs;
    }

    /**
     * Computes the F-values for a single instance.
     */
    protected double[] getFs(Instance instance) throws Exception{
	
	double [] pred = new double [m_numClasses];
	double [] instanceFs = new double [m_numClasses]; 
	
	//add up the predictions from the simple regression functions
	for (int i = 0; i < m_numRegressions; i++) {
	    float predSum = 0;
	    for (int j = 0; j < m_numClasses; j++) {
		pred[j] = (float)m_regressions[j][i].classifyInstance(instance);
		predSum += pred[j];
	    }
	    predSum /= m_numClasses;
	    for (int j = 0; j < m_numClasses; j++) {
		instanceFs[j] += (pred[j] - predSum) * (m_numClasses - 1) 
		    / m_numClasses;
	    }
	}	
	
	return instanceFs; 
    } 
    
    /**
     * Computes the F-values for a set of instances.
     */
    protected double[][] getFs(Instances data) throws Exception{
	
	double[][] dataFs = new double[data.numInstances()][];
       
	for (int k = 0; k < data.numInstances(); k++) {
	    dataFs[k] = getFs(data.instance(k));
	}
	
	return dataFs;	
    }   

    /**
     * Computes the p-values (probabilities for the different classes) from the F-values for a set of instances.
     */
    protected double[][] getProbs(double[][] dataFs){
	
	int numInstances = dataFs.length;
	double[][] probs = new double[numInstances][];
	
	for (int k = 0; k < numInstances; k++) {       
	    probs[k] = probs(dataFs[k]);
	}
	return probs;
    }
    
    /**
     * Returns the likelihood of the Y-values (actual class probabilities) given the 
     * p-values (current probability estimates).
     */
    protected float logLikelihood(float[][] dataYs, float[][] probs) {
	
	float logLikelihood = 0;
	for (int i = 0; i < dataYs.length; i++) {
	    for (int j = 0; j < m_numClasses; j++) {
		if (dataYs[i][j] == 1.0) {
		    logLikelihood -= Math.log(probs[i][j]);
		}
	    }
	}
	return logLikelihood / (float)dataYs.length;
    }

    /**
     * Returns an array of the indices of the attributes used in the logistic model.
     * The first dimension is the class, the second dimension holds a list of attribute indices.
     * Attribute indices start at zero.
     * @return the array of attribute indices
     */
    public int[][] getUsedAttributes(){
	
	int[][] usedAttributes = new int[m_numClasses][];
	
	//first extract coefficients
	double[][] coefficients = getCoefficients();
	
	for (int j = 0; j < m_numClasses; j++){
	    
	    //boolean array indicating if attribute used
	    boolean[] attributes = new boolean[m_numericDataHeader.numAttributes()];
	    for (int i = 0; i < attributes.length; i++) {
		//attribute used if coefficient > 0
		if (!Utils.eq(coefficients[j][i + 1],0)) attributes[i] = true;
	    }
	    	    
	    int numAttributes = 0;
	    for (int i = 0; i < m_numericDataHeader.numAttributes(); i++) if (attributes[i]) numAttributes++;
	    
	    //"collect" all attributes into array of indices
	    int[] usedAttributesClass = new int[numAttributes];
	    int count = 0;
	    for (int i = 0; i < m_numericDataHeader.numAttributes(); i++) {
		if (attributes[i]) {
		usedAttributesClass[count] = i;
		count++;
		} 
	    }
	    
	    usedAttributes[j] = usedAttributesClass;
	}
	
	return usedAttributes;
    }

    /**
     * The number of LogitBoost iterations performed (= the number of simple regression functions fit).
     */
    public int getNumRegressions() {
	return m_numRegressions;
    }

    /**
     * Sets the parameter "maxIterations".
     */
    public void setMaxIterations(int maxIterations) {
	m_maxIterations = maxIterations;
    }
    
    /**
     * Sets the option "heuristicStop".
     */
    public void setHeuristicStop(int heuristicStop){
	m_heuristicStop = heuristicStop;
    }

    /**
     * Returns the maxIterations parameter.
     */
    public int getMaxIterations(){
	return m_maxIterations;
    }
        
    /**
     * Returns an array holding the coefficients of the logistic model.
     * First dimension is the class, the second one holds a list of coefficients.
     * At position zero, the constant term of the model is stored, then, the coefficients for
     * the attributes in ascending order.
     * @return the array of coefficients
     */
    protected double[][] getCoefficients(){
	double[][] coefficients = new double[m_numClasses][m_numericDataHeader.numAttributes() + 1];
	for (int j = 0; j < m_numClasses; j++) {
	    //go through simple regression functions and add their coefficient to the coefficient of
	    //the attribute they are built on.
	    for (int i = 0; i < m_numRegressions; i++) {
		
		double slope = m_regressions[j][i].getSlope();
		double intercept = m_regressions[j][i].getIntercept();
		int attribute = m_regressions[j][i].getAttributeIndex();
		
		coefficients[j][0] += intercept;
		coefficients[j][attribute + 1] += slope;
	    }
	}
	return coefficients;
    }

    /**
     * Returns the fraction of all attributes in the data that are used in the logistic model (in percent).
     * An attribute is used in the model if it is used in any of the models for the different classes.
     */
    public float percentAttributesUsed(){	
	boolean[] attributes = new boolean[m_numericDataHeader.numAttributes()];
	
	double[][] coefficients = getCoefficients();
	for (int j = 0; j < m_numClasses; j++){
	    for (int i = 1; i < m_numericDataHeader.numAttributes() + 1; i++) {
		//attribute used if it is used in any class, note coefficients are shifted by one (because
		//of constant term).
		if (!Utils.eq(coefficients[j][i],0)) attributes[i - 1] = true;
	    }
	}
	
	//count number of used attributes (without the class attribute)
	float count = 0;
	for (int i = 0; i < attributes.length; i++) if (attributes[i]) count++;
	return count / (float)((m_numericDataHeader.numAttributes() - 1) * 100.0);
    }
    
    /**
     * Returns a description of the logistic model (i.e., attributes and coefficients).
     */
    public String toString(){
	
	StringBuffer s = new StringBuffer();	

	//get used attributes
	int[][] attributes = getUsedAttributes();
	
	//get coefficients
	double[][] coefficients = getCoefficients();
	
	for (int j = 0; j < m_numClasses; j++) {
	    s.append("\nClass "+j+" :\n");
	    //constant term
	    s.append(Utils.doubleToString(coefficients[j][0],4,2)+" + \n");
	    for (int i = 0; i < attributes[j].length; i++) {		
		//attribute/coefficient pairs
		s.append("["+m_numericDataHeader.attribute(attributes[j][i]).name()+"]");
		s.append(" * " + Utils.doubleToString(coefficients[j][attributes[j][i]+1],4,2));
		if (i != attributes[j].length - 1) s.append(" +");
		s.append("\n");	    
	    }
	}	
	return new String(s);
    }

    /** 
     * Returns class probabilities for an instance.
     *
     * @exception Exception if distribution can't be computed successfully
     */
    public double[] distributionForInstance(Instance instance) throws Exception {
	
	instance = (Instance)instance.copy();	

	//set to numeric pseudo-class
      	instance.setDataset(m_numericDataHeader);		
	
	//calculate probs via Fs
	return probs(getFs(instance));
    }

    /**
     * Cleanup in order to save memory.
     */
    public void cleanup() {
	//save just header info
	m_train = new Instances(m_train,0);
	m_numericData = null;	
    }
}








