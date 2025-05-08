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
- **MarketplaceClientInterface**:
  Interface for MarketplaceClient
   
- **MarketplaceClientGUI**:
  - GUI client using Swing interface:
  - Displays login, registration, and role-based menus
  - Sends user actions to the server via socket
  - Capabilities:
        - Browse products with image preview
        - Purchase items
        - Upload product images (sellers)
        - Real-time chat with sellers or clients
        - View and update wallet balance
        - Visualize chat history in dialogs
        - Handles disconnections and logout cleanly
    
- **MarketplaceServerInterface**:
  Interface for MarketplaceServer
   
- **MarketplaceServer**:
    - Entry point for the server:
    - Listens on port 8888 for client commands and 8889 for image uploads
    - Contains a static ClientHandler class
    - Handles one client per thread
    - Implements synchronized file I/O to ensure thread safety
    - Handles:
          - Login/signup
          - Seller and client menus
          - Product listing, purchase, and wallet operations
          - Chat message exchange (stored in chats/)
          - Image upload handling
      
- **RunLocalTestCase**:
  - It contains the testcases - similar to what we put forward during phase 1
  - We couldn't test all the functionality since all the methods are private and are called in a menu. We aim to try resolving it to include more tests by the phase 3. 
