package excel_macro.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


// Code by bkromhout (github), Read_and_Write_Excel_Modified

public class ExcelHolder {
    private File excelFile;
    private FileInputStream fileIn;
    private boolean isValid;
    public Workbook wb;

    public ExcelHolder(String filePath) {
    	excelFile = new File(filePath);
        isValid = false;
    }
    
    public String[] sheetNames()
    {
    	String[] theSheets = new String[wb.getNumberOfSheets()];
    	for(int ind=0; ind < theSheets.length; ind++)
    	{
    		theSheets[ind]=wb.getSheetName(ind);
    	}
    	
    	return theSheets;
    }
    
    public void readFileAndOpenWorkbook() throws IOException, InvalidFormatException {
    	readFileAndOpenWorkbook(null);
    }
    
    
    // If a defaultSheetName different from null is provided, this method not only reads an 
    // existing Excel file, but actually ensures an Excel file exists by creating one if needed. In this
    // case, it uses the defaultSheetName to create a first sheet.
    public void readFileAndOpenWorkbook(String defaultSheetName) throws IOException, InvalidFormatException {
    	if(!(defaultSheetName==null))
    	{
    		ensureExcelFileExists(excelFile, defaultSheetName);
    	}
        fileIn = new FileInputStream(excelFile);
        wb = WorkbookFactory.create(fileIn);
        isValid = true;
    }
    
    

    void writeOutAndCloseWorkbook() throws IOException {
        FileOutputStream fileOut = new FileOutputStream(excelFile);
        wb.write(fileOut);
        if (fileIn != null) fileIn.close();
        fileOut.close();
        isValid = false;
    }
    
    

    private void ensureExcelFileExists(File excelFile, String defaultSheetName) throws IOException {
        if (!excelFile.exists()) {
            XSSFWorkbook wb = new XSSFWorkbook();
            wb.createSheet(defaultSheetName);
            FileOutputStream tempOut = new FileOutputStream(excelFile);
            wb.write(tempOut);
        }
    }
}
