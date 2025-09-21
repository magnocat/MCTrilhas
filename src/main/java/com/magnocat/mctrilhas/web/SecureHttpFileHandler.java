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

/**
 * Um manipulador HTTP seguro para servir arquivos estáticos.
 * <p>
 * Esta classe é projetada para servir arquivos de um diretório raiz específico,
 * com uma verificação de segurança crucial para prevenir ataques de "Path Traversal",
 * garantindo que apenas arquivos dentro do diretório web possam ser acessados.
 */
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

    /**
     * Constrói um novo manipulador de arquivos.
     * @param webRootPath O caminho para o diretório raiz de onde os arquivos serão servidos.
     */
    public SecureHttpFileHandler(Path webRootPath) {
        // Garante que o caminho é absoluto e normalizado
        this.webRootDir = webRootPath.toAbsolutePath().normalize();
    }

    @Override
    /**
     * Lida com uma requisição HTTP para um arquivo estático.
     * Valida o caminho solicitado, verifica se o arquivo existe e o serve com o tipo MIME correto.
     *
     * @param exchange O objeto da troca HTTP, contendo a requisição e a resposta.
     * @throws IOException Se ocorrer um erro de I/O durante o processamento da requisição.
     */
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String pathStr = uri.getPath().equals("/") ? "/index.html" : uri.getPath();
        String resourcePath = pathStr.startsWith("/") ? pathStr.substring(1) : pathStr;

        // Resolve o caminho do arquivo solicitado de forma segura
        Path requestedFile = webRootDir.resolve(resourcePath).normalize();

        // **A VERIFICAÇÃO DE SEGURANÇA MAIS IMPORTANTE**
        // Garante que o caminho solicitado está DENTRO do diretório web raiz.
        // Isso impede ataques de Path Traversal (ex: /../../plugins/config.yml)
        if (!requestedFile.startsWith(webRootDir)) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        File file = requestedFile.toFile();

        // Verifica se o arquivo existe no disco e se é um arquivo (não um diretório).
        if (!file.exists() || !file.isFile()) {
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

    /**
     * Envia uma resposta de erro HTTP padrão.
     * @param exchange O objeto da troca HTTP.
     * @param statusCode O código de status do erro (ex: 403, 404).
     * @param message A mensagem de erro.
     */
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }

    /**
     * Determina o tipo MIME de um arquivo com base em sua extensão.
     * @param fileName O nome do arquivo.
     * @return A string do tipo MIME correspondente, ou "application/octet-stream" como padrão.
     */
    private String getMimeType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            String extension = fileName.substring(lastDot + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}