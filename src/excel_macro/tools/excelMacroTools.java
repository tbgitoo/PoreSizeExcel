package excel_macro.tools;




import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import ij.IJ;
import ij.io.OpenDialog;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;


public class excelMacroTools {




	// 0-based row numbers 
	// Data starts from the second row
	protected static int FIRST_DATA_ROW = 1;
	// And the column headers are in the first row
	protected static int HEADER_ROW = 0;

	// Via a file selection dialog, get the path to the Excel file to read
	public static String getExcelFilePath(String defaultPath)
	{
		OpenDialog  od = new OpenDialog("Select xlsx to read file list", defaultPath);    //file dialog
		String fileName = od.getFileName();
		if (fileName == null) return null;
		String fileDir = od.getDirectory();
		String path = fileDir + fileName;
		return(path);
	}





	// Get the header row in the Excel file and convert to an array of Strings
	public static String[] excelHeaders(Sheet sheet, int header_row)
	{
		// Get the header row
		Row headers = sheet.getRow(header_row);

		if(headers==null)
		{
			return new String[0];
		}

		// number of cells available, defines the length of the list
		int n_headers=headers.getLastCellNum();

		if(n_headers<0)
		{
			return new String[0];
		}

		// Initialize list
		String[] stringHeaders=new String[n_headers];


		// Go through the headers
		for(int ind=0; ind<n_headers; ind++)
		{

			// Get each cell
			Cell theCell=headers.getCell(ind);

			// Get the headers as strings
			stringHeaders[ind]=extractStringFromCell(theCell);


		}
		// return the result
		return stringHeaders;


	}
	// The idea here is that if the columns exist, then its index will be returned as the
	// result, whereas if they do not, -1 is returned

	public static int findColumnIndex(Row headers, String col)
	{
		int final_index=-1;
		int n_headers=headers.getLastCellNum();
		for(int indHeaders=0; indHeaders<n_headers; indHeaders++)
		{
			Cell theCell=headers.getCell(indHeaders);
			if(theCell.getCellTypeEnum()==CellType.STRING)
			{

				if(col.equals(theCell.getStringCellValue()) && !(col.equals("")))
				{
					final_index=indHeaders;
				}


			}

		}

		return final_index;

	}

	// Convenience function to find the indexes of several columns by their names
	// repeatedly applies findColumnIndex 

	public static int[] findColumnIndexes(Row headers, String[] cols)
	{



		int[] colIndexes = new int[cols.length];

		for(int indCols=0; indCols<cols.length; indCols++)
		{
			colIndexes[indCols]=findColumnIndex(headers,cols[indCols]);

		}





		return colIndexes;  


	}


	public static int[] findFileAndFolderColumn(Sheet sheet, int header_row)
	{
		return findFileAndFolderColumn(sheet, header_row, "File", "Folder");

	}

	public static int[] findFileAndFolderColumn(Sheet sheet, int header_row, String fileColumName, String folderColumnName)
	{
		// Get the first data row (or create it, if it doesn't exist). We'll use this to determine what column
		// index to start writing our data to this time.
		Row headers = sheet.getRow(header_row);

		int fileColumn = findColumnIndex(headers,fileColumName);

		int folderColumn = findColumnIndex(headers,folderColumnName);

		if(fileColumn==-1 || folderColumn==-1)
		{
			return null;
		}
		return new int[]{folderColumn,fileColumn};

	}

	public static Sheet openSheetInWorkbook(Workbook wb, int sheetNumber) {
		if (wb.getNumberOfSheets() == 0) {
			// If there are no sheets, we can't read one
			return null;
			// There are sheets, check whether its in range  
		} else if (sheetNumber < 0 || sheetNumber >= wb.getNumberOfSheets()) {
			// sheetNumber out of range, select first sheet
			sheetNumber=0; 
		}

		return wb.getSheetAt(sheetNumber);

	}

