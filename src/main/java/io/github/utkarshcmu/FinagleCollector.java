package io.github.utkarshcmu;

import io.prometheus.client.Collector;

import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class FinagleCollector extends Collector {
    private static final Logger LOGGER = Logger.getLogger(FinagleCollector.class.getName());
    
    private String metricsUrl;
    private String histogramsUrl;
    
    public FinagleCollector(String host, int port) {
		this.metricsUrl = "http://" + host + ":" + port + "/admin/metrics.json";
		this.histogramsUrl = "http://" + host + ":" + port + "/admin/histograms.json";
    	// For local testing:
		//this.metricsUrl = "http://localhost:3000/response";
		//this.histogramsUrl = "http://localhost:3000/response2";
	}

    private String transformMetricName(String s) {
    	// Converts the metric names to lower case and use underscores
    	return s.replaceAll("/", "__").replaceAll("\\.","_").toLowerCase();
    }
    
    private JSONObject getResponse(String url) throws IOException, ParseException {
    	JSONParser jsonParser = new JSONParser();
    	URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        JSONObject jsonObject = (JSONObject)jsonParser.parse(new InputStreamReader(con.getInputStream(), "UTF-8"));      
        con.disconnect();
        return jsonObject;
    }
    
    private void scrape(List<MetricFamilySamples> mfs) throws CloneNotSupportedException, IOException, ParseException {
      
      JSONObject metricsJsonObject = this.getResponse(this.metricsUrl);
      for (Object key : metricsJsonObject.keySet()) {
          String keyStr = (String) key;
          double keyValue = new Double(metricsJsonObject.get(keyStr).toString());
          List<String> labelNames = new ArrayList<String>();
          List<String> labelValues = new ArrayList<String>();
          labelNames.add("original_key");
          labelValues.add(keyStr);
          String metricName = transformMetricName(keyStr);
          List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>();
          samples.add(new MetricFamilySamples.Sample(metricName, labelNames, labelValues, keyValue));
          mfs.add(new MetricFamilySamples(metricName, Type.GAUGE, keyStr, samples));
      }

      JSONObject histJsonObject = this.getResponse(this.histogramsUrl);
      for (Object key : histJsonObject.keySet()) {    	  
    	  String keyStr = (String) key;
    	  System.out.println("key: " + keyStr);
    	  String bucketName = transformMetricName(keyStr) + "_bucket";
    	  String counterName = transformMetricName(keyStr) + "_count";
    	  JSONArray valArray = (JSONArray) histJsonObject.get(keyStr);
    	  double le = 0;
    	  double count = 0;
    	  List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>();
    	  System.out.println("values: " + valArray.size());
    	  for (int i = 0; i <= valArray.size(); i++) {
    		  List<String> labelNames = new ArrayList<String>();
              List<String> labelValues = new ArrayList<String>();
              labelNames.add("original_key");
              labelValues.add(keyStr);
              if (i < valArray.size()) {
            	  JSONObject bracket = (JSONObject) valArray.get(i);
        		  le = new Double(bracket.get("upperLimit").toString());
        		  labelNames.add("le");
                  labelValues.add(Double.toString(le));
        		  count += new Double(bracket.get("count").toString());
        		  samples.add(new MetricFamilySamples.Sample(bucketName, labelNames, labelValues, count));  
              } else {
            	  labelNames.add("le");
                  labelValues.add("+Inf");
                  samples.add(new MetricFamilySamples.Sample(bucketName, labelNames, labelValues, count));
              }		  
    	  }
    	  List<String> labelNames = new ArrayList<String>();
          List<String> labelValues = new ArrayList<String>();
          labelNames.add("original_key");
          labelValues.add(keyStr);
    	  samples.add(new MetricFamilySamples.Sample(counterName, labelNames, labelValues, count));
    	  mfs.add(new MetricFamilySamples(bucketName, Type.HISTOGRAM, keyStr, samples));
      }
      
    }

    public List<MetricFamilySamples> collect() {
      long start = System.nanoTime();
      double error = 0;
      List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
      try {
        scrape(mfs);
      } catch (Exception e) {
        error = 1;
        LOGGER.log(Level.WARNING, "Finagle scrape failed", e);
      }
      List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>();
      samples.add(new MetricFamilySamples.Sample(
          "finagle_exporter_scrape_duration_seconds", new ArrayList<String>(), new ArrayList<String>(), (System.nanoTime() - start) / 1.0E9));
      mfs.add(new MetricFamilySamples("finagle_exporter_scrape_duration_seconds", Type.GAUGE, "Time this Finagle scrape took, in seconds.", samples));

      samples = new ArrayList<MetricFamilySamples.Sample>();
      samples.add(new MetricFamilySamples.Sample(
          "finagle_exporter_scrape_error", new ArrayList<String>(), new ArrayList<String>(), error));
      mfs.add(new MetricFamilySamples("finagle_exporter_scrape_error", Type.GAUGE, "Non-zero if this scrape failed.", samples));
      return mfs;
    }

    /**
     * Convenience function to run standalone.
     * host is args[0]
     * port is args[1]
     */
    public static void main(String[] args) throws Exception {
      String host = "localhost";
      int port = 9990;
      if (args.length > 0) {
        host = args[0];
      } else if (args.length > 1) {
    	host = args[0];
    	port = Integer.parseInt(args[1]);
      }
      FinagleCollector fc = new FinagleCollector(host, port);
      for(MetricFamilySamples mfs : fc.collect()) {
        System.out.println(mfs);
      }
    }
}

