package com.example.atlasevents;
import org.mindrot.jbcrypt.BCrypt;

/**
 * This class will be responsible for salting and hashing passwords amd checking password match
 * Ensures passwords are not stored as plaintext
 */
public class PasswordHasher {
    public PasswordHasher(){}

    /**
     *
     * @param password the password to be hashed
     * @return a salted and hashed password
     */
    public String passHash(String password){
        String hashedPW = BCrypt.hashpw(password, BCrypt.gensalt());
        return hashedPW;
    }

    /**
     *
     * @param password password that is entered by user
     * @param hashed    salted and hashed password stored in database
     * @return  true if passwords match
     */
    public boolean checkPass(String password, String hashed){
        return BCrypt.checkpw(password,hashed);
    }
}
