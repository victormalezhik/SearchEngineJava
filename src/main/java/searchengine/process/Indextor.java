package searchengine.process;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;

import static java.lang.Thread.sleep;

@RequiredArgsConstructor
@Getter
public class Indextor {
    private final String pageUrl;
    private final Site site;
    private Page page;

    public Page connectAndCreatePage(){
        try {
            sleep(1000);
            Connection connection = Jsoup.connect(pageUrl).timeout(30000);
            Document doc = connection.get();
            page = new Page();
            page.setSite(site);
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
