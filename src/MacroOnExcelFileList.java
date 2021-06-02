


import java.awt.AWTEvent;
import java.awt.Choice;
import java.awt.TextField;
import java.io.File;
import java.io.IOException;
import java.util.StringJoiner;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import excel_macro.tools.ExcelHolder;
import excel_macro.tools.PathHandler;
import excel_macro.tools.excelMacroTools;
import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;

public class MacroOnExcelFileList implements PlugIn, DialogListener {

	// Internal variable representing the Excel file 
	protected ExcelHolder holderToUse; 	


	public static String sheetName=null;

	// The name of the folder Column in the Excel file
	public static String folderColumnName="Folder";
	
	public static boolean useFolderColumn=false;

	

	// The name of the file column in the Excel file
	public static String fileColumnName="File";

	
	// Name of the column that contains the macro (or "" if the macro should be read from 
	// the dialog rather than the Excel file)
	public static String macroColumnInExcel="Macro";

	// Common root folder for construction of the full path; usually this will be changed by the
	// user

	public static String collaborator_for_config="Folder of Excel file";

	public static String root_folder=PathHandler.get_root_path_batch_report(collaborator_for_config);

	public static String excel_folder=null;

	public static boolean use_preconfigured = true;

	// The macro to apply
	public static String theMacro="";



	// 0-based row numbers 
	// Data starts from the second row
	public static int FIRST_DATA_ROW = 1;
	// And the column headers are in the first row
	public static int HEADER_ROW = 0;

	protected Workbook wb=null;

	protected Sheet sheet=null;
	
	
	public static boolean doCheckOnly = false;
	
	public static boolean doNotOpenPriorToMacro = false;


	protected void updateEnabled(GenericDialog gd)
	{

		Vector v=gd.getStringFields();
		TextField rootFolderTextField=(TextField)v.elementAt(0);
		rootFolderTextField.setEnabled(!use_preconfigured);

		Vector v2=gd.getChoices();
		Choice rootFolderChoiceField=(Choice)v2.elementAt(0);
		rootFolderChoiceField.setEnabled(use_preconfigured);

		if(use_preconfigured)
		{
			if(collaborator_for_config.equals("Folder of Excel file"))
			{
				root_folder=excel_folder;
			} else
			{
				root_folder=PathHandler.get_root_path_batch_report(collaborator_for_config);
			}
			if(!root_folder.equals(rootFolderTextField.getText()))
			{
				rootFolderTextField.setText(root_folder);
			}
		}

	}

	// For the options dialog, the user entered some new value, we should update the
	// corresponding plugin variables
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {


		if(gd.getName().equals("Worksheet_dialog"))
		{
			return dialogItemChangedWorksheet(gd,e);
		}

		use_preconfigured = gd.getNextBoolean();

		collaborator_for_config = gd.getNextChoice();

		root_folder = gd.getNextString();

		theMacro = gd.getNextText();

		macroColumnInExcel = gd.getNextChoice();

		if(macroColumnInExcel.equals("-"))
		{
			macroColumnInExcel="";
		}

		fileColumnName = gd.getNextChoice();

		folderColumnName = gd.getNextChoice();
		if(folderColumnName.equals("-"))
		{
			folderColumnName="";
			useFolderColumn=false;
		} else {
			useFolderColumn=true;
		}

		doCheckOnly = gd.getNextBoolean();
		
		doNotOpenPriorToMacro=gd.getNextBoolean();
		
		updateEnabled(gd);


		return true;
	}


	public boolean dialogItemChangedWorksheet(GenericDialog gd, AWTEvent e) {
		// TODO Auto-generated method stub
		sheetName=gd.getNextChoice();

		return true;
	}

