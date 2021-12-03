
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.ReEncryptRequest;

public class Main {

    // From slides -------------------------------------
    static String myKeyId = "AKIAX742FUDUQXXXXXXX";
    static String mySecretKey = "MY_SECRET_KEY";
    static final String CIPHER = "DES";
    static final String CIPHER_GOOD ="RSA/ECB/OAEPPadding";

    static final String VALID_PATH1 = "./test/file1.txt";
    static final String VALID_PATH2 = "./test/file2.txt";
    static final String DEFAULT_VALID_PATH = "./test/file3.txt";

    public static void main(String[] args) {
        AWSCredentials creds =
            getCreds(myKeyId, mySecretKey);
        System.out.println(creds.getAWSSecretKey());

        AWSCredentials creds2 = getCreds();
        System.out.println(creds2.getAWSSecretKey());

        run1();
        run2();

        reEncrypt1();
        reEncrypt2();
    }

    // Bad
    static AWSCredentials getCreds(String id, String key) {
        return new BasicAWSCredentials(id, key);
    }

    // Good
    static AWSCredentials getCreds() {
        DefaultAWSCredentialsProviderChain creds =
            new DefaultAWSCredentialsProviderChain();
        return creds.getCredentials();
    }

    // Bad
    public static void run1() {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER);
            System.out.println(cipher);
        }
        catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    // Good
    public static void run2() {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_GOOD);
            System.out.println(cipher);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    //Bad
    public static void createCookie1(final HttpServletResponse response) {
        Cookie cookie = new Cookie("name", "value");
        response.addCookie(cookie);

    }

    // Good
    public static void createCookie2(final HttpServletResponse response) {
        Cookie cookie = new Cookie("name", "value");
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    // Bad
    public static void reEncrypt1() {
        AWSKMS client = AWSKMSClientBuilder.standard().build();
        ByteBuffer sourceCipherTextBlob = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0});

        DecryptRequest req = new DecryptRequest()
            .withCiphertextBlob(sourceCipherTextBlob);
        ByteBuffer plainText = client.decrypt(req).getPlaintext();

        EncryptRequest res = new EncryptRequest()
            .withKeyId("NewKeyId")
            .withPlaintext(plainText);
        ByteBuffer ciphertext = client.encrypt(res).getCiphertextBlob();
        System.out.println(ciphertext);
    }

    // Good
    public static void reEncrypt2() {
        ByteBuffer sourceCipherTextBlob = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0});

        ReEncryptRequest req = new ReEncryptRequest()
            .withCiphertextBlob(sourceCipherTextBlob)
            .withDestinationKeyId("NewKeyId");
        ByteBuffer ciphertext = req.getCiphertextBlob();
        System.out.println(ciphertext);
    }

    private String decode(final String val, final String enc) {
        try {
            return java.net.URLDecoder.decode(val, enc);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Bad
    public void pathTraversal1(HttpServletRequest request) throws IOException {
        javax.servlet.http.Cookie[] theCookies = request.getCookies();
        String path = "";
        if (theCookies != null) {
            for (javax.servlet.http.Cookie theCookie : theCookies) {
                if (theCookie.getName().equals("thePath")) {
                    path = decode(theCookie.getValue(), "UTF-8");
                    break;
                }
            }
        }
        if (!path.equals("")) {
            String fileName = path + ".txt";
            String decStr = new String(org.apache.commons.codec.binary.Base64.decodeBase64(
                org.apache.commons.codec.binary.Base64.encodeBase64(fileName.getBytes())));
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(decStr);
            java.io.FileDescriptor fd = fileOutputStream.getFD();
            System.out.println(fd.toString());
        }
    }

    // Good
    public void pathTraversal2(HttpServletRequest request) throws IOException {
        javax.servlet.http.Cookie[] theCookies = request.getCookies();
        String path = "";
        if (theCookies != null) {
            for (javax.servlet.http.Cookie theCookie : theCookies) {
                if (theCookie.getName().equals("thePath")) {
                    path = decode(theCookie.getValue(), "UTF-8");
                    break;
                }
            }
        }
        String fileName = "";
        if (!path.equals("")) {
            if (path.equals(VALID_PATH1)) {
                fileName = VALID_PATH1;
            } else if (path.equals(VALID_PATH2)) {
                fileName = VALID_PATH2;
            } else {
                fileName = DEFAULT_VALID_PATH;
            }
            String decStr = new String(org.apache.commons.codec.binary.Base64.decodeBase64(
                org.apache.commons.codec.binary.Base64.encodeBase64(fileName.getBytes())));
            try(java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(decStr))
            {
              java.io.FileDescriptor fd = fileOutputStream.getFD();
              System.out.println(fd.toString());
            } catch(Exception exception) {
              System.out.println(exception);
            }

        }
    }
}
