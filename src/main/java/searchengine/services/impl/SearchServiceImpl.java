package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.lemmatisation.LemmasFromText;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;
import searchengine.searchResponse.SearchResponse;
import searchengine.services.SearchService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;

    private static Site site;

    @Override
    public Map<String, Object> searchByQuery(String query,String siteUrl) {
        if(query.isBlank()){
            return generateErrorResponse("Задан пустой поисковый запрос");
        }
        site = siteRepository.findByUrl(siteUrl);

        try {
            Set<Lemma> lemmasSetFromQuery = clearOutFromMostFrequentLemmas(Objects.requireNonNull(getLemmasFromQuery(query)));
            if (lemmasSetFromQuery.isEmpty()){
                throw new RuntimeException();
            }

            List<Lemma> sortedLemmasList = sortLemmas(lemmasSetFromQuery);
            List<Page> pagesForCountRelev = findPagesForEveryLemma(sortedLemmasList);
            if (pagesForCountRelev.isEmpty()) {
                throw new RuntimeException();
            }

            Map<Page,Float> pagesWithRelev = getRelevForPages(pagesForCountRelev,sortedLemmasList);
            return generateSuccessResponse(getResponseAfterSearch(pagesWithRelev,sortedLemmasList));
        }
        catch (Exception exception) {
            return generateErrorResponse("Указанная страница не найдена");
        }
    }

    private Set<Lemma> getLemmasFromQuery(String query){
        try{
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            LemmasFromText lemmasFromText = new LemmasFromText(luceneMorphology);

            Set<String> lemmasSetFromQuery = lemmasFromText.getLemmasSet(query);

            return getUniqueLemmasFromQuery(lemmasSetFromQuery);
        }
        catch (Exception exception){
            exception.printStackTrace();
            return null;
        }
    }

    private Set<Lemma> getUniqueLemmasFromQuery(Set<String> lemmasSetFromQuery){
        Set<Lemma> uniqueLemmasFromQuery = new HashSet<>();

        lemmasSetFromQuery.forEach( lemma -> {
            List<Lemma> resultFromSearchOfLemma = lemmaRepository.findAllByLemma(lemma);
            if(!resultFromSearchOfLemma.isEmpty()){
                if(site == null) {
                    uniqueLemmasFromQuery.addAll(resultFromSearchOfLemma);
                }
                else {
                    resultFromSearchOfLemma.forEach(lemma1 -> {
                        if(lemma1.getSite() == site){
                            uniqueLemmasFromQuery.add(lemma1);
                        }
                    });
                }
            }
        });

        return uniqueLemmasFromQuery;
    }

    private Set<Lemma> clearOutFromMostFrequentLemmas(Set<Lemma> lemmas){
        Set<Lemma> filteredLemmasSet = new HashSet<>();
        lemmas.forEach( lemma -> {
            List<Index> indexesOfLemma = indexRepository.findByLemmaId(lemma.getId());
            if(indexesOfLemma.size() < 2){
                filteredLemmasSet.add(lemma);
            }
        });
        return filteredLemmasSet;
    }

    private List<Lemma> sortLemmas(Set<Lemma> lemmas){
        Comparator<Lemma> lemmaComparator = Comparator.comparing(Lemma::getFrequency);
        List<Lemma> sortedLemmas = new ArrayList<>(lemmas);
        sortedLemmas.sort(lemmaComparator);
        return sortedLemmas;
    }

    private List<Page> findPagesForEveryLemma(List<Lemma> sortedLemmas){
        List<Lemma> lemmas = new ArrayList<>(sortedLemmas);
        List<Index> indexesForFirstLemma = indexRepository.findByLemmaId(lemmas.get(0).getId());
        lemmas.remove(0);

        List<Page> pagesForLemmas = findPageForFirstLemma(indexesForFirstLemma);

        Iterator<Page> iterator = pagesForLemmas.iterator();
        while (iterator.hasNext()) {
            Page page = iterator.next();
            for (Lemma lemma : lemmas) {
                List<Index> indexList = indexRepository.findByLemmaId(lemma.getId());
                List<Page> pagesWithLemma = new ArrayList<>();

                indexList.forEach(index -> {
                    pagesWithLemma.add(index.getPage());
                });

                if (!pagesWithLemma.contains(page)) {
                    iterator.remove();
                    break;
                }
            }
        }

        return pagesForLemmas;
    }

    private List<Page> findPageForFirstLemma(List<Index> indexesForFirstLemma){
        List<Page> pagesForLemmas = new ArrayList<>();
        indexesForFirstLemma.forEach(index -> {
            if (site == null) {
                pagesForLemmas.add(index.getPage());
            }
            else {
                Page pageFromIndex = index.getPage();
                if(pageFromIndex.getSite() == site){
                    pagesForLemmas.add(pageFromIndex);
                }
            }
        });
        return pagesForLemmas;
    }



    private Map<Page,Float> getRelevForPages(List<Page> pages, List<Lemma> lemmaList){
        Map<Page,Float> pagesWithAbsRelev = new HashMap<>();
        float maxAbsRelevAmoungPages = 0;

        for(Page page : pages){
           float absRelevForLem = 0;
           for(Lemma lemma : lemmaList){
               Index index = indexRepository.findByPageIdAndLemmaId(page.getId(), lemma.getId());
               absRelevForLem = index.getRank() + absRelevForLem;
           }
           pagesWithAbsRelev.put(page,absRelevForLem);
           if (absRelevForLem > maxAbsRelevAmoungPages){
               maxAbsRelevAmoungPages = absRelevForLem;
           }
        }

        for(Page page : pagesWithAbsRelev.keySet()){
            float oldValue = pagesWithAbsRelev.get(page);
            float newValueOfRev = oldValue/maxAbsRelevAmoungPages;
            pagesWithAbsRelev.put(page,newValueOfRev);
        }

        if(pagesWithAbsRelev.size() > 1){
            return sortPagesByRelev(pagesWithAbsRelev);
        }
        return pagesWithAbsRelev;
    }

    private Map<Page, Float> sortPagesByRelev(Map<Page,Float> pagesWithRelev){
        List<Map.Entry<Page,Float>> listPages = new ArrayList<>(pagesWithRelev.entrySet());
        listPages.sort(new Comparator<Map.Entry<Page, Float>>() {
            @Override
            public int compare(Map.Entry<Page, Float> o1, Map.Entry<Page, Float> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        LinkedHashMap<Page,Float> sortedPagesByRelev = new LinkedHashMap<>();
        for (Map.Entry<Page,Float> entry : listPages){
            sortedPagesByRelev.put(entry.getKey(),entry.getValue());
        }
        return sortedPagesByRelev;
    }


    private List<SearchResponse> getResponseAfterSearch(Map<Page,Float> pagesWithRelev, List<Lemma> lemmaList){
        List<SearchResponse> listOfResponses = new ArrayList<>();
        pagesWithRelev.forEach((page, relev) -> {
            lemmaList.forEach(lemma -> {
                SearchResponse searchResponse = getSearchResponse(page,relev,lemma);
                listOfResponses.add(searchResponse);
            });
        });
        return listOfResponses;
    }

    private SearchResponse getSearchResponse(Page page, Float relev, Lemma lemma){
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setRelevance(relev);
        if(site == null) {
            searchResponse.setSite("All sites");
            searchResponse.setSiteName("");
        }
        else {
            searchResponse.setSite(site.getUrl());
            searchResponse.setSiteName(site.getName());
        }
        searchResponse.setUri(page.getPath());
        searchResponse.setTitle(Jsoup.parse(page.getContent()).title());
        searchResponse.setSnippet(generateSnippet(page.getContent(), lemma.getLemma()));
        return searchResponse;
    }

    private String generateSnippet(String content, String lemma){
        try {
            String pageText = Jsoup.parse(content).text();

            String[] words = pageText.split("\\s+");

            StringBuilder snippetBuilder = new StringBuilder();

            int wordCount = 0;
            for (String word : words) {
                if (wordCount >= 30) {
                    break;
                }
                if (word.toLowerCase().contains(lemma.toLowerCase())) {
                    snippetBuilder.append("<b>").append(word).append("</b>").append(" ");
                } else {
                    snippetBuilder.append(word).append(" ");
                }
                wordCount++;
            }
            return snippetBuilder.toString();
        }
        catch (Exception exception){
            exception.printStackTrace();
            return "";
        }
    }

    private Map<String, Object> generateErrorResponse(String errorMessage){
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("result", false);
        errorMap.put("error", errorMessage);
        return errorMap;
    }

    private Map<String,Object> generateSuccessResponse(List<SearchResponse> result){
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("result", true);
        responseMap.put("count", result.size());
        responseMap.put("data", result);
        return responseMap;
    }
}
