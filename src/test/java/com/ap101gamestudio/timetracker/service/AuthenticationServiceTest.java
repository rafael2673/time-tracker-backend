package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.JwtAuthenticationResponse;
import com.ap101gamestudio.timetracker.dto.RegisterRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.security.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;


    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldThrowExceptionWhenRegisteringExistingEmail() {
        String email = "teste@teste.com";
        RegisterRequest request = new RegisterRequest("Teste", email, "123456", null);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User(email, "hash", "Teste")));

        Assertions.assertThrows(DomainException.class, () -> authenticationService.register(request));
    }

    @Test
    void shouldRegisterNewUserSuccessfully() {
        String email = "novo@teste.com";
        RegisterRequest request = new RegisterRequest("Novo", email, "123456", null);

        Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode("123456")).thenReturn("hashed_password");
        Mockito.when(jwtService.generateToken(any(User.class))).thenReturn("fake_token");

        JwtAuthenticationResponse response = authenticationService.register(request);

        Assertions.assertEquals("fake_token", response.accessToken());
        Mockito.verify(userRepository, Mockito.times(1)).save(any(User.class));
    }
}