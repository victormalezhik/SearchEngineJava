package searchengine.services;

import java.util.Map;

public interface IndexingService {
    Map<String, Object> siteAndPageIndexing();

    Map<String, Object> stopIndexing();
}
