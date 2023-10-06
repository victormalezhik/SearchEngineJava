package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesList;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    private static boolean indexingOn;
    private List<SiteIndexing> siteIndexingsList = new CopyOnWriteArrayList<>();



    @Override
    public Map<String, Object> siteAndPageIndexing() {
        if(!indexingOn) {
            truncateTables();
            indexingOn = true;

            List<searchengine.config.Site> sites = sitesList.getSites();
            ForkJoinPool pool = new ForkJoinPool();
            sites.stream().parallel().forEach(s -> {
                Site site = createNewSite(s);
                SiteIndexing siteIndexing = new SiteIndexing(pageRepository, siteRepository, lemmaRepository,indexRepository,site);
                siteIndexingsList.add(siteIndexing);
                pool.invoke(siteIndexing);
            });

            return getSuccessResponse();
        }
        else {
            return getErrorResponse("Индексация уже запущена");
        }
    }

    private Site createNewSite(searchengine.config.Site siteFromApplicationFile){
        Site site = new Site();
        site.setName(siteFromApplicationFile.getName());
        site.setUrl(siteFromApplicationFile.getUrl());
        site.setStatus(Status.INDEXING);
        site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteRepository.save(site);
        return site;
    }

    public void truncateTables() {
        indexRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        siteRepository.deleteAll();
    }
    @Override
    public Map<String, Object> stopIndexing(){
        if (!siteIndexingsList.isEmpty()) {
            siteIndexingsList.stream().forEach(SiteIndexing::stopIndexing);
            return getSuccessResponse();
        } else {
            return getErrorResponse("Индексация не запущена");
        }
    }

    private Map<String,Object> getErrorResponse(String errorMessage){
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("result", false);
        errorMap.put("error", errorMessage);
        return errorMap;
    }

    private Map<String,Object> getSuccessResponse(){
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("result", true);
        return responseMap;
    }
}

