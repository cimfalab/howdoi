package howdoi;

import org.jsoup.nodes.Document;

public interface Handler {
    boolean isQuestion(String link);

    String getAnswerQueryString();

    String getAnswer(String link, Document doc) throws Exception;
}
