package com.magnocat.mctrilhas.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SecureHttpFileHandler implements HttpHandler {

    private final Path webRootDir;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff2", "font/woff2");
    }

    public SecureHttpFileHandler(Path webRootPath) {
        // Garante que o caminho é absoluto e normalizado
        this.webRootDir = webRootPath.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String pathStr = uri.getPath().equals("/") ? "/index.html" : uri.getPath();

        // Resolve o caminho do arquivo solicitado de forma segura
        Path requestedFile = webRootDir.resolve(pathStr.substring(1)).normalize();

        // **A VERIFICAÇÃO DE SEGURANÇA MAIS IMPORTANTE**
        // Garante que o caminho solicitado está DENTRO do diretório web raiz.
        // Isso impede ataques de Path Traversal (ex: /../../plugins/config.yml)
        if (!requestedFile.startsWith(webRootDir)) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        File file = requestedFile.toFile();

        if (!file.exists() || !file.isFile()) { // Garante que é um arquivo e não um diretório
            File notFoundPage = webRootDir.resolve("404.html").toFile();
            if (notFoundPage.exists()) {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(404, notFoundPage.length());
                try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(notFoundPage)) {
                    fis.transferTo(os);
                }
            } else {
                sendError(exchange, 404, "Not Found");
            }
            return;
        }

        // Serve o arquivo com o tipo MIME correto
        String mimeType = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, file.length());
        try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(os);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }

    private String getMimeType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            String extension = fileName.substring(lastDot + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}