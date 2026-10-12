package io.github.marcosaalbanojunior.prontuario.domain.service.quickentry;

import io.github.marcosaalbanojunior.prontuario.domain.model.quickentry.ParseOutcome;
interface ArgsParser {

    ParseOutcome parse(String args);
}
