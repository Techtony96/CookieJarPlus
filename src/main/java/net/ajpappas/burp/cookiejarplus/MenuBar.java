package net.ajpappas.burp.cookiejarplus;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.ui.menu.BasicMenuItem;
import burp.api.montoya.ui.menu.Menu;

import java.awt.Frame;
import java.awt.FileDialog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuBar implements BurpExtension {

    private static final DateTimeFormatter EXP_FORMAT = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz uuuu");
    
    @Override
    public void initialize(MontoyaApi api)
    {
        api.logging().logToOutput("CookieJarPlus is loading...");

        api.extension().setName("CookieJarPlus");

        BasicMenuItem exportCookies = BasicMenuItem.basicMenuItem("Export Cookies")
                .withAction(() -> exportCookies(api));
        BasicMenuItem importCookies = BasicMenuItem.basicMenuItem("Import Cookies")
                .withAction(() -> importCookies(api));


        Menu menu = Menu.menu("Cookie Jar Plus").withMenuItems(exportCookies, importCookies);

        api.userInterface().menuBar().registerMenu(menu);

    }

    private void exportCookies(MontoyaApi api) {
        // Get all cookies to export
        String cookies = api.http().cookieJar().cookies().stream()
                .map(this::formatCookie)
                .collect(Collectors.joining("\n"));

        // Select a file
        FileDialog dialog = new FileDialog((Frame)null, "Select File to Save As");
        dialog.setMode(FileDialog.SAVE);
        dialog.setFile("cookies.txt");
        dialog.setVisible(true);
        String file = dialog.getFile();
        String path = dialog.getDirectory();
        dialog.dispose();
        api.logging().logToOutput("Export File: " + path + "/" + file);

        if (file == null) // User clicked cancel
            return;

        try {
            Files.writeString(Path.of(path, file), cookies);
        } catch (Exception e) {
            api.logging().logToError("Unable to export cookies: " + e.getMessage());
        }
    }

    private void importCookies(MontoyaApi api) {
        // Select a file
        FileDialog dialog = new FileDialog((Frame)null, "Select File to Open");
        dialog.setMode(FileDialog.LOAD);
        dialog.setVisible(true);
        String file = dialog.getFile();
        String path = dialog.getDirectory();
        dialog.dispose();
        api.logging().logToOutput("Import File: " + path + "/" + file);

        if (file == null) // User clicked cancel
            return;

        // Get file contents
        try (Stream<String> cookies = Files.lines(Paths.get(path + "/" + file))) {
            cookies.map(s -> s.split("\t"))
                    .forEach(cookie -> addCookie(api, cookie[2], cookie[3], cookie[1], cookie[0], ZonedDateTime.now().plusYears(1)));
        } catch (IOException e) {
            api.logging().logToError("Exception while reading file " + file);
        }
    }

    private void addCookie(MontoyaApi api, String name, String value, String path, String domain, ZonedDateTime expiration) {
        api.logging().logToOutput("Loading cookie " + name);
        api.http().cookieJar().setCookie(name, value, path, domain, expiration);
    }

    private String formatCookie(Cookie c) {
        return String.format("%s\t%s\t%s\t%s\t%s", c.domain(), c.path(), c.name(), c.value(), c.expiration().map(EXP_FORMAT::format).orElse(""));
    }
}
