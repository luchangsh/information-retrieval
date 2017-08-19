import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.tika.exception.TikaException;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

@SuppressWarnings("deprecation")
public class ExtractContent {
	public static void main(String[] args) throws IOException,SAXException, TikaException {
		String dirPath = "C:/NYTimesData/NYTimesDownloadData"; //Input file directory
		String outfile = "big.txt"; //Output file name
		
        try {
			PrintWriter writer = new PrintWriter(outfile);
			File dir = new File(dirPath);
			
            for (File file : dir.listFiles()) {
				AutoDetectParser parser = new AutoDetectParser();
			    BodyContentHandler handler = new BodyContentHandler(-1);
			    Metadata metadata = new Metadata();
			    FileInputStream stream = new FileInputStream(file);
			    
                try {
			    	parser.parse(stream, handler, metadata);	
			    } finally {
			    	stream.close();
			    }
			    
                String str = handler.toString();
		        LanguageIdentifier identifier = new LanguageIdentifier(str); //Determine the language
		        String language = identifier.getLanguage();
		        
                if (language.equals("en")) { //Only process English pages
				    String[] words = str.split("\\s+");
			        
                    for (String word : words)
			        	writer.print(word + " ");
		        }
			}
            
			writer.flush();
			writer.close();
		} catch (FileNotFoundException exception) {
			System.out.println("Can't open output file.");
		} catch (IOException e) {
			System.out.println("Parse documents error.");
			e.printStackTrace();
		}			
	}	
}
