package repz.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Arquivos", description = "Upload e preview de fotos de perfil")
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/files")
public interface FileController {

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto de perfil", description = "Faz upload de uma imagem e a vincula ao usuário autenticado. Retorna a URL de acesso.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Arquivo enviado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    ResponseEntity<String> upload(
            @Parameter(description = "Arquivo de imagem")
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) Authentication authentication);

    @GetMapping("/preview")
    @Operation(summary = "Preview de arquivo", description = "Gera e retorna a URL temporária de acesso ao arquivo pelo nome do objeto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URL gerada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    ResponseEntity<String> preview(
            @Parameter(description = "Nome do objeto no storage (ex: users/1/uuid.jpg)")
            @RequestParam String fileName);

    @GetMapping("/me")
    @Operation(summary = "Minha foto de perfil", description = "Retorna a URL temporária da foto do usuário autenticado, ou 204 se ele não tiver foto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URL da foto retornada"),
            @ApiResponse(responseCode = "204", description = "Usuário não possui foto"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    ResponseEntity<String> minhaFoto(@Parameter(hidden = true) Authentication authentication);
}
