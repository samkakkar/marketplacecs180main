package src;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
/**
 * MarketplaceServer.java
 * A GUI-based client application for
 * interacting with a marketplace server.
 *
 * @author samridhi
 * @version 07/05/2025
 */

public class MarketplaceServer {
    private static ServerSocket serverSocket;
    private static boolean running = true;

    public static final String PRODUCTS_DIR = "products/";
    public static final String CHATS_DIR = "chats/";
    public static final String CLIENTS_FILE = "clients.txt";
    public static final String SELLERS_FILE = "sellers.txt";
    public static final String BALANCES_FILE = "balances.txt";
    public static final String TRANSACTIONS_FILE = "transactions.txt";
    public static final String IMAGE_DIR = "images/";

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(8881);
            System.out.println("Server started on port 8881");

            initializeDirectories();
            initializeFiles();

            new Thread(MarketplaceServer::handleImageRequests).start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running = false;
                try {
                    if (serverSocket != null) serverSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing server socket: " + e.getMessage());
                }
            }));

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    static void initializeDirectories() {
        new File(PRODUCTS_DIR).mkdirs();
        new File(CHATS_DIR).mkdirs();
        new File(IMAGE_DIR).mkdirs();
    }

    static void initializeFiles() throws IOException {
        new File(CLIENTS_FILE).createNewFile();
        new File(SELLERS_FILE).createNewFile();
        new File(BALANCES_FILE).createNewFile();
        new File(TRANSACTIONS_FILE).createNewFile();
    }


    public static void handleImageRequests() {
        try (ServerSocket imageServerSocket = new ServerSocket(8882)) {
            System.out.println("Image server started on port 8882");

            while (running) {
                Socket clientSocket = imageServerSocket.accept();
                new Thread(() -> {
                    try (
                            InputStream is = clientSocket.getInputStream();
                            OutputStream os = clientSocket.getOutputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is))
                    ) {
                        String command = reader.readLine();
                        if (command == null) return;

                        if (command.startsWith("UPLOAD:")) {
                            // Handle image upload
                            String imageName = command.substring(7).trim();
                            File imageFile = new File(IMAGE_DIR + imageName);
                            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                            }
                            os.write("UPLOAD_SUCCESS".getBytes());
                        } else {
                            // Handle image download
                            String imageName = command.trim();
                            File imageFile = new File(IMAGE_DIR + imageName);
                            if (imageFile.exists()) {
                                try (FileInputStream fis = new FileInputStream(imageFile)) {
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    while ((bytesRead = fis.read(buffer)) != -1) {
                                        os.write(buffer, 0, bytesRead);
                                    }
                                }
                            } else {
                                os.write("Image not found".getBytes());
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Image server error: " + e.getMessage());
                    } finally {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            System.err.println("Error closing image socket: " + e.getMessage());
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Image server failed: " + e.getMessage());
        }
    }

    public static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String currentUser;
        private boolean isSeller;

        public void setOut(PrintWriter out) {
            this.out = out;
        }

        public void setIn(BufferedReader in) {
            this.in = in;
        }
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                out.println("Welcome to the Marketplace Server!");
                sendMainMenu();

                while (true) {
                    String choice = in.readLine();
                    if (choice == null) break;

                    switch (choice) {
                        case "1":
                            if (handleLogin()) {
                                if (isSeller) {
                                    handleSellerMenu();
                                } else {
                                    handleClientMenu();
                                }
                            } else {
                                out.println("LOGIN_FAILED");
                            }
                            break;
                        case "2":
                            handleAccountCreation();
                            break;
                        case "3":
                            out.println("Goodbye!");
                            return;
                        default:
                            out.println("Invalid option. Please try again.");
                    }
                    sendMainMenu();
                }
            } catch (IOException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client connection: " + e.getMessage());
                }
            }
        }

        void sendMainMenu() {
            out.println("=== MAIN MENU ===");
            out.println("1. Login");
            out.println("2. Create New Account");
            out.println("3. Exit");
            out.println("===END MENU===");
            out.println("Please enter your choice (1-3):");
        }

        boolean handleLogin() throws IOException {
            out.println("Enter username:");
            String username = in.readLine();
            if (username == null) return false;

            out.println("Enter password:");
            String password = in.readLine();
            if (password == null) return false;

            out.println("Are you a Seller (1) or Client (2)?");
            String role = in.readLine();
            if (role == null) return false;

            if ("1".equals(role)) {
                if (checkCredentials(SELLERS_FILE, username, password)) {
                    currentUser = username;
                    isSeller = true;
                    out.println("LOGIN_SUCCESS_SELLER");
                    out.println("ROLE:SELLER");
                    return true;
                }
            }

            if ("2".equals(role)) {
                if (checkCredentials(CLIENTS_FILE, username, password)) {
                    currentUser = username;
                    isSeller = false;
                    out.println("LOGIN_SUCCESS_CLIENT");
                    out.println("ROLE:CLIENT");
                    return true;
                }
            }

            return false;
        }

        void handleAccountCreation() throws IOException {
            out.println("Enter username:");
            String username = in.readLine();
            out.println("Enter password:");
            String password = in.readLine();
            out.println("Are you a Seller (1) or Client (2)?");
            String role = in.readLine();

            if (checkUserExists(username)) {
                out.println("Account already exists.");
                return;
            }

            String file = role.equals("1") ? SELLERS_FILE : CLIENTS_FILE;
            synchronized (file.intern()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    writer.write(username + ":" + password);
                    writer.newLine();
                }
            }

            updateBalance(username, 100.00);
            out.println("Account created successfully with starting balance of $100.00");
        }

        boolean checkCredentials(String file, String username, String password) throws IOException {
            synchronized (file.intern()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        boolean checkUserExists(String username) throws IOException {
            return checkUserExists(username, CLIENTS_FILE) || checkUserExists(username, SELLERS_FILE);
        }

        boolean checkUserExists(String username, String file) throws IOException {
            synchronized (file.intern()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(username + ":")) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        void handleClientMenu() throws IOException {
            boolean firstTime = true;

            while (true) {
                try {
                    if (firstTime) {
                        out.println("=== CLIENT MENU ===");
                        out.println("1. Shop");
                        out.println("2. Chat with Seller");
                        out.println("3. Search Products");
                        out.println("4. Top Up Wallet");
                        out.println("5. View Balance");
                        out.println("6. View Transaction History");
                        out.println("7. Delete Account");
                        out.println("8. Logout");
                        out.println("===END_MENU===");
                        firstTime = false;
                    }

                    out.println("Please select your choice (1-8):");
                    String choice = in.readLine();

                    if (choice == null) {
                        System.out.println("[INFO] Client disconnected.");
                        break;
                    }

                    switch (choice) {
                        case "1": handleShopping(); break;
                        case "2": handleClientChat(); break;
                        case "3": handleProductSearch(); break;
                        case "4": handleTopUp(); break;
                        case "5": out.println("Your current balance: $" + getBalance(currentUser)); break;
                        case "6": viewTransactionHistory(currentUser); break;
                        case "7": deleteAccount(currentUser); return;
                        case "8":
                            out.println("Logging out...");
                            return;
                        default:
                            out.println("Invalid choice, try again.");
                    }

                } catch (IOException e) {
                    System.out.println("[INFO] Client forcibly disconnected from client menu.");
                    break;
                }
            }
        }


        void handleShopping() throws IOException {
            String[] sellers = getAllSellers();
            out.println("AVAILABLE_SELLERS");
            for (int i = 0; i < sellers.length; i++) {
                out.println((i + 1) + ". " + sellers[i]);
            }
            out.println("END_SELLERS");

            out.println("Select a seller to view products (Enter number):");
            String choiceStr = in.readLine();
            try {
                int sellerChoice = Integer.parseInt(choiceStr);
                if (sellerChoice > 0 && sellerChoice <= sellers.length) {
                    String selectedSeller = sellers[sellerChoice - 1];
                    String[] products = getProducts(selectedSeller);

                    out.println("SELLER_PRODUCTS");

                    List<String> sentProducts = new ArrayList<>();
                    int count = 1;

                    for (String p : products) {
                        p = p.trim();
                        if (!sentProducts.contains(p)) {
                            out.println(count + ". " + p);
                            sentProducts.add(p);
                            count++;
                        }
                    }

                    for (String p : sentProducts) {
                        String[] parts = p.split(",");
                        if (parts.length >= 3 && !parts[2].equalsIgnoreCase("none")) {
                            out.println("IMG:" + parts[2].trim());
                        }
                    }

                    out.println("END_PRODUCTS");

                    out.println("Enter product number to purchase (or 0 to cancel):");
                    String productChoice = in.readLine();
                    int productNum = Integer.parseInt(productChoice);

                    if (productNum > 0 && productNum <= sentProducts.size()) {
                        String[] productInfo = sentProducts.get(productNum - 1).split(",");
                        String productName = productInfo[0];
                        double price = Double.parseDouble(productInfo[1]);

                        out.println("Confirm purchase of '" + productName + "' for $" + price + "? (yes/no)");
                        String confirm = in.readLine();

                        if ("yes".equalsIgnoreCase(confirm)) {
                            processPurchase(currentUser, selectedSeller, price, productName);
                        } else {
                            out.println("Purchase cancelled.");
                        }
                    }
                } else {
                    out.println("Invalid seller selection.");
                }
            } catch (NumberFormatException e) {
                out.println("Please enter valid numbers.");
            }
        }


        void processPurchase(String buyer, String seller, double amount, String productName) throws IOException {
            double buyerBalance = getBalance(buyer);

            if (buyerBalance < amount) {
                out.println("Payment failed: Insufficient funds. Your balance: $" + buyerBalance);
                return;
            }

            updateBalance(buyer, -amount);
            updateBalance(seller, amount);
            recordTransaction(buyer, seller, amount, "Purchase: " + productName);

            out.println("Payment successful! Remaining balance: $" + getBalance(buyer));
        }

        double getBalance(String user) throws IOException {
            try (BufferedReader reader = new BufferedReader(new FileReader(BALANCES_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2 && parts[0].equals(user)) {
                        return Double.parseDouble(parts[1]);
                    }
                }
            }
            return 0.0;
        }

        void updateBalance(String user, double amount) throws IOException {
            List<String> lines = new ArrayList<>();
            boolean found = false;

            try (BufferedReader reader = new BufferedReader(new FileReader(BALANCES_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2 && parts[0].equals(user)) {
                        double updated = Double.parseDouble(parts[1]) + amount;
                        lines.add(user + ":" + updated);
                        found = true;
                    } else {
                        lines.add(line);
                    }
                }
            }

            if (!found) {
                lines.add(user + ":" + amount);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(BALANCES_FILE))) {
                for (String l : lines) {
                    writer.write(l);
                    writer.newLine();
                }
            }
        }

        void handleTopUp() throws IOException {
            out.println("Enter amount to top up:");
            String input = in.readLine();
            try {
                double amount = Double.parseDouble(input);
                if (amount <= 0) {
                    out.println("Amount must be positive.");
                    return;
                }
                updateBalance(currentUser, amount);
                recordTransaction("SYSTEM", currentUser, amount, "Top-up");
                out.println("Top up successful. New balance: $" + getBalance(currentUser));
            } catch (NumberFormatException e) {
                out.println("Invalid amount.");
            }
        }
        void handleSellerMenu() throws IOException {
            boolean firstTime = true;
            while (true) {
                try {
                    if (firstTime) {
                        out.println("=== SELLER MENU ===");
                        out.println("1. Add Product");
                        out.println("2. Delete Product");
                        out.println("3. View My Products");
                        out.println("4. Chat with Clients");
                        out.println("5. View Balance");
                        out.println("6. View Transaction History");
                        out.println("7. Delete Account");
                        out.println("8. Logout");
                        out.println("===END_MENU===");
                        firstTime = false;
                    }

                    out.println("Please select your choice (1-8):");
                    String choice = in.readLine();
                    if (choice == null) break;

                    switch (choice) {
                        case "1": handleAddProduct(); break;
                        case "2": handleDeleteProduct(); break;
                        case "3": viewProducts(currentUser); break;
                        case "4": handleSellerChat(); break;
                        case "5": out.println("Your current balance: $" + getBalance(currentUser)); break;
                        case "6": viewTransactionHistory(currentUser); break;
                        case "7": deleteAccount(currentUser); return;
                        case "8": out.println("Logging out..."); return;
                        default: out.println("Invalid choice, try again.");
                    }
                } catch (IOException e) {
                    System.out.println("Seller disconnected unexpectedly.");
                    break;
                }
            }
        }


        void handleAddProduct() throws IOException {
            out.println("Enter product name:");
            String name = in.readLine();

            out.println("Enter price:");
            String priceStr = in.readLine();

            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                out.println("Invalid price input.");
                return;
            }

            out.println("Will you upload a product image? (yes/no):");
            String sendImage = in.readLine();
            String imageFileName = "none";

            if ("yes".equalsIgnoreCase(sendImage)) {
                imageFileName = currentUser + "_" + System.currentTimeMillis() + ".png";
                System.out.println("Sending image upload request with filename: " + imageFileName);
                out.println("SEND_IMAGE_NOW:" + imageFileName);

                // Wait for confirmation
                String uploadStatus = in.readLine();
                if (!"IMAGE_UPLOADED".equals(uploadStatus)) {
                    out.println("Image upload failed.");
                    return;
                }

                out.println("Expecting image upload to image server as: " + imageFileName);
            }

            synchronized (PRODUCTS_DIR.intern()) {
                File f = new File(PRODUCTS_DIR + currentUser + ".txt");
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
                    bw.write(name + "," + price + "," + imageFileName);
                    bw.newLine();
                }
            }

            out.println("Product added successfully.");
        }



        private int indexOf(byte[] data, byte[] pattern) {
            outer:
            for (int i = 0; i <= data.length - pattern.length; i++) {
                for (int j = 0; j < pattern.length; j++) {
                    if (data[i + j] != pattern[j]) continue outer;
                }
                return i;
            }
            return -1;
        }


        void handleDeleteProduct() throws IOException {
            out.println("Enter product name to delete:");
            String name = in.readLine();

            File file = new File(PRODUCTS_DIR + currentUser + ".txt");
            File temp = new File(PRODUCTS_DIR + currentUser + "_temp.txt");

            try (BufferedReader reader = new BufferedReader(new FileReader(file));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(name + ",")) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }

            file.delete();
            temp.renameTo(file);
            out.println("Product deleted.");
        }

        String[] getAllSellers() throws IOException {
            List<String> sellers = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(SELLERS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length > 0) sellers.add(parts[0]);
                }
            }
            return sellers.toArray(new String[0]);
        }

        String[] getProducts(String seller) throws IOException {
            List<String> products = new ArrayList<>();
            File file = new File("products/" + seller + ".txt");

            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            products.add(line);
                        }
                    }
                }
            }

            return products.toArray(new String[0]);
        }

        void viewProducts(String seller) throws IOException {
            String[] products = getProducts(seller);
            out.println("=== PRODUCT LIST ===");
            for (String p : products) {
                out.println(p);
            }
            out.println("===END OF PRODUCTS===");
        }

        void handleProductSearch() throws IOException {
            out.println("Enter product name to search:");
            String query = in.readLine();
            if (query == null || query.trim().isEmpty()) return;

            boolean found = false;
            String[] sellers = getAllSellers();
            for (String s : sellers) {
                for (String p : getProducts(s)) {
                    if (p.toLowerCase().contains(query.toLowerCase())) {
                        if (!found) out.println("=== SEARCH RESULTS ===");
                        found = true;
                        out.println(s + ": " + p);
                    }
                }
            }
            out.println(found ? "END_RESULTS" : "NOT AVAILABLE");
        }

        void recordTransaction(String from, String to, double amount, String note) throws IOException {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE, true))) {
                writer.write(from + "|" + to + "|" + amount + "|" + note + "|" + System.currentTimeMillis());
                writer.newLine();
            }
        }

        void viewTransactionHistory(String user) throws IOException {
            out.println("=== TRANSACTION HISTORY ===");
            try (BufferedReader reader = new BufferedReader(new FileReader(TRANSACTIONS_FILE))) {
                String line;
                boolean has = false;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4 && (parts[0].equals(user) || parts[1].equals(user))) {
                        has = true;
                        String direction = parts[0].equals(user) ? "To " + parts[1] : "From " + parts[0];
                        out.println(direction + ": $" + parts[2] + " - " + parts[3]);
                    }
                }
                if (!has) out.println("No transactions.");
            }
            out.println("===END OF HISTORY===");
        }


        void deleteAccount(String user) throws IOException {
            deleteFromFile(CLIENTS_FILE, user);
            deleteFromFile(SELLERS_FILE, user);
            new File(PRODUCTS_DIR + user + ".txt").delete();
            new File(CHATS_DIR + user + "_chat.txt").delete();
        }

        void deleteFromFile(String file, String user) throws IOException {
            File input = new File(file);
            File temp = new File(file + ".tmp");

            try (BufferedReader reader = new BufferedReader(new FileReader(input));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(user + ":")) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }

            input.delete();
            temp.renameTo(input);
        }

        void handleClientChat() throws IOException {
            String[] sellers = getAllSellers();
            out.println("=== AVAILABLE SELLERS ===");
            for (int i = 0; i < sellers.length; i++) {
                out.println((i + 1) + ". " + sellers[i]);
            }
            out.println("===END OF SELLERS LIST===");
            out.println("Enter the number of the seller you want to chat with:");
            String indexStr = in.readLine();
            try {
                int index = Integer.parseInt(indexStr);
                if (index > 0 && index <= sellers.length) {
                    String seller = sellers[index - 1];
                    File chatFile = new File(CHATS_DIR + seller + "_" + currentUser + "_chat.txt");
                    chatLoop(chatFile, "Client");
                }
            } catch (Exception e) {
                out.println("Invalid selection.");
            }
        }

        private void handleSellerChat() throws IOException {
            File chatFolder = new File(CHATS_DIR);
            if (!chatFolder.exists() || !chatFolder.isDirectory()) {
                out.println("No chat history folder found.");
                return;
            }

            File[] chatFiles = chatFolder.listFiles((dir, name) ->
                    name.toLowerCase().contains(currentUser.toLowerCase()) && name.endsWith("_chat.txt"));

            System.out.println("[DEBUG] Chat files for seller " + currentUser + ":");
            if (chatFiles != null && chatFiles.length > 0) {
                for (File f : chatFiles) {
                    System.out.println(" - " + f.getName());
                }
            }

            if (chatFiles == null || chatFiles.length == 0) {
                out.println("No active chat sessions found.");
                return;
            }

            out.println("=== ACTIVE CLIENT CHATS ===");
            List<File> availableChats = new ArrayList<>();
            for (int i = 0; i < chatFiles.length; i++) {
                File f = chatFiles[i];
                String partnerName = extractChatPartnerName(f.getName(), currentUser);
                out.println((i + 1) + ". " + partnerName);
                availableChats.add(f);
            }

            out.println("Enter the number of the chat to view:");
            String selection = in.readLine();

            int index;
            try {
                index = Integer.parseInt(selection.trim()) - 1;
            } catch (NumberFormatException e) {
                out.println("Invalid selection.");
                return;
            }

            if (index < 0 || index >= availableChats.size()) {
                out.println("Invalid chat selection.");
                return;
            }

            File selectedChat = availableChats.get(index);
            sendChatHistory(selectedChat);
        }
        private void sendChatHistory(File chatFile) {
            try (BufferedReader reader = new BufferedReader(new FileReader(chatFile))) {
                out.println("=== CHAT HISTORY ===");

                String line;
                while ((line = reader.readLine()) != null) {
                    out.println(line);
                }

                out.println("===END OF HISTORY===");
            } catch (IOException e) {
                out.println("Failed to read chat history: " + e.getMessage());
            }
        }
        private String extractChatPartnerName(String filename, String currentUser) {
            String nameWithoutExt = filename.replace("_chat.txt", "");
            String[] parts = nameWithoutExt.split("_");
            for (String part : parts) {
                if (!part.equalsIgnoreCase(currentUser)) {
                    return part;
                }
            }
            return "Unknown";
        }






        void sendAllProductsToClient() throws IOException {
            String[] sellers = getAllSellers();
            List<String> allProducts = new ArrayList<>();

            out.println("SELLER_PRODUCTS");

            int counter = 1;
            for (String seller : sellers) {
                String[] products = getProducts(seller);
                for (String product : products) {
                    allProducts.add(counter + ". " + product);
                    counter++;
                }
            }

            for (String line : allProducts) {
                out.println(line);
            }

            out.println("END_PRODUCTS");
        }


        void chatLoop(File chatFile, String role) throws IOException {
            out.println("=== CHAT HISTORY ===");
            if (chatFile.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(chatFile))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        out.println(line);
                    }
                }
            }
            out.println("===END OF HISTORY===");

            while (true) {
                out.println("Enter your message (type 'exit' to end chat):");
                String msg = in.readLine();
                if (msg == null || msg.equalsIgnoreCase("exit")) break;

                try (BufferedWriter w = new BufferedWriter(new FileWriter(chatFile, true))) {
                    w.write(role + " [" + currentUser + "]: " + msg);
                    w.newLine();
                }
                out.println("Message sent.");
            }
        }
    }
}



