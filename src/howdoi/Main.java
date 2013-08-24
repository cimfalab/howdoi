package howdoi;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
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
        String link = null;
        for (int i = 0; i < links.size(); i++) {
            link = links.get(i);
            if (isQuestion(link)) {
                if (pos == 1)
                    break;
                else {
                    pos = pos - 1;
                    continue;
                }
            }
        }
        return link;
    }

    private String getAnswer(int pos, CommandLine cmd, List<String> links) throws Exception {
        String link = getLinkAtPos(links, pos);
        Document doc = getResult(link + "?answertab=votes");
        Element firstAnswer = doc.select(".answer").get(0);
        List<Element> instructions = add(firstAnswer.select("pre"), firstAnswer.select("code"));

        // TODO: the StackOverflow tags for color, --all argument
        String answer = "";
        if (instructions.isEmpty()) {
            answer = firstAnswer.select(".post-text").get(0).text();
        } else {
            answer = formatOutput(instructions.get(0).text());
        }
        if (answer == null || answer.isEmpty())
            answer = "< no answer given >";
        answer = answer.trim();
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

    private String getInstructions(CommandLine cmd, OutputStream out) throws Exception {
        String query = StringUtil.join(Arrays.asList(cmd.getArgs()), " ");
        print(out, "Searching answers for the query '" + query + "'...\n");
        List<String> links = getLinks(query);
        if (links.isEmpty())
            return "";
        int numAnswers = Integer.parseInt(cmd.getOptionValue("num-answers", "1"));
        List<String> answers = new ArrayList<String>();
        boolean appendHeader = numAnswers > 1;
        int initialPosition = Integer.parseInt(cmd.getOptionValue("pos", "1"));
        for (int answerNumber = 0; answerNumber < numAnswers; answerNumber++) {
            int currentPosition = answerNumber + initialPosition;
            String answer = getAnswer(currentPosition, cmd, links);
            if (answer == null)
                continue;
            if (appendHeader)
                answer = String.format("--- Answer %d -> %s\n%s", currentPosition, links.get(answerNumber), answer);
            answer += "\n";
            answers.add(answer);
        }
        return StringUtil.join(answers, "\n");
    }

    public static void run(String[] args, OutputStream out) throws Exception {
        Options options = new Options();
        options.addOption("p", "pos", true, "select answer in specified position (default: 1)");
        options.addOption("n", "num-answers", true, "number of answers to return");
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);
        
        if (cmd.getArgs().length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("howdoi", options);
            return;
        }
        Main main = new Main();
        String insturctions = main.getInstructions(cmd, out);
        if (insturctions == null || insturctions.isEmpty())
            insturctions = "Sorry, couldn\'t find any help with that topic\n";
        print(out, insturctions);
    }

    private static void print(OutputStream out, String s) throws Exception {
        out.write(s.getBytes("UTF-8"));
        out.flush();
    }

    public static void main(String[] args) throws Exception {
        Main.run(args, System.out);
    }
}
