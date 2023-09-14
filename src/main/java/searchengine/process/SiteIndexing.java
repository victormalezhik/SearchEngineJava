package searchengine.process;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

@RequiredArgsConstructor
@Getter
@Setter
public class SiteIndexing extends RecursiveAction {

    private Set<Page> pages = Collections.synchronizedSet(new HashSet<>());


    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final Site site;

    @Override
    protected void compute() {
        try{
            sleep(2000);
            Connection connection = Jsoup.connect(site.getUrl()).timeout(30000);
            Document document = connection.get();
            Elements elements = document.select("body").select("a");
            elements.stream().parallel().forEach(element -> {
                String pageUrl = element.absUrl("href");
                if(isCorrectLink(pageUrl)){
                    Indextor indextor = new Indextor(pageUrl,site);
                    if (indextor.connectAndCreatePage() != null){
                        pages.add(indextor.connectAndCreatePage());
                    }
                }
            });
        }
        catch (Exception exception){
            exception.printStackTrace();
        }
        synchronized (site) {
            pages.forEach(page -> {
                if (checkDuplicatePage(page)) {
                    pageRepository.save(page);
                    site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
                    siteRepository.save(site);
                }
            });
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);
        }

    }

    private boolean checkDuplicatePage(Page page){
        return pageRepository.findByPath(page.getPath()) == null;
    }

    private boolean isCorrectLink(String link) {
        Pattern patternFileLink = Pattern.compile("([^\\s]+(\\.(?i)(jpg|png|gif|bmp|pdf))$)");
        Pattern patternAnchor = Pattern.compile("#([\\w\\-]+)?$");
        if(!link.contains("www.") && site.getUrl().contains("www.")){
            return !patternFileLink.matcher(link).find() &&
                    !patternAnchor.matcher(link).find() &&
                    link.contains(site.getUrl().replace("www.",""));
        }
        return !patternFileLink.matcher(link).find() &&
                !patternAnchor.matcher(link).find() &&
                link.contains(site.getUrl());
    }
}
