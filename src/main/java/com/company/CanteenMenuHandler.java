package com.company;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CanteenMenuHandler {
    private String url;
    private Pattern pattern;
    private List<String> hauptgerichte, hauptgerichteSelbstentnahme, frontcookings, hauptgaenge;
    private int latestWeekOfYear;

    public CanteenMenuHandler(String url, int weekOfYear) throws IOException {
        this.url = url;
        this.latestWeekOfYear = weekOfYear;

        pattern = Pattern.compile("([a-zA-Zäöüß, ´()-]+[ ]?(\\r\\n|\\r|\\n))+(\\d,\\d\\d[ ]?€[ ]?(\\r\\n|\\r|\\n)?)+");

        hauptgerichte = new ArrayList<>();
        hauptgerichteSelbstentnahme = new ArrayList<>();
        frontcookings = new ArrayList<>();
        hauptgaenge = new ArrayList<>();

        load();
    }

    private void load() throws IOException {
        URL file = new URL(url);

        PDDocument document = PDDocument.load(file.openStream());
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        pdfTextStripper.setStartPage(1);
        pdfTextStripper.setEndPage(1);
        String text = pdfTextStripper.getText(document).trim().replaceAll(" +", " ");

        document.close();

        extractHauptgerichte(text);
        extractHauptgerichteSelbstentnahme(text);
        extractFrontcookings(text);
        extractHauptgaenge(text);
    }

    public boolean isRecent(int currentWeekOfYear) {
        return currentWeekOfYear == latestWeekOfYear;
    }

    public List<String> getMenuOfDay(DayOfWeek weekDay) {
        int index = weekDay.ordinal();

        List<String> menu = new ArrayList<>();

        if (index < hauptgerichte.size()) {
            menu.add(hauptgerichte.get(index));
        }

        if (index < hauptgerichteSelbstentnahme.size()) {
            menu.add(hauptgerichteSelbstentnahme.get(index));
        }

        if (index < frontcookings.size()) {
            menu.add(frontcookings.get(index));
        }

        if (index < hauptgaenge.size()) {
            menu.add(hauptgaenge.get(index));
        }

        return menu;
    }

    private void extractHauptgerichte(String pdfText) {
        String contentString = pdfText.split("Hauptgericht", 2)[1].split("Zusatzstoffe und Allergene", 2)[0].trim();

        Matcher matcher = pattern.matcher(contentString);

        while (matcher.find()) {
            hauptgerichte.add(cleanString(matcher.group()));
        }
    }

    private void extractHauptgerichteSelbstentnahme(String pdfText) {
        String contentString = pdfText.split("Hauptgericht zur ", 2)[1].split("Zusatzstoffe und Allergene", 2)[0].trim().replace("Selbstentnahme ", "");

        Matcher matcher = pattern.matcher(contentString);

        while (matcher.find()) {
            hauptgerichteSelbstentnahme.add(cleanString(matcher.group()));
        }
    }

    private void extractFrontcookings(String pdfText) {
        String contentString = pdfText.split("Frontcooking", 2)[1].split("Zusatzstoffe und Allergene", 2)[0].trim();

        Matcher matcher = pattern.matcher(contentString);

        while (matcher.find()) {
            frontcookings.add(cleanString(matcher.group()));
        }
    }

    private void extractHauptgaenge(String pdfText) {
        String contentString = pdfText.split("Hauptgang", 2)[1].split("Zusatzstoffe und Allergene", 2)[0].trim();

        Matcher matcher = pattern.matcher(contentString);

        while (matcher.find()) {
            hauptgaenge.add(cleanString(matcher.group()));
        }
    }

    private String cleanString(String string) {
        return string.replace("\n", " ").replace("\r", " ").trim().replaceAll(" +", " ");
    }

    enum WeekDay {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY
    }
}
