package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.lemmatisation.LemmasFromText;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexPageService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingPageServiceImpl implements IndexPageService{
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    private static Site site;
    private static Page page;

    @Override
    public Map<String, Object> indexingPage(String url) {
        String decodedUrl = urlDecoding(url);

        if(!decodedUrl.isBlank()) {
            if (isCorrectLink(decodedUrl) && isUrlFromExistedSite(decodedUrl)) {
                checkPageOnIndexing(decodedUrl);
                saveLemmasFromPage(decodedUrl);
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("result", true);
                return responseMap;
            } else {
               return getErrorResponse("Данная страница находится " +
                        "за пределами сайтов указанных в конфигурационном файле");
            }
        }
        else {
            return getErrorResponse("Неправильная ссылка или она пуста");
        }
    }

    public void indexingPages(String url) {
        String decodedUrl = urlDecoding(url);

        if(!decodedUrl.isBlank()) {
            if (isCorrectLink(decodedUrl) && isUrlFromExistedSite(decodedUrl)) {
                saveLemmasFromPage(decodedUrl);
            }
        }
    }

    private String urlDecoding(String url){
        String decodedUrl = "";

        try {
            decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8).replace("url=","");
            return decodedUrl;
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        return decodedUrl;
    }

    private boolean isUrlFromExistedSite(String url){
        List<Site> listSites = new ArrayList<>();
        siteRepository.findAll().forEach(listSites::add);

        for(Site siteFromDb: listSites){
            if(!url.contains("www.") && siteFromDb.getUrl().contains("www.")){
                if(url.contains(siteFromDb.getUrl().replace("www.",""))) {
                    site = siteFromDb;
                    return true;
                }
            }
            else if(url.contains(siteFromDb.getUrl())) {
                site = siteFromDb;
                return true;
            }
        }
        return false;
    }

    private boolean isCorrectLink(String url) {
        Pattern patternFileLink = Pattern.compile("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf))$)");
        Pattern patternAnchor = Pattern.compile("#([\\w\\-]+)?$");
        return !patternFileLink.matcher(url).find() &&
                !patternAnchor.matcher(url).find();
    }


    private void saveLemmasFromPage(String url){
        try{
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            LemmasFromText lemmasFromText = new LemmasFromText(luceneMorphology);

            page = pageRepository.findByPath(urlFormatter(url));
            Map<String,Integer> lemmasMap = lemmasFromText.collectLemmas(page.getContent());
            lemmasMap.forEach((lem,amountOnPage) -> {
                List<Lemma> lemmaList = lemmaRepository.findAllByLemma(lem);

                if(lemmaList.isEmpty()) {
                    setLemmaAndIndexFromLemmasMap(lem,amountOnPage);
                }
                else if(lemmaList.size() == 1) {
                    updateExistedLemma(lemmaList);
                }
                else {
                    lemmaList.forEach( l -> {
                        if(l.getSite() == site){
                            updateExistedLemma(lemmaList);
                        }
                    });
                }
            });
        }
        catch (Exception exception){
            exception.printStackTrace();
        }
    }

    private void setLemmaAndIndexFromLemmasMap(String lem, int amountOnPage){
        Lemma lemma = new Lemma();
        lemma.setSite(site);
        lemma.setLemma(lem);
        lemma.setFrequency(1);

        Index index = new Index();
        index.setLemma(lemma);
        index.setPage(page);
        index.setRank(amountOnPage);

        lemmaRepository.save(lemma);
        indexRepository.save(index);
    }

    private void updateExistedLemma(List<Lemma> lemmaList){
        Lemma existdedLemma = lemmaList.get(0);
        existdedLemma.setFrequency(existdedLemma.getFrequency() + 1);
        lemmaRepository.save(existdedLemma);
    }

    private void checkPageOnIndexing(String url){
        Page existedPage = pageRepository.findByPath(urlFormatter(url));

        if(existedPage == null){
            PageCollector pageCollector = new PageCollector(url, site);
            page = pageCollector.connectAndCreatePage();
            pageRepository.save(page);
        }else {
            Integer existedPageId = existedPage.getId();
            List<Index> indexesWithExistedPage = indexRepository.findByPageId(existedPageId);
            indexesWithExistedPage.forEach(index -> {
                lemmaRepository.delete(index.getLemma());
                indexRepository.delete(index);
            });
            pageRepository.delete(existedPage);
        }
    }


    private String urlFormatter(String url){
        if (site.getUrl().contains("www.") && !url.contains("www.")) {
            return url.replace(site.getUrl().replace("www.",""), "");
        } else if (site.getUrl().contains("www.") && url.contains("www.")) {
            String path = url.replace(site.getUrl(), "");
            if (path.isBlank()){
                return "/";
            }
            else {
                return path;
            }
        } else {
            return url.replace(site.getUrl(), "");
        }
    }

    private Map<String, Object> getErrorResponse(String errorMessage){
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("result", false);
        errorMap.put("error", errorMessage);
        return errorMap;
    }
}
