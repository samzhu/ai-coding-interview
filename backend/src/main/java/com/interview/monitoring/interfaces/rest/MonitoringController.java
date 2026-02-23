package com.interview.monitoring.interfaces.rest;

import com.interview.monitoring.MonitoringActivityRequest;
import com.interview.monitoring.application.InterviewMonitoringService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}")
class MonitoringController {

    private final InterviewMonitoringService monitoringService;

    MonitoringController(InterviewMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping(value = "/monitor/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter monitorStream(@PathVariable UUID interviewId) {
        return monitoringService.subscribe(interviewId);
    }

    @PostMapping("/activity/code")
    @ResponseBody
    void reportCodeActivity(@PathVariable UUID interviewId,
                            @RequestBody MonitoringActivityRequest request) {
        monitoringService.notifyCodeChange(interviewId, request.filePath(), request.code());
    }
}
