import java.util.Set;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class MyCrawler extends WebCrawler {	
	private final static Pattern FILTERS = Pattern.compile(
		".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|m4v|rm|smil|wmv|swf|wma|zip|rar|gz))$"
	);
	
	private CrawlStat myCrawlStat;
	
	public MyCrawler() {
		myCrawlStat = new CrawlStat();
	}
	
	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();
		if (FILTERS.matcher(href).matches()) return false;
		return href.startsWith("https://www.nytimes.com/") || href.startsWith("http://www.nytimes.com/");
	}
	
	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();
		String contentType = page.getContentType();
		if (contentType.indexOf(';') != -1) contentType = contentType.substring(0, contentType.indexOf(';'));
		int size = page.getContentData().length;
		int numberOfOutlinks = 0;
		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			Set<WebURL> links = htmlParseData.getOutgoingUrls();
			for (WebURL link : links) {
				if (link.getURL().startsWith("https://www.nytimes.com/") ||
					link.getURL().startsWith("http://www.nytimes.com/")) { // New URL resides in the website
					myCrawlStat.addUrls(link.getURL().replace(',', '-') + ",OK");
				} else {												   // New URL points outside of the website
					myCrawlStat.addUrls(link.getURL().replace(',', '-') + ",N_OK");
				}
			}
			numberOfOutlinks = links.size();
		}
		myCrawlStat.addVisited(url + "," + size + "," + numberOfOutlinks + "," + contentType);
	}
	
	@Override
	public Object getMyLocalData() {
		return myCrawlStat;
	}
	
	@Override
	protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
		myCrawlStat.addFetched(webUrl.getURL() + "," + statusCode + " " + statusDescription);
	}
}
