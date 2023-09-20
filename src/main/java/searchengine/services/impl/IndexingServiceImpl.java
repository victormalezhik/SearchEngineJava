package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.process.SiteIndexing;
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
                Site site = new Site();
                site.setName(s.getName());
                site.setUrl(s.getUrl());
                site.setStatus(Status.INDEXING);
                site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
                siteRepository.save(site);
                SiteIndexing siteIndexing = new SiteIndexing(pageRepository, siteRepository, site);
                siteIndexingsList.add(siteIndexing);
                pool.invoke(siteIndexing);
            });
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", true);
            return responseMap;
        }
        else {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("result", false);
            errorMap.put("error", "Индексация уже запущена");
            return errorMap;
        }
    }

    public void truncateTables() {
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }
    @Override
    public Map<String, Object> stopIndexing(){
        if (!siteIndexingsList.isEmpty()) {
            siteIndexingsList.stream().forEach(SiteIndexing::stopIndexing);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", true);
            return responseMap;
        } else {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("result", false);
            errorMap.put("error", "Индексация не запущена");
            return errorMap;
        }
    }
}

