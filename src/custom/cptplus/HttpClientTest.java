package custom.cptplus;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import java.io.IOException;

public class HttpClientTest {
    public static void main(String[] args) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:9091/akom/");

        String json = "[31,35,14]";
        StringEntity entity = new StringEntity(json);
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        double startTime=System.currentTimeMillis();
        CloseableHttpResponse response = client.execute(httpPost);
        HttpEntity httpEntity =response.getEntity();
        client.close();
        double endTime=System.currentTimeMillis();
        System.out.println("delay: "+(endTime-startTime));
    }
}
