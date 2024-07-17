package com.ibm.sms_length_app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

public class AddingNewReport {
    public static void main(String[] args) {
        String path = "/Users/luis/Documents/My_obsidian/";
        HashMap<String, ArrayList<String>> parent_list = new HashMap<String, ArrayList<String>>();
        HashMap<String, ArrayList<String>> children_list = new HashMap<String, ArrayList<String>>();
        ConcurrentSkipListSet<String> obsidian_ordners = new ConcurrentSkipListSet<String>();
        LinkedList<String> general_structure = new LinkedList<String>();
        Map<String, String> obsidian_elements = new ConcurrentHashMap<>();
        ArrayList<String> already_parsed = new ArrayList<String>();
        already_parsed.add(".DS_Store");
        already_parsed.add(".obsidian");
        already_parsed.add(".trash");
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
        //Filling parents_list with empty ArrayLists
        for(String element : obsidian_elements.keySet()) {
            ArrayList<String> parents = new ArrayList<>();
            parent_list.put(element, parents);
        }
        //Going through all the elements and looking for their parents and children
        for(String location: obsidian_elements.keySet()) {
            ArrayList<String> children = find_all_links(location);
            children_list.put(location, children);
            for(String find_child: children){
                //System.out.println(find_child);
                for(String child: obsidian_elements.keySet()){
                    System.out.println(obsidian_elements.get(child));
                    String compare = obsidian_elements.get(child);
                    compare = compare.substring(0,compare.length()-3);
                    if(find_child.equals(compare)){
                        ArrayList<String> parents = parent_list.get(child);
                        String parent = obsidian_elements.get(location).substring(0,obsidian_elements.get(location).length()-3);
                        if (!parents.contains(parent)){
                            parents.add(parent);
                        }
                        parent_list.put(child,parents);
                    }
                }
            }
        }
        /* 
        for(String test1 : children_list.keySet()){
            System.out.println(test1+" Children: " + children_list.get(test1));
        }
        for(String test1 : parent_list.keySet()){
            System.out.println(test1+" parents: " + parent_list.get(test1));
        }
        */
        
        

        JSONArray jsonArray = new JSONArray();
        int id = 0;
        String short_ordner;
        for(String ordner : general_structure) {
            short_ordner = ordner.substring(path.length(),ordner.length());
            for(String element : obsidian_elements.keySet()) {
                if ((element).contains(ordner)) {
                    jsonArray.put(creating_json_element(id ,short_ordner ,element, obsidian_elements,children_list, parent_list));
                    id++;
                }
            }
        }
       
        writing_JSON("./obsidian.json", jsonArray);
    }

    public static JSONObject creating_json_element(int id, String category,String location, Map<String, String> elements, Map<String, ArrayList<String>> children,Map<String, ArrayList<String>> parents) {
        JSONObject jsonObject = new JSONObject();
        StringBuilder content = new StringBuilder();
        File file = new File(location);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null) {
                content.append(line+"<br/>");
            }
            reader.close();
            jsonObject.put("id", id);
            jsonObject.put("name", elements.get(location).substring(0, elements.get(location).length() - 3));
            jsonObject.put("location", location);
            jsonObject.put("category", category);
            jsonObject.put("content", content.toString());
            jsonObject.put("children", children.get(location));
            jsonObject.put("parents", parents.get(location));
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

    public static ArrayList<String> find_all_links(String location) {
        ArrayList<String> links = new ArrayList<String>();
        File file = new File(location);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null) {
                links.addAll(find_links_line(line));
            }
            reader.close();
        } catch (IOException e) {

        }
        return links;
    }

    public static ArrayList<String> find_links_line(String content) {
        ArrayList<String> links = new ArrayList<String>();
        ArrayList<String> links_without_png = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(content);

        while(matcher.find()) {
            links.add(matcher.group(1));
        }

        for(String link : links) {
            if (!link.contains(".png")) {
                links_without_png.add(link);
            }
        }
        
        return links_without_png;
    }

    public static void writing_JSON(String location, JSONArray jsonArray) {
        try(FileWriter fileWriter = new FileWriter(location)) {
            fileWriter.write(jsonArray.toString(4));
            fileWriter.flush();
            System.out.println("Writing to file: " + location);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
