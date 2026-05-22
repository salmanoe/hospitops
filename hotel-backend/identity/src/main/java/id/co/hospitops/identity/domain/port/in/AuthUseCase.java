package id.co.hospitops.identity.domain.port.in;

import id.co.hospitops.identity.application.command.LoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;

public interface AuthUseCase {
    LoginResponse login(LoginCommand command);

    void logout(String token);
}
