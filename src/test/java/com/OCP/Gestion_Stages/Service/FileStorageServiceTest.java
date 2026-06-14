package com.OCP.Gestion_Stages.Service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileStorageService — stockage des fichiers sur disque")
class FileStorageServiceTest {

    @TempDir
    Path tmp;

    private FileStorageService service() {
        return new FileStorageService(tmp.toString());
    }

    @Test
    @DisplayName("store puis read : aller-retour identique")
    void store_then_read_roundtrip() {
        FileStorageService svc = service();
        byte[] data = "contenu du cv".getBytes();

        String chemin = svc.store(data, "mon cv.pdf");

        assertThat(chemin).isNotNull().endsWith("_mon_cv.pdf");
        assertThat(svc.read(chemin)).isEqualTo(data);
    }

    @Test
    @DisplayName("store d'un contenu null/vide renvoie null")
    void store_null_or_empty_returns_null() {
        FileStorageService svc = service();
        assertThat(svc.store(null, "x.pdf")).isNull();
        assertThat(svc.store(new byte[0], "x.pdf")).isNull();
    }

    @Test
    @DisplayName("le nom de fichier est nettoyé (un seul segment, pas de séparateur)")
    void store_sanitizes_filename() {
        FileStorageService svc = service();
        String chemin = svc.store("x".getBytes(), "evil name!.pdf");
        assertThat(chemin).doesNotContain("/").doesNotContain(" ").doesNotContain("!").contains("evil");
    }

    @Test
    @DisplayName("delete supprime le fichier")
    void delete_removes_file() {
        FileStorageService svc = service();
        String chemin = svc.store("x".getBytes(), "a.pdf");

        svc.delete(chemin);

        assertThatThrownBy(() -> svc.read(chemin)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("read(null) renvoie null, delete(null) ne plante pas")
    void null_safe() {
        FileStorageService svc = service();
        assertThat(svc.read(null)).isNull();
        svc.delete(null); // ne doit pas lever d'exception
    }

    @Test
    @DisplayName("anti path-traversal : un chemin remontant hors du dossier est rejeté")
    void path_traversal_rejected() {
        FileStorageService svc = service();
        assertThatThrownBy(() -> svc.read("../../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
