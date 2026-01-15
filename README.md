# Document-Management-System
Semester project for SWEN 3 FH Technikum Wien

## Testing HOWTO
**Integration Test:**
To verify the document upload validation and process integrity:
1. Ensure Docker is NOT running (the test uses in-memory H2 database).
2. Run the test via Maven wrapper:
   ```bash
   cd DocumentDAL
   ./mvnw -Dtest=DocumentUploadIntegrationTest test