	// Entry point called by ImageJ, this method runs the plugin
	public void run(String arg) 
	{


		// Ask the user for an Excel file and try to find a workbood and sheet
		if(!initializeExcelFile())
		{
			return;
		}

		// Show the options diaolog for the user to enter desired input values
		if(!doDialog())
		{
			return;
		}
		
		// check whether all the files can be found
		
		if(!checkExcelFile())
		{
			return;
		}


		// Now we have everything initialized, start evaluation 

		if(doCheckOnly)
		{
			IJ.showMessage("All files detected OK");
			return;
		}
		
		if(!processExcelFile())
		{
			return;
		}







	}


	public boolean initializeExcelFile()
	{

		
		// Get the path to the Excel file via a file open dialog
		String filePath = excelMacroTools.getExcelFilePath("");

		boolean foundFile=true;

		// See whether we can open the Excel file
		try {
			holderToUse = new ExcelHolder(filePath);
			holderToUse.readFileAndOpenWorkbook();
		} catch (IOException e) {
			IJ.handleException(e);
			foundFile=false;
		} catch (InvalidFormatException e) {
			IJ.handleException(e);
			foundFile=false;
		} catch (NullPointerException e)
		{
			foundFile=false;
		}

		if(!foundFile)
		{
			return false;
		}
		
		
		
		String[] fragments=filePath.split(Pattern.quote(File.separator));
		
		String joinCharacter = File.separator;
		if(!joinCharacter.equals("/"))
		{
			joinCharacter=File.separator.concat(File.separator);
		}
		
		StringJoiner sj = new StringJoiner(joinCharacter, "", "");
		for(int index=0; index<fragments.length-1; index++)
		{
			sj.add(fragments[index]);
		}
		 
		 excel_folder = sj.toString();

		// Here, we already have an Excel file (otherwise the code above, 
		// throwing the Exceptions, terminates
		// the plugin)

		// Get the work book in the Excel sheet
		wb = holderToUse.wb;

		// Get the list of sheets

		if(!doDialogSheets())
		{
			return false;
		}

		// Get sheet in workbook. This opens a dialog
		sheet = openSheetInWorkbook(wb);

		// Find columns labelled "File" and "Folder", which we absolutely need 
		// to run the plugin
		
		
		if(!findFileAndFolderColumn(sheet))
		{
			
			fileColumnName="File";
			folderColumnName="Folder";
			
			if(!findFileAndFolderColumn(sheet))
			{
				fileColumnName=null;
				folderColumnName=null;
		
			}
		}

		// Check whether there are actual data rows
		if(sheet.getLastRowNum()<FIRST_DATA_ROW)
		{
			IJ.error("No data rows found");
			return false;
		}


		return true;

	}

	// This is dry run, the plugin checks whether the files exist. If not, complain and save
	// the user the time of a plugin aborted midway
	public boolean checkExcelFile()
	{
		int fileColumn = getColumn(fileColumnName);
		int folderColumn = getColumn(folderColumnName);
		
		// Run through all the data rows	
		for(int ind=FIRST_DATA_ROW; ind<=sheet.getLastRowNum(); ind++)
		{
			
			Row theRow = sheet.getRow(ind);
			
			Cell theFile = theRow.getCell(fileColumn);
			
			Cell theFolder = null;
			if(folderColumn > -1 )
			{
				theFolder = theRow.getCell(folderColumn);
			}
			
			
			
			String filePath = "";
			
			if(!root_folder.equals(""))
			{
				filePath = filePath+root_folder+"/";
			}
			
			if(folderColumn > -1 )
			{
				if(!excelMacroTools.extractStringFromCell(theFolder).equals(""))
					filePath=filePath+excelMacroTools.extractStringFromCell(theFolder)+"/";
			}
			
			if(theFile == null)
			{
				IJ.showMessage("Empty file cell"+
			     "\nColumn "+(fileColumn+1)+", Row "+(ind+1)+" empty\n"+
				 "Incomplete line? Space characters? Delete this line? Lines below?");
			    return false;
			}
			
			filePath = filePath+theFile.getStringCellValue();
			
			File f = new File(filePath);
			if(!f.exists() ) { 
			    IJ.showMessage("Could not find file\n"+filePath);
			    return false;
			}
			if(f.isDirectory() ) { 
			    IJ.showMessage("The following is a directory, not a file:\n"+filePath);
			    return false;
			}
			
			


		}

		return true;

	}

