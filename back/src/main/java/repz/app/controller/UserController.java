package repz.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import repz.app.dto.request.AdminCreateRequest;
import repz.app.dto.request.UserCreateRequest;
import repz.app.dto.request.UserPutRequest;
import repz.app.dto.request.UserSelfUpdateRequest;
import repz.app.dto.response.UserGetResponse;

import java.util.List;

@Tag(name = "Usuários", description = "Cadastro e administração de usuários")
@SecurityRequirement(name = "bearer-jwt")
@RequestMapping("/api/users")
public interface UserController {

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Lista os usuários ativos cadastrados. Requer perfil ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuários encontrados"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    ResponseEntity<List<UserGetResponse>> findAll();

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário", description = "Retorna os dados de um usuário pelo ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    ResponseEntity<UserGetResponse> findById(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Integer id);

    @PostMapping
    @Operation(
            summary = "Criar usuário",
            description = "Cria um usuário (ALUNO, PERSONAL ou GERENTE) já vinculado a uma academia. Requer perfil ADMIN ou GERENTE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Perfil solicitado não permitido")
    })
    ResponseEntity<Void> criar(
            @RequestBody @Valid UserCreateRequest userCreateRequest,
            @Parameter(hidden = true) Authentication authentication);

    @PostMapping("/admin")
    @Operation(
            summary = "Criar administrador",
            description = "Cria um usuário com perfil ADMIN. Requer perfil ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Admin criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    ResponseEntity<Void> criarAdmin(
            @RequestBody @Valid AdminCreateRequest adminCreateRequest);

    @GetMapping("/me")
    @Operation(summary = "Meu perfil", description = "Retorna os dados do usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil retornado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    ResponseEntity<UserGetResponse> obterMeuPerfil(
            @Parameter(hidden = true) Authentication authentication);

    @PutMapping("/me")
    @Operation(summary = "Atualizar meu perfil", description = "Atualiza nome, e-mail e senha do usuário autenticado. Disponível para qualquer perfil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    ResponseEntity<Void> atualizarMeuPerfil(
            @RequestBody @Valid UserSelfUpdateRequest userSelfUpdateRequest,
            @Parameter(hidden = true) Authentication authentication);

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário. Requer perfil ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    ResponseEntity<Void> atualizar(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Integer id,
            @RequestBody @Valid UserPutRequest userPutRequest);

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar usuário", description = "Reativa um usuário desativado. Requer perfil ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário ativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    ResponseEntity<Void> ativar(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Integer id);

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativar usuário", description = "Desativa um usuário por soft delete. Requer perfil ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário desativado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    ResponseEntity<Void> desativar(
            @Parameter(description = "ID do usuário", example = "1")
            @PathVariable Integer id);

    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Atualizar foto de perfil",
            description = "Faz upload de uma imagem (JPG ou PNG, máx. 5 MB) e a vincula ao perfil do usuário autenticado. Disponível para qualquer perfil."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido (formato, tamanho ou ausente)"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    ResponseEntity<UserGetResponse> atualizarFotoPerfil(
            @Parameter(description = "Imagem de perfil (JPG ou PNG, máx. 5 MB)")
            @RequestParam("foto") MultipartFile foto,
            @Parameter(hidden = true) Authentication authentication);
}
