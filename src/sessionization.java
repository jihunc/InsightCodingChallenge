import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class Node{
	public Node() {
		firstDate = "";
		remainTime = 0;
		count = 0;
		duration = 0;
		inputValue = null;
	}
	public Node(String firstDate, int remainTime, int count, int duration, String[] inputValue) {
		this.firstDate = firstDate;
		this.remainTime = remainTime;
		this.count = count;
		this.duration = duration;
		this.inputValue = inputValue;
		
	}
	public String getFirstDate() {
		return firstDate;
	}
	public int getRTime() {
		return remainTime;
	}
	public int getCount() {
		return count;
	}
	public int getDuration() {
		return duration;
	}
	public String[] getInData() {
		return inputValue;
	}
	
	private String firstDate; // session start date
	private int remainTime; // remained time to end current session
	private int count; // the count of web page request until now
	private int duration; // duration from session start to recent web page access
	private String[] inputValue; // line data read from input file
	
}

public class sessionization {
	// To analyze header information and identify index number of information needed
	public static int[] headerAnal(String line, int[] hIndex) {
		String[] headerArr = line.split(",");
		//System.out.println(Arrays.toString(headerArr));
		for(int i = 0; i < headerArr.length; i++) {
			if(headerArr[i].equals("ip")) {
				hIndex[0] = i;
			}
			else if(headerArr[i].equals("date")) {
				hIndex[1] = i;
			}
			else if(headerArr[i].equals("time")) {
				hIndex[2] = i;
			}
			else if(headerArr[i].equals("cik")) {
				hIndex[3] = i;
			}
			else if(headerArr[i].equals("accession")) {
				hIndex[4] = i;
			}
			else if(headerArr[i].equals("extention")) {
				hIndex[5] = i;
			}
		}
		
		return hIndex;
	}
	
	// To simplify the information to three major fields (ip address, date, web page unique id)
	public static String[] simpleLine(String line, int[] hIndex) {
		//System.out.println(line);
		String[] curArr = line.split(",");
		String ip = curArr[hIndex[0]];
		String date = curArr[hIndex[1]];
		String time = curArr[hIndex[2]];
		String cik = curArr[hIndex[3]];
		String accession = curArr[hIndex[4]];
		String extention = curArr[hIndex[5]];
		//System.out.println("ip: "+ip+", date: "+date+", time: "+time+", cik: "+cik+", acc: "+accession+", ext: "+extention);
		
		//date combination (format: yyyy-MM-dd hh:mm:ss)
		String dateSt = date+" "+time;
		
		//single web page id
		String webId = cik+accession+extention;
		
		//ip,dateSt,webId
		String[] simpleL = {ip,dateSt,webId};
		
		return simpleL;
	}
	
