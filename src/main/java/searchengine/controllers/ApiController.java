package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexPageService;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    private final IndexPageService indexPageService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, IndexPageService indexPageService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.indexPageService = indexPageService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        if (indexingService.siteAndPageIndexing().containsValue(true)) {
            return ResponseEntity.ok(indexingService.siteAndPageIndexing());
        }
        return ResponseEntity.badRequest().body(indexingService.siteAndPageIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing(){
        if (indexingService.stopIndexing().containsValue(true)) {
            return ResponseEntity.ok(indexingService.stopIndexing());
        }
        return ResponseEntity.badRequest().body(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage/{url}")
    public ResponseEntity<Map<String, Object>> indexPage(@PathVariable String url) {
        if (indexPageService.indexingPage().containsValue(true)) {
            return ResponseEntity.ok(indexPageService.indexingPage());
        }
        return ResponseEntity.badRequest().body(indexPageService.indexingPage());
    }
}
