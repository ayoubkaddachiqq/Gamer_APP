package tn.esprit.demo.util;

import java.security.SecureRandom;

public class TokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateSixDigitCode() {
        int value = 100000 + RANDOM.nextInt(900000);
        return Integer.toString(value);
    }
}
