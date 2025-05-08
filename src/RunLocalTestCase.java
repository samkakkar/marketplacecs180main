package src;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import src.MarketplaceServer;
import src.MarketplaceServerInterface;

import java.io.*;
import java.lang.String;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


import static org.junit.Assert.*;

/**
 * Localtestcases
 *
 * @author sherry
 * @version 08/05/2025
 */

public class RunLocalTestCase {
    private static final String TEST_DIR = "test_run/";
    private MarketplaceServer.ClientHandler handler;

    // asked gpt to generate sample data for me
    private void createMockClientAndSellerFiles(String clientsPath, String sellersPath, String balancesPath) throws IOException {
        // Create mock clients with usernames and passwords
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(clientsPath))) {
            writer.write("alice:password123\n");
            writer.write("bob:securepass\n");
            writer.write("charlie:password456\n");
        }

        // Create mock sellers with usernames and passwords
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sellersPath))) {
            writer.write("seller1:sellpass1\n");
            writer.write("seller2:sellpass2\n");
            writer.write("seller3:sellpass3\n");
        }

        // Create mock balances with usernames and starting balances
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(balancesPath))) {
            writer.write("alice:100.0\n");
            writer.write("bob:100.0\n");
            writer.write("charlie:50.0\n");
        }
    }

    @Before
    public void setup() throws Exception {
        System.setProperty("products.dir", MarketplaceServerInterface.PRODUCTS_DIR);
        System.setProperty("chats.dir", MarketplaceServerInterface.CHATS_DIR);
        System.setProperty("clients.file", MarketplaceServerInterface.CLIENTS_FILE);
        System.setProperty("sellers.file", MarketplaceServerInterface.SELLERS_FILE);
        System.setProperty("balances.file", MarketplaceServerInterface.BALANCES_FILE);

        new File(MarketplaceServerInterface.PRODUCTS_DIR).mkdirs();
        new File(MarketplaceServerInterface.CHATS_DIR).mkdirs();

        File balancesFile = new File(MarketplaceServerInterface.BALANCES_FILE);
        if (!balancesFile.exists()) {
            balancesFile.createNewFile(); // Create the file if it doesn't exist
        }

        createMockClientAndSellerFiles(MarketplaceServerInterface.CLIENTS_FILE, MarketplaceServerInterface.SELLERS_FILE, MarketplaceServerInterface.BALANCES_FILE);

        handler = new MarketplaceServer.ClientHandler(null);
    }

    // === TESTS ===

    @Test(timeout = 1000)
    public void testAccountCreation() throws Exception {
        handler.setOut(new PrintWriter(System.out, true));
        handler.setIn(new BufferedReader(new StringReader("testuser\ntestpass\n2\n")));

        Method handleAccountCreationMethod = MarketplaceServer.ClientHandler.class.getDeclaredMethod("handleAccountCreation");
        handleAccountCreationMethod.setAccessible(true);
        handleAccountCreationMethod.invoke(handler);

        assertTrue(new File(MarketplaceServerInterface.CLIENTS_FILE).exists());

        String content = readFile(MarketplaceServerInterface.CLIENTS_FILE);
        assertTrue(content.contains("testuser:testpass"));

        String balanceContent = readFile(MarketplaceServerInterface.BALANCES_FILE);
        assertTrue(balanceContent.contains("testuser:100.0"));
    }

    @Test(timeout = 1000)
    public void testDeleteAccount() throws Exception {
        String username = "testuser";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MarketplaceServerInterface.CLIENTS_FILE, true))) {
            writer.write(username + ":password\n");
        }

        Method deleteAccountMethod = MarketplaceServer.ClientHandler.class.getDeclaredMethod("deleteAccount", String.class);
        deleteAccountMethod.setAccessible(true);
        deleteAccountMethod.invoke(handler, username);

        try (BufferedReader reader = new BufferedReader(new FileReader(MarketplaceServerInterface.CLIENTS_FILE))) {
            String line;
            boolean accountDeleted = true;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(username + ":")) {
                    accountDeleted = false;
                    break;
                }
            }
            assertTrue("Account was not deleted.", accountDeleted);
        }
    }

    @Test(timeout = 1000)
    public void testCheckUserExists() throws Exception {
        String username = "checkUser";
        File clientsFile = new File(System.getProperty("clients.file"));

        if (!clientsFile.exists()) {
            clientsFile.createNewFile();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(clientsFile, true))) {
            writer.write(username + ":abc\n");
        }

        Method checkUserExistsMethod = MarketplaceServer.ClientHandler.class.getDeclaredMethod("checkUserExists", String.class);
        checkUserExistsMethod.setAccessible(true);  // Make the method accessible

        assertTrue((Boolean) checkUserExistsMethod.invoke(handler, username));  // Should return true as "checkUser" exists.

        assertFalse((Boolean) checkUserExistsMethod.invoke(handler, "nonexistentUser"));  // Should return false as this user does not exist.
    }

    @Test(timeout = 1000)
    public void testGetAllSellers() throws Exception {
        File sellersFile = new File(System.getProperty("sellers.file"));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sellersFile))) {
            writer.write("sell1:pw1\nsell2:pw2\n");
        }

        Method getAllSellersMethod = MarketplaceServer.ClientHandler.class.getDeclaredMethod("getAllSellers");
        getAllSellersMethod.setAccessible(true);

        Object result = getAllSellersMethod.invoke(handler);

        String[] sellers = (String[]) result;

        assertArrayEquals(new String[]{"sell1", "sell2"}, sellers);
    }

    @Test(timeout = 1000)
    public void testCheckCredentials() throws Exception {
        // Ensure the clients file exists
        File clientsFile = new File(System.getProperty("clients.file"));
        if (!clientsFile.exists()) {
            clientsFile.createNewFile();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(clientsFile))) {
            writer.write("userA:passA\n");
        }

        Method checkCredentialsMethod = MarketplaceServer.ClientHandler.class.getDeclaredMethod("checkCredentials", String.class, String.class, String.class);
        checkCredentialsMethod.setAccessible(true); // Make the private method accessible

        boolean resultValid = (boolean) checkCredentialsMethod.invoke(handler, clientsFile.getAbsolutePath(), "userA", "passA");
        boolean resultInvalid = (boolean) checkCredentialsMethod.invoke(handler, clientsFile.getAbsolutePath(), "userA", "wrongPass");
        assertTrue(resultValid);
        assertFalse(resultInvalid);
    }

    @After
    public void cleanup() {
        deleteDir(new File(TEST_DIR));
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    private void setField(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = MarketplaceServer.ClientHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, value);
    }

    // Helper method to read file content
    private String readFile(String path) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(new File(path).toPath()));
    }
}