	public int getColumn(String colName) {
		// TODO Auto-generated method stub
		return excelMacroTools.findColumnIndex(sheet.getRow(HEADER_ROW), colName);
	}

	// This function runs through the Excel file, and assembles and runs the macros for
	// each line
	public boolean processExcelFile()
	{
		
		int fileColumn = getColumn(fileColumnName);
		int folderColumn = -1;
		if(useFolderColumn)
		{
			folderColumn=getColumn(folderColumnName);
		}
		

		// We need a resutsTable to hold all the results
		ResultsTable allResults = new ResultsTable();

		// If appropriate, try to find the column for the macro
		int macroColumn=-1;

		Row headers = sheet.getRow(HEADER_ROW);

		if(macroColumnInExcel!=null && !(macroColumnInExcel.equals("")))
		{

			macroColumn = excelMacroTools.findColumnIndex(headers,macroColumnInExcel);
			if(macroColumn==-1)
			{
				IJ.error("Column \""+macroColumnInExcel+"\" not found in Excel File");
				return false;
			}
		}

		// Get the column headers in Excel, we will need them to construct the results table
		String[] excelHeadings= excelMacroTools.excelHeaders(sheet,HEADER_ROW);


		// Run through all the data rows	
		for(int ind=FIRST_DATA_ROW; ind<=sheet.getLastRowNum(); ind++)
		{
			// Progess indicator (for what it is worth in ImageJ, as this often gets
			// hidden by overlaying windows or plugin messages)
			IJ.showProgress(ind-FIRST_DATA_ROW, sheet.getLastRowNum()-FIRST_DATA_ROW);
			IJ.showStatus("MacroOnExcelFileList: Processing image "+(ind-FIRST_DATA_ROW+1)+" of "+(sheet.getLastRowNum()-FIRST_DATA_ROW+1));


			// Get the standard ResultsTable and reset it so that the macros can fill it
			Analyzer.getResultsTable().reset();

			// Get the current row and run the macros using it
			Row currentRow = sheet.getRow(ind);

			String returnMacro=runMacrosForExcelRowWithReplacement(currentRow, fileColumn, folderColumn, macroColumn,headers,!doNotOpenPriorToMacro);

			
			
			String[] additionalHeaders={"Actual_macro"};
			
			String[] additionalData={returnMacro};

			// Now the standard result table is filled, we need to assemble the information with 
			// the Excel information (general headers and current data row
			assembleResults(allResults, excelHeadings, currentRow,additionalHeaders,additionalData );

			// Assembly done, close the standard results window to keep clean
			if(ResultsTable.getResultsWindow()!=null)
			{
				ResultsTable.getResultsWindow().close(false);
			}

			// Make sure enough precision is available for 
			// small numbers
			allResults.setPrecision(-4);

			// show the updated final results table
			allResults.show("Results MacroOnExcelFileList");







		}


		return true;	

	}

	public boolean doDialogSheets()
	{

		String [] availableSheets = holderToUse.sheetNames();


		// If there is only single sheet, no need to show any choice
		if(availableSheets.length==1)
		{
			sheetName=availableSheets[0];
			return true;
		}

		GenericDialog gd = new GenericDialog("MacroOnExcelFileList Plugin - Worksheet");
		gd.addMessage("Select Sheet in the Excel file");

		gd.setName("Worksheet_dialog");



		sheetName=guessFileSheetName(holderToUse,sheetName,"Sheet1");

		gd.addChoice("Sheet", availableSheets, sheetName);

		gd.addDialogListener(this);

		// Show the dialog
		gd.showDialog(); 


		// Allow termination of the plugin when the user hits cancel
		return !(gd.wasCanceled());



	}




