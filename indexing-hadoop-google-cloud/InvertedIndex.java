import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class InvertedIndex {	
	public static class InvertedIndexMapper extends Mapper<LongWritable, Text, Text, Text> {
		private Text docId = new Text();
		private Text word = new Text();
		
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			StringTokenizer tokenizer = new StringTokenizer(value.toString());
			docId.set(tokenizer.nextToken());
			
			while (tokenizer.hasMoreTokens()) {
				word.set(tokenizer.nextToken());
				context.write(word, docId);
			}
		}		
	}
	
	public static class InvertedIndexReducer extends Reducer<Text, Text, Text, Text> {
		private Map<String, Integer> map = new HashMap<>();
		private StringBuilder sb = new StringBuilder();
		
		public void reduce(Text word, Iterable<Text> docIds, Context context) throws IOException, InterruptedException {
			for (Text id : docIds) {
				String docId = id.toString();
				int count = map.containsKey(docId) ? map.get(docId) : 0;
				map.put(docId, count + 1);
			}			
			for (Map.Entry<String, Integer> entry : map.entrySet()) {
				sb.append(entry.getKey());
				sb.append(':');
				sb.append(entry.getValue());
				sb.append('\t');
			}			
			
			context.write(word, new Text(sb.toString()));
			map.clear();
			sb.setLength(0);
		}
	}
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		
		Job job = Job.getInstance(conf, "inverted index");		
		job.setJarByClass(InvertedIndex.class);
		job.setMapperClass(InvertedIndexMapper.class);
		job.setReducerClass(InvertedIndexReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);		
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));		
		
		job.waitForCompletion(true);
	}
}
