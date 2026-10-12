package io.github.marcosaalbanojunior.prontuario.entries.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.MissingField;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.QuickEntryType;
import io.github.marcosaalbanojunior.prontuario.entries.application.port.in.RejectionCause;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.MissingInfo;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.QuickCommand;
import io.github.marcosaalbanojunior.prontuario.entries.domain.quick.RejectionReason;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

// The inbound port mirrors these domain enums by name so other modules never see the domain.
class PortEnumAlignmentTest {

    @Test
    void shouldMirrorDomainEnumsByName() {
        assertThat(names(QuickEntryType.values())).isEqualTo(names(QuickCommand.values()));
        assertThat(names(MissingField.values())).isEqualTo(names(MissingInfo.values()));
        assertThat(names(RejectionCause.values())).isEqualTo(names(RejectionReason.values()));
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }
}
