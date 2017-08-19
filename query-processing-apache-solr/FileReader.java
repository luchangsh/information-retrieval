import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;
import java.util.HashMap;

public class FileReader {
	private String filename;
	private Map<String, String> fileUrlMap;
	private Map<String, String> urlFileMap;
	
	public FileReader(String filename) {
		this.filename = filename;
		fileUrlMap = new HashMap<>();
		urlFileMap = new HashMap<>();
	}
	
	public void readFile() throws FileNotFoundException {
		File inFile = new File(filename);
		Scanner in = new Scanner(inFile);
		try {
			readData(in);
		} finally {
			in.close();
		}
	}
	
	public Map<String, String> getFileUrlMap() {
		return fileUrlMap;
	}

	public Map<String, String> getUrlFileMap() {
		return urlFileMap;
	}

	private void readData(Scanner in) {
		while (in.hasNextLine()) {
			String line = in.nextLine();
			String[] line_split = line.split(",");
			String file = line_split[0];
			String url = line_split[1];
			fileUrlMap.put(file, url);
			urlFileMap.put(url, file);
		}
	}
	
}
