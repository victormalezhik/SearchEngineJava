package searchengine.lemmatisation;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.util.List;

public class CheckLemmatisation {
    public static void main(String[] args) {
        String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            LemmasFromText lemmasFromText = new LemmasFromText(luceneMorphology);
            lemmasFromText.collectLemmas(text);
        }
        catch ( Exception exception){
            System.out.println(exception.getMessage());
        }

    }
}
