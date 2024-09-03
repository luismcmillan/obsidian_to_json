package com.ibm.sms_length_app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class removingText {

    // Methode, um den Text zwischen ``` und ``` zu entfernen
    public static String removeTextBetweenBackticks(String content) {

        Pattern pattern = Pattern.compile("```.*?```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.replaceAll("");  
    }

    public static void main(String[] args) {
        String content = "Hallo\n```Python\nimport torch\n\n# Erstellen eines 2x3 Tensors\ntensor = torch.tensor([[1, 2, 3], [4, 5, 6]])\n\n# Elementweise Multiplikation\nresult = tensor * 2\nprint(result)\n```\nLuis";
        System.out.println(content);
        String cleanedContent = removeTextBetweenBackticks(content);
        System.out.println(cleanedContent);
    }
}
