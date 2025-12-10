# Document Management System

## Overview
The Document Management System (DMS) is a web application designed to manage documents efficiently. It allows users to upload, view, and manage documents through a user-friendly interface.

## Project Structure
```
DocumentGUI
├── src
│   └── api
│       └── client.js
├── FrontPage.html
├── DocumentDetail.html
└── README.md
```

## Setup Instructions
1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd DocumentGUI
   ```

2. **Install Dependencies**
   Ensure you have Node.js installed. Then, run:
   ```bash
   npm install
   ```

3. **Run the Application**
   Open `FrontPage.html` in your web browser to access the Document Management System.

## Usage
- **FrontPage.html**: This is the main interface where users can upload and view documents.
- **DocumentDetail.html**: This page displays detailed information about a selected document.

## API Interaction
The application interacts with the backend API through functions defined in `src/api/client.js`. Key functions include:
- `getDocument(documentId)`: Fetches details of a specific document.
- `postDocument(documentData)`: Uploads a new document to the server.

## Contributing
Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for more details.