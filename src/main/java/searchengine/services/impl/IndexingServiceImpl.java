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
import java.util.List;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesList;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired


    private final SiteRepository siteRepository;

    Logger logger = Logger.getLogger(IndexingServiceImpl.class.getName());

    @Override
    public String getSitesList() {
        truncateTables();
        List<searchengine.config.Site> sites = sitesList.getSites();
        sites.forEach(this::siteIndexing);
        //ForkJoinPool pool = new ForkJoinPool();
        //pool.submit(() -> sites.parallelStream().forEach(this::siteIndexing)).join();
        return "1";
    }

    public void truncateTables() {
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }

    public void siteIndexing(searchengine.config.Site sitesBeforeIndexing) {
        Site site = new Site();
        site.setName(sitesBeforeIndexing.getName());
        site.setUrl(sitesBeforeIndexing.getUrl());
        site.setStatus(Status.INDEXING);
        site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteRepository.save(site);
        SiteIndexing siteIndexing = new SiteIndexing(pageRepository, siteRepository,site);
        siteIndexing.invoke();
    }
}

