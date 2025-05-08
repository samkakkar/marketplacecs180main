package src;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
/**
 * MarketplaceClientGUI.java
 * A GUI-based client application for
 * interacting with a marketplace server.
 *
 * @author samridhi
 * @version 07/05/2025
 */

public class MarketplaceClientGUI extends JFrame implements MarketplaceClientInterface  {
    private List<String> chatMessagesBuffer = new ArrayList<>();
    private boolean inChatMode = false;
    private String currentChatPartner = "";
    private boolean isClientRole = false;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextArea outputArea;
    private JComboBox<String> actionComboBox;
    private JButton submitButton;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    private JTextField inputField;
    private JButton inputSendButton;
    private JCheckBox rememberMe;

    private boolean awaitingInput = false;
    private boolean collectingMenu = false;
    private String currentUser = null;

    private List<String> sellerBuffer = new ArrayList<>();
    private boolean collectingSellers = false;
    private boolean collectingProducts = false;
    private List<String> productBuffer = new ArrayList<>();

    public MarketplaceClientGUI() {
        initializeGUI();
        connectToServer();
        startResponseHandler();
    }

    public void initializeGUI() {
        setTitle("Marketplace Client");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "login");
        mainPanel.add(createMainInterface(), "main");
        mainPanel.add(createChatPanel(), "chat");

        add(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create title label with larger font
        JLabel titleLabel = new JLabel("Marketplace Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Create a centered panel for the form
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Create form panel with smaller components
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Username field
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        usernameField = new JTextField(15);
        usernameField.setText(ConfigManager.getRememberedUsername());
        formPanel.add(usernameField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(15);
        formPanel.add(passwordField, gbc);

        // Show password checkbox
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JCheckBox showPassword = new JCheckBox("Show Password");
        showPassword.addActionListener(e -> {
            passwordField.setEchoChar(showPassword.isSelected() ? '\0' : '•');
        });
        formPanel.add(showPassword, gbc);

        // Password strength indicator
        gbc.gridx = 1;
        gbc.gridy = 3;
        JPanel strengthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel strengthLabel = new JLabel("Password Strength: ");
        JProgressBar strengthBar = new JProgressBar(0, 100);
        strengthBar.setStringPainted(true);
        strengthBar.setString("Weak");
        strengthBar.setForeground(Color.RED);
        strengthPanel.add(strengthLabel);
        strengthPanel.add(strengthBar);
        formPanel.add(strengthPanel, gbc);

        // Update password strength
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateStrength(); }
            public void removeUpdate(DocumentEvent e) { updateStrength(); }
            public void insertUpdate(DocumentEvent e) { updateStrength(); }

            /**
             * - Gets the current password from the field
             * - Checks strength using PasswordStrengthChecker
             * - Updates the progress bar color and value:
             *   - Red for weak passwords
             *   - Orange for medium passwords
             *   - Green (for strong passwords
             * - Updates the strength label text
             */
            private void updateStrength() {
                String password = new String(passwordField.getPassword());
                PasswordStrengthChecker.Strength strength = PasswordStrengthChecker.checkStrength(password);
                switch (strength) {
                    case STRONG:
                        strengthBar.setValue(100);
                        strengthBar.setString("Strong");
                        strengthBar.setForeground(Color.GREEN);
                        break;
                    case MEDIUM:
                        strengthBar.setValue(66);
                        strengthBar.setString("Medium");
                        strengthBar.setForeground(Color.ORANGE);
                        break;
                    case WEAK:
                        strengthBar.setValue(33);
                        strengthBar.setString("Weak");
                        strengthBar.setForeground(Color.RED);
                        break;
                }
            }
        });

        // Role combo box
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        roleCombo = new JComboBox<>(new String[]{"Client", "Seller"});
        formPanel.add(roleCombo, gbc);

        // Remember me checkbox
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        rememberMe = new JCheckBox("Remember Me");
        rememberMe.setSelected(ConfigManager.getStayLoggedIn());
        if (ConfigManager.getStayLoggedIn() && !ConfigManager.getRememberedUsername().isEmpty()) {
            usernameField.setText(ConfigManager.getRememberedUsername());
        }
        formPanel.add(rememberMe, gbc);

