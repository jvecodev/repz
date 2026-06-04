package repz.app.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RelatorioIAUpdateRequest(@NotBlank String conteudo) {}