	// Show the dialog for the user options
	public boolean doDialog()
	{

		GenericDialog gd = new GenericDialog("MacroOnExcelFileList Plugin - Options");


		// Root folder (for finding the image files, the paths in the Excel
		// file are supposed to be relative to this root folder
		gd.addMessage("Root folder");

		gd.addCheckbox("Use preconfigured paths", use_preconfigured);
		
		String[] pathChoices = new String[PathHandler.collaborators.length+1];
		
		for(int ind=0; ind<PathHandler.collaborators.length; ind++)
		{
			pathChoices[ind]=PathHandler.collaborators[ind];
		}
		

		pathChoices[PathHandler.collaborators.length]="Folder of Excel file";
		
		gd.addChoice("Root folder configuration for", pathChoices, collaborator_for_config);

		gd.addStringField("Manual root folder", root_folder, 100);
		

		// Possibility to provide a macro here; if a macro column name is provided
		// below, this will be overridden
		gd.addMessage("Macro to run on each file");
		gd.addTextAreas(theMacro, null, 4, 108);




		// Override from the Excel file
		// This allows to have a different macro for every line and therefore image file
		gd.addMessage("Use Macro from Excel File? If yes, indicate column, otherwise leave empty (-)");



		String[] macroCols=potentialMacroColumns();

		String[] choices = new String[macroCols.length+1];

		choices[0]="-";

		for(int ind=1; ind<choices.length; ind++)
		{
			choices[ind]=macroCols[ind-1];
		}

		macroColumnInExcel=guessMacroColumnForDialog(macroColumnInExcel, choices);

		gd.addChoice("Macro column", choices, macroColumnInExcel);

		fileColumnName=guessColumnNameForDialog(fileColumnName,excelMacroTools.excelHeaders(sheet, HEADER_ROW),"File");
		folderColumnName=guessColumnNameForDialog(folderColumnName,excelMacroTools.excelHeaders(sheet, HEADER_ROW),"Folder");

		gd.addMessage("Column in the Excel File that contains the file to load");

		gd.addChoice("File column", excelMacroTools.excelHeaders(sheet, HEADER_ROW), fileColumnName);

		gd.addMessage("Column in the Excel File that contains the folder (relative to the root folder above) in which the file is located");
		gd.addMessage("If the image files are all in the root folder indicated above, choose empty (-).");

		String[] folderColsExcel=excelMacroTools.excelHeaders(sheet, HEADER_ROW);

		String[] folderColsChoice = new String[folderColsExcel.length+1];

		folderColsChoice[0]="-";

		for(int ind=1; ind<choices.length; ind++)
		{
			folderColsChoice[ind]=folderColsExcel[ind-1];
		}
		
		String preselected = folderColumnName;
		if(!useFolderColumn | folderColumnName.equals(""))
		{
			preselected="-";
		}
		
		gd.addChoice("Folder column", folderColsChoice, preselected);

		gd.addCheckbox("Check file presence only", doCheckOnly);
		
		gd.addCheckbox("Do not open file prior to Macro", doNotOpenPriorToMacro);
		
		updateEnabled(gd);

		// To change the variables when the user edits the dialog
		gd.addDialogListener(this);

		// Show the dialog
		gd.showDialog(); 


		// Allow termination of the plugin when the user hits cancel
		return !(gd.wasCanceled());

	}

	public static String guessColumnNameForDialog(String currentValue, String[] choices,String defaultValue)
	{
		// Try to find a match for an as long starting substring as possible

		if(currentValue==null || currentValue.equals(""))
		{
			return choices[0];
		}

		int l=currentValue.length();

		while(l>0)
		{
			String searchString = currentValue.substring(0, l);
			for(int ind=0; ind<choices.length; ind++)
			{
				if(choices[ind].length()>=searchString.length() )
				{
					if(choices[ind].subSequence(0, l).equals(searchString))
					{
						return choices[ind];
					}
				}
			}
			l--;
		}
		// column not found, try to find "Macro" or a fragment of it
		l=defaultValue.length();
		while(l>0)
		{
			String searchString = defaultValue.substring(0, l);
			for(int ind=0; ind<choices.length; ind++)
			{
				if(choices[ind].length()>=searchString.length() )
				{
					if(choices[ind].subSequence(0, l).equals(searchString))
					{
						return choices[ind];
					}
				}
			}
			l--;
		}

		return choices[0]; // if everything fails
	}

