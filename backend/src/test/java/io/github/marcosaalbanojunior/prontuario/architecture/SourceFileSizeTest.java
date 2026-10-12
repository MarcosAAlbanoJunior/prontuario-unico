package io.github.marcosaalbanojunior.prontuario.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

// Large classes usually hide more than one responsibility; split them instead of growing them.
@DisabledInNativeImage
@DisplayName("Source file size (AGENTS.md section 5.1)")
class SourceFileSizeTest {

    private static final int MAX_LINES = 200;
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Test
    @DisplayName("keeps every main source file within 200 lines")
    void should_stayWithinLimit_when_sourceFilesGrow() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            var oversized = files
                    .filter(file -> file.toString().endsWith(".java"))
                    .filter(file -> lineCount(file) > MAX_LINES)
                    .map(file -> MAIN_SOURCES.relativize(file) + " (" + lineCount(file) + " lines)")
                    .toList();

            assertThat(oversized).as("source files over %d lines", MAX_LINES).isEmpty();
        }
    }

    private static long lineCount(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