	// To change string to date
	public static Date changeToDate(String stDate) {
		SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date dateValue = new Date();
		try {
			dateValue = fm.parse(stDate);
			//System.out.println("date: "+dateValue);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return dateValue;
	}
	
	// To input data into the map
	public static Map<String,Node> setToMap(Map<String,Node> keyMap, String[] inputLine, int p) {
		if(keyMap.containsKey(inputLine[0])) {
			Node curVal = keyMap.get(inputLine[0]);
			String firstDate = curVal.getFirstDate();
			int count = curVal.getCount();
			int duration = curVal.getDuration();
			String[] curData = curVal.getInData();
			duration += durCal(curData[1],inputLine[1]);
			
			curVal = new Node(firstDate,p,count+1,duration,inputLine);
			keyMap.put(inputLine[0], curVal);
		}
		else {
			String firstDate = inputLine[1];
			Node curVal = new Node(firstDate,p,1,1,inputLine);
			keyMap.put(inputLine[0], curVal);
		}
		
		return keyMap;
	}
	
	// To calculate duration of two string type date information in seconds
	public static int durCal(String first,String second) {
		int result = 0;
		Date dateValue1 = changeToDate(first);
		Date dateValue2 = changeToDate(second);
		long diffInMilliS = Math.abs(dateValue1.getTime() - dateValue2.getTime());
		long diffInSeconds = diffInMilliS/1000;
		result = (int) diffInSeconds;
		
		return result;
	}
	
	public static void main(String[] args) throws IOException {
		String sessionOut = "";
		File file = new File("../input/inactivity_period.txt");
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = "";
		int p = 0;
		while((line = reader.readLine())!=null) {
			p = Integer.parseInt(line);
		}
		//System.out.println(p);
		reader.close();
		File logFile = new File("../input/log.csv");
		reader = new BufferedReader(new FileReader(logFile));
		line = reader.readLine();
		int[] hIndex = {-1,-1,-1,-1,-1,-1};
		
		//define header index
		hIndex = headerAnal(line, hIndex);
		if(hIndex[0] == -1 || hIndex[1] == -1 || hIndex[2] == -1 || hIndex[3] == -1 || hIndex[4] == -1 || hIndex[5] == -1) {
			System.out.println("Error in header of log.csv file!!");
			return;
		}
		//System.out.println("ip: "+hIndex[0]+", date: "+hIndex[1]+", time: "+hIndex[2]+", cik: "+hIndex[3]+", acc: "+hIndex[4]+", ext: "+hIndex[5]);
		
		line = reader.readLine();
		if(line == null) {
			System.out.println("There is no data in log.csv file!!");
			return;
		}
		
		//to maintain the order of entry input
		Map<String,Node> keyMap = new LinkedHashMap<String,Node>();
		
		//first simplified date as ip,dateSt,webId
		String[] simpleLF = simpleLine(line, hIndex);
		//System.out.println("First simplified date: "+Arrays.toString(simpleLF));
		keyMap = setToMap(keyMap, simpleLF, p);
		
		//get date value to set as start date
		Date startDate = changeToDate(simpleLF[1]);
		//System.out.println("start date: "+startDate);
		
		while((line = reader.readLine())!=null) {
			
			//simplified date as ip,dateSt,webId
			String[] simpleL = simpleLine(line, hIndex);
			//System.out.println("simplified date: "+Arrays.toString(simpleL));
			Date tarDate = changeToDate(simpleL[1]);
			if(tarDate.equals(startDate)) {
				keyMap = setToMap(keyMap, simpleL, p);
			}
			else {
				while(!tarDate.equals(startDate)) {
					//eliminate sessions ended
					Iterator<Map.Entry<String,Node>> iter = keyMap.entrySet().iterator();
					while(iter.hasNext()) {
						Map.Entry<String, Node> entry = iter.next();
						Node curVal = entry.getValue();
						if(curVal.getRTime() == 0) {
							String[] outStArr = curVal.getInData();
							String outSt = outStArr[0]+","+curVal.getFirstDate()+","+outStArr[1]+","+curVal.getDuration()+","+curVal.getCount();
							sessionOut += outSt+"\n";
							//System.out.println(outSt);
							iter.remove();
						}
						else {//reduce time remained
							curVal = new Node(curVal.getFirstDate(),curVal.getRTime()-1,curVal.getCount(),curVal.getDuration(),curVal.getInData());
							entry.setValue(curVal);
						}
					}
					
					//increase time (1 second)
					startDate = new Date(startDate.getTime()+1000);
					
				}
				keyMap = setToMap(keyMap, simpleL, p);
			}
		}
		Iterator<Map.Entry<String,Node>> iter = keyMap.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<String, Node> entry = iter.next();
			Node curVal = entry.getValue();
			String[] outStArr = curVal.getInData();
			String outSt = outStArr[0]+","+curVal.getFirstDate()+","+outStArr[1]+","+curVal.getDuration()+","+curVal.getCount();
			sessionOut += outSt+"\n";
			//System.out.println(outSt);
		}
		reader.close();
		
		File fileout = new File("../output/sessionization.txt");
		FileOutputStream fileos = new FileOutputStream(fileout);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileos));
		bw.write(sessionOut);
		bw.close();
		
	
	}
}