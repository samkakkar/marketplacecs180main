# MarketPlaceCS180
### CS180 Final Project - Phase 3 (GUI IMPLEMENTATION)
Samridhi Kakkar, Huron, Sherry Zhao
## 1. How to compile and run program  
### MarketplaceProject
1) **MarketplaceServer.java**: Main server with client handling logic
2) **MarketplaceServerInterface.java**: Interface declaring server methods
3) **MarketplaceClientGUI.java**: client that connects to the server
4) **MarketplaceClientInterface.java**: Interface for the client
5) **clients.txt**: Stores client credentials
6) **sellers.txt**: Stores seller credentials
7) **balances.txt**: Stores user balances
8) **transactions.txt**: Stores transaction history
9) **products**: Folder with each seller's product list
10) **chats**: Folder with chat files between sellers and clients
11) **images**: Folder with images of products

_**Run the MarketplaceServer.java and then the MarketplaceClient.java**_
    
## 2. Submission record  
- Submitted project on Vocareum by Samridhi Kakkar
- Report submitted by Samridhi Kakkar 
- Presentation submitted by Sherry Zhao
 
## 3. Description of each class
**MarketplaceClientInterface**:
    - An interface defining the essential contract that all client implementations (e.g., GUI or CLI) must follow.

**MarketplaceClientGUI**:
    - A JFrame-based Swing GUI that acts as the client for the marketplace. Implements MarketplaceClientInterface.

**Features:**
    - Login and Registration forms with password strength checking and “remember me” support
    - Distinct menus for Clients and Sellers, dynamically populated from the server
    - Product browsing interface with image previews (via image socket)
    - Product purchasing interface with confirmation dialogs
    - Seller interface to upload new products with optional image upload
    - Real-time chat with sellers or clients (including persistent chat history)
    - Wallet management: view balance, top-up, view transactions
    - Graceful logout and disconnection handling

**Sockets Used:**
    - Port 8881 for main commands and responses
    - Port 8882 for image uploads and downloads

**MarketplaceServerInterface**:
    - An interface defining the structure and responsibilities of a server in the marketplace.

**MarketplaceServer**:
    - The main class for running the server-side logic.
        - Responsibilities:
            1) Listens on port 8881 for client connections and handles each client in a separate thread via ClientHandler
            2) Hosts a secondary image server on port 8882 for handling image uploads/downloads
            3) Maintains synchronized I/O operations for thread safety with file-based storage

**Server Directories and Files:**
- products/: Each seller’s product listings
- chats/: Chat logs between sellers and clients
- images/: Uploaded product images
- clients.txt, sellers.txt: User credentials
- balances.txt: Tracks wallet balance
- transactions.txt: Stores purchase and top-up history

**Core Functionalities:**
- Secure login/signup
- Dynamic seller/client menu handling
- Product listing, searching, and purchasing with image handling
- Persistent chat messaging
- Wallet top-up and transaction tracking
- Account deletion for both roles

**RunLocalTestCase**
This class contains test cases designed to simulate Phase 1 functionality.

**Limitations:**
- Many core methods are private or deeply embedded within menus, restricting direct testing
- Plans to refactor for more comprehensive unit testing in Phase 3
