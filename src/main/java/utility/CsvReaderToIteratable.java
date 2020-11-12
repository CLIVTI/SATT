package utility;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.HashBasedTable; 
import com.google.common.collect.Table;

import org.matsim.matrices.Matrix;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvException;
import org.matsim.utils.MemoryObserver;



public class CsvReaderToIteratable {
	private String CSV_FILE_PATH;
	private char saparater;
	
	
	public CsvReaderToIteratable(String sampleCsvFilePath, char saparater) {
		this.CSV_FILE_PATH=sampleCsvFilePath;
		this.saparater=saparater;
	}

	
	public Table<String, String, String> readTableWithUniqueID(String id) throws IOException, CsvException {
		
		List<String[]> records= readTable();
//		Reader reader = Files.newBufferedReader(Paths.get(this.CSV_FILE_PATH));
//	    CSVParser parser = new CSVParserBuilder()
//	        .withSeparator(this.saparater)
//	        .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_QUOTES)
//	        .withIgnoreLeadingWhiteSpace(true)
//	        .build();
//	    CSVReader csvReader = new CSVReaderBuilder(reader)
//	            .withCSVParser(parser)
//	            .build();
//	    
//        // first row header
//	    List<String[]> records = csvReader.readAll();
	    String[] header= records.get(0);
	    
	    Table<String, String, String> output = HashBasedTable.create(); 
	    int headerIDLocation=0;
	    for (String headerID:header) {
	    	if(headerID.equals(id)) {
	    		break;
	    	}
	    	headerIDLocation++;
	    }
	    
	    records.remove(0);
	    
	    int numberOfRows=records.size();
	    for (int i =0; i<(numberOfRows);i++) {
	        // System.out.println("Id : " + record[headerIDLocation]);
	    	String[] record=records.get(0);
	        for (int counter=0; counter<record.length;counter++) {
	        	if (counter!=headerIDLocation) {
	        		if (!record[headerIDLocation].isEmpty()) {
	        			output.put(record[headerIDLocation], header[counter], record[counter]);
	        		}
	        		
	        	}
	        }
	        records.remove(0);

	    }
	    return output;
	}
	
	
	
	
	public Table<String, String, String> readTableWithUniqueID(int headerIDLocation) throws IOException, CsvException {
		// int headerIDLocation gives which column is the ID column 
		List<String[]> records= readTable();
	    String[] header= records.get(0);    
	    Table<String, String, String> output = HashBasedTable.create();     
	    records.remove(0);
	    int numberOfRows=records.size();
	    for (int i =0; i<(numberOfRows);i++) {
	    	String[] record=records.get(0);
	        for (int counter=0; counter<record.length;counter++) {
	        	if (counter!=headerIDLocation) {
	        		if (!record[headerIDLocation].isEmpty()) {
	        			output.put(record[headerIDLocation], header[counter], record[counter]);
	        		}
	        	}
	        }
	        records.remove(0);
	        // MemoryObserver.printMemory();
	    }
	    
	    

	    return output;

	}
	
	
	public Matrix readODMatrixWithUniqueID(int headerIDLocation) throws IOException, CsvException {
		// int headerIDLocation gives which column is the ID column 
		List<String[]> records= readTable();
	    String[] header= records.get(0);       
	    records.remove(0);
	    int numberOfZones=records.size();
	    Matrix odMatrix = new Matrix("ODMatrix", "Dummy");
	    for (int i =0; i<(numberOfZones);i++) {
	    	String[] record=records.get(0);
	        // System.out.println("row: "+(i+1) + " reads in: " + (record.length-1) + " OD.");
	        for (int counter=0; counter<record.length;counter++) {
	        	if (counter!=headerIDLocation) {
	        		odMatrix.setEntry(record[headerIDLocation], header[counter], Double.parseDouble(record[counter]));
	        	}
	        }
	        records.remove(0);

	    }
	    

	    return odMatrix;

	}
	
	
	private List<String[]> readTable() throws IOException, CsvException{
		Reader reader = Files.newBufferedReader(Paths.get(this.CSV_FILE_PATH),Charset.forName("ISO-8859-1"));
	    CSVParser parser = new CSVParserBuilder()
	        .withSeparator(this.saparater)
	        .withFieldAsNull(CSVReaderNullFieldIndicator.EMPTY_QUOTES)
	        .withIgnoreLeadingWhiteSpace(true)
	        .build();
	    CSVReader csvReader = new CSVReaderBuilder(reader)
	            .withCSVParser(parser)
	            .build();
	    List<String[]> records= csvReader.readAll();
	    csvReader.close();
	    reader.close();
		return records;
	    
	}
	
	
	

	public String getCSV_FILE_PATH() {
		return CSV_FILE_PATH;
	}

	public char getSaparater() {
		return saparater;
	}





	

}
