package net.ajpappas.burp.cookiejarplus;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.menu.BasicMenuItem;
import burp.api.montoya.ui.menu.Menu;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

public class MenuBar implements BurpExtension {
    
    @Override
    public void initialize(MontoyaApi api)
    {
        api.logging().logToOutput("CookieJarPlus is loading...");

        api.extension().setName("CookieJarPlus");

        BasicMenuItem loadCookies = BasicMenuItem.basicMenuItem("Load Cookies")
                .withAction(() -> loadCookies(api));


        Menu menu = Menu.menu("Cookie Jar Plus").withMenuItems(loadCookies);

        api.userInterface().menuBar().registerMenu(menu);

    }

    private void loadCookies(MontoyaApi api) {
        // Select a file
        FileDialog dialog = new FileDialog((Frame)null, "Select File to Open");
        dialog.setMode(FileDialog.LOAD);
        dialog.setVisible(true);
        String file = dialog.getFile();
        String path = dialog.getDirectory();
        dialog.dispose();
        api.logging().logToOutput("File: " + path + "/" + file);

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
}
