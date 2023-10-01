package searchengine.process;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.impl.IndexingPageServiceImpl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

@RequiredArgsConstructor
@Getter
@Setter
public class SiteIndexing extends RecursiveAction{
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;

    private final Site site;

    private volatile boolean indexingRunning = true;

    public void stopIndexing() {
        indexingRunning = false;
    }

    @Override
    protected void compute() {
        try {
            sleep(2000);
            Connection connection = Jsoup.connect(site.getUrl()).timeout(30000);
            Document document = connection.get();
            Elements elements = document.select("body").select("a");
            indexingRunning = true;
            for (Element element : elements) {
                if (indexingRunning) {
                    String pageUrl = element.absUrl("href");
                    if (isCorrectLink(pageUrl)) {
                        Indextor indextor = new Indextor(pageUrl, site);
                        Page newPage = indextor.connectAndCreatePage();
                        if (newPage != null) {
                            if (checkDuplicatePage(newPage)) {
                                pageRepository.save(newPage);
                                new IndexingPageServiceImpl(siteRepository,pageRepository,lemmaRepository,indexRepository).indexingPages(pageUrl);
                                site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
                            }
                        }
                    }
                } else {
                    break;
                }
            }
            if (indexingRunning) {
                site.setStatus(Status.INDEXED);
            } else {
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация остановлена пользователем");
            }

            siteRepository.save(site);

        } catch (Exception exception) {
            site.setStatus(Status.FAILED);
            site.setLastError(exception.getMessage());
            siteRepository.save(site);
        }
    }
    private boolean checkDuplicatePage(Page page) {
        try {
            Page copyPage = pageRepository.findByPath(page.getPath());
            return copyPage == null;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isCorrectLink(String link) {
        Pattern patternFileLink = Pattern.compile("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf))$)");
        Pattern patternAnchor = Pattern.compile("#([\\w\\-]+)?$");
        if (!link.contains("www.") && site.getUrl().contains("www.")) {
            return !patternFileLink.matcher(link).find() &&
                    !patternAnchor.matcher(link).find() &&
                    link.contains(site.getUrl().replace("www.", ""));
        }
        return !patternFileLink.matcher(link).find() &&
                !patternAnchor.matcher(link).find() &&
                link.contains(site.getUrl());
    }
}
