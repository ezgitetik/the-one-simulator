package custom.predictionclient;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;

public class BasePredictionClient {

    private static final String HOST = "http://localhost:9091/";
    private String url;
    private HttpPost httpPost;

    public BasePredictionClient(String url) {
        this.url = url;
        setLogLevel();
    }

    public String getPrediction(List<Integer> sequence) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        if (this.httpPost == null) {
            this.httpPost = getHttpPostClient();
        }

        String json = "[31,35,14]";
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);

        CloseableHttpResponse response = client.execute(httpPost);
        HttpEntity httpEntity = response.getEntity();
        String content = EntityUtils.toString(httpEntity);
        client.close();
        return content;
    }

    private HttpPost getHttpPostClient() {
        HttpPost httpPost = new HttpPost(HOST + this.url);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        return httpPost;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private static void setLogLevel(){
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");
    }
}
