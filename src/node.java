import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class RTEntry {
	int nhop = 0;
	int dist = 0;
}


class node {

	int id;
	int destination;
	String message;
	int delay;

	int seq_no=0;

	String ToFileName = "";
	String FromFileName = "";
	String ReceivedFileName = "";

	Reader Rdr;

	HashMap<Integer, RTEntry> rTable = new HashMap<Integer, RTEntry>();

	enum EdgeType{
		NA, UNIDIR, BIDIR, MPR
	}

	int[] last_hello = new int[10];
	int[] last_tc = new int[10];
	String[] prev_hello = new String[10];
	int[] latest_seq = new int[10];
	EdgeType[] neighbour_table = new EdgeType[10];

	ArrayList<Set<Integer>> two_hop_nei_access_thr = new ArrayList<Set<Integer>>(10);
	ArrayList<Set<Integer>> from_one_hop = new ArrayList<Set<Integer>>(10);
	ArrayList<Set<Integer>> tc_table = new ArrayList<Set<Integer>>(10);
	ArrayList<Set<Integer>> thr_last_hop = new ArrayList<Set<Integer>>(10);

	Set<Integer> MPR_set = new HashSet<Integer>();

	Set<Integer> MS_set = new HashSet<Integer>();

	Set<Integer> one_hop_neighbor_set = new HashSet<Integer>();

	Set<Integer> two_hop_neighbor_set = new HashSet<Integer>();

	public node(int id, int destination, String message, int delay) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.destination = destination;
		this.message = message;
		this.delay = delay;

		this.ToFileName = "to"+this.id+".txt";
		this.FromFileName = "from"+this.id+".txt";
		this.ReceivedFileName = this.id+"received.txt";

		Arrays.fill(last_hello, -1);
		Arrays.fill(last_tc, -1);
		Arrays.fill(prev_hello, "");
		Arrays.fill(latest_seq, 0);
		Arrays.fill(neighbour_table, EdgeType.NA);

		for(int i=0;i<10;i++) {
			two_hop_nei_access_thr.add(new HashSet<Integer>());
			from_one_hop.add(new HashSet<Integer>());
			tc_table.add(new HashSet<Integer>());
			thr_last_hop.add(new HashSet<Integer>());
		}

		Rdr = new Reader(ToFileName);

		File createtofile = new File(ToFileName);
		File createfromfile = new File(FromFileName);

