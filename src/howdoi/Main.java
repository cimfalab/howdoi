package howdoi;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
    Handler handler;
    static Scheme https;

    private synchronized static void initializeSSL() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }
                
                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }
                
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{ tm }, null);
            SSLSocketFactory sslSocketFactory = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            https = new Scheme("https", 443, sslSocketFactory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> getLinks(String site, String query) throws Exception {
        List<String> list = new ArrayList<String>();

        String q = "http://www.google.com/search?q=site:" + site + "%20";
        Document doc = getResult(q + URLEncoder.encode(query, "UTF-8"));
        Elements elements = doc.select(".l");
        Iterator<Element> iterator = elements.iterator();
        while (iterator.hasNext()) {
            String href = iterator.next().attr("href");
            list.add(href);
        }
        elements = doc.select(".r a");
        iterator = elements.iterator();
        while (iterator.hasNext()) {
            String href = iterator.next().attr("href");
            list.add(href);
        }
        return list;
    }

    private String getLinkAtPos(List<String> links, int pos) {
        String link = null;
        for (int i = 0; i < links.size(); i++) {
            if (handler.isQuestion(links.get(i))) {
                if (pos == 1) {
                    link = links.get(i);
                    break;
                } else {
                    pos = pos - 1;
                    continue;
                }
            }
        }
        return link;
    }

    private String getAnswer(int pos, CommandLine cmd, List<String> links, OutputStream out) throws Exception {
        String link = getLinkAtPos(links, pos);
        if (link == null) {
            print(out, "There is no corresponding question link\n");
            return "";
        }
        //print(out, "Getting answer from " + (link + handler.getAnswerQueryString()) + "...\n");
        Document doc = getResult(link + handler.getAnswerQueryString());
        String answer = handler.getAnswer(link, doc, out);

        if (answer == null || answer.isEmpty())
            answer = "< no answer given >";
        answer = answer.trim();
        return answer;
    }

    private Document getResult(String url) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();

        // javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
        if (url.startsWith("https")) {
            if (https == null)
                initializeSSL();
            ClientConnectionManager ccm = httpclient.getConnectionManager();
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(https);
        }

        URIBuilder builder = new URIBuilder(url);
        /*
        builder.setScheme("http").setHost("www.google.com").setPath("/search")
            .setParameter("q", "site:stackoverflow.com " + "format date bash");
        */
        URI uri = builder.build();
        HttpGet request = new HttpGet(uri);
        // If User-Agent is not set, the href attribute is prepended like "/url?q=http://stackoverflow.com/questions/1401482/yyyy-mm-dd-format-date-in-shell-script".
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows; Windows NT 6.1) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.46 Safari/536.5");

        HttpResponse response = httpclient.execute(request);
        try {
            HttpEntity entity = response.getEntity();
            byte[] bytes = EntityUtils.toByteArray(entity);
            EntityUtils.consume(entity);
            return Jsoup.parse(new ByteArrayInputStream(bytes), "UTF-8", "");
        } finally {
            request.releaseConnection();
        }
    }

    private String getInstructions(CommandLine cmd, OutputStream out) throws Exception {
        String site = cmd.getOptionValue("site", "stackoverflow.com");
        String query = StringUtil.join(Arrays.asList(cmd.getArgs()), " ");
        print(out, String.format("Searching answers for the query '%s' in %s...\n", query, site));
        List<String> links = getLinks(site, query);
        if (links.isEmpty())
            return "";
        int numAnswers = Integer.parseInt(cmd.getOptionValue("num-answers", "1"));
        print(out, String.format("Got %d links. Starting to visit each link... (Max %d links)\n", links.size(), numAnswers));
        List<String> answers = new ArrayList<String>();
        boolean appendHeader = numAnswers > 1;
        int initialPosition = Integer.parseInt(cmd.getOptionValue("pos", "1"));
        for (int answerNumber = 0; answerNumber < numAnswers; answerNumber++) {
            int currentPosition = answerNumber + initialPosition;
            String link = links.get(answerNumber);
            print(out, String.format(" Visiting... (%d)\n", answerNumber));
            String answer = getAnswer(currentPosition, cmd, links, out);
            if (answer == null)
                continue;
            if (!handler.useCustomFormat()) {
                if (appendHeader)
                    answer = String.format("--- Answer %d -> %s\n%s", currentPosition, link, answer);
            }
            answer += "\n";
            answers.add(answer);
        }
        return handler.formatOutput(answers);
    }

    public void run(String[] args, OutputStream out) throws Exception {
        run(args, new StackoverflowHandler(), out);
    }

    public void run(String[] args, Handler handler, OutputStream out) throws Exception {
        this.handler = handler;
        if (this.handler == null) {
            this.handler = new StackoverflowHandler();
        }

        Options options = new Options();
        options.addOption("p", "pos", true, "select answer in specified position (default: 1)");
        options.addOption("n", "num-answers", true, "number of answers to return");
        options.addOption("site", true, "site to be queried");
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.getArgs().length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("howdoi", options);
            return;
        }
        String insturctions = getInstructions(cmd, out);
        if (insturctions == null || insturctions.isEmpty())
            insturctions = "Sorry, couldn\'t find any help with that topic\n";
        print(out, insturctions);
    }

    private static void print(OutputStream out, String s) throws Exception {
        out.write(s.getBytes("UTF-8"));
        out.flush();
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.run(args, System.out);
    }
}