	public static Sheet openSheetInWorkbook(Workbook wb, String sheetName) {
		if (wb.getNumberOfSheets() == 0) {
			// If there are no sheets, we can't read one
			return null;
			// There are sheets, check whether its in range  
		} 

		return wb.getSheet(sheetName);

	}

	public static String extractStringFromCell(Cell theCell)
	{

		if(theCell==null)
		{
			return "";
		}
		String s="";
		switch(theCell.getCellTypeEnum())
		{
		case FORMULA:
			switch(theCell.getCachedFormulaResultTypeEnum())
			{
			case BLANK:
				break;
			case BOOLEAN:
				if(theCell.getBooleanCellValue()) {s="1";} else {s="0"; }
				break;
			case ERROR:
				break;
			case NUMERIC:
				s=""+theCell.getNumericCellValue();
				break;
			case STRING:
				s=theCell.getStringCellValue();
				break;
			case _NONE:
				break;
			default:
				break;

			}
			break;
		case NUMERIC:
			s=""+theCell.getNumericCellValue();
			break;
		case STRING:
			s=theCell.getStringCellValue();
			break;
		case BLANK:
			break;
		case ERROR:
			s=theCell.getStringCellValue();
		case _NONE:
			break;
		case BOOLEAN:
			if(theCell.getBooleanCellValue()) {s="1";} else {s="0"; }
			break;

		default:
			break;


		}
		return s;

	}

	public static String runMacrosForExcelRow(Row theRow, int fileColumn, int folderColumn, int macroColumn,String root_folder)
	{
		return(runMacrosForExcelRowWithReplacement(theRow,fileColumn,folderColumn,macroColumn,null, root_folder, ""));
	}	

	public static String runMacrosForExcelRow(Row theRow, int fileColumn, int folderColumn, int macroColumn,String root_folder, String defaultMacro)
	{
		return(runMacrosForExcelRowWithReplacement(theRow,fileColumn,folderColumn,macroColumn,null, root_folder, defaultMacro));
	}

	public static String runMacrosForExcelRowWithReplacement(Row theRow, int fileColumn, int folderColumn, int macroColumn, Row headerRow,String root_folder,String defaultMacro)
	{
		return (runMacrosForExcelRowWithReplacement( theRow,  fileColumn,  folderColumn,  macroColumn,  headerRow, root_folder, defaultMacro,true));
	}

	// Return: the core part of the macro
	public static String runMacrosForExcelRowWithReplacement(Row theRow, int fileColumn, int folderColumn, int macroColumn, Row headerRow,String root_folder,String defaultMacro, boolean doOpen)
	{


		Interpreter interp = new Interpreter();



		Cell theFile = theRow.getCell(fileColumn);

		Cell theFolder = null;
		// We use -1 to encode that the folder column functionality is not used
		if(folderColumn>-1)
		{


			theFolder = theRow.getCell(folderColumn);

		}

		String openMacro = "open(\"";

		if(!root_folder.equals(""))
		{
			openMacro = openMacro+root_folder+"/";
		}
		// Add the folder to the file path if needed
		if(folderColumn>-1)
		{
			if(theFolder!=null)
			{
			if(!theFolder.getStringCellValue().equals(""))
				{
					openMacro = openMacro+theFolder.getStringCellValue()+"/";
				}
			}
		}
		// Add the file, which is needed regardless of whether the folder is used
		openMacro = openMacro+theFile.getStringCellValue()+"\"); ";

		String theMacro=defaultMacro;

		if(macroColumn>-1)
		{
			Cell theMacroCell = theRow.getCell(macroColumn);
			theMacro = theMacroCell.getStringCellValue();
		}



		// We have header row, so we can start replacing values. We are looking for
		// strings of the type %colname%
		// The idea here is to avoid error-prone duplicates of the same paths
		// in the Folder and the Macro column of the Excel file
		if(headerRow != null)
		{
			theMacro = replaceColumnAlias(theMacro,theRow,headerRow);
		}



		// Also, use the root folder configured in the graphical interface if desired
		// This allows to avoid computer-specific paths in the Excel file
		theMacro = replaceRootFolderAlias(theMacro,root_folder);





		String full_macro =  theMacro+ " "+"close();";

		if(doOpen)
		{
			full_macro=openMacro+" " +full_macro;
		}






		interp.run(full_macro);

		return(full_macro);


	}

