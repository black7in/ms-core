package com.bustrack.mscore;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/mock-files")
public class MockFilesController {

    private static final String BASE = System.getProperty("java.io.tmpdir") + "/bustrack-uploads";

    @GetMapping("/**")
    public ResponseEntity<byte[]> serveFile(HttpServletRequest req) throws Exception {
        var path = req.getRequestURI().replace("/mock-files/", "");
        var file = new File(BASE, path);
        if (!file.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Files.readAllBytes(file.toPath()));
    }

    @PutMapping("/upload/**")
    public ResponseEntity<?> uploadFile(HttpServletRequest req) throws Exception {
        var path = req.getRequestURI().replace("/mock-files/upload/", "");
        var file = new File(BASE, path);
        file.getParentFile().mkdirs();
        try (var out = new FileOutputStream(file)) { req.getInputStream().transferTo(out); }
        return ResponseEntity.ok().build();
    }
}
