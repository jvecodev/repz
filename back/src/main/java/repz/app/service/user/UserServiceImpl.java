package repz.app.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import repz.app.dto.request.AdminCreateRequest;
import repz.app.dto.request.UserCreateRequest;
import repz.app.dto.request.UserPutRequest;
import repz.app.dto.request.UserSelfUpdateRequest;
import repz.app.dto.response.UserGetResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.*;
import repz.app.persistence.mapper.UserMapper;
import repz.app.persistence.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AcademiaRepository academiaRepository;
    private final PlanoRepository planoRepository;
    private final PersonalRepository personalRepository;
    private final AlunoRepository alunoRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final Mensagens mensagens;

    @Override
    public List<UserGetResponse> findAll() {
        return userRepository.findByDeletedAtIsNull()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserGetResponse findById(Integer id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                mensagens.get("usuario.nao.encontrado")));
        return userMapper.toResponse(user);
    }

    @Override
    public void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void criarUsuario(UserCreateRequest dto, Authentication auth) {

        if (dto.role() == UserRole.ADMIN) {
            throw new AccessDeniedException(mensagens.get("usuario.criacao.role.negada", dto.role()));
        }

        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    mensagens.get("usuario.email.ja.cadastrado"));
        }

        Academia academia = academiaRepository.findById(dto.academiaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        mensagens.get("academia.nao.encontrada")));

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(dto.role());
        user.setActive(true);
        user = userRepository.save(user);

        switch (dto.role()) {
            case ALUNO -> {
                if (dto.planoId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            mensagens.get("usuario.criacao.aluno.plano.obrigatorio"));
                }
                Plano plano = planoRepository.findByIdAndAcademiaId(dto.planoId(), dto.academiaId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                mensagens.get("plano.nao.encontrado")));
                if (alunoRepository.existsByUsuarioIdAndAcademiaId(user.getId(), dto.academiaId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            mensagens.get("aluno.ja.matriculado"));
                }
                Aluno aluno = new Aluno();
                aluno.setUsuario(user);
                aluno.setAcademia(academia);
                aluno.setPlano(plano);
                aluno.setDataInicio(LocalDate.now());
                aluno.setAtivo(true);
                alunoRepository.save(aluno);
            }
            case PERSONAL -> {
                Personal personal = new Personal();
                personal.setUser(user);
                personal.setAcademia(academia);
                personal.setAtivo(true);
                personalRepository.save(personal);
            }
            case GERENTE -> {
                academia.setResponsibleUser(user);
                academiaRepository.save(academia);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    mensagens.get("usuario.criacao.role.invalida"));
        }
    }

    @Override
    public void criarAdmin(AdminCreateRequest dto) {

        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    mensagens.get("usuario.email.ja.cadastrado"));
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public void atualizar(Integer id, UserPutRequest dto) {

        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                mensagens.get("usuario.nao.encontrado")));

        if (!user.getEmail().equalsIgnoreCase(dto.email())) {
            garantirEmailDisponivel(dto.email());
            user.setEmail(dto.email());
        }
        user.setName(dto.name());
        if (dto.role() != null) {
            user.setRole(dto.role());
        }
        if (dto.active() != null) {
            user.setActive(dto.active());
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void atualizarMeuPerfil(UserSelfUpdateRequest dto, Authentication auth) {

        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException(mensagens.get("auth.usuario.nao.autenticado"));
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                mensagens.get("usuario.nao.encontrado")));

        if (!user.getEmail().equalsIgnoreCase(dto.email())) {
            garantirEmailDisponivel(dto.email());
            user.setEmail(dto.email());
        }
        user.setName(dto.name());
        if (dto.senha() != null && !dto.senha().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.senha()));
        }
        userRepository.save(user);
    }

    private void garantirEmailDisponivel(String email) {
        userRepository.findByEmail(email).ifPresent(outro -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    mensagens.get("usuario.email.ja.cadastrado"));
        });
    }

    @Override
    public void desativar(Integer id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                mensagens.get("usuario.nao.encontrado")));
        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void ativar(Integer id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                mensagens.get("usuario.nao.encontrado")));
        user.setActive(true);
        user.setDeletedAt(null);
        userRepository.save(user);
    }
}
