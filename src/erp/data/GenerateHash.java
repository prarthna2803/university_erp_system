package erp.data;

import org.mindrot.jbcrypt.BCrypt;

public class GenerateHash {
    public static void main(String[] args) {
        String pw = "12345";
        String hash = BCrypt.hashpw(pw, BCrypt.gensalt());
        System.out.println(hash);
    }
}
