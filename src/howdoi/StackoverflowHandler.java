package howdoi;

import java.io.OutputStream;
import java.util.List;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class StackoverflowHandler implements Handler {
    public boolean isQuestion(String link) {
        return link.matches(".*questions/\\d+/.*");
    }

    public String getAnswerQueryString() {
        return "?answertab=votes";
    }

    public String getAnswer(String link, Document doc, OutputStream out) throws Exception {
        String answer = "";
        Elements elements = doc.select(".answer");
        if (elements.size() > 0) {
            Element firstAnswer = elements.get(0);
            List<Element> instructions = HandlerUtil.add(firstAnswer.select("pre"), firstAnswer.select("code"));

            // TODO: the StackOverflow tags for color, --all argument
            if (instructions.isEmpty()) {
                answer = firstAnswer.select(".post-text").get(0).text();
            } else {
                answer = formatOutput(instructions.get(0).text());
            }
        }
        return answer;
    }

    private String formatOutput(String text) {
        return text;
    }

    public boolean useCustomFormat() {
        return false;
    }

    public String formatOutput(List<String> answers) {
        return StringUtil.join(answers, "\n");
    }

    public String formatVisiting(int answerNumber, String link) {
        return String.format(" Visiting... [%d]\n", answerNumber + 1);
    }
}
