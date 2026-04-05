package erp.service;

public class AuthResult {
    public int userId;
    public String role;

    public AuthResult(int userId, String role) {
        this.userId = userId;
        this.role = role;
    }
}