	public static String guessFileSheetName(ExcelHolder theHolder)
	{
		return guessFileSheetName(theHolder, null, "Sheet1");

	}

	public static String guessFileSheetName(ExcelHolder theHolder, String currentValue, String defaultValue)
	{

		String [] choices = theHolder.sheetNames(); 

		// If no current value is provided, we go through the sheets and look at the header rows
		// if we can find both a column starting with "File" and a column starting with "Folder", we
		// consider things to be a hit
		if(currentValue == null || currentValue.length()==0)
		{
			// try to match both file and folders
			for(int ind=0; ind<choices.length; ind++)
			{
				boolean matchFile=false;
				boolean matchFolder=false;
				Sheet currentSheet = theHolder.wb.getSheet(choices[ind]);
				String [] currentHeaders = excelMacroTools.excelHeaders(currentSheet, HEADER_ROW);

				for(int h=0; h<currentHeaders.length; h++)
				{
					if(currentHeaders[h].startsWith("File"))
					{
						matchFile=true;
						if(matchFolder)
						{
							return choices[ind];
						}
					}
					if(currentHeaders[h].startsWith("Folder"))
					{
						matchFolder=true;
						if(matchFile)
						{
							return choices[ind];
						}
					}

				}


			}

			// OK, finding both a file and folder column didn't work, try to find at least
			// one of them
			for(int ind=0; ind<choices.length; ind++)
			{
				Sheet currentSheet = theHolder.wb.getSheet(choices[ind]);
				String [] currentHeaders = excelMacroTools.excelHeaders(currentSheet, HEADER_ROW);

				for(int h=0; h<currentHeaders.length; h++)
				{
					if(currentHeaders[h].startsWith("File"))
					{

						return choices[ind];

					}
					if(currentHeaders[h].startsWith("Folder"))
					{

						return choices[ind];

					}

				}


			}
		}

		// Try to find a match by name if a current value is provided



		String fromCurrentValue = searchForFragment(currentValue, choices);

		if(fromCurrentValue != null)
		{
			return fromCurrentValue;
		}

		String fromDefaultValue = searchForFragment(defaultValue, choices);
		if(fromDefaultValue != null)
		{
			return fromDefaultValue;
		}

		return choices[0]; // if everything fails
	}


	public static String searchForFragment(String fragment, String[] choices)
	{
		// No choices offered
		if(choices==null || choices.length==0)
		{
			return null;
		}
		// The search fragment is empty
		if(fragment == null || fragment.length()==0)
		{
			return choices[0];
		}



		int l=fragment.length();

		while(l>0)
		{
			String searchString = fragment.substring(0, l);
			for(int ind=0; ind<choices.length; ind++)
			{
				if(choices[ind].length()>=searchString.length() )
				{
					if(choices[ind].subSequence(0, l).equals(searchString))
					{
						return choices[ind];
					}
				}
			}
			l--;
		}

		// OK, nothing found, return first element by default
		return choices[0];


	}



	public static String guessMacroColumnForDialog(String currentValue, String[] choices)
	{
		if(currentValue==null || currentValue.equals("") || currentValue.equals("-"))
		{
			return choices[0];
		}

		String guess_M= guessColumnNameForDialog(currentValue,choices,"Macro");
		
		String guess_m= guessColumnNameForDialog(currentValue,choices,"macro");
		
		if(guess_M.equals(guess_m))
		{
			return guess_M;
		} else
		{
			if(!guess_M.matches("Macro"))
			{
				return guess_m;
			}
		}
		
		return guess_M;
		



	}






