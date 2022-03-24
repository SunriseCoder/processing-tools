package app.misc;

import java.io.IOException;
import java.util.Scanner;

import util.DownloadUtils;
import util.DownloadUtils.Response;
import utils.FileUtils;

public class DownloadUrlApp {

    public static void main(String[] args) throws IOException {
        System.out.print("Enter URL: ");
        Scanner scanner = new Scanner(System.in);
        String url = scanner.next();

        System.out.print("Enter filename to save: ");
        String filename = scanner.next();
        scanner.close();

        Response page = DownloadUtils.downloadPageByGet(url, null, null);
        FileUtils.saveToFile(page.body, filename);
        System.out.println("Done");
    }
}
