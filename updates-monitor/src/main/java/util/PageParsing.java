package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PageParsing {

    public static List<String> exctractSectionsFromPage(Document document, String htmlTag, String mustContainText) throws IOException {
        List<String> result = new ArrayList<>();

        Elements scriptNodes = document.select(htmlTag);
        for (Element scriptNode : scriptNodes) {
            if (scriptNode.data().contains(mustContainText)) {
                String string = scriptNode.data();
                result.add(string);
            }
        }

        return result;
    }
}
