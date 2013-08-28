package howdoi;

import java.util.List;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class StackoverflowHandler implements Handler {
    public boolean isQuestion(String link) {
        return link.matches(".*questions/\\d+/.*");
    }

    public String getAnswerQueryString() {
        return "?answertab=votes";
    }

    public String getAnswer(String link, Document doc) throws Exception {
        Element firstAnswer = doc.select(".answer").get(0);
        List<Element> instructions = HandlerUtil.add(firstAnswer.select("pre"), firstAnswer.select("code"));

        String answer = "";
        // TODO: the StackOverflow tags for color, --all argument
        if (instructions.isEmpty()) {
            answer = firstAnswer.select(".post-text").get(0).text();
        } else {
            answer = formatOutput(instructions.get(0).text());
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
}