		try {
			createtofile.createNewFile();
			createfromfile.createNewFile();
		}
		catch(IOException e)  {
			System.out.println("There was an error creating the files");
			e.printStackTrace();
		}



	}

	public node(int id, int destination) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.destination = destination;

		Arrays.fill(last_hello, -1);
		Arrays.fill(last_tc, -1);
		Arrays.fill(prev_hello, "");
		Arrays.fill(latest_seq, 0);
		Arrays.fill(neighbour_table, EdgeType.NA);
		
		this.ToFileName = "to"+this.id+".txt";
		this.FromFileName = "from"+this.id+".txt";
		this.ReceivedFileName = this.id+"received.txt";

		for(int i=0;i<10;i++) {
			two_hop_nei_access_thr.add(new HashSet<Integer>());
			from_one_hop.add(new HashSet<Integer>());
			tc_table.add(new HashSet<Integer>());
			thr_last_hop.add(new HashSet<Integer>());
		}

		Rdr = new Reader(ToFileName);


		File createtofile = new File(ToFileName);
		File createfromfile = new File(FromFileName);

		if(!createtofile.exists() && !createfromfile.exists()) {
			try {
				createtofile.createNewFile();
				createfromfile.createNewFile();
			}
			catch(IOException e)  {
				System.out.println("There was an error creating the files");
				e.printStackTrace();
			}
		}
	}


	public static void main(String[] args) {

		node node;
		System.out.println("Node Starting");

		if(args.length<2) {
			System.out.println("Insufficient arguments passed\nRun the program as\njava node <node_id> <destination> [Message] [message_time_delay]");
			return;
		}

		if(args.length>2) {
			node = new node(Integer.parseInt(args[0]), Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
		}
		else {
			node = new node(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
		}

		int time = 0;

		boolean sentDataMessage = false;

		int DataMessageDelay = node.delay;

		boolean updateRT = false;


		while(time < 120) {
			updateRT = false;
			System.out.println("Time: " + time);
			
			updateRT |= node.FileProcess(time);

			System.out.println("Debug Check1");
			
			
			for(int i=0;i<10 ;i++) {
				if((time - node.last_tc[i]) >= 30 && node.thr_last_hop.get(i).isEmpty()) {
					node.RemoveTcInfoFrom(i);
					updateRT |= true; 
				}
			}
			
			System.out.println("Debug Check2");

			if(updateRT) {
				node.CalculateRT();
			}
			
			System.out.println("Debug Check3");

			if(node.id!=node.destination && !sentDataMessage && time==DataMessageDelay) {
				System.out.println("Time to send DATA message");
				if(node.rTable.containsKey(node.destination)) {
					System.out.println("Entry found for DATA message");
					node.SendDataMsg(node.rTable.get(node.destination).nhop);
					sentDataMessage = true;
				}
				else {
					DataMessageDelay += 30;
				}
			}
			
			System.out.println("Debug Check4");

			if(time%5==0) {
				node.SendHelloMsg();
			}
			
			System.out.println("Debug Check5");

			if(time % 10 == 0 && !node.MS_set.isEmpty()) {
				node.SendTcMsg();
			}
			
			System.out.println("Debug Check6");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			time+=1;
		}
		System.out.println("Node Exiting");
	}


	boolean FileProcess(int time) {

		boolean neighbor_changed = false;
		boolean tc_changed = false;

		String filename = ToFileName;

		List<String> newLines = Rdr.readFile();

		for(String line: newLines) {
			System.out.println("Hello FileProcess:  " + line);
			String[] messageDetails = line.split("\\s+");

			System.out.println("Hello FileProcess Debug -4");
			
			int fromnbr = Integer.parseInt(messageDetails[1]);

			String type = messageDetails[2];
			
			System.out.println("Hello FileProcessDebug:  "+ type );

			if(type.equals("HELLO")) {
				neighbor_changed |= HandleHelloMsg(line, time);
				System.out.println("HELLO FileProcessDebug1 ");
			}
			else if(type.equals("TC")) {
				tc_changed |= HandleTcMsg(line, time);
				System.out.println("HELLO FileProcessDebug1 ");
			}
			else if(type.equals("DATA")) {
				HandleDataMsg(line);
				System.out.println("HELLO FileProcessDebug1 ");
			}
		}


		for(int i=0;i<10;i++) {
			if(one_hop_neighbor_set.contains(i) && (time - last_hello[i])>=15) {
				RemoveNeighbour(i);
				neighbor_changed |=true;
			}
		}
		
		System.out.println("HELLO FileProcessDebug2 ");

		if(neighbor_changed) {
			CalculateMprSet();
		}
		System.out.println("HELLO FileProcessDebug2 ");

		return (neighbor_changed || tc_changed);

	}

	public void RemoveTcInfoFrom(int i) {
		for(int j:thr_last_hop.get(i)) {
			tc_table.get(j).remove(i);
		}
		thr_last_hop.get(i).clear();
	}

	public void CalculateRT() {
		rTable.clear();

		for(int it:one_hop_neighbor_set) {
			RTEntry entry = new RTEntry();
			entry.nhop=it;
			entry.dist=1;
			rTable.put(it,entry);
		}

		for(int it:two_hop_neighbor_set) {
			RTEntry entry = new RTEntry();
			entry.nhop = two_hop_nei_access_thr.get(it).iterator().next();
			entry.dist = 2;
			rTable.put(it,entry);
		}

		int h=2;
		boolean entry_added = true;
		while(entry_added) {
			entry_added = false;

			for(int i=0;i<10;i++) {
				for(int it:tc_table.get(i)) {
					if(i == id || rTable.containsKey(i)) {
						break;
					}
					if(rTable.containsKey(it) && rTable.get(it).dist==h) {
						RTEntry entry = new RTEntry();
						entry.nhop = rTable.get(it).nhop;
						entry.dist = h+1;
						rTable.put(i,entry);

						entry_added = true;
						break;
					}
				}
			}

			h+=1;
		}
	}

	public void SendDataMsg(int next_hop) {
		StringBuilder data_msg = new StringBuilder("");
		data_msg.append(next_hop + " " + id + " DATA " + id + " " + destination + " " + message);
		WriteToFile(data_msg.toString(), false);
	}

	public void SendHelloMsg() {
		StringBuilder hello_msg = new StringBuilder("");

		hello_msg.append("* " + id + " HELLO");

		hello_msg.append(" UNIDIR");

		for(int i=0;i<10;i++) {
			if(neighbour_table[i] == EdgeType.UNIDIR) {
				hello_msg.append(" " + i);
			}
		}

		hello_msg.append(" BIDIR");

		for(int i=0;i<10;i++) {
			if(neighbour_table[i] == EdgeType.BIDIR) {
				hello_msg.append(" " + i);
			}
		}

		hello_msg.append(" MPR");

		for(int i=0;i<10;i++) {
			if(neighbour_table[i] == EdgeType.MPR) {
				hello_msg.append(" " + i);
			}
		}

		WriteToFile(hello_msg.toString(), false);
	}

	public void SendTcMsg() {
		seq_no+=1;

		StringBuilder tc_msg = new StringBuilder("");

		tc_msg.append("* " + id + " TC " + id + " " + seq_no + " MS");

		for(int it:MS_set) {
			tc_msg.append(" " + it);
		}

		WriteToFile(tc_msg.toString(), false);
	}

	public boolean HandleHelloMsg(String msg, int time) {

		System.out.println("Handling Hello Message");
		
		int fromnbr;
		int nbr;
		
		System.out.println("HELLO Debug1 ");
		
		String[] messageDetails = msg.split("\\s+");
		
		System.out.println("HELLO Debug2 "+Arrays.toString(messageDetails));
		
		System.out.println("HELLO Debug2 ");

		ArrayList<Integer> unidir_of_fromnbr = new ArrayList<Integer>();
		
		System.out.println("HELLO Debug3 ");
		ArrayList<Integer> bidir_of_fromnbr = new ArrayList<Integer>();
		System.out.println("HELLO Debug4 ");
		ArrayList<Integer> mpr_of_fromnbr = new ArrayList<Integer>();
		System.out.println("HELLO Debug5 ");

		boolean id_is_unidir_of_fromnbr = false;
		System.out.println("HELLO Debug6 ");
		boolean id_is_bidir_of_fromnbr = false;
		System.out.println("HELLO Debug7 ");
		boolean id_is_mpr_of_fromnbr = false;
		System.out.println("HELLO Debug8 ");

		fromnbr = Integer.parseInt(messageDetails[1]);
		System.out.println("HELLO Debug9 ");

		last_hello[fromnbr] = time;
		System.out.println("HELLO Debug10 ");
		System.out.println("HELLO Debug10:  " +  prev_hello[fromnbr]);
		System.out.println("HELLO Debug10:  " +  msg);
		

		if(msg.equals(prev_hello[fromnbr])) {
			return false;
		}
		
		System.out.println("HELLO Debug11 ");

		prev_hello[fromnbr] = msg;
		
		System.out.println("HELLO Debug12 ");
		
		int i=3;
		if(messageDetails[i].equals("UNIDIR")) {
				i++;
				while(i<messageDetails.length && !messageDetails[i].equals("BIDIR")) {
					nbr = Integer.parseInt(messageDetails[i]);
					if(nbr==id) {
						id_is_unidir_of_fromnbr = true;
					}
					else {
						unidir_of_fromnbr.add(nbr);
					}
					i++;
				}
		}
		
		System.out.println("HELLO Debug UNIDIR processed");
		System.out.println("HELLO DEBUG UNIDIR" + messageDetails[i]);
		if(messageDetails[i].equals("BIDIR")) {
				i++;
				while(i<messageDetails.length && !messageDetails[i].equals("MPR")) {
					nbr = Integer.parseInt(messageDetails[i]);
					if(nbr==id) {
						id_is_bidir_of_fromnbr = true;
					}
					else {
						bidir_of_fromnbr.add(nbr);
					}
					i++;
				}
			}
		System.out.println("HELLO Debug BIDIR processed");
		if(messageDetails[i].equals("MPR")) {
				i++;
				while(i<messageDetails.length) {
					nbr = Integer.parseInt(messageDetails[i]);
					if(nbr==id) {
						id_is_mpr_of_fromnbr = true;
					}
					else {
						mpr_of_fromnbr.add(nbr);
					}
					i++;
				}
		}
		System.out.println("HELLO Debug MPR processed");
		
		System.out.println("List " + id + ": " + unidir_of_fromnbr);
		System.out.println("List " + id + ": " + bidir_of_fromnbr);
		System.out.println("List " + id + ": " + mpr_of_fromnbr);
		
		if(id_is_unidir_of_fromnbr || id_is_bidir_of_fromnbr || id_is_mpr_of_fromnbr) {

			neighbour_table[fromnbr] = EdgeType.BIDIR;

			one_hop_neighbor_set.add(fromnbr);

			for(int it: from_one_hop.get(fromnbr)) {
				two_hop_nei_access_thr.get(it).remove(fromnbr);
				if(two_hop_nei_access_thr.get(it).isEmpty()) {
					two_hop_neighbor_set.remove(it);
				}
			}
			from_one_hop.get(fromnbr).clear();

			for(int it: bidir_of_fromnbr) {
				from_one_hop.get(fromnbr).add(it);
				two_hop_nei_access_thr.get(it).add(fromnbr);
			}

			for(int it: mpr_of_fromnbr) {
				from_one_hop.get(fromnbr).add(it);
				two_hop_nei_access_thr.get(it).add(fromnbr);
			}

			two_hop_neighbor_set.clear();

			for(int j=0;j<10;j++) {
				if(!two_hop_nei_access_thr.get(j).isEmpty()) {
					two_hop_neighbor_set.add(j);
				}
			}

			if(MS_set.contains(fromnbr) && !id_is_mpr_of_fromnbr) {
				MS_set.remove(fromnbr);
			}
			else if(id_is_mpr_of_fromnbr) {
				MS_set.add(fromnbr);
			}

		}
		else {
			neighbour_table[fromnbr] = EdgeType.UNIDIR;
		}

		return true;
	}

	public boolean HandleTcMsg(String msg, int time) {
		int fromnbr;
		int srcnode;
		int seqno;
		
		System.out.println("Hello HandleTcMsgDebug1");

		String[] messageDetails = msg.split("\\s+");
		
		System.out.println("Hello HandleTcMsgDebug1");

		fromnbr = Integer.parseInt(messageDetails[1]);
		srcnode = Integer.parseInt(messageDetails[3]);
		seqno = Integer.parseInt(messageDetails[4]);
		
		System.out.println("Hello HandleTcMsgDebug2 :" +  Arrays.toString(messageDetails));
		
		System.out.println("Hello HandleTcMsgDebug3 " + fromnbr + " " + srcnode + " " +seqno);
		System.out.println("Hello HandleTcMsgDebug4 latest_seq :" + latest_seq[srcnode]);

		if(srcnode == id || seqno <=latest_seq[srcnode]) {
			return false;
		}
		
		System.out.println("Hello HandleTcMsgDebug5");

		latest_seq[srcnode] = seqno;

		System.out.println("Hello HandleTcMsgDebug6");
		
		if(MS_set.contains(fromnbr)) {
			ForwardTcMsg(msg, fromnbr);
			System.out.println("Hello HandleTcMsgDebug7");
		}

		System.out.println("Hello HandleTcMsgDebug9");
		
		for(int it: thr_last_hop.get(srcnode)) {
			tc_table.get(it).remove(srcnode);
			System.out.println("Hello HandleTcMsgDebug10");
		}
		
		System.out.println("Hello HandleTcMsgDebug11");

		thr_last_hop.get(srcnode).clear();

		int i=6;
		while(i<messageDetails.length) {
			thr_last_hop.get(srcnode).add(Integer.parseInt(messageDetails[i]));
			tc_table.get(Integer.parseInt(messageDetails[i])).add(srcnode);
			i++;
		}

		return true;

	}

	public void HandleDataMsg(String msg) {
		int nxthop;
		int fromnbr;
		int srcnode;
		int dstnode;

		StringBuilder content = new StringBuilder("");

		String[] messageDetails = msg.split("\\s+");

		nxthop = Integer.parseInt(messageDetails[0]);
		fromnbr = Integer.parseInt(messageDetails[1]);
		srcnode = Integer.parseInt(messageDetails[3]);
		dstnode = Integer.parseInt(messageDetails[4]);

		int i=5;
		while(i<messageDetails.length) {
			content.append(messageDetails[i]);
			i++;
		}

		System.out.println(content.toString());

		if(dstnode == id) {
			WriteToFile(content.toString(), true);
			return;
		}

		if(rTable.containsKey(dstnode)) {
			ForwardDataMsg(msg, nxthop, fromnbr, rTable.get(dstnode).nhop);
		}

	}

	public void RemoveNeighbour(int i) {
		one_hop_neighbor_set.remove(i);
		neighbour_table[i] = EdgeType.NA;
		MS_set.remove(i);

		System.out.println("Hello RemoveNeighbour Debug1");
		
		
		for(int it:from_one_hop.get(i)) {
			System.out.println("Hello RemoveNeighbour Debug2");
			two_hop_nei_access_thr.get(it).remove(i);
			System.out.println("Hello RemoveNeighbour Debug3");
			if(two_hop_nei_access_thr.get(it).isEmpty()) {
				two_hop_neighbor_set.remove(it);
				System.out.println("Hello RemoveNeighbour Debug3");
			}
		}
		
		System.out.println("Hello RemoveNeighbour Debug5");

		from_one_hop.get(i).clear();

		if(!two_hop_nei_access_thr.get(i).isEmpty()) {
			two_hop_neighbor_set.add(i);
		}

	}

	public void CalculateMprSet() {
		MPR_set.clear();

		for(int i=0;i<10;i++) {
			if(neighbour_table[i] == EdgeType.MPR) {
				neighbour_table[i]=EdgeType.BIDIR;
			}
		}

		Set<Integer> remaining_two_hop = new HashSet<Integer>(two_hop_neighbor_set);

		int[] cnt = new int[10];
		int best_candidate;

		while(!remaining_two_hop.isEmpty()) {
			for(int i=0;i<10;i++) {
				cnt[i]=0;
			}

			best_candidate = -1;

			for(int it: remaining_two_hop) {
				for(int candidate:two_hop_nei_access_thr.get(it)) {
					if(MPR_set.contains(candidate)) {
						continue;
					}

					cnt[candidate]+=1;

					if((best_candidate==-1) || (cnt[candidate] > cnt[best_candidate])) {
						best_candidate = candidate;
					}
				}
			}

			MPR_set.add(best_candidate);
			neighbour_table[best_candidate] = EdgeType.MPR;

			for(int it: from_one_hop.get(best_candidate)) {
				remaining_two_hop.remove(it);
			}
		}
	}

	public void WriteToFile(String msg, boolean receive) {

		BufferedWriter WriteFile = null;
		try {
			if(receive) {
				File createreceivefile = new File(ReceivedFileName);
				if(!createreceivefile.exists()) {
					createreceivefile.createNewFile();
				}
				WriteFile = new BufferedWriter(new FileWriter(ReceivedFileName,true));
			}
			else {
				WriteFile = new BufferedWriter(new FileWriter(FromFileName,true));
			}
		}
		catch (IOException e) {
			// TODO: handle exception
			System.out.println("There was an error initializing/opening the file");
		}
		try {
			WriteFile.write(msg);
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

	public void ForwardDataMsg(String msg, int nxthop, int fromnbr, int next_hop) {
		StringBuilder message = new StringBuilder("");

		message.append(next_hop + " " + id);

		String[] copyMessage = msg.split("\\s+");

		int i=2;
		while(i<copyMessage.length) {
			message.append(" " + copyMessage[i]);
			i++;
		}

		WriteToFile(message.toString(), false);
	}

	public void ForwardTcMsg(String msg, int fromnbr) {

		StringBuilder temp = new StringBuilder("");

		temp.append("* " + id);

		int i=2;

		String[] copyMsg = msg.split("\\s+");

		while(i<copyMsg.length) {
			temp.append(" " + copyMsg[i]);
			i++;
		}

		WriteToFile(temp.toString(),false);

	}

}

class Reader {

	int count;
	String filename="";

	/** Creates a new instance of Reader */
	public Reader(String filename) 
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













































//import java.io.BufferedReader;
//import java.nio.file.*;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.lang.reflect.Array;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Scanner;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Stream;
//
//
//class neighborTable {
//	String Status;
//	ArrayList<Integer> TwoHopwNbr = new ArrayList<Integer>();
//	ArrayList<Integer> OneHopNbr = new ArrayList<Integer>();
//	ArrayList<Integer> MPRNbr = new ArrayList<Integer>();
//	int time;
//}
//
//class topologyTable {
//	ArrayList<Integer> TwoHopwNbr = new ArrayList<Integer>();
//	int DestMpr;
//	//seq No is num
//	int num;
//	int htime;
//}
//
//class routingTable {
//	int NxtHop;
//	int dist;
//}
//
//public class node {
//	
//	int id;
//	int destination;
//	String message;
//	int delay;
//	
//	neighborTable neighborTable = new neighborTable();
//	topologyTable topologyTable = new topologyTable();
//	routingTable routingTable = new routingTable();
//	HashMap<Integer, neighborTable> Nbrs = new HashMap<Integer, neighborTable>();
//	ArrayList<Integer> MSList = new ArrayList<Integer>();
//	HashMap<Integer, Boolean> TwoHList = new HashMap<Integer, Boolean>();
//	HashMap<Integer, topologyTable> TopoList = new HashMap<Integer, topologyTable>();
//	HashMap<Integer, routingTable> RoutingTable = new HashMap<Integer, routingTable>();
//	
//	static int NumberofLines = 0;
//	
//	
//	public node (int id, int destination, String message, int delay) {
//		this.id = id;
//		this.destination = destination;
//		this.message = message;
//		this.delay = delay;
//	}
//	
//	public node(int id, int destination) {
//		this.id = id;
//		this.destination = destination;
//	}
//	
//	public boolean TopoUpdate(int time) {
//		Iterator i = TopoList.entrySet().iterator();
//		
//		while(i.hasNext()) {
//			Map.Entry mapElement
//            = (Map.Entry)i.next();
//			topologyTable tp = (topologyTable) mapElement.getValue();
//			int difference = time - tp.htime;
//			if(difference>30) {
//				i.remove();
//				return true;
//				
//			}
//		}
//		
//		return false;
//	}
//
//	public boolean removeNeighbor(int time) {
//		
//		Iterator i = Nbrs.entrySet().iterator();
//		
//		while(i.hasNext()) {
//			Map.Entry mapElement
//            = (Map.Entry)i.next();
//			neighborTable nt = (neighborTable) mapElement.getValue();
//			int difference = time - nt.time;
//			if(difference>15) {
//				i.remove();
//				for(int a=0;a<MSList.size();a++) {
//					if(MSList.get(a)==(int)mapElement.getKey()) {
//						MSList.remove(a);
//					}
//				}
//				return true;
//			}
//		}
//		
//		return false;
//	}
//
//	public void updateRT(int id2) {
//		RoutingTable.clear();
//		
//		Iterator i = Nbrs.entrySet().iterator();
//		
//		while(i.hasNext()) {
//			Map.Entry mapElement
//            = (Map.Entry)i.next();
//			routingTable rt = new routingTable();
//			rt.dist = 1;
//			rt.NxtHop = (int) mapElement.getKey();
//			RoutingTable.put((int) mapElement.getKey(), rt);
//			neighborTable nt = (neighborTable)mapElement.getValue();
//			if(nt.Status.equals("MPR")) {
//				for(int n: nt.TwoHopwNbr) {
//					if(n!=id2) {
//						routingTable routingTable = new routingTable();
//						routingTable.dist = 2;
//						routingTable.NxtHop = (int) mapElement.getKey();
//						RoutingTable.put(n, routingTable);
//					}
//				}
//				for(int n: nt.OneHopNbr) {
//					if(n!=id2) {
//						routingTable routingTable = new routingTable();
//						routingTable.dist = 2;
//						routingTable.NxtHop = (int) mapElement.getKey();
//						RoutingTable.put(n, routingTable);
//					}
//				}
//				for(int n: nt.MPRNbr) {
//						routingTable routingTable = new routingTable();
//						routingTable.dist = 2;
//						routingTable.NxtHop = (int) mapElement.getKey();
//						RoutingTable.put(n, routingTable);
//				}
//			}
//		}
//		
//		boolean temp = true;
//		
//		i = RoutingTable.entrySet().iterator();
//		
//		ArrayList<Integer> vst = new ArrayList<Integer>();
//		
//		while(temp && !TopoList.isEmpty()) {
//			
//			Iterator j = TopoList.entrySet().iterator();
//			
//			while(j.hasNext()) {
//				Map.Entry mapElement
//	            = (Map.Entry)j.next();
//				if((int)mapElement.getKey()==id2) {
//					vst.add((int)mapElement.getKey());
//					continue;
//				}
//				if(RoutingTable.containsKey((int)mapElement.getKey())) {
//					
//					if(vst.contains((int)mapElement.getKey())) {
//						continue;
//					}
//					else {
//						vst.add((int) mapElement.getKey());
//					}
//					
//					if(RoutingTable.containsKey((int) mapElement.getKey())) {
//						if(RoutingTable.get((int)mapElement.getKey()).dist !=1) {
//							topologyTable tp = (topologyTable) mapElement.getValue();
//							for(int k:tp.TwoHopwNbr) {
//								if(!RoutingTable.containsKey(k) && k!=id2) {
//									temp = true;
//									routingTable rt = new routingTable();
//									rt.NxtHop = RoutingTable.get((int) mapElement.getKey()).NxtHop;
//									rt.dist = RoutingTable.get((int) mapElement.getKey()).dist + 1;
//									RoutingTable.put(k, rt);
//								}
//							}
//							if(vst.size()==TopoList.size()) {
//								temp = false;
//							}
//						}
//					}
//				}
//				if(vst.size()==TopoList.size()) {
//					temp = false;
//				}
//			}
//			
//		}
//		
//		return;
//		
//	}
//
//	public void processLine(int time, String line) {
//		// TODO Auto-generated method stub
//		if(line.isEmpty()) {
//			return;
//		}
//		node.NumberofLines++;
//		if(line.contains("DATA")) {
//			
//			//DATA Message
//			PrintWriter ReceivedFile = null;
//			try {
//				ReceivedFile = new PrintWriter(new FileOutputStream(
//					    new File(id+"received.txt"),true));
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} 
//			List<String> details = new ArrayList<String>();
//			Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
//			while (m.find())
//			    details.add(m.group(1));
//			
//			int srcnode = Integer.parseInt(details.get(3));
//			int dstnode = Integer.parseInt(details.get(4));
//			String message = details.get(5);
//			
//			if(dstnode == id) {
//				ReceivedFile.println(message);
//			}
//			else {
//				updateRT(id);
//				if(!RoutingTable.isEmpty()) {
//					if(RoutingTable.containsKey(dstnode)) {
//						int nxthop = RoutingTable.get(dstnode).NxtHop;
//						ConstructDataMessage(nxthop, id, srcnode, dstnode, message);
//					}
//				}
//			}
//			ReceivedFile.flush();
//			ReceivedFile.close();
//		}
//		
//		else if(line.contains("HELLO")) {
//			System.out.println("inside hello");
//			//HELLO Message
//			List<String> details = new ArrayList<String>();
//			Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
//			while (m.find())
//			    details.add(m.group(1));
//			
//			int node = Integer.parseInt(details.get(1));
//			String message = details.get(5);
//			int i=3;
//			ArrayList<Integer> nodeuninbr = new ArrayList<Integer>();
//			ArrayList<Integer> nodebidirnbr = new ArrayList<Integer>();
//			ArrayList<Integer> nodembr = new ArrayList<Integer>();
//			if(i<details.size() && details.get(i).equals("UNIDIR")) {
//				i++;
//				while(!details.get(i).equals("BIDIR") && !details.get(i).equals("MPR")) {
//					nodeuninbr.add(Integer.parseInt(details.get(i)));
//					i++;
//				}
//			}
//			if(i<details.size() && details.get(i).equals("BIDIR")) {
//				i++;
//				while(!details.get(i).equals("MPR")) {
//					nodebidirnbr.add(Integer.parseInt(details.get(i)));
//					i++;
//				}
//			}
//			i++;
//			while(i<details.size()) {
//				nodembr.add(Integer.parseInt(details.get(i)));
//			}
//			
//			if(Nbrs.containsKey(node)) {
//				neighborTable nt = Nbrs.get(node);
//				nt.time=time;
//				nt.TwoHopwNbr.clear();
//				nt.OneHopNbr.clear();
//				nt.MPRNbr.clear();
//				
//				if(!nodeuninbr.isEmpty()) {
//					nt.OneHopNbr.addAll(nodeuninbr);
//					for(int a: nodeuninbr) {
//						if(a==id) {
//							nt.Status="BIDIR";
//						}
//					}
//				}
//				
//				if(!nodebidirnbr.isEmpty()) {
//					nt.TwoHopwNbr.addAll(nodebidirnbr);
//				}
//				
//				if(!nodembr.isEmpty()) {
//					nt.MPRNbr.addAll(nodembr);
//					if(nt.MPRNbr.contains(id)) {
//						if(!MSList.contains(node)) {
//							MSList.add(node);
//						}
//					}
//				}
//				Nbrs.put(node, nt);
//			}
//			
//		}
//		
//		else {
//			
//			//TC Message
//			PrintWriter FromFile = null;
//			try {
//				FromFile = new PrintWriter(new FileOutputStream(
//					    new File("From"+id+".txt"),true));
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			boolean FreshData = false;
//			
//			List<String> details = new ArrayList<String>();
//			Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
//			while (m.find())
//			    details.add(m.group(1));
//			
//			int receivedFromNode = Integer.parseInt(details.get(1));
//			int srcnode = Integer.parseInt(details.get(3));
//			int seqno = Integer.parseInt(details.get(4));
//			
//			ArrayList<Integer> nodems = new ArrayList<Integer>();
//			
//			int i=6;
//			while(i<details.size()) {
//				nodems.add(Integer.parseInt(details.get(i)));
//			}
//			
//			if(!nodems.isEmpty()) {
//				if(TopoList.containsKey(srcnode)) {
//					FreshData = updateTopo(time, srcnode, seqno, nodems);
//				}
//				else {
//					topologyTable tp = new topologyTable();
//					tp.TwoHopwNbr.addAll(nodems);
//					tp.num = seqno;
//					tp.htime = time;
//					TopoList.put(srcnode,tp);
//				}
//			}
//			
//			if(srcnode != id && FreshData) {
//				if(MSList.contains(receivedFromNode)) {
//					
//					StringBuilder s = new StringBuilder("");
//					s.append("* " + id + " TC ");
//					for(int j=3;j<details.size();j++) {
//						s.append(" " + details.get(j));
//					}
//					
//					FromFile.println(s.toString());
//				}
//			}
//			FromFile.flush();
//			FromFile.close();
//			
//			
//		}
//	}
//	
//	private boolean updateTopo(int time, int srcnode, int seqno, ArrayList<Integer> nodems) {
//		// TODO Auto-generated method stub
//		
//		if(TopoList.containsKey(srcnode)) {
//			topologyTable tp = TopoList.get(srcnode);
//			
//			if(tp.num>seqno) {
//				return false;
//			}
//			if(tp.TwoHopwNbr.equals(nodems)) {
//				tp.htime = time;
//			}
//			else {
//				tp.TwoHopwNbr.clear();
//				tp.TwoHopwNbr.addAll(nodems);
//				tp.num=seqno;
//				tp.htime=time;
//			}
//		}
//		
//		return true;
//	}
//
//	public static void main(String[] args) {
//		int id;
//		int destination;
//		node node;
//		String message = null;
//		int delay = 0;
//		id = Integer.parseInt(args[0]);
//		boolean messageSent = false;
//		destination = Integer.parseInt(args[1]);
//		if(args.length>2) {
//			id = Integer.parseInt(args[0]);
//			destination = Integer.parseInt(args[1]);
//			message = args[2];
//			StringBuilder temp = new StringBuilder("");
//			temp.append("\""+message+"\"");
//			message = temp.toString();
//			System.out.println(message);
//			delay = Integer.parseInt(args[3]);
//			node = new node(id,destination,message,destination);
//		}
//		else {
//			node = new node(id, destination);
//		}
//		File file = new File("From"+id+".txt");
//		try {
//			file.createNewFile();
//		} catch (IOException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
//		file = new File("To"+id+".txt");
//		try {
//			file.createNewFile();
//		} catch (IOException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
//		file = new File(id+"received.txt");
//		try {
//			file.createNewFile();
//		} catch (IOException e2) {
//			// TODO Auto-generated catch block
//			e2.printStackTrace();
//		}
//		int i=0;
//		int seqno = 1;
//		while(i<120) {
//				final Integer innerMi = new Integer(i);
//				Path path = Paths.get("To"+id+".txt");
//				try {
//					Stream<String> lines = Files.lines(path).skip(node.NumberofLines);
//					lines.forEachOrdered(line -> node.processLine(innerMi,line));
//					
//				} catch (IOException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//			
//			if(id!=destination) {
//				if(i==delay && !messageSent) {
//					node.updateRT(node.id);
//					while(!messageSent) {
//						PrintWriter FromFile = null;
//						try {
//							FromFile = new PrintWriter(new FileOutputStream(
//								    new File("From"+id+".txt"),true));
//						} catch (FileNotFoundException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						if(node.RoutingTable.containsKey(destination)) {
//							FromFile.write(node.ConstructDataMessage(node.RoutingTable.get(destination).NxtHop, id, id, destination, message));
//							messageSent = true;
//							FromFile.flush();
//							FromFile.close();
//						}
//						else {
//							delay+=30;
//						}
//					}
//				}
//			}
//			
//			if(!node.Nbrs.isEmpty()) {
//				Iterator<Entry<Integer, neighborTable>> itr = node.Nbrs.entrySet().iterator();
//				
//				while(itr.hasNext()) {
//					Map.Entry<Integer, neighborTable> mapElement = (Map.Entry)itr.next();
//					
//					if(mapElement.getValue().Status.equals("UNIDIR")) {
//						for(int a: mapElement.getValue().TwoHopwNbr) {
//							if(!node.TwoHList.containsKey(a)) {
//								
//								mapElement.getValue().Status="MPR";
//								node.TwoHList.put(a, true);			
//							}
//						}
//						
//						for(int a: mapElement.getValue().MPRNbr) {
//							if(!node.TwoHList.containsKey(a)) {
//								
//								mapElement.getValue().Status="MPR";
//								node.TwoHList.put(a, true);			
//							}
//						}
//					}
//					
//				}
//			}
//			
//			if(i%5==0) {
//				PrintWriter FromFile = null;
//				try {
//					FromFile = new PrintWriter(new FileOutputStream(
//						    new File("From"+id+".txt"),true));
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				//Send Hello Message
//				System.out.println("Sending HELLO message....");
//				StringBuilder uni = new StringBuilder("");
//				StringBuilder multi = new StringBuilder("");
//				StringBuilder mpr = new StringBuilder("");
//				
//				Iterator<Entry<Integer, neighborTable>> itr = node.Nbrs.entrySet().iterator();
//				
//				while(itr.hasNext()) {
//					Map.Entry<Integer, neighborTable> mapElement
//		            = (Map.Entry)itr.next();
//					neighborTable nt = (neighborTable) mapElement.getValue();
//					if(nt.Status.equals("UNIDIR")) {
//						uni.append((int)mapElement.getKey() + " ");
//					}
//					if(nt.Status.equals("BIDIR")) {
//						multi.append((int)mapElement.getKey() + " ");
//					}
//					if(nt.Status.equals("MPR")) {
//						mpr.append((int)mapElement.getKey() + " ");
//					}
//					
//				}
//				
//				String HELLO = "* " + id + " HELLO UNIDIR "+uni.toString()+"BIDIR " + multi.toString() + "MPR " + mpr.toString();
//				FromFile.println(HELLO);
//				FromFile.flush();
//				FromFile.close();
//			}
//			
//			if(i%10==0) {
//				PrintWriter FromFile = null;
//				try {
//					FromFile = new PrintWriter(new FileOutputStream(
//						    new File("From"+id+".txt"),true));
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println("Sending TC message....");
//				StringBuilder s = new StringBuilder("");
//				if(!node.MSList.isEmpty()) {
//					s.append("* "+ id + " TC " + id + " " + seqno + " MS");
//					for(int a:node.MSList) {
//						s.append(" "+a);
//					}
//					System.out.println(s);
//					FromFile.println(s.toString());
//				}
//				seqno+=1;
//				FromFile.flush();
//				FromFile.close();
//				
//			}
//			
//			if(node.removeNeighbor(i) || node.TopoUpdate(i)) {
//				node.RoutingTable.clear();
//				node.updateRT(id);
//			}
//			
//			i+=1;
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		System.out.println("Exiting");
//	}
//
//
//	public String ConstructDataMessage(int nxthop, int fromnbr, int srcnode, int dstnode, String message) {
//		return nxthop + " " + fromnbr + " DATA " + srcnode + " " + dstnode + " " + message + "\n";
//	}
//	
////	public static String ConstructDataMessage(int nxthop, int fromnbr, int srcnode, int dstnode, String message) {
////		return nxthop + " " + fromnbr + " DATA " + srcnode + " " + dstnode + " " + message + "\n";
////	}
//}
