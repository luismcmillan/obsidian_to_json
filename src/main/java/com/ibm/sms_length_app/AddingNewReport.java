package com.ibm.sms_length_app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class AddingNewReport {
    public static void main(String[] args) {
        String path = "/Users/luis/Documents/My_obsidian/";
        HashMap<String, ArrayList<String>> children_list = new HashMap<String, ArrayList<String>>();
        ConcurrentSkipListSet<String> obsidian_ordners = new ConcurrentSkipListSet<String>();
        LinkedList<String> general_structure = new LinkedList<String>();
        Map<String, String> obsidian_elements = new ConcurrentHashMap<>();
        ArrayList<String> already_parsed = new ArrayList<String>();
        already_parsed.add(".DS_Store");
        already_parsed.add(".obsidian");
        already_parsed.add(".trash");
        already_parsed.add(".git");
        Path directory = Paths.get(path);
        //Looks for the first ordners and elements
        if (Files.exists(directory) && Files.isDirectory(directory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path file : stream) {
                    if (!file.getFileName().toString().contains(".png") &&
                            !file.getFileName().toString().contains(".obsidian") &&
                            !file.getFileName().toString().contains(".trash") &&
                            !file.getFileName().toString().contains(".DS_Store")) {
                        if (file.getFileName().toString().contains(".md")) {
                            
                            obsidian_elements.put(path + file.getFileName().toString(),
                                    file.getFileName().toString());
                        } else {
                            obsidian_ordners.add(path + file.getFileName().toString());
                            general_structure.add(path + file.getFileName().toString());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int new_size = obsidian_ordners.size();
        int old_size = 0;

        //Looks in each ordner for more ordners and elements until there are no more ordners
        while (new_size != old_size) {
            old_size = new_size;
            for (String ordner : obsidian_ordners) {

                Path directory2 = Paths.get(ordner);
                if (Files.exists(directory2) && Files.isDirectory(directory2)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory2)) {
                        for (Path file : stream) {

                            if (!file.getFileName().toString().contains(".png") &&
                                    !file.getFileName().toString().contains(".obsidian") &&
                                    !file.getFileName().toString().contains(".trash") &&
                                    !file.getFileName().toString().contains(".DS_Store")) {
                                if (file.getFileName().toString().contains(".md")) {
                                    obsidian_elements.put(ordner + "/" + file.getFileName().toString(),
                                            file.getFileName().toString());
                                } else {
                                    obsidian_ordners.add(ordner + "/" + file.getFileName().toString());
                                }
                            }

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            new_size = obsidian_ordners.size();
        }
        //Going through all the elements and looking for their parents and children
        for(String location: obsidian_elements.keySet()) {
            ArrayList<String> children = find_all_links(location);
            children_list.put(location, children);
            
        }
        JSONArray jsonArray = new JSONArray();
        int id = 0;
        String short_ordner;
        int count = 0;
        for(String ordner : general_structure) {
            
            System.out.println(count+" "+ordner);
            count++;
            short_ordner = ordner.substring(path.length(),ordner.length());
            for(String element : obsidian_elements.keySet()) {
                
                if ((element).contains(ordner+"/")) {
                    
                    jsonArray.put(creating_json_element(id ,short_ordner ,element, obsidian_elements,children_list));
                    id++;
                }
            }
        }
        System.out.println(jsonArray.length());
        
        Database.update_database("database-1.cva46s8aqjab.eu-central-1.rds.amazonaws.com","5432","obsidian","postgres","my_database_password",jsonArray);
        
        
        
        writing_JSON("/Users/luis/Documents/linked_in_website/this-is-me/obsidian.json", jsonArray);
        writing_JSON("/Users/luis/Documents/Obsidian_to_java/obsidian.json", jsonArray);
    }

    public static JSONObject creating_json_element(int id, String category,String location, Map<String, String> elements, Map<String, ArrayList<String>> children) {
        JSONObject jsonObject = new JSONObject();
        StringBuilder content = new StringBuilder();
        File file = new File(location);
        Boolean is_boss = false;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null) {
                content.append(line+"<br/>");
            }
            if (content.indexOf("[Boss]") != -1) {
                is_boss = true;
            }
            reader.close();
            jsonObject.put("id", id);
            jsonObject.put("name", elements.get(location).substring(0, elements.get(location).length() - 3));
            jsonObject.put("location", location);
            jsonObject.put("category", category);
            jsonObject.put("priority", children.get(location).size());
            jsonObject.put("is_boss", is_boss);
            jsonObject.put("content", content.toString());
            jsonObject.put("children", children.get(location));
        } catch (IOException e) {

        }
        return jsonObject;
    }

    public static JSONObject creating_json_ordner(String location, Map<String, String> elements) {
        JSONObject jsonObject = new JSONObject();
        ArrayList<String> links = new ArrayList<String>();
        File file = new File(location);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null) {
                links.addAll(find_links_line(line));
            }
            reader.close();
            jsonObject.put("name", elements.get(location).substring(0, elements.get(location).length() - 3));
            jsonObject.put("location", location);
            jsonObject.put("links", links);
        } catch (IOException e) {

        }
        return jsonObject;
    }

    public static String removeTextBetweenBackticks(String content) {
        Pattern pattern = Pattern.compile("```.*?```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.replaceAll("");  // Entfernt den gefundenen Text
    }

    // Methode, um Links in einer Zeile zu finden
    public static ArrayList<String> find_links_line(String content) {
        ArrayList<String> links = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String link = matcher.group(1);
        // Prüfen, ob der Link nicht auf .png endet
            if (!link.endsWith(".png") && !link.equals("Boss")) {
                if (link.contains("/")) {
                    link = link.substring(0, link.indexOf("/"));
                }
                links.add(link);
            }
        }
        
        return links;
    }

    // Methode, um alle Links aus einer Datei zu finden
    public static ArrayList<String> find_all_links(String location) {
        ArrayList<String> links = new ArrayList<String>();
        File file = new File(location);
        StringBuilder contentBuilder = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            reader.close();

            // Gesamten Inhalt verarbeiten
            String content = contentBuilder.toString();

            // Entferne den Inhalt zwischen den Backticks
            content = removeTextBetweenBackticks(content);

            // Finde alle Links im verbleibenden Text
            links.addAll(find_links_line(content));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return links;
    }

    public static void writing_JSON(String location, JSONArray jsonArray) {
        try(FileWriter fileWriter = new FileWriter(location)) {
            System.out.println("JSON SIZE = "+jsonArray.length());
            fileWriter.write(jsonArray.toString(4));
            fileWriter.flush();
            System.out.println("Writing to file: " + location);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
}
