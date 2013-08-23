package howdoi;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
    final static String SEARCH_URL = "http://www.google.com/search?q=site:stackoverflow.com%20";

    private List<Element> add(Elements e1, Elements e2) throws Exception {
        List<Element> list = new ArrayList<Element>();

        Iterator<Element> iterator = e1.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        iterator = e2.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    private List<String> getLinks(String query) throws Exception {
        List<String> list = new ArrayList<String>();

        Document doc = getResult(SEARCH_URL + URLEncoder.encode(query, "UTF-8"));
        Elements elements = doc.select(".l");
        Iterator<Element> iterator = elements.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next().attr("href"));
        }
        elements = doc.select(".r a");
        iterator = elements.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next().attr("href"));
        }
        return list;
    }

    private boolean isQuestion(String link) {
        return link.matches(".*questions/\\d+/.*");
    }

    private String getLinkAtPos(List<String> links, int pos) {
        String link = links.get(pos);
        if (isQuestion(link)) {
            return link;
        }
        return null;
    }

    private String getAnswer(int num, List<String> links) throws Exception {
        String link = getLinkAtPos(links, num);
        Document doc = getResult(link + "?answertab=votes");
        Element firstAnswer = doc.select(".answer").get(0);
        List<Element> instructions = add(firstAnswer.select("pre"), firstAnswer.select("code"));

        // TODO: the StackOverflow tags
        // args['tags'] = [t.text for t in html('.post-tag')]
        // TODO: args all
        String answer = formatOutput(instructions.get(0).text());
        
        return answer;
    }

    private String formatOutput(String text) {
        return text;
    }

    private Document getResult(String url) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        URIBuilder builder = new URIBuilder(url);
        /*
        builder.setScheme("http").setHost("www.google.com").setPath("/search")
            .setParameter("q", "site:stackoverflow.com " + "format date bash");
        */
        URI uri = builder.build();
        //System.out.println("uri - " + uri);
        HttpGet request = new HttpGet(uri);
        // If User-Agent is not set, the href attribute is prepended like "/url?q=http://stackoverflow.com/questions/1401482/yyyy-mm-dd-format-date-in-shell-script".
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows; Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");
        
        HttpResponse response = httpclient.execute(request);
        try {
            //System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            byte[] bytes = EntityUtils.toByteArray(entity);
            String html = new String(bytes, "UTF-8");
            //System.out.println("response [" + html + "]");
            EntityUtils.consume(entity);
            return Jsoup.parse(new ByteArrayInputStream(bytes), "UTF-8", "");
        } finally {
            request.releaseConnection();
        }
    }

    private void run() throws Exception {
        List<String> links = getLinks("format date bash");
        if (links.isEmpty())
            return;
        String answer = getAnswer(0, links);
        System.out.println(answer);
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.run();
    }
}
