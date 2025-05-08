package src;
/**
 * PHASE 3 PROJECT MARKETPLACE
 *
 * @author Samridhi
 * @version 7/5/25
 */

public class PasswordStrengthChecker {
    public enum Strength {
        WEAK, MEDIUM, STRONG
    }

    public static Strength checkStrength(String password) {
        if (password == null || password.length() < 8) {
            return Strength.WEAK;
        }

        boolean hasNumber = false;
        boolean hasSpecial = false;
        boolean hasUpper = false;
        boolean hasLower = false;

        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) hasNumber = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        if (!hasNumber || password.length() < 12) {
            return Strength.WEAK;
        }

        int score = 0;
        // Base points for meeting minimum requirements
        score += 2; 

        // Additional points for complexity
        if (hasSpecial) score += 2;
        if (hasUpper) score += 1;
        if (hasLower) score += 1;
        if (password.length() >= 16) score += 2;

        // Adjusted thresholds
        if (score >= 4) return Strength.STRONG;
        if (score >= 3) return Strength.MEDIUM;
        return Strength.WEAK;
    }

    public static String getStrengthColor(Strength strength) {
        switch (strength) {
            case STRONG: return "#00FF00"; // Green
            case MEDIUM: return "#FFA500"; // Orange
            case WEAK: return "#FF0000";   // Red
            default: return "#808080";     // Gray
        }
    }
}
