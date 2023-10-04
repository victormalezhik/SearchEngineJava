package searchengine.services;

import java.util.Map;

public interface SearchService {
    Map<String, Object> searchByQuery(String query, String site);
}
