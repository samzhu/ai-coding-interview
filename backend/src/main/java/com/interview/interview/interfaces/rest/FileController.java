package com.interview.interview.interfaces.rest;

import com.interview.execution.ContainerFile;
import com.interview.interview.application.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/files")
class FileController {

    private final FileService fileService;

    FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    List<ContainerFile> listFiles(@PathVariable UUID interviewId) {
        return fileService.listFiles(interviewId);
    }

    @GetMapping("/content")
    FileContentResponse readFile(@PathVariable UUID interviewId,
                                 @RequestParam String path) {
        String content = fileService.readFile(interviewId, path);
        return new FileContentResponse(path, content);
    }

    @PutMapping("/content")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void writeFile(@PathVariable UUID interviewId,
                   @RequestParam String path,
                   @RequestBody WriteFileRequest request) {
        fileService.writeFile(interviewId, path, request.content());
    }

    record FileContentResponse(String filePath, String content) {
    }

    record WriteFileRequest(String content) {
    }
}
