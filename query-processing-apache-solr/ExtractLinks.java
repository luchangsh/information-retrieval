import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ExtractLinks {
	
	public static void main(String[] args) {
		String dirPath = "NYTimesData/NYTimesDownloadData";
		String mapfile = "NYTimesData/mapNYTimesDataFile.csv";
		String outfile = "edgeList.txt";
		
        try {
			FileReader reader = new FileReader(mapfile);
			reader.readFile();
			Map<String, String> fileUrlMap = reader.getFileUrlMap();
			Map<String, String> urlFileMap = reader.getUrlFileMap();
			PrintWriter writer = new PrintWriter(outfile);
			File dir = new File(dirPath);
			Set<String> edges = new HashSet<>();
			
            for (File file : dir.listFiles()) {
				Document doc = Jsoup.parse(file, "UTF-8", fileUrlMap.get(file.getName()));
				Elements links = doc.select("a[href]");
				
                for (Element link : links) {
					String url = link.attr("href").trim();
					if (urlFileMap.containsKey(url)) {
						edges.add(file.getName() + " " + urlFileMap.get(url));
					}
				}				
			}			
			
            for (String s : edges) {		
				writer.println(s);
			}			
			
            writer.flush();
			writer.close();
		} catch (FileNotFoundException exception) {
			System.out.println("Mapfile not found or Can't open output file.");
		} catch (IOException e) {
			System.out.println("Parse documents error.");
			e.printStackTrace();
		}		
	}
}
