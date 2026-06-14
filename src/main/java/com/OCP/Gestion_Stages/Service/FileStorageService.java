package com.OCP.Gestion_Stages.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Stockage des fichiers uploadés sur le disque local (au lieu de bytea en base).
 * Renvoie/consomme des chemins relatifs au dossier racine ({@code app.storage.path}).
 * Backward-compatible : les anciennes lignes gardent leur contenu en base, ce service
 * n'intervient que pour les fichiers possédant un chemin.
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.storage.path:./uploads}") String path) {
        this.root = Paths.get(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("Dossier de stockage des fichiers : {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier de stockage : " + root, e);
        }
    }

    /** Écrit le contenu sur disque, renvoie le chemin relatif à stocker en base (null si contenu vide). */
    public String store(byte[] content, String originalName) {
        if (content == null || content.length == 0) return null;
        String safeName = (originalName == null || originalName.isBlank())
                ? "fichier" : originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String relative = UUID.randomUUID() + "_" + safeName;
        Path target = resolve(relative);
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new RuntimeException("Erreur écriture fichier sur disque : " + relative, e);
        }
        return relative;
    }

    /** Lit le contenu d'un fichier à partir de son chemin relatif. */
    public byte[] read(String relativePath) {
        if (relativePath == null) return null;
        Path target = resolve(relativePath);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture fichier sur disque : " + relativePath, e);
        }
    }

    /** Supprime le fichier (silencieux si absent ou erreur). */
    public void delete(String relativePath) {
        if (relativePath == null) return;
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("Suppression fichier échouée {} : {}", relativePath, e.getMessage());
        }
    }

    /** Résout et valide qu'on reste bien sous le dossier racine (anti path-traversal). */
    private Path resolve(String relativePath) {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Chemin de fichier invalide : " + relativePath);
        }
        return target;
    }
}