	public String[] potentialMacroColumns()
	{
		// Make it simple for now: All columns available
		return excelMacroTools.excelHeaders(sheet, HEADER_ROW);
		/*
		int fileColumn = getColumn(fileColumnName);
		int folderColumn = getColumn(folderColumnName);
		
		
		String[] columns_available=excelMacroTools.excelHeaders(sheet, HEADER_ROW);

		String[] columns_eligible=new String[columns_available.length-2];

		int ind_eligible=0;

		for(int ind=0;ind<columns_available.length; ind++)
		{
			if(ind != fileColumn && ind != folderColumn)
			{
				columns_eligible[ind_eligible]=columns_available[ind];
				ind_eligible++;
			}
		}

		return columns_eligible;

*/


	}

	// Convenience function to find the indexes of several columns by their names
	// repeatedly applies findColumnIndex 




	public boolean findFileAndFolderColumn(Sheet sheet)
	{
		
		int[] folderFile = excelMacroTools.findFileAndFolderColumn(sheet, HEADER_ROW, fileColumnName,folderColumnName);

		

		int fileColumn = -1;

		int folderColumn = -1;

		if(folderFile != null)
		{
			folderColumn = folderFile[0];
			fileColumn = folderFile[1];
		}

		if(fileColumn==-1 || folderColumn==-1)
		{
			return false;
		}
		
		folderColumnName=getColumnName(folderColumn);
		fileColumnName=getColumnName(fileColumn);
		
		return true;

	}



	public String getColumnName(int col) {
		
		String[] theHeaders=excelMacroTools.excelHeaders(sheet, HEADER_ROW);
		if(col>=0 & col<theHeaders.length)
		{
			return theHeaders[col];
		
		}
		return null;
	}

	public Sheet openSheetInWorkbook(Workbook wb) {


		return excelMacroTools.openSheetInWorkbook(wb, sheetName);


	}


	// Helper function, runs the macros for a given row
	public static String runMacrosForExcelRow(Row theRow, int fileColumn, int folderColumn, int macroColumn)
	{
		return(runMacrosForExcelRow(theRow,fileColumn,folderColumn,macroColumn,true));
	}
	
	public static String runMacrosForExcelRow(Row theRow, int fileColumn, int folderColumn, int macroColumn,boolean doOpen)
	{
		return(runMacrosForExcelRowWithReplacement(theRow,fileColumn,folderColumn,macroColumn,null,doOpen));
	}


	// Helper function, runs the macros for a given row, possibility to pass default macro if no
	// macro is provided in the Excel sheet
	// Return: the core part of the macro
	public static String runMacrosForExcelRowWithReplacement(Row theRow, int fileColumn, int folderColumn, int macroColumn, Row headerRow)
	{

		 return runMacrosForExcelRowWithReplacement(theRow, fileColumn, folderColumn, macroColumn, headerRow,true);

		


	}
	
	public static String runMacrosForExcelRowWithReplacement(Row theRow, int fileColumn, int folderColumn, int macroColumn, Row headerRow,boolean doOpen)
	{

		 return excelMacroTools.runMacrosForExcelRowWithReplacement(theRow, fileColumn, folderColumn, macroColumn, headerRow, root_folder, theMacro,doOpen);

		


	}
	
	public static void assembleResults(ResultsTable newTable, String[] excelHeadings, Row currentRow )
	{
		assembleResults(newTable, excelHeadings, currentRow, null, null );
		
	}

	// Helper function:
	// Combine the current results table with the overall results table
	public static void assembleResults(ResultsTable newTable, String[] excelHeadings, Row currentRow, String[] additionalHeaders, String[] additionalData )
	{

		// Get a reference to the standard results table
		ResultsTable rt=Analyzer.getResultsTable();

		excelMacroTools.assembleResults(rt, newTable, excelHeadings, currentRow,additionalHeaders,additionalData);

	}




}


