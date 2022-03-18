package app.misc;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.JsonNode;

import util.PageParsing;
import utils.JSONUtils;

public class PageParseScriptSections {

    public static void main(String[] args) throws Exception {
        Document document = Jsoup.parse(new File("test-data/youtube/watch-page.html"), "UTF-8");

        // Video Details
        String mustContainText = "var ytInitialPlayerResponse = ";
        //String textToFind = "First steps";
        //String mustContainText = "2016-08-15";
        String textToFind = "2016-08-15";

        // Media Formats
        //String mustContainText = "var ytInitialPlayerResponse = ";
        //String textToFind = "videoplayback";

        List<String> sections = PageParsing.exctractSectionsFromPage(document, "script", mustContainText);
        System.out.println("Parsed " + sections.size() + " section(s).");

        // Saving sections to disk
        try (PrintWriter pw = new PrintWriter("test-data/youtube/parsed-data.txt")) {
            pw.write("Total " + sections.size() + " section(s)\n");
            for (int i = 0; i < sections.size(); i++) {
                String section = sections.get(i);
                pw.write("=== Section " + i + " ===\n");
                pw.write(section);
                pw.write("\n\n");
            }
        }

        for (int i = 0; i < sections.size(); i++) {
            System.out.println("Search in section " + i + ":");
            String section = sections.get(i);
            String jsonString = JSONUtils.extractJsonSubstringFromString(section);
            JsonNode json = JSONUtils.parseJSON(jsonString);
            List<String> entryPaths = JSONUtils.findAllEntriesRecursively(json, textToFind);
            for (int j = 0; j < entryPaths.size(); j++) {
                System.out.println("\t" + entryPaths.get(j));
            }
        }
    }
}
