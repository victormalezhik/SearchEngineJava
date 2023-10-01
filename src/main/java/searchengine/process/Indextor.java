package searchengine.process;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
            page.setPath(pathMaker());
            page.setCode(connection.response().statusCode());
            page.setContent(doc.html());
            page.setSite(site);
            return page;
        }
        catch (HttpStatusException | InterruptedException exception){
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String pathMaker(){
        if (site.getUrl().contains("www.") && !pageUrl.contains("www.")) {
            return pageUrl.replace(site.getUrl().replace("www.",""), "");
        } else if (site.getUrl().contains("www.") && pageUrl.contains("www.")) {
            String path = pageUrl.replace(site.getUrl(), "");
            if (path.isBlank()){
                return "/";
            }
            else {
                return path;
            }
        } else {
            return pageUrl.replace(site.getUrl(), "");
        }
    }
}
