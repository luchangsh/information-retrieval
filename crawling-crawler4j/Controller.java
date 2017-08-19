import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Controller {
	private static final boolean DEBUG = true;
	
	public static void main(String[] args) throws Exception {
		String crawlStorageFolder = "data/crawl";
		int numberOfCrawlers = 7;
		
		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(crawlStorageFolder);
		config.setMaxPagesToFetch(20000);
		config.setMaxDepthOfCrawling(16);
		config.setIncludeBinaryContentInCrawling(true);
		
		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
		
		controller.addSeed("http://www.nytimes.com/");
		controller.start(MyCrawler.class, numberOfCrawlers);
		
		List<Object> crawlersLocalData = controller.getCrawlersLocalData();
		List<String> fetched = new ArrayList<String>();
		List<String> visited = new ArrayList<String>();
		List<String> urls = new ArrayList<String>();
		
		for (Object localData : crawlersLocalData) {
			CrawlStat stat = (CrawlStat) localData;
			for (String s : stat.getFetched()) {
				fetched.add(s);
			}
			for (String s : stat.getVisited()) {
				visited.add(s);
			}
			for (String s : stat.getUrls()) {
				urls.add(s);
			}			
		}
		
		try {
			generateCSVFile(fetched, "fetch");
			generateCSVFile(visited, "visit");
			generateCSVFile(urls, "urls");
		} catch (FileNotFoundException e) {
			System.out.println("Generating CSV files failed.");
			e.printStackTrace();
		}
		
		try {
			generateCrawlReport(fetched, visited, urls);
		} catch (FileNotFoundException e) {
			System.out.println("Generating Crawl Report failed.");
			e.printStackTrace();
		}		
	}
	
	private static void generateCSVFile(List<String> list, String fileName) throws FileNotFoundException {
		PrintWriter out = new PrintWriter("data/" + fileName + "_NYTimes.csv");
		try {
			for (String s : list) {
				out.println(s);
			}			
		} finally {
			out.close();
		}
	}
	
	private static void generateCrawlReport(List<String> fetched, List<String> visited, List<String> urls) throws FileNotFoundException {
		List<String[]> fetched_parsed = parseCSVFile(fetched);
		List<String[]> visited_parsed = parseCSVFile(visited);
		List<String[]> urls_parsed = parseCSVFile(urls);
		
		int[] fetchStat = countFetchStat(fetched_parsed);
		int[] outgoingUrlStat = countOutgoingURLsStat(urls_parsed);

		Map<String, Integer> statusCodeStat = countStatusCodesStat(fetched_parsed);
		List<String> statuses = new ArrayList<>();
		for (String k : statusCodeStat.keySet()) {
			statuses.add(k);
		}
		Collections.sort(statuses);
		
		int[] sizeStat = countFileSizeStat(visited_parsed);
		
		Map<String, Integer> contentTypeStat = countContentTypeStat(visited_parsed);
		List<String> contents = new ArrayList<>();
		for (String k : contentTypeStat.keySet()) {
			contents.add(k);
		}
		Collections.sort(contents, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o2.compareTo(o1);
			}			
		});
		
		PrintWriter out = new PrintWriter("data/CrawlReport_NYTimes.txt");
		try {			
			out.println("Fetch Statistics");
			out.println("================");
			out.println("# fetches attempted: " + fetchStat[0]);
			out.println("# fetches succeeded: " + fetchStat[1]);
			out.println("# fetches aborted: " + fetchStat[2]);
			out.println("# fetches failed: " + fetchStat[3]);
			if (DEBUG) {
				int tmp = fetchStat[1] + fetchStat[2] + fetchStat[3];
				out.println(fetchStat[0] + " == " + tmp);
			}
			out.println();
			
			out.println("Outgoing URLs:");
			out.println("==============");
			out.println("Total URLs extracted: " + outgoingUrlStat[0]);
			out.println("# unique URLs extracted: " + outgoingUrlStat[1]);
			out.println("# unique URLs within News Site: " + outgoingUrlStat[2]);
			out.println("# unique URLs outside News Site: " + outgoingUrlStat[3]);
			if (DEBUG) {
				int tmp = outgoingUrlStat[2] + outgoingUrlStat[3];
				out.println(outgoingUrlStat[1] + " == " + tmp);
			}
			out.println();
			
			out.println("Status Codes:");
			out.println("=============");
			for (String s : statuses) {
				out.println(s + ": " + statusCodeStat.get(s));
			}
			if (DEBUG) {
				int cnt = 0;
				for (String s : statuses) {
					cnt += statusCodeStat.get(s);
					if (s.startsWith("200")) {
						out.println(fetchStat[1] + " == " + statusCodeStat.get(s));
					}
				}
				out.println(fetchStat[0] + " == " + cnt);
			}
			out.println();
			
			out.println("File Sizes:");
			out.println("===========");
			out.println("< 1KB: " + sizeStat[0]);
			out.println("1KB ~ <10KB: " + sizeStat[1]);
			out.println("10KB ~ <100KB: " + sizeStat[2]);
			out.println("100KB ~ <1MB: " + sizeStat[3]);
			out.println(">= 1MB: " + sizeStat[4]);
			if (DEBUG) {
				int tmp = sizeStat[0] + sizeStat[1] + sizeStat[2] + sizeStat[3] + sizeStat[4];
				out.println(fetchStat[1] + " == " + tmp);
			}
			out.println();
			
			out.println("Content Types:");
			out.println("==============");
			for (String c : contents) {
				out.println(c + ": " + contentTypeStat.get(c));
			}
			if (DEBUG) {
				int cnt = 0;
				for (String c : contents) {
					cnt += contentTypeStat.get(c);
				}
				out.println(fetchStat[1] + " == " + cnt);
			}
		} finally {
			out.close();
		}
	}
	
	private static List<String[]> parseCSVFile(List<String> list) {
		List<String[]> res = new ArrayList<>();
		if (list == null || list.size() == 0) return res;
		for (String s : list) {
			res.add(s.split(","));
		}
		return res;
	}
	
	private static int[] countFetchStat(List<String[]> list) {
		int[] res = new int[4];
		if (list == null || list.size() == 0) return res;
		for (String[] s : list) {
			res[0]++;										// res[0]: attempted
			char c = s[1].charAt(0);
			if ( c == '2') {								// res[1]: succeeded
				res[1]++;
			} else if (c == '4' || c == '5') {  			// res[3]: failed
				res[3]++;
			} else {										// res[2]: aborted
				res[2]++;
			}
		}
		return res;
	}
	
	// Calculate the information needed for the outgoing URLs section in the CrawlReport
	private static int[] countOutgoingURLsStat(List<String[]> list) {
		int[] res = new int[4];
		if (list == null || list.size() == 0) return res;
		res[0] = list.size();							// res[0]: total URLs
		Set<String> uniqueUrls = new HashSet<>();
		for (String[] s : list) {						// s[0]: URLs, s[1]: OK or N_OK
			if (uniqueUrls.add(s[0])) {					// add() return true if successful 
				res[1]++;								// res[1]: unique URLs
				if (s[1].equals("OK")) {				
					res[2]++;							// res[2]: within URLs
				} else if (s[1].equals("N_OK")) {		
					res[3]++;							// res[3]: outside URLs
				}
			}
		}		
		return res;
	}

	private static Map<String, Integer> countStatusCodesStat(List<String[]> list) {
		Map<String, Integer> map = new HashMap<>();
		if (list == null || list.size() == 0) return map;
		for (String[] s : list) {
			int count = map.getOrDefault(s[1], 0);
			map.put(s[1], count + 1);
		}
		return map;
	}
	
	private static int[] countFileSizeStat(List<String[]> list) {
		int[] res = new int[5];
		if (list == null || list.size() == 0) return res;
		for (String[] s : list) {
			double val = Integer.parseInt(s[1]) / 1024.0; // 1KB == 1024bytes
			if (val < 1) {				// < 1KB
				res[0]++;
			} else if (val < 10) {		// 1KB ~ <10KB
				res[1]++;
			} else if (val < 100) {		// 10KB ~ <100KB
				res[2]++;
			} else if (val < 1024) {	// 100KB ~ <1MB (1MB == 1024KB)
				res[3]++;
			} else {					// >= 1MB
				res[4]++;
			}
		}
		return res;
	}
	
	private static Map<String, Integer> countContentTypeStat(List<String[]> list) {
		Map<String, Integer> map = new HashMap<>();
		if (list == null || list.size() == 0) return map;
		for (String[] s : list) {
			int count = map.getOrDefault(s[3], 0);
			map.put(s[3], count + 1);
		}
		return map;
	}
}
