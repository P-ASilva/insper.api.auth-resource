package insper.api.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import insper.api.account.AccountController;
import insper.api.account.AccountIn;
import insper.api.account.AccountOut;
import insper.api.account.LoginIn;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class AuthService {

    @Autowired
    private AccountController accountController;

    @Autowired
    private JwtService jwtService;

    @SuppressWarnings("null")
    @CircuitBreaker(name = "authService", fallbackMethod = "fallbackRegister")
    public String register(Register in) {
        final String password = in.password().trim();
        if (null == password || password.isEmpty()) throw new IllegalArgumentException("Password is required");
        if (password.length() < 4) throw new IllegalArgumentException("Password must be at least 4 characters long");

        ResponseEntity<AccountOut> response = accountController.create(AccountIn.builder()
            .name(in.name())
            .email(in.email())
            .password(password)
            .build()
        );
        if (response.getStatusCode().isError()) throw new IllegalArgumentException("Invalid credentials");
        if (null == response.getBody()) throw new IllegalArgumentException("Invalid credentials");
        return response.getBody().id();
    }

    @CircuitBreaker(name = "authService", fallbackMethod = "fallbackAuthenticate")
    public LoginOut authenticate(String email, String password) {
        ResponseEntity<AccountOut> response = accountController.login(LoginIn.builder()
            .email(email)
            .password(password)
            .build()
        );
        if (response.getStatusCode().isError()) throw new IllegalArgumentException("Invalid credentials");
        if (null == response.getBody()) throw new IllegalArgumentException("Invalid credentials");
        final AccountOut account = response.getBody();

        // Cria um token JWT
        @SuppressWarnings("null")
        final String token = jwtService.create(account.id(), account.name(), "regular");

        return LoginOut.builder()
            .token(token)
            .build();
    }

    @CircuitBreaker(name = "authService", fallbackMethod = "fallbackSolve")
    public Token solve(String token) {
        return jwtService.getToken(token);
    }

    // Métodos de fallback
    public String fallbackRegister(Register in, Throwable t) {
        // Lógica de fallback para register
        System.out.println("Fallback register method triggered: " + t.getMessage());
        return "fallback-register";
    }

    public LoginOut fallbackAuthenticate(String email, String password, Throwable t) {
        // Lógica de fallback para authenticate
        System.out.println("Fallback authenticate method triggered: " + t.getMessage());
        return LoginOut.builder()
            .token("fallback-token")
            .build();
    }

    public Token fallbackSolve(String token, Throwable t) {
        // Lógica de fallback para solve
        System.out.println("Fallback solve method triggered: " + t.getMessage());
        return new Token("fallback-token");
    }
}
