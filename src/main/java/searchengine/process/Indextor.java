package searchengine.process;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.Optional;

import static java.lang.Thread.sleep;

@RequiredArgsConstructor
@Getter
@Setter
public class Indextor {
    private final String pageUrl;
    private final Site site;

    public Page connectAndCreatePage(){
        try {
            sleep(1000);
            Connection connection = Jsoup.connect(pageUrl).timeout(30000);
            Document doc = connection.get();
            Page page = new Page();
            if (site.getUrl().contains("www.") && !pageUrl.contains("www.")) {
                page.setPath(pageUrl.replace(site.getUrl().replace("www.",""), ""));
            }
            else {
                page.setPath(pageUrl.replace(site.getUrl(), ""));
            }
            page.setCode(connection.response().statusCode());
            page.setContent(doc.html());
            return page;
        }
        catch (HttpStatusException | InterruptedException exception){
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
