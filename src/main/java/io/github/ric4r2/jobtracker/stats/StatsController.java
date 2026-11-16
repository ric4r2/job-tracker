package io.github.ric4r2.jobtracker.stats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/api/stats")
    public StatsService.StatsResponse summary() {
        return stats.summary();
    }
}
