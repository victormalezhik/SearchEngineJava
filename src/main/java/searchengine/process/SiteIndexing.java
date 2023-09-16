package searchengine.process;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

@RequiredArgsConstructor
@Getter
@Setter
public class SiteIndexing extends RecursiveAction {
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;

    private final Site site;
    @Override
    public void compute() {
        try{
            sleep(2000);
            Connection connection = Jsoup.connect(site.getUrl()).timeout(30000);
            Document document = connection.get();
            Elements elements = document.select("body").select("a");
            elements.stream().parallel().forEach(element -> {
                String pageUrl = element.absUrl("href");
                if(isCorrectLink(pageUrl)){
                    Indextor indextor = new Indextor(pageUrl,site);
                    if (indextor.connectAndCreatePage() != null && checkDuplicatePage(indextor.connectAndCreatePage())){
                        saveSite(indextor.connectAndCreatePage());
                        site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
                    }
                }
            });
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);
        }
        catch (Exception exception){
            exception.printStackTrace();
            System.out.println(exception.getMessage());
        }
    }
    @Transactional
    public void saveSite(Page page){
        //в этих строчках возникает ошибка о которой я говорил
        page.setSite(site);
        pageRepository.save(page);
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
