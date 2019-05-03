# CSVImporter
Plugin to import CSV files into Apromore


###### How to use:

Click on import file to choose CSV log, it will display the first 100 rows of the file. Then click on Convert to XES, it will parse and create XES formate file saved at the base directory.


###### Code Structure:

> Index.zul:

  includes ZK components:
  
   - upload button to import CSV file
   - attrBox: div to display drop-down lists to define attributes
   - grid: to display 100 rows from the CSV file.
   - Convert to XES button to create XES from the imported file

  
> CSVImporterController.java:

  - The main class and the connector between Java source and ZK components
  - Has 3 methods so far:
  
    - uploadFile: 
        Works as an event listener for the upload button.
        Allows uploading CSV format only.
        Sets Model and Row renderer for the grid by calling displayCSVContent()
        Enable "Convert to XES" button
        
    - displayCSVContent:
      Show first 100 rows of the file to the user,
      display drop-down lists
      
    - toXES:
      Works as an event listener when submitting the "Convert to XES" button to create an XES file.
      
 
> Parse.java:

   To detect timestamp format and parse the value.
 
 
> LogModel.java:

   To create model of the imported csv data, used in CsvToXes.prepareXesModel()
 
 
> CsvToXes.java: handles creating the XES file
 
  - prepareXesModel method:
      Crates List<LogModel> and parses time stamps based on Parse class.
      Uses a number of helper methods to auto allocate the positions of main attributes (Case id, timestamp, activity), these methods are: headerPos, getPos, checkFields
        
  - createXLog method:
      Takes List<LogModel> as an argument, the one created by prepareXesModel, then creates XLog using XES library.
    
  - toXESfile:
      Takes XLog as argument and creates and saves test.XES file to the main directory.
  
  - setLists:
    to set a drop down list for each column of the imported file, and auto select the attribute.
 
      
        


















