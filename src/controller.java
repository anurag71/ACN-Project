import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class controller {
	
	HashMap<Integer, String> FromFileInfo = new HashMap<Integer, String>();
	HashMap<Integer, String> ToFileInfo = new HashMap<Integer, String>();
	HashMap<Integer, Integer> numofLines = new HashMap<Integer, Integer>();
	
	boolean[][] adjacency_matrix = new boolean[10][10];
	
	ControllerReader[] rdr = new ControllerReader[10];
	
	class TopologyEntry{
		int time;
		int src;
		int dst;
		boolean active;
	}
	
	ArrayList<TopologyEntry> topologyEntries = new ArrayList<controller.TopologyEntry>();
	
	int topoEntryCounter = 0;
	
	public controller() {
		// TODO Auto-generated constructor stub
		for(int i=0;i<10;i++) {
			FromFileInfo.put(i, "From"+i+".txt");
			ToFileInfo.put(i, "To"+i+".txt");
			numofLines.put(i,0);
			
			rdr[i] = new ControllerReader("from"+i+".txt");
		}
		
		ParseTopology();
	}
	
	
	
//	private ArrayList<Integer> parseTopologyFile(int i, int nodeNum)
//	{
//		Scanner tplgy = null;
//		try {
//			tplgy = new Scanner(new File("topology.txt"));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String line = "";
//		ArrayList<Integer> nbrs = new ArrayList<Integer>();
//		while (tplgy.hasNext())
//		{
//			line = tplgy.nextLine();
//			String buffer = "";
//			List<String> token = new ArrayList<String>();
//			token = Arrays.asList(line.split("\\s+"));
//			if (!token.isEmpty())
//			{
//				int dest = Integer.parseInt(token.get(3));
//				if (Integer.parseInt(token.get(2)) == nodeNum)
//				{
//					if (token.get(1).equals("UP"))
//					{
//						nbrs.add(dest);
//					}
//					else
//					{
//						if ((token.get(1).equals("DOWN")) && (i >= Integer.parseInt(token.get(0))))
//						{
//							for(int k=0;k<nbrs.size();k++) {
//								if(nbrs.get(k)==dest) {
//									nbrs.remove(k);
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		return nbrs;
//	}
//	private void writeToFile(int nbr, String message)
//	{
//		int index = 0;
//		Scanner to = new Scanner(ToFileInfo.get(nbr));
//		if (message.contains("TC"))
//		{
//				while (to.hasNext())
//				{
//						String line = "";
//						line = to.nextLine();
//
//				if (line.contains(message))
//				{
//					to.close();
//					return;
//				}
//			}
//		}
//		to.close();
//		PrintWriter ito = null;
//		try {
//			ito = new PrintWriter(new FileOutputStream(new File(ToFileInfo.get(nbr)),true));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		};
//		ito.println(message);
//		ito.flush();
//		ito.close();
//	}
//
//	private void readFromFile(int ts, int nodeNum)
//	{
//		Scanner from = null;
//		try {
//			from = new Scanner(new File(FromFileInfo.get(nodeNum)));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String line = "";
//		ArrayList<String> lines = new ArrayList<String>();
//		int i=0;
//		while (from.hasNextLine())
//		{
//			if(i<numofLines.get(nodeNum)) {
//				i++;
//				continue;
//			}
//			line = from.nextLine();
//			System.out.println(line);
//			 if (line.equals(""))
//			 {
//							break;
//			 }
//				List<String> token = new ArrayList<String>();
//				token = Arrays.asList(line.split("\\s+"));
//				if (!token.isEmpty())
//				{
//					ArrayList<Integer> nbrs;
//					if (token.get(2).equals("HELLO"))
//					{
//						nbrs = parseTopologyFile(ts, Integer.parseInt(token.get(1)));
//	//C++ TO JAVA CONVERTER TODO TASK: Iterators are only converted within the context of 'while' and 'for' loops:
//						for (int k:nbrs)
//						{
//	//C++ TO JAVA CONVERTER TODO TASK: Iterators are only converted within the context of 'while' and 'for' loops:
//							writeToFile(k, line);
//						}
//					}
//					if (token.get(2).equals("TC"))
//					{
//											nbrs = parseTopologyFile(ts, Integer.parseInt(token.get(1)));
//	//C++ TO JAVA CONVERTER TODO TASK: Iterators are only converted within the context of 'while' and 'for' loops:
//											for (int k:nbrs)
//											{
//	//C++ TO JAVA CONVERTER TODO TASK: Iterators are only converted within the context of 'while' and 'for' loops:
//							if (k != Integer.parseInt(token.get(3)))
//							{
//	//C++ TO JAVA CONVERTER TODO TASK: Iterators are only converted within the context of 'while' and 'for' loops:
//														writeToFile(k, line);
//							}
//											}
//					}
//
//					if (token.get(2).equals("DATA"))
//					{
//						writeToFile(Integer.parseInt(token.get(0)), line);
//					}
//					i++;
//			}
//			lines.add(line);
//		}
//		numofLines.put(nodeNum, i);
//	}

	
	
	public static void main(String[] args) {
//		int i = 0;
		controller controller = new controller();
//		while(i<120) {
//			for(int j =0;j<6;j++) {
//				controller.readFromFile(i,j);
//			}
//			i+=1;
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		System.out.println("Controller starting");
		
		int time = 0;
		
		while(time < 120) {
			
			System.out.println("Time Elapsed: " + (time+1));
			
			controller.UpdateTopology(time);
			
			for(int i=0;i<10;i++) {
				String pathname = "from"+i+".txt";
				
				List<String> newLines = controller.rdr[i].readFile();
				
				for(String line:newLines) {
					
					if(line.charAt(0)=='*') {
						for(int j=0;j<10;j++) {
							if(j==i || !controller.adjacency_matrix[i][j]) {
								continue;
							}
							
							String filePath = "to"+j+".txt";
                            BufferedWriter WriteFile = null;
							try {
								WriteFile = new BufferedWriter(new FileWriter(filePath,true));
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								System.out.println("There was an error initializing/opening the file");
								e1.printStackTrace();
							}
                            try {
								WriteFile.write(line);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								System.out.println("There was error writing to file");
								e.printStackTrace();
							}
                            try {
								WriteFile.write("\n");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								System.out.println("There was error writing new line to file");
								e.printStackTrace();
							}
                            try {
								WriteFile.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								System.out.println("There was error closing the file");
								e.printStackTrace();
							}
						}
					}
					else {
						int destination = Character.getNumericValue((line.charAt(0)));
						
						if(destination == i || controller.adjacency_matrix[i][destination]) {
							String filePath = "to"+destination+".txt";
                            BufferedWriter WriteFile = null;
							try {
								WriteFile = new BufferedWriter(new FileWriter(filePath,true));
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								System.out.println("There was an error initializing/opening the file");
								e1.printStackTrace();
							}
                            try {
								WriteFile.write(line);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								System.out.println("There was error writing to file");
								e.printStackTrace();
							}
                            try {
								WriteFile.write("\n");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								System.out.println("There was error writing new line to file");
								e.printStackTrace();
							}
                            try {
								WriteFile.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								System.out.println("There was error closing the file");
								e.printStackTrace();
							}
						}
					}
				}
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			time+=1;
			
		}
		
		System.out.println("Controller exiting");
	}
	
	public void UpdateTopology(int time) {
		
		int EntryToBeProcessed = topoEntryCounter;
		while(EntryToBeProcessed<topologyEntries.size() && topologyEntries.get(EntryToBeProcessed).time <=time) {
			
			adjacency_matrix[topologyEntries.get(EntryToBeProcessed).src][topologyEntries.get(EntryToBeProcessed).dst] = topologyEntries.get(EntryToBeProcessed).active;
			EntryToBeProcessed++;
		}
		topoEntryCounter = EntryToBeProcessed;
		
		System.out.println("***************************************");
		
		for(int i=0;i<10;i++) {
			for(int j=0;j<10;j++) {
				System.out.print(adjacency_matrix[i][j] + " ");
			}
			System.out.println();
		}
	}
	
	public void ParseTopology() {
		
		String line="";
		
		BufferedReader ReadFile = null;
		try {
			ReadFile = new BufferedReader(new FileReader("topology.txt"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			System.out.println("There was an error initializing the topology file reader");
			e1.printStackTrace();
		}
		try {
			while((line = ReadFile.readLine()) != null) {
				int time;
				int src;
				int dst;
				boolean active;
				
				String[] temp = line.split("\\s+");
				
				time = Integer.parseInt(temp[0]);
				if(temp[1].equals("UP")) {
					active = true;
				}
				else {
					active = false;
				}
				
				src = Integer.parseInt(temp[2]);
				dst = Integer.parseInt(temp[3]);
				
				TopologyEntry entry = new TopologyEntry();
				entry.time = time;
				entry.src = src;
				entry.dst = dst;
				entry.active = active;
				
				topologyEntries.add(entry);
				
				topoEntryCounter = 0;
			}
		}
		catch(IOException e) {
			System.out.println("There was an error topology file.");
			e.printStackTrace();
		}
		
	}
}

class ControllerReader {

    int count;
    String filename="";
    
    /** Creates a new instance of Reader */
    public ControllerReader(String filename) 
    {
    	this.filename = filename;
    
        try
        {
                    String pathname = filename;
                    File SharedFile = new File(pathname);
                    FileWriter SFile = new FileWriter(SharedFile);
                    SFile.close();
                    count = 0;
                
            
        }
        catch(Exception e)
        {
            System.out.println(e + "in InitReader");
        }
    }
    
    /*========= read output files and write into input files ==========*/
    List<String> readFile()
    {
    	List<String> lines = new ArrayList<String>();
        try
        {                              
                    String str = this.filename;
                    BufferedReader ReadFile = new BufferedReader(new FileReader(str));
                    int temp = 0;
                    while((str = ReadFile.readLine()) != null)
                    {
                        ++temp;
                        if(temp > count) /* new msg */
                        {
//                                    String filePath = "WhatRead";
//                                    BufferedWriter WriteFile = new BufferedWriter(new FileWriter(filePath,true));
//                                    WriteFile.write(str);
//                                    WriteFile.write("\n");
//                                    WriteFile.close();
                        	lines.add(str);
                        }
                   }
                   count = temp;
                   
        }
        catch(Exception e)
        {
            System.out.println(e + " in readFile()");
        }
        
        return lines;
    }
                  
           
    /**
     * @param args the command line arguments
     */
}
