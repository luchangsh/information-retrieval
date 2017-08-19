import java.util.ArrayList;
import java.util.List;

public class CrawlStat {
	private List<String> fetched;
	private List<String> visited;
	private List<String> urls;
	
	public CrawlStat() {
		fetched = new ArrayList<>();
		visited = new ArrayList<>();
		urls = new ArrayList<>();
	}

	public List<String> getFetched() {
		return fetched;
	}

	public void setFetched(List<String> fetched) {
		this.fetched = fetched;
	}
	
	public void addFetched(String s) {
		fetched.add(s);
	}

	public List<String> getVisited() {
		return visited;
	}

	public void setVisited(List<String> visited) {
		this.visited = visited;
	}
	
	public void addVisited(String s) {
		visited.add(s);
	}

	public List<String> getUrls() {
		return urls;
	}

	public void setUrls(List<String> urls) {
		this.urls = urls;
	}
	
	public void addUrls(String s) {
		urls.add(s);
	}
}
