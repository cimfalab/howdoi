package howdoi;

import java.io.OutputStream;
import java.util.List;

import org.jsoup.nodes.Document;

public interface Handler {
    boolean isQuestion(String link);

    String getAnswerQueryString();

    String getAnswer(String link, Document doc, OutputStream out) throws Exception;

    boolean useCustomFormat();

    String formatOutput(List<String> answers);
    String formatVisiting(int answerNumber, String link);
}
