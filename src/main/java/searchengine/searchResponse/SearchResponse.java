package searchengine.searchResponse;

import lombok.Data;

@Data
public class SearchResponse {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    float relevance;
}