        // Add enter key listeners
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    passwordField.requestFocus();
                }
            }
        });

        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (titleLabel.getText().equals("Marketplace Login")) {
                        handleLogin();
                    } else {
                        handleRegistration();
                    }
                }
            }
        });

        centerPanel.add(formPanel);
        panel.add(centerPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        JButton forgotPasswordButton = new JButton("Forgot Password?");
        JButton exitButton = new JButton("Exit");

        loginButton.addActionListener(e -> {
            titleLabel.setText("Marketplace Login");
            if (rememberMe.isSelected()) {
                ConfigManager.setRememberedUsername(usernameField.getText());
                ConfigManager.setStayLoggedIn(true);
            }
            handleLogin();
        });

        registerButton.addActionListener(e -> {
            titleLabel.setText("Marketplace Register");
            handleRegistration();
        });

        forgotPasswordButton.addActionListener(e -> {
            showForgotPasswordDialog();
        });

        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(forgotPasswordButton);
        buttonPanel.add(exitButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }
    private JPanel createMainInterface() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        actionComboBox = new JComboBox<>();
        submitButton = new JButton("Select Action");
        submitButton.setEnabled(false);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> handleLogout());

        JPanel actionPanel = new JPanel(new BorderLayout(5, 5));
        actionPanel.add(logoutButton, BorderLayout.WEST);
        actionPanel.add(actionComboBox, BorderLayout.CENTER);
        actionPanel.add(submitButton, BorderLayout.EAST);

        topPanel.add(actionPanel, BorderLayout.CENTER);

        submitButton.addActionListener(e -> handleActionSelection());
        actionComboBox.addActionListener(e -> submitButton.setEnabled(actionComboBox.getSelectedItem() != null));

        outputArea = new JTextArea("Welcome to the Marketplace!\n");
        outputArea.setFont(new Font("SansSerif", Font.BOLD, 16));
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        inputSendButton = new JButton("Send");

        inputSendButton.addActionListener(e -> {
            String input = inputField.getText().trim();
            if (!input.isEmpty()) {
                out.println(input);
                appendToOutput("You: " + input);
                inputField.setText("");

                if (awaitingInput) {
                    awaitingInput = false;
                }
            }
        });

        inputPanel.add(new JLabel("Enter response:"), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(inputSendButton, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(10, 10));
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        JTextField chatInputField = new JTextField();
        JButton sendButton = new JButton("Send");

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(chatInputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        sendButton.addActionListener(e -> {
            String msg = chatInputField.getText().trim();
            if (!msg.isEmpty()) {
                out.println(msg);
                chatArea.append("You: " + msg + "\n");
                chatInputField.setText("");
            }
        });

        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        return chatPanel;
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String role = (String) roleCombo.getSelectedItem();
        isClientRole = role.equals("Client");

        if (username.isEmpty() || password.isEmpty()) {
            appendToOutput("Please fill in all fields to login.");
            return;
        }

        currentUser = username;
        out.println("1");
        out.println(username);
        out.println(password);
        out.println(role.equals("Seller") ? "1" : "2");

        usernameField.setText("");
        passwordField.setText("");
    }
    private void handleRegistration() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            appendToOutput("Please fill in all fields to register.");
            return;
        }

        out.println("2");
        out.println(username);
        out.println(password);
        out.println(role.equals("Seller") ? "1" : "2");

        usernameField.setText("");
        passwordField.setText("");
    }

    private void handleActionSelection() {
        String selected = (String) actionComboBox.getSelectedItem();
        if (selected == null || selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a valid action from the dropdown.", "No Action Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String choice = selected.split("\\.")[0].trim();
        appendToOutput("Selected: " + selected);
        out.println(choice);
        actionComboBox.setSelectedIndex(-1);
        submitButton.setEnabled(false);
    }

//    private void handleLogout() {
//        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
//        if (confirm == JOptionPane.YES_OPTION) {
//            try {
//                if (out != null) out.println("8");
//                if (in != null) in.close();
//                if (out != null) out.close();
//                if (socket != null) socket.close();
//            } catch (IOException e) {
//                appendToOutput("Error during logout: " + e.getMessage());
//            }
//
//            currentUser = null;
//            actionComboBox.removeAllItems();
//            outputArea.setText("Welcome to the Marketplace!\n");
//            cardLayout.show(mainPanel, "login");
//
//            connectToServer();
//            startResponseHandler();
//        }
//    }
private void handleLogout() {
    int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
    if (confirm == JOptionPane.YES_OPTION) {
        if (out != null) {
            out.println("8");
        }

        currentUser = null;
        actionComboBox.removeAllItems();
        outputArea.setText("Welcome to the Marketplace!\n");
        cardLayout.show(mainPanel, "login");

    }
}

    private void handleServerResponse(String response) {
        if (response.startsWith("ROLE:")) {
            isClientRole = response.trim().equals("ROLE:CLIENT");
            appendToOutput("[DEBUG] Role assigned by server: " + (isClientRole ? "Client" : "Seller"));
            return;
        }

        if (response.contains("=== CLIENT MENU ===") || response.contains("=== SELLER MENU ===")) {
            collectingMenu = true;
            actionComboBox.removeAllItems();
            return;
        }

        switch (response) {
            case "LOGIN_SUCCESS_CLIENT":
            case "LOGIN_SUCCESS_SELLER":
                collectingMenu = true;
                actionComboBox.removeAllItems();
                cardLayout.show(mainPanel, "main");
                appendToOutput("Login successful. Waiting for menu...");
                return;
            case "LOGIN_FAILED":
                JOptionPane.showMessageDialog(this, "Invalid login credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            case "AVAILABLE_SELLERS":
                collectingSellers = true;
                sellerBuffer.clear();
                return;
            case "END_SELLERS":
                collectingSellers = false;
                if (isClientRole) {
                    showSellerSelectionPopup(sellerBuffer);
                }
                return;
            case "SELLER_PRODUCTS":
                collectingProducts = true;
                productBuffer.clear();
                return;
            case "END_PRODUCTS":
                collectingProducts = false;
                showProductSelectionPopup(productBuffer);
                return;
        }

        if (collectingMenu) {
            if (response.equalsIgnoreCase("END_MENU") || response.equalsIgnoreCase("===END_MENU===")) {
                collectingMenu = false;
                appendToOutput("Menu received. Select an option from the dropdown.");
            } else if (response.matches("^\\d+\\.\\s+.*")) {
                if (((DefaultComboBoxModel<String>) actionComboBox.getModel()).getIndexOf(response.trim()) == -1) {
                    actionComboBox.addItem(response.trim());
                }
            } else {
                appendToOutput("[Menu debug] " + response);
            }
            return;
        }

        if (collectingSellers) {
            sellerBuffer.add(response.trim());
            return;
        }

        if (collectingProducts) {
            if (response.startsWith("IMG:")) return;

            if (response.matches("^\\d+\\.\\s+.+")) {
                String trimmed = response.trim();
                if (!productBuffer.contains(trimmed)) {
                    productBuffer.add(trimmed);
                }
            }

            return;
        }

        if (response.startsWith("Confirm purchase of")) {
            awaitingInput = true;
            appendToOutput(response);
            return;
        }

        if (response.startsWith("IMG:")) {
            showProductImage(response.substring(4).trim());
            return;
        }

        if (response.startsWith("=== CHAT HISTORY ===")) {
            chatMessagesBuffer = new ArrayList<>();
            inChatMode = true;
            return;
        }

        if (response.startsWith("===END OF HISTORY===") && inChatMode) {
            inChatMode = false;
            showChatInterface(chatMessagesBuffer, isClientRole, currentChatPartner);
            return;
        }

        if (inChatMode) {
            chatMessagesBuffer.add(response);
            return;
        }

        if (response.startsWith("Enter the number of the seller you want to chat with:")) {
            currentChatPartner = "Seller";
        }

        if (response.startsWith("Enter the number of the chat session to view:")) {
            currentChatPartner = "Client";
        }

        if (response.startsWith("SEND_IMAGE_NOW:")) {
            String targetFileName = response.substring("SEND_IMAGE_NOW:".length()).trim();

            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                sendImageToServer(selectedFile, targetFileName);
                out.println("IMAGE_UPLOADED");
            } else {
                appendToOutput("Image selection cancelled.");
                out.println("IMAGE_UPLOAD_CANCELLED");
            }
            return;
        }







        appendToOutput(response);
    }
    private void sendImageToServer(File file, String targetFileName) {
        try (
                Socket imageSocket = new Socket("localhost", 8882);
                DataOutputStream dos = new DataOutputStream(imageSocket.getOutputStream());
                FileInputStream fis = new FileInputStream(file)
        ) {
            dos.writeUTF("UPLOAD:" + targetFileName);
            dos.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();

            DataInputStream dis = new DataInputStream(imageSocket.getInputStream());
            String response = dis.readUTF();
            appendToOutput("Server upload response: " + response);

        } catch (IOException ex) {
            appendToOutput("Image upload failed: " + ex.getMessage());
        }
    }



//    private void sendImageToServer(File file, String targetFileName) {
//        try (
//                Socket imageSocket = new Socket("localhost", 8889);
//                OutputStream os = imageSocket.getOutputStream();
//                PrintWriter writer = new PrintWriter(os, true);
//                FileInputStream fis = new FileInputStream(file)
//        ) {
//            // Indicate upload command
//            writer.println("UPLOAD:" + targetFileName);
//
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = fis.read(buffer)) != -1) {
//                os.write(buffer, 0, bytesRead);
//            }
//            os.flush(); // Ensure all data is sent
//
//            // Read server response
//            InputStream is = imageSocket.getInputStream();
//            bytesRead = is.read(buffer);
//            if (bytesRead != -1) {
//                String response = new String(buffer, 0, bytesRead).trim();
//                if ("UPLOAD_SUCCESS".equals(response)) {
//                    appendToOutput("Image sent successfully to image server.");
//                } else {
//                    appendToOutput("Image upload failed: " + response);
//                }
//            } else {
//                appendToOutput("No response from server.");
//            }
//        } catch (IOException ex) {
//            appendToOutput("Image upload failed: " + ex.getMessage());
//        }
//    }

    /**
     * - Creates and shows the forgot password dialog.
     * - Adds username input field and Submit and Cancel buttons
     */
    private void showForgotPasswordDialog() {
        JDialog dialog = new JDialog(this, "Forgot Password", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JLabel label = new JLabel("Enter your username:");
        JTextField usernameField = new JTextField(20);
        inputPanel.add(label);
        inputPanel.add(usernameField);

        JPanel forgotPasswordButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton submitButton = new JButton("Submit");
        JButton cancelButton = new JButton("Cancel");

        submitButton.addActionListener(ev -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                // In a real application, this would send a password reset email
                JOptionPane.showMessageDialog(dialog,
                        "If an account exists with that username, a password reset link will be sent to the associated email.",
                        "Password Reset",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter a username.",
                        "Input Required",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelButton.addActionListener(ev -> dialog.dispose());

        forgotPasswordButtonPanel.add(submitButton);
        forgotPasswordButtonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(forgotPasswordButtonPanel, BorderLayout.SOUTH);

        // Make sure the dialog is visible and on top
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }


    private void showChatInterface(List<String> messages, boolean isClient, String partnerName) {
        JDialog chatDialog = new JDialog(this, "Chat with " + partnerName, false);
        chatDialog.setSize(500, 400);
        chatDialog.setLayout(new BorderLayout());

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        messages.forEach(msg -> chatArea.append(msg + "\n"));

        JScrollPane scrollPane = new JScrollPane(chatArea);

        JTextField messageField = new JTextField();
        JButton sendBtn = new JButton("Send");

        sendBtn.addActionListener(e -> {
            String msg = messageField.getText().trim();
            if (!msg.isEmpty()) {
                out.println(msg);
                chatArea.append((isClient ? "You (Client): " : "You (Seller): ") + msg + "\n");
                messageField.setText("");
            }
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        chatDialog.add(scrollPane, BorderLayout.CENTER);
        chatDialog.add(bottomPanel, BorderLayout.SOUTH);
        chatDialog.setLocationRelativeTo(this);
        chatDialog.setVisible(true);
    }

    private void showSellerSelectionPopup(List<String> sellers) {
        if (sellers.isEmpty()) {
            appendToOutput("No sellers available.");
            return;
        }

        String selectedSeller = (String) JOptionPane.showInputDialog(
                this,
                "Select a seller:",
                "Available Sellers",
                JOptionPane.PLAIN_MESSAGE,
                null,
                sellers.toArray(),
                sellers.get(0)
        );

        if (selectedSeller != null && !selectedSeller.trim().isEmpty()) {
            int selectedIndex = sellers.indexOf(selectedSeller);
            out.println((selectedIndex + 1));
            appendToOutput("Selected seller: " + selectedSeller);
        } else {
            appendToOutput("No seller selected.");
            out.println("0");
        }
    }

    private void showProductSelectionPopup(List<String> products) {
        if (products.isEmpty()) {
            appendToOutput("No products available.");
            return;
        }

        List<String> validProducts = new ArrayList<>();

        for (String p : products) {
            if (p.matches("^\\d+\\.\\s+.*")) {
                validProducts.add(p.trim());
            }
        }

        if (validProducts.isEmpty()) {
            appendToOutput("No valid products available to display.");
            return;
        }

        String selectedProduct = (String) JOptionPane.showInputDialog(
                this,
                "Select a product to view details:",
                "Available Products",
                JOptionPane.PLAIN_MESSAGE,
                null,
                validProducts.toArray(),
                validProducts.get(0)
        );

        if (selectedProduct != null && !selectedProduct.trim().isEmpty()) {

            String cleanProduct = selectedProduct.replaceFirst("^\\d+\\.\\s*", "").trim();

            String[] parts = cleanProduct.split(",");
            if (parts.length >= 2) {
                int confirmView = JOptionPane.showConfirmDialog(
                        this,
                        "Do you want to view this product's image and purchase options?",
                        "View Product",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirmView == JOptionPane.YES_OPTION) {
                    if (parts.length >= 3 && !parts[2].trim().equalsIgnoreCase("none")) {
                        showProductImage(parts[2].trim());
                    }

                    int confirmBuy = JOptionPane.showConfirmDialog(
                            this,
                            "Do you want to purchase this product?\n" + parts[0].trim() + " for $" + parts[1].trim(),
                            "Confirm Purchase",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (confirmBuy == JOptionPane.YES_OPTION) {
                        int selectedIndex = validProducts.indexOf(selectedProduct) + 1;
                        out.println(selectedIndex);
                        out.println("yes");
                        appendToOutput("You confirmed purchase: " + parts[0]);
                    } else {
                        out.println("0");
                        appendToOutput("Purchase cancelled.");
                    }


                } else {
                    appendToOutput("Product view cancelled.");
                    out.println("0");
                }
            } else {
                appendToOutput("Invalid product format: " + cleanProduct);
                out.println("0");
            }
        } else {
            appendToOutput("No product selected.");
            out.println("0");
        }
    }


    private void showProductImage(String imageFileName) {
        try {
            File tempDir = new File("temp_images");
            if (!tempDir.exists()) tempDir.mkdirs();

            File imgFile = new File(tempDir, imageFileName);

            try (
                    Socket imageSocket = new Socket("localhost", 8889);
                    DataOutputStream dos = new DataOutputStream(imageSocket.getOutputStream());
                    DataInputStream dis = new DataInputStream(imageSocket.getInputStream());
                    FileOutputStream fos = new FileOutputStream(imgFile)
            ) {
                dos.writeUTF(imageFileName);

                long fileSize = dis.readLong();
                if (fileSize <= 0) {
                    appendToOutput("Image not found on server.");
                    JOptionPane.showMessageDialog(this, "Image not found on server.", "Image Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                while (remaining > 0) {
                    int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) break;
                    fos.write(buffer, 0, read);
                    remaining -= read;
                }
            }

            if (!imgFile.exists() || imgFile.length() == 0) {
                appendToOutput("Downloaded image file is empty.");
                JOptionPane.showMessageDialog(this, "Image could not be loaded.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
            if (icon.getIconWidth() == -1) {
                appendToOutput("Image decoding failed.");
                JOptionPane.showMessageDialog(this, "Corrupted image file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Image scaled = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            JLabel imgLabel = new JLabel(new ImageIcon(scaled));
            JOptionPane.showMessageDialog(this, imgLabel, "Product Image", JOptionPane.PLAIN_MESSAGE);

        } catch (IOException ex) {
            appendToOutput("Error fetching image: " + ex.getMessage());
        }
    }



    private void connectToServer() {
        try {
            socket = new Socket("localhost", 8881);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            appendToOutput("Connected to server");
            cardLayout.show(mainPanel, "login");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void startResponseHandler() {
        new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    String finalResponse = response;
                    SwingUtilities.invokeLater(() -> handleServerResponse(finalResponse));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    appendToOutput(" Disconnected from server.");
                    JOptionPane.showMessageDialog(this, "Disconnected from server. Please restart the app.");
                    System.exit(0);
                });
            }
        }).start();
    }


    private void appendToOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MarketplaceClientGUI().setVisible(true));
    }
}

