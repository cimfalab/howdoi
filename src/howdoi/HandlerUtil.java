package howdoi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HandlerUtil {
    public static List<Element> add(Elements e1, Elements e2) throws Exception {
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
}