	public static String replaceRootFolderAlias(String theMacro, String root_folder)
	{
		return(theMacro.replaceAll("%root_folder%", root_folder));
	}

	public static String replaceColumnAlias(String theMacro, Row theRow, Row headerRow) {



		String finalMacro=theMacro;

		int n_headers=headerRow.getLastCellNum();
		for(int indHeaders=0; indHeaders<n_headers; indHeaders++)
		{
			if(headerRow.getCell(indHeaders).getCellTypeEnum()==CellType.STRING)
			{
				String colName=headerRow.getCell(indHeaders).getStringCellValue();
				String searchString = "%"+colName+"%";
				finalMacro = finalMacro.replaceAll
						(searchString, extractStringFromCell(theRow.getCell(indHeaders)));
			}
		}


		return finalMacro;
	}
	// This function assembles the results in the sourceTable with data from the Excel file (header row excelHeadings, data row currentRow)
	public static void assembleResults(ResultsTable sourceTable, ResultsTable newTable, String[] excelHeadings, Row currentRow )
	{
		assembleResults( sourceTable,  newTable,  excelHeadings, currentRow, null,null );

	}

	//This function assembles the results in the sourceTable with data from the Excel file (header row excelHeadings, data row currentRow)
	// Additional columns and values can be added "manually" via additionalHeaders and additionalHeaders
	// If provided, additionalHeaders and additionalHeaders need to have the same length, otherwise they should be both null
	public static void assembleResults(ResultsTable sourceTable, ResultsTable newTable, String[] excelHeadings, Row currentRow, String [] additionalHeaders, String[] additionalData )
	{


		// the current measurement normally is also the last one, so this
		// is a bit of as trick to get the length of the table
		int current_measurement=sourceTable.getCounter();

		if(current_measurement==0) // No results reported during the macro
		{
			newTable.incrementCounter();
			for(int ind_h=0; ind_h<excelHeadings.length; ind_h++)
			{


				newTable.addValue(excelHeadings[ind_h],
						extractStringFromCell(currentRow.getCell(ind_h)));

			}
			if(!(additionalHeaders==null) && !(additionalData==null) )
			{
				for(int h=0; h<additionalHeaders.length; h++)
				{
					newTable.addValue(additionalHeaders[h], additionalData[h]);
				}
			}
			
		} else {

			// These are the headings of the results table
			String[] headings=sourceTable.getHeadings();

			// Assemble and transfer row by row
			for(int ind_m=0; ind_m<current_measurement; ind_m++)
			{
				newTable.incrementCounter();

				// Copy the Excel values (they will get copied many times if there are many measurements
				// resulting from a single Excel row, but that is OK as it allows to know which 
				// image, and measurement conditions, were associated with which measurement

				for(int ind_h=0; ind_h<excelHeadings.length; ind_h++)
				{


					newTable.addValue(excelHeadings[ind_h],
							extractStringFromCell(currentRow.getCell(ind_h)));

				}

				// Add the measurements, going through the columns
				for(int h=0; h<headings.length; h++)
				{
					String theHeading = headings[h];

					int colIndex = sourceTable.getColumnIndex(theHeading);



					newTable.addValue(theHeading,
							sourceTable.getStringValue(colIndex, ind_m));

				}

				if(!(additionalHeaders==null) && !(additionalData==null) )
				{
					for(int h=0; h<additionalHeaders.length; h++)
					{
						newTable.addValue(additionalHeaders[h], additionalData[h]);
					}
				}


			}


		}

	}





}


