package com.tangluck.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/")
    public String root() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Tang Luck Operations</title>
                  <style>
                    body { margin: 0; font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f6f7f4; color: #171b16; }
                    main { max-width: 720px; margin: 72px auto; padding: 0 24px; }
                    h1 { margin: 0 0 12px; font-size: 36px; }
                    p { color: #667066; line-height: 1.6; }
                    nav { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 28px; }
                    a { display: inline-flex; align-items: center; min-height: 44px; padding: 0 16px; border-radius: 8px; background: #236347; color: #fff; text-decoration: none; font-weight: 700; }
                    a.secondary { background: #fff; color: #236347; border: 1px solid #d9ded8; }
                    code { background: #fff; border: 1px solid #d9ded8; border-radius: 6px; padding: 2px 6px; }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>Tang Luck Operations</h1>
                    <p>Backend service is running. Product and operations pages are served by the frontend development server at <code>http://127.0.0.1:5175</code>.</p>
                    <nav>
                      <a href="http://127.0.0.1:5175/app/register">Open user registration</a>
                      <a class="secondary" href="http://127.0.0.1:5175/admin">Open operations console</a>
                      <a class="secondary" href="/health">View health status</a>
                    </nav>
                  </main>
                </body>
                </html>
                """;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
