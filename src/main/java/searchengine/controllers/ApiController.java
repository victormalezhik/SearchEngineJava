package searchengine.controllers;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexPageService;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RestController
@ComponentScan
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
        Map<String,Object> result = indexingService.siteAndPageIndexing();
        if (result.containsValue(true)) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        Map<String,Object> result = indexingService.stopIndexing();
        if (result.containsValue(true)) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestBody String url) {
        Map<String,Object> result = indexPageService.indexingPage(url);
        if (result.containsValue(true)) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }
}
