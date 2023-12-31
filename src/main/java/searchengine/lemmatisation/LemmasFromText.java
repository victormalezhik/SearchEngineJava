package searchengine.lemmatisation;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;

import java.util.*;

@Getter
@Setter
public class LemmasFromText {
    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String [] particlesNames = new String[]{"МЕЖД","ПРЕДЛ","СОЮЗ"};

    public LemmasFromText(LuceneMorphology luceneMorphology){
        this.luceneMorphology = luceneMorphology;
    }

    public Map<String,Integer> collectLemmas(String text){
        String clearedText = clearTextFromHtml(text);

        String[] words = arrayContainsRussianWords(clearedText);
        HashMap<String, Integer> lemmasMap = new HashMap<>();

        for(String word : words){
            if (word.isBlank()){
                continue;
            }

            List<String> wordBaseForms =  luceneMorphology.getMorphInfo(word);
            if(wordBaseFormIsParticle(wordBaseForms)){
                continue;
            }
            List<String> normalForm = luceneMorphology.getNormalForms(word);
            if (normalForm.isEmpty()){
                continue;
            }

            String normalWord = normalForm.get(0);

            if(lemmasMap.containsKey(normalWord)){
                lemmasMap.put(normalWord, lemmasMap.get(normalWord) + 1);
            }
            else {
                lemmasMap.put(normalWord,1);
            }
        }
        return lemmasMap;
    }

    public Set<String> getLemmasSet(String text){
        String[] words = arrayContainsRussianWords(text);
        Set<String> lemmasFromQuery = new HashSet<>();
        for(String word : words){
            if (word.isBlank()){
                continue;
            }
            List<String> wordBaseForms =  luceneMorphology.getMorphInfo(word);
            if(wordBaseFormIsParticle(wordBaseForms)){
                continue;
            }
            List<String> normalForm = luceneMorphology.getNormalForms(word);
            if (normalForm.isEmpty()){
                continue;
            }
            String normalWord = normalForm.get(0);
            lemmasFromQuery.add(normalWord);
        }
        return lemmasFromQuery;
    }

    private String clearTextFromHtml(String text){
       return Jsoup.parse(text).text();
    }

    private String[] arrayContainsRussianWords(String text){
        return text.toLowerCase(Locale.ROOT).
                replaceAll("([^а-я\\s])", " ").
                trim().
                split("\\s+");
    }

    private boolean wordBaseFormIsParticle(List<String> wordBaseForms){
        return wordBaseForms.stream().anyMatch(this::hasParticle);
    }

    private boolean hasParticle(String wordBase){
        for(String particleName : particlesNames){
            if(wordBase.toUpperCase().contains(particleName)){
                return true;
            }
        }
        return false;
    }
}